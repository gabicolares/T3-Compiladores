import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final Lexer lexer;
    private final TabSimb ts = new TabSimb();
    private final List<String> erros = new ArrayList<>();

    private TS_entry currClass;
    private TS_entry currMethod;
    private boolean hasMain = false;

    public Parser(Reader reader) throws IOException {
        this.lexer = new Lexer(reader);
        inicializaTiposBase();
    }

    private void inicializaTiposBase() {
        ts.insert(new TS_entry("int", TypeInfo.INT, ClasseID.TipoBase, "global"));
        ts.insert(new TS_entry("boolean", TypeInfo.BOOLEAN, ClasseID.TipoBase, "global"));
    }

    public boolean parse() {
        try {
            program();
            expect(TokenType.EOF, "fim do arquivo");
        } catch (RuntimeException ex) {
            erroAtual(ex.getMessage());
            sincronizaAte(TokenType.EOF);
        }

        ts.listar();

        if (erros.isEmpty()) {
            System.out.println("\nAnálise concluída sem erros semânticos.");
            return true;
        }

        System.err.println("\nErros encontrados:");
        for (String erro : erros) {
            System.err.println("  " + erro);
        }
        return false;
    }

    private void program() {
        while (!check(TokenType.EOF)) {
            classDeclaration();
        }

        if (!hasMain) {
            erroAtual("programa não possui classe com método main");
        }
    }

    private void classDeclaration() {
        expect(TokenType.CLASS, "'class'");
        Token className = expect(TokenType.IDENTIFIER, "nome da classe");

        TS_entry oldClass = currClass;
        TS_entry classEntry = ts.pesquisaClasse(className.lexeme);
        if (classEntry != null) {
            erro(className, "classe '" + className.lexeme + "' já declarada");
        } else {
            classEntry = new TS_entry(className.lexeme, TypeInfo.classType(className.lexeme), ClasseID.NomeClasse, "global");
            ts.insert(classEntry);
        }
        currClass = classEntry;

        if (match(TokenType.EXTENDS)) {
            Token superName = expect(TokenType.IDENTIFIER, "nome da superclasse");
            TS_entry superEntry = ts.pesquisaClasse(superName.lexeme);
            if (superEntry == null) {
                erro(superName, "superclasse '" + superName.lexeme + "' não declarada antes do uso");
            } else if (classEntry != null) {
                classEntry.setSuperClasse(superEntry);
            }
        }

        expect(TokenType.LBRACE, "'{'");

        boolean sawMethod = false;
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            if (check(TokenType.PUBLIC)) {
                sawMethod = true;
                if (isMainMethodStart()) {
                    mainMethod();
                } else {
                    methodDeclaration();
                }
            } else if (!sawMethod && isVarDeclStart()) {
                varDeclaration(ClasseID.Atributo, className.lexeme);
            } else if (isVarDeclStart()) {
                erroAtual("declaração de atributo após método não é permitida nesta gramática");
                varDeclaration(ClasseID.Atributo, className.lexeme);
            } else {
                erroAtual("declaração inválida dentro da classe '" + className.lexeme + "'");
                lexer.next();
            }
        }

        expect(TokenType.RBRACE, "'}'");
        currClass = oldClass;
    }

    private boolean isMainMethodStart() {
        return check(TokenType.PUBLIC)
            && check(1, TokenType.STATIC)
            && check(2, TokenType.VOID)
            && check(3, TokenType.MAIN);
    }

    private void mainMethod() {
        Token publicToken = expect(TokenType.PUBLIC, "'public'");
        expect(TokenType.STATIC, "'static'");
        expect(TokenType.VOID, "'void'");
        Token mainToken = expect(TokenType.MAIN, "'main'");
        expect(TokenType.LPAREN, "'('");
        expect(TokenType.STRING, "'String'");
        expect(TokenType.LBRACKET, "'['");
        expect(TokenType.RBRACKET, "']'");
        Token argName = expect(TokenType.IDENTIFIER, "nome do parâmetro do main");
        expect(TokenType.RPAREN, "')'");

        if (hasMain) {
            erro(publicToken, "mais de um método main declarado");
        }
        hasMain = true;

        TS_entry previousMethod = currMethod;
        String classScope = currClass != null ? currClass.getId() : "<classe-desconhecida>";
        if (ts.pesquisa("main", classScope, ClasseID.NomeMetodo) != null) {
            erro(mainToken, "método 'main' já declarado na classe '" + classScope + "'");
        }
        currMethod = new TS_entry("main", TypeInfo.VOID, ClasseID.NomeMetodo, classScope);
        currMethod.addParametro(new TS_entry(argName.lexeme, TypeInfo.classType("String[]"), ClasseID.NomeParam, methodScope()));
        ts.insert(currMethod);

        expect(TokenType.LBRACE, "'{'");
        while (isVarDeclStart()) {
            varDeclaration(ClasseID.VarLocal, methodScope());
        }
        statementListUntil(TokenType.RBRACE);
        expect(TokenType.RBRACE, "'}'");

        currMethod = previousMethod;
    }

    private void methodDeclaration() {
        expect(TokenType.PUBLIC, "'public'");
        TypeInfo returnType = type();
        Token methodName = expect(TokenType.IDENTIFIER, "nome do método");
        String classScope = currClass != null ? currClass.getId() : "<classe-desconhecida>";

        if (ts.pesquisa(methodName.lexeme, classScope, ClasseID.NomeMetodo) != null) {
            erro(methodName, "método '" + methodName.lexeme + "' já declarado na classe '" + classScope + "'");
        }

        TS_entry previousMethod = currMethod;
        currMethod = new TS_entry(methodName.lexeme, returnType, ClasseID.NomeMetodo, classScope);
        ts.insert(currMethod);

        expect(TokenType.LPAREN, "'('");
        if (!check(TokenType.RPAREN)) {
            paramList();
        }
        expect(TokenType.RPAREN, "')'");

        expect(TokenType.LBRACE, "'{'");
        while (isVarDeclStart()) {
            varDeclaration(ClasseID.VarLocal, methodScope());
        }
        statementListUntil(TokenType.RETURN);
        expect(TokenType.RETURN, "'return'");
        TypeInfo returnedType = expression();
        if (!isAssignable(returnType, returnedType)) {
            erroAtual("retorno incompatível no método '" + methodName.lexeme + "': esperado " + returnType + ", encontrado " + returnedType);
        }
        expect(TokenType.SEMICOLON, "';'");
        expect(TokenType.RBRACE, "'}'");

        currMethod = previousMethod;
    }

    private void paramList() {
        do {
            TypeInfo paramType = type();
            Token paramName = expect(TokenType.IDENTIFIER, "nome do parâmetro");
            String scope = methodScope();

            if (ts.pesquisa(paramName.lexeme, scope) != null) {
                erro(paramName, "parâmetro '" + paramName.lexeme + "' já declarado no método '" + currMethod.getId() + "'");
            } else {
                TS_entry param = new TS_entry(paramName.lexeme, paramType, ClasseID.NomeParam, scope);
                ts.insert(param);
                currMethod.addParametro(param);
            }
        } while (match(TokenType.COMMA));
    }

    private void varDeclaration(ClasseID categoria, String scope) {
        TypeInfo varType = type();
        Token varName = expect(TokenType.IDENTIFIER, "nome da variável");

        if (ts.pesquisa(varName.lexeme, scope) != null) {
            erro(varName, "identificador '" + varName.lexeme + "' já declarado no escopo '" + scope + "'");
        } else {
            ts.insert(new TS_entry(varName.lexeme, varType, categoria, scope));
        }
        expect(TokenType.SEMICOLON, "';'");
    }

    private TypeInfo type() {
        if (match(TokenType.INT)) {
            if (match(TokenType.LBRACKET)) {
                erroAtual("arrays não são tratados nesta versão da linguagem");
                expect(TokenType.RBRACKET, "']'");
                return TypeInfo.ERROR;
            }
            return TypeInfo.INT;
        }

        if (match(TokenType.BOOLEAN)) {
            return TypeInfo.BOOLEAN;
        }

        Token id = expect(TokenType.IDENTIFIER, "tipo");
        if (ts.pesquisaClasse(id.lexeme) == null) {
            erro(id, "classe '" + id.lexeme + "' usada como tipo antes de ser declarada");
        }
        return TypeInfo.classType(id.lexeme);
    }

    private void statementListUntil(TokenType stopToken) {
        while (!check(stopToken) && !check(TokenType.EOF)) {
            statement();
        }
    }

    private void statement() {
        if (match(TokenType.LBRACE)) {
            statementListUntil(TokenType.RBRACE);
            expect(TokenType.RBRACE, "'}'");
            return;
        }

        if (match(TokenType.IF)) {
            expect(TokenType.LPAREN, "'('");
            TypeInfo cond = expression();
            if (!cond.isBoolean() && !cond.isError()) {
                erroAtual("condição do if deve ser boolean, mas foi encontrada expressão do tipo " + cond);
            }
            expect(TokenType.RPAREN, "')'");
            statement();
            expect(TokenType.ELSE, "'else'");
            statement();
            return;
        }

        if (match(TokenType.WHILE)) {
            expect(TokenType.LPAREN, "'('");
            TypeInfo cond = expression();
            if (!cond.isBoolean() && !cond.isError()) {
                erroAtual("condição do while deve ser boolean, mas foi encontrada expressão do tipo " + cond);
            }
            expect(TokenType.RPAREN, "')'");
            statement();
            return;
        }

        if (match(TokenType.SOUT)) {
            expect(TokenType.LPAREN, "'('");
            TypeInfo printed = expression();
            if (!printed.isInt() && !printed.isBoolean() && !printed.isError()) {
                erroAtual("System.out.println aceita int ou boolean nesta versão, mas recebeu " + printed);
            }
            expect(TokenType.RPAREN, "')'");
            expect(TokenType.SEMICOLON, "';'");
            return;
        }

        if (check(TokenType.IDENTIFIER)) {
            Token id = expect(TokenType.IDENTIFIER, "identificador");

            if (match(TokenType.LBRACKET)) {
                erro(id, "arrays não são tratados nesta versão da linguagem");
                expression();
                expect(TokenType.RBRACKET, "']'");
                expect(TokenType.ASSIGN, "'='");
                expression();
                expect(TokenType.SEMICOLON, "';'");
                return;
            }

            expect(TokenType.ASSIGN, "'='");
            TS_entry variable = lookupVariable(id.lexeme);
            TypeInfo leftType = variable != null ? variable.getTipo() : TypeInfo.ERROR;
            if (variable == null) {
                erro(id, "variável '" + id.lexeme + "' não declarada no escopo atual");
            }
            TypeInfo rightType = expression();
            if (!isAssignable(leftType, rightType)) {
                erro(id, "atribuição incompatível para '" + id.lexeme + "': esperado " + leftType + ", encontrado " + rightType);
            }
            expect(TokenType.SEMICOLON, "';'");
            return;
        }

        erroAtual("comando inválido");
        lexer.next();
    }

    private TypeInfo expression() {
        return andExpression();
    }

    private TypeInfo andExpression() {
        TypeInfo left = equalityExpression();
        while (match(TokenType.AND)) {
            TypeInfo right = equalityExpression();
            if ((!left.isBoolean() || !right.isBoolean()) && !left.isError() && !right.isError()) {
                erroAtual("operador && exige operandos boolean, mas recebeu " + left + " e " + right);
            }
            left = TypeInfo.BOOLEAN;
        }
        return left;
    }

    private TypeInfo equalityExpression() {
        TypeInfo left = relationalExpression();
        while (check(TokenType.EQ) || check(TokenType.NEQ)) {
            Token op = lexer.next();
            TypeInfo right = relationalExpression();
            if (!typesComparable(left, right)) {
                erro(op, "operador '" + op.lexeme + "' recebeu tipos incompatíveis: " + left + " e " + right);
            }
            left = TypeInfo.BOOLEAN;
        }
        return left;
    }

    private TypeInfo relationalExpression() {
        TypeInfo left = additiveExpression();
        while (match(TokenType.LT)) {
            TypeInfo right = additiveExpression();
            if ((!left.isInt() || !right.isInt()) && !left.isError() && !right.isError()) {
                erroAtual("operador < exige operandos int, mas recebeu " + left + " e " + right);
            }
            left = TypeInfo.BOOLEAN;
        }
        return left;
    }

    private TypeInfo additiveExpression() {
        TypeInfo left = multiplicativeExpression();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = lexer.next();
            TypeInfo right = multiplicativeExpression();
            if ((!left.isInt() || !right.isInt()) && !left.isError() && !right.isError()) {
                erro(op, "operador '" + op.lexeme + "' exige operandos int, mas recebeu " + left + " e " + right);
            }
            left = TypeInfo.INT;
        }
        return left;
    }

    private TypeInfo multiplicativeExpression() {
        TypeInfo left = unaryExpression();
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            Token op = lexer.next();
            TypeInfo right = unaryExpression();
            if ((!left.isInt() || !right.isInt()) && !left.isError() && !right.isError()) {
                erro(op, "operador '" + op.lexeme + "' exige operandos int, mas recebeu " + left + " e " + right);
            }
            left = TypeInfo.INT;
        }
        return left;
    }

    private TypeInfo unaryExpression() {
        if (match(TokenType.NOT)) {
            TypeInfo value = unaryExpression();
            if (!value.isBoolean() && !value.isError()) {
                erroAtual("operador ! exige expressão boolean, mas recebeu " + value);
            }
            return TypeInfo.BOOLEAN;
        }
        return postfixExpression();
    }

    private TypeInfo postfixExpression() {
        TypeInfo current = primaryExpression();

        while (match(TokenType.DOT)) {
            if (check(TokenType.IDENTIFIER) && "length".equals(lexer.peek().lexeme)) {
                Token lengthToken = lexer.next();
                erro(lengthToken, "arrays e '.length' não são tratados nesta versão da linguagem");
                current = TypeInfo.ERROR;
                continue;
            }

            Token methodName = expect(TokenType.IDENTIFIER, "nome do método");
            expect(TokenType.LPAREN, "'('");
            List<TypeInfo> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    args.add(expression());
                } while (match(TokenType.COMMA));
            }
            expect(TokenType.RPAREN, "')'");

            if (!current.isClass()) {
                if (!current.isError()) {
                    erro(methodName, "chamada de método em expressão não-objeto do tipo " + current);
                }
                current = TypeInfo.ERROR;
                continue;
            }

            TS_entry method = lookupMethod(current.getClassName(), methodName.lexeme);
            if (method == null) {
                erro(methodName, "método '" + methodName.lexeme + "' não encontrado na classe '" + current.getClassName() + "' nem em suas superclasses");
                current = TypeInfo.ERROR;
                continue;
            }

            validateArguments(methodName, method, args);
            current = method.getTipo();
        }

        return current;
    }

    private TypeInfo primaryExpression() {
        if (match(TokenType.NUMBER)) {
            return TypeInfo.INT;
        }

        if (match(TokenType.TRUE) || match(TokenType.FALSE)) {
            return TypeInfo.BOOLEAN;
        }

        if (check(TokenType.IDENTIFIER)) {
            Token id = expect(TokenType.IDENTIFIER, "identificador");
            TS_entry entry = lookupVariable(id.lexeme);
            if (entry == null) {
                erro(id, "variável '" + id.lexeme + "' não declarada no escopo atual");
                return TypeInfo.ERROR;
            }
            return entry.getTipo();
        }

        if (match(TokenType.THIS)) {
            if (currClass == null || currMethod == null || "main".equals(currMethod.getId())) {
                erroAtual("uso de this fora de método de instância");
                return TypeInfo.ERROR;
            }
            return TypeInfo.classType(currClass.getId());
        }

        if (match(TokenType.NEW)) {
            if (match(TokenType.INT)) {
                if (match(TokenType.LBRACKET)) {
                    erroAtual("arrays não são tratados nesta versão da linguagem");
                    expression();
                    expect(TokenType.RBRACKET, "']'");
                    return TypeInfo.ERROR;
                }
                erroAtual("uso inválido de new int sem array");
                return TypeInfo.ERROR;
            }

            Token className = expect(TokenType.IDENTIFIER, "nome da classe");
            if (ts.pesquisaClasse(className.lexeme) == null) {
                erro(className, "classe '" + className.lexeme + "' usada em new antes de ser declarada");
            }
            expect(TokenType.LPAREN, "'('");
            expect(TokenType.RPAREN, "')'");
            return TypeInfo.classType(className.lexeme);
        }

        if (match(TokenType.LPAREN)) {
            TypeInfo inside = expression();
            expect(TokenType.RPAREN, "')'");
            return inside;
        }

        erroAtual("expressão inválida");
        lexer.next();
        return TypeInfo.ERROR;
    }

    private void validateArguments(Token methodName, TS_entry method, List<TypeInfo> args) {
        List<TS_entry> params = method.getParametros();
        if (args.size() != params.size()) {
            erro(methodName, "método '" + method.getId() + "' espera " + params.size() + " argumento(s), mas recebeu " + args.size());
            return;
        }

        for (int i = 0; i < args.size(); i++) {
            TypeInfo expected = params.get(i).getTipo();
            TypeInfo actual = args.get(i);
            if (!isAssignable(expected, actual)) {
                erro(methodName, "argumento " + (i + 1) + " de '" + method.getId() + "' incompatível: esperado " + expected + ", encontrado " + actual);
            }
        }
    }

    private boolean isAssignable(TypeInfo expected, TypeInfo actual) {
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.isError() || actual.isError()) {
            return true;
        }
        if (expected.equals(actual)) {
            return true;
        }
        if (expected.isClass() && actual.isClass()) {
            return isSubclass(actual.getClassName(), expected.getClassName());
        }
        return false;
    }

    private boolean typesComparable(TypeInfo left, TypeInfo right) {
        if (left.isError() || right.isError()) {
            return true;
        }
        if (left.equals(right)) {
            return true;
        }
        if (left.isClass() && right.isClass()) {
            return isSubclass(left.getClassName(), right.getClassName()) || isSubclass(right.getClassName(), left.getClassName());
        }
        return false;
    }

    private boolean isSubclass(String childName, String parentName) {
        if (childName.equals(parentName)) {
            return true;
        }
        TS_entry current = ts.pesquisaClasse(childName);
        while (current != null && current.getSuperClasse() != null) {
            current = current.getSuperClasse();
            if (current.getId().equals(parentName)) {
                return true;
            }
        }
        return false;
    }

    private TS_entry lookupVariable(String name) {
        if (currMethod != null) {
            String scope = methodScope();
            TS_entry localOrParam = ts.pesquisa(name, scope);
            if (localOrParam != null) {
                return localOrParam;
            }
        }

        if (currClass != null) {
            TS_entry classCursor = currClass;
            while (classCursor != null) {
                TS_entry field = ts.pesquisa(name, classCursor.getId(), ClasseID.Atributo);
                if (field != null) {
                    return field;
                }
                classCursor = classCursor.getSuperClasse();
            }
        }

        return null;
    }

    private TS_entry lookupMethod(String className, String methodName) {
        TS_entry classCursor = ts.pesquisaClasse(className);
        while (classCursor != null) {
            TS_entry method = ts.pesquisa(methodName, classCursor.getId(), ClasseID.NomeMetodo);
            if (method != null) {
                return method;
            }
            classCursor = classCursor.getSuperClasse();
        }
        return null;
    }

    private boolean isVarDeclStart() {
        if (check(TokenType.INT) || check(TokenType.BOOLEAN)) {
            return true;
        }
        return check(TokenType.IDENTIFIER) && check(1, TokenType.IDENTIFIER);
    }

    private String methodScope() {
        String className = currClass != null ? currClass.getId() : "<classe-desconhecida>";
        String methodName = currMethod != null ? currMethod.getId() : "<metodo-desconhecido>";
        return className + "." + methodName;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            lexer.next();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        return lexer.peek().type == type;
    }

    private boolean check(int offset, TokenType type) {
        return lexer.peek(offset).type == type;
    }

    private Token expect(TokenType type, String expected) {
        Token token = lexer.peek();
        if (token.type == type) {
            return lexer.next();
        }
        throw new RuntimeException("esperado " + expected + ", encontrado '" + token.lexeme + "' na linha " + token.line + ", coluna " + token.column);
    }

    private void erroAtual(String message) {
        erro(lexer.peek(), message);
    }

    private void erro(Token token, String message) {
        erros.add("linha " + token.line + ", coluna " + token.column + ": " + message);
    }

    private void sincronizaAte(TokenType stop) {
        while (!check(stop) && !check(TokenType.EOF)) {
            lexer.next();
        }
    }

    public static void main(String[] args) throws IOException {
        Reader reader;
        if (args.length > 0) {
            reader = new FileReader(args[0]);
        } else {
            reader = new InputStreamReader(System.in);
        }

        Parser parser = new Parser(reader);
        boolean ok = parser.parse();
        if (!ok) {
            System.exit(1);
        }
    }
}
