import java.util.ArrayList;
import java.util.List;

public class Semantico {
    private final TabSimb ts = new TabSimb();
    private TS_entry currClass;
    private TS_entry currMethod;
    private int errors = 0;
    private boolean hasMain = false;

    public Semantico() {
        ts.insert(new TS_entry("int", TypeInfo.INT, ClasseID.TipoBase, "global"));
        ts.insert(new TS_entry("boolean", TypeInfo.BOOLEAN, ClasseID.TipoBase, "global"));
    }

    public int getErrors() {
        return errors;
    }

    public boolean hasMain() {
        return hasMain;
    }

    public void erro(int line, String msg) {
        errors++;
        System.err.printf("Erro (linha: %2d) %s%n", line, msg);
    }

    public void listarTS() {
        ts.listar();
    }

    public void startClass(String className, int line) {
        TS_entry existing = ts.pesquisaClasse(className);
        if (existing != null) {
            erro(line, "(sem) classe >" + className + "< já declarada");
            currClass = existing;
            return;
        }

        currClass = new TS_entry(className, TypeInfo.classType(className), ClasseID.NomeClasse, "global");
        ts.insert(currClass);
    }

    public void finishClass() {
        currClass = null;
        currMethod = null;
    }

    public void setSuperClass(String superName, int line) {
        if (currClass == null) {
            return;
        }

        if (currClass.getId().equals(superName)) {
            erro(line, "(sem) classe >" + currClass.getId() + "< não pode herdar dela mesma");
            return;
        }

        TS_entry superClass = ts.pesquisaClasse(superName);
        if (superClass == null) {
            erro(line, "(sem) superclasse >" + superName + "< não declarada antes do uso");
            return;
        }

        currClass.setSuperClasse(superClass);
    }

    public void startMethod(String methodName, TypeInfo returnType, int line) {
        if (currClass == null) {
            erro(line, "(sem) método >" + methodName + "< declarado fora de classe");
            return;
        }

        if (ts.pesquisa(methodName, currClass.getId(), ClasseID.NomeMetodo) != null) {
            erro(line, "(sem) método >" + methodName + "< já declarado na classe >" + currClass.getId() + "<");
        }

        currMethod = new TS_entry(methodName, returnType, ClasseID.NomeMetodo, currClass.getId());
        ts.insert(currMethod);
    }

    public void startMain(String paramName, int line) {
        if (currClass == null) {
            erro(line, "(sem) main declarado fora de classe");
            return;
        }

        if (hasMain) {
            erro(line, "(sem) mais de um método main declarado");
        }
        hasMain = true;

        if (ts.pesquisa("main", currClass.getId(), ClasseID.NomeMetodo) != null) {
            erro(line, "(sem) método main já declarado na classe >" + currClass.getId() + "<");
        }

        currMethod = new TS_entry("main", TypeInfo.VOID, ClasseID.NomeMetodo, currClass.getId());
        ts.insert(currMethod);

        TS_entry param = new TS_entry(paramName, TypeInfo.classType("String[]"), ClasseID.NomeParam, methodScope());
        ts.insert(param);
        currMethod.addParametro(param);
    }

    public void finishMain() {
        currMethod = null;
    }

    public void finishMethod(TypeInfo returnedType, int line) {
        if (currMethod != null && !isAssignable(currMethod.getTipo(), returnedType)) {
            erro(line, "(sem) retorno incompatível no método >" + currMethod.getId() + "<: esperado " + currMethod.getTipo() + ", encontrado " + returnedType);
        }
        currMethod = null;
    }

    public TypeInfo resolveClassType(String className, int line) {
        if (ts.pesquisaClasse(className) == null) {
            erro(line, "(sem) classe >" + className + "< usada como tipo antes de ser declarada");
        }
        return TypeInfo.classType(className);
    }

    public void addParam(String name, TypeInfo type, int line) {
        if (currMethod == null) {
            erro(line, "(sem) parâmetro >" + name + "< declarado fora de método");
            return;
        }

        String scope = methodScope();
        if (ts.pesquisa(name, scope) != null) {
            erro(line, "(sem) parâmetro ou variável >" + name + "< já declarado(a) no escopo >" + scope + "<");
            return;
        }

        TS_entry param = new TS_entry(name, type, ClasseID.NomeParam, scope);
        ts.insert(param);
        currMethod.addParametro(param);
    }

    public void addVariable(String name, TypeInfo type, int line) {
        if (currClass == null) {
            erro(line, "(sem) variável >" + name + "< declarada fora de classe");
            return;
        }

        String scope;
        ClasseID categoria;
        if (currMethod == null) {
            scope = currClass.getId();
            categoria = ClasseID.Atributo;
            if (lookupFieldInSupers(name) != null) {
                erro(line, "(sem) atributo >" + name + "< já declarado em superclasse");
            }
        } else {
            scope = methodScope();
            categoria = ClasseID.VarLocal;
        }

        if (ts.pesquisa(name, scope) != null) {
            erro(line, "(sem) identificador >" + name + "< já declarado no escopo >" + scope + "<");
            return;
        }

        ts.insert(new TS_entry(name, type, categoria, scope));
    }

    public void validateAssignment(String name, TypeInfo exprType, int line) {
        TS_entry variable = lookupVariable(name);
        if (variable == null) {
            erro(line, "(sem) variável >" + name + "< não declarada no escopo atual");
            return;
        }

        if (!isAssignable(variable.getTipo(), exprType)) {
            erro(line, "(sem) tipos incompatíveis em atribuição para >" + name + "<: esperado " + variable.getTipo() + ", encontrado " + exprType);
        }
    }

    public void validateCondition(String command, TypeInfo type, int line) {
        if (!type.isBoolean() && !type.isError()) {
            erro(line, "(sem) condição do " + command + " deve ser boolean, mas foi encontrado " + type);
        }
    }

    public void validatePrint(TypeInfo type, int line) {
        if (!type.isInt() && !type.isBoolean() && !type.isError()) {
            erro(line, "(sem) System.out.println aceita int ou boolean nesta versão, mas recebeu " + type);
        }
    }

    public TypeInfo lookupVariableType(String name, int line) {
        TS_entry entry = lookupVariable(name);
        if (entry == null) {
            erro(line, "(sem) variável >" + name + "< não declarada no escopo atual");
            return TypeInfo.ERROR;
        }
        return entry.getTipo();
    }

    public TypeInfo currentThisType(int line) {
        if (currClass == null || currMethod == null || "main".equals(currMethod.getId())) {
            erro(line, "(sem) uso de this fora de método de instância");
            return TypeInfo.ERROR;
        }
        return TypeInfo.classType(currClass.getId());
    }

    public TypeInfo validateNew(String className, int line) {
        if (ts.pesquisaClasse(className) == null) {
            erro(line, "(sem) classe >" + className + "< usada em new antes de ser declarada");
        }
        return TypeInfo.classType(className);
    }

    public TypeInfo validateArithmetic(String op, TypeInfo left, TypeInfo right, int line) {
        if ((!left.isInt() || !right.isInt()) && !left.isError() && !right.isError()) {
            erro(line, "(sem) operador " + op + " exige operandos int, mas recebeu " + left + " e " + right);
        }
        return TypeInfo.INT;
    }

    public TypeInfo validateLess(TypeInfo left, TypeInfo right, int line) {
        if ((!left.isInt() || !right.isInt()) && !left.isError() && !right.isError()) {
            erro(line, "(sem) operador < exige operandos int, mas recebeu " + left + " e " + right);
        }
        return TypeInfo.BOOLEAN;
    }

    public TypeInfo validateAnd(TypeInfo left, TypeInfo right, int line) {
        if ((!left.isBoolean() || !right.isBoolean()) && !left.isError() && !right.isError()) {
            erro(line, "(sem) operador && exige operandos boolean, mas recebeu " + left + " e " + right);
        }
        return TypeInfo.BOOLEAN;
    }

    public TypeInfo validateEquality(TypeInfo left, TypeInfo right, int line) {
        if (!typesComparable(left, right)) {
            erro(line, "(sem) operador de igualdade recebeu tipos incompatíveis: " + left + " e " + right);
        }
        return TypeInfo.BOOLEAN;
    }

    public TypeInfo validateNot(TypeInfo type, int line) {
        if (!type.isBoolean() && !type.isError()) {
            erro(line, "(sem) operador ! exige boolean, mas recebeu " + type);
        }
        return TypeInfo.BOOLEAN;
    }

    public TypeInfo validateMethodCall(TypeInfo objectType, String methodName, Object argObj, int line) {
        if (!objectType.isClass()) {
            if (!objectType.isError()) {
                erro(line, "(sem) tentativa de chamada de método em tipo básico >" + objectType + "<");
            }
            return TypeInfo.ERROR;
        }

        TS_entry method = lookupMethod(objectType.getClassName(), methodName);
        if (method == null) {
            erro(line, "(sem) método >" + methodName + "< não declarado na classe >" + objectType.getClassName() + "< nem em suas superclasses");
            return TypeInfo.ERROR;
        }

        validateArguments(method, argObj, line);
        return method.getTipo();
    }

    @SuppressWarnings("unchecked")
    public List<TypeInfo> prependArg(TypeInfo first, Object restObj) {
        List<TypeInfo> args = new ArrayList<TypeInfo>();
        args.add(first);
        if (restObj instanceof List) {
            args.addAll((List<TypeInfo>) restObj);
        }
        return args;
    }

    private String methodScope() {
        String className = currClass != null ? currClass.getId() : "<classe-desconhecida>";
        String methodName = currMethod != null ? currMethod.getId() : "<metodo-desconhecido>";
        return className + "." + methodName;
    }

    private TS_entry lookupVariable(String name) {
        if (currMethod != null) {
            TS_entry local = ts.pesquisa(name, methodScope());
            if (local != null) {
                return local;
            }
        }

        TS_entry cursor = currClass;
        while (cursor != null) {
            TS_entry field = ts.pesquisa(name, cursor.getId(), ClasseID.Atributo);
            if (field != null) {
                return field;
            }
            cursor = cursor.getSuperClasse();
        }
        return null;
    }

    private TS_entry lookupFieldInSupers(String name) {
        TS_entry cursor = currClass != null ? currClass.getSuperClasse() : null;
        while (cursor != null) {
            TS_entry field = ts.pesquisa(name, cursor.getId(), ClasseID.Atributo);
            if (field != null) {
                return field;
            }
            cursor = cursor.getSuperClasse();
        }
        return null;
    }

    private TS_entry lookupMethod(String className, String methodName) {
        TS_entry cursor = ts.pesquisaClasse(className);
        while (cursor != null) {
            TS_entry method = ts.pesquisa(methodName, cursor.getId(), ClasseID.NomeMetodo);
            if (method != null) {
                return method;
            }
            cursor = cursor.getSuperClasse();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void validateArguments(TS_entry method, Object argObj, int line) {
        List<TypeInfo> args = (List<TypeInfo>) argObj;
        List<TS_entry> params = method.getParametros();

        if (args.size() != params.size()) {
            erro(line, "(sem) método >" + method.getId() + "< espera " + params.size() + " argumento(s), mas recebeu " + args.size());
            return;
        }

        for (int i = 0; i < args.size(); i++) {
            TypeInfo expected = params.get(i).getTipo();
            TypeInfo actual = args.get(i);
            if (!isAssignable(expected, actual)) {
                erro(line, "(sem) argumento " + (i + 1) + " de >" + method.getId() + "< incompatível: esperado " + expected + ", encontrado " + actual);
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

        TS_entry cursor = ts.pesquisaClasse(childName);
        while (cursor != null && cursor.getSuperClasse() != null) {
            cursor = cursor.getSuperClasse();
            if (cursor.getId().equals(parentName)) {
                return true;
            }
        }
        return false;
    }
}
