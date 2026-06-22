%{
  import java.io.*;
  import java.util.*;
%}

%token CLASS, EXTENDS, PUBLIC, STATIC, VOID, MAIN
%token STRING, INT, BOOLEAN, IF, ELSE, WHILE, SOUT
%token NEW, TRUE, FALSE, THIS, LENGTH, RETURN
%token AND, EQ, NEQ
%token Identifier, Number

%nonassoc '<' EQ NEQ
%left AND
%left '+' '-'
%left '*' '/'
%right '!'
%left '['
%left '.'

%type <sval> Identifier
%type <obj> Type BaseType Exp RealParamList RealParamListAux

%%

Program : ClassDeclarationList
        ;

ClassDeclarationList : ClassDeclarationList ClassDeclaration
                     | ClassDeclaration
                     ;

ClassDeclaration : ClassHeader '{' VarDeclarationList MethodDeclarationList '}'
                   { sem.finishClass(); }
                 ;

ClassHeader : CLASS Identifier
              { sem.startClass($2, lexer.getLine()); }
              ClassExtendsOpt
            ;

ClassExtendsOpt : EXTENDS Identifier
                  { sem.setSuperClass($2, lexer.getLine()); }
                |
                ;

VarDeclarationList : VarDeclarationList VarDeclaration
                   |
                   ;

VarDeclaration : Type Identifier ';'
                 { sem.addVariable($2, (TypeInfo)$1, lexer.getLine()); }
               ;

MethodDeclarationList : MethodDeclarationList MethodDeclaration
                      | MethodDeclarationList MainMethod
                      |
                      ;

MethodDeclaration : MethodHeader '(' ParamList ')' '{' DeclOrStatList RETURN Exp ';' '}'
                    { sem.finishMethod((TypeInfo)$8, lexer.getLine()); }
                  ;

MethodHeader : PUBLIC Type Identifier
               { sem.startMethod($3, (TypeInfo)$2, lexer.getLine()); }
             ;

MainMethod : MainHeader '{' DeclOrStatList '}'
             { sem.finishMain(); }
           ;

MainHeader : PUBLIC STATIC VOID MAIN '(' STRING '[' ']' Identifier ')'
             { sem.startMain($9, lexer.getLine()); }
           ;

ParamList : ParamListNonEmpty
          |
          ;

ParamListNonEmpty : Param
                  | ParamListNonEmpty ',' Param
                  ;

Param : Type Identifier
        { sem.addParam($2, (TypeInfo)$1, lexer.getLine()); }
      ;

DeclOrStatList : DeclOrStatList LocalVarDeclaration
               | DeclOrStatList Statement
               |
               ;

LocalVarDeclaration : BaseType Identifier ';'
                      { sem.addVariable($2, (TypeInfo)$1, lexer.getLine()); }
                    | Identifier Identifier ';'
                      { sem.addVariable($2, sem.resolveClassType($1, lexer.getLine()), lexer.getLine()); }
                    ;

BaseType : INT
           { $$ = TypeInfo.INT; }
         | BOOLEAN
           { $$ = TypeInfo.BOOLEAN; }
         | INT '[' ']'
           {
             yyerror("(sem) arrays não são tratados nesta versão da linguagem");
             $$ = TypeInfo.ERROR;
           }
         ;

Type : BaseType
       { $$ = $1; }
     | Identifier
       { $$ = sem.resolveClassType($1, lexer.getLine()); }
     ;

Statement : '{' StatementList '}'
          | IF '(' Exp ')' Statement ELSE Statement
            { sem.validateCondition("if", (TypeInfo)$3, lexer.getLine()); }
          | WHILE '(' Exp ')' Statement
            { sem.validateCondition("while", (TypeInfo)$3, lexer.getLine()); }
          | SOUT '(' Exp ')' ';'
            { sem.validatePrint((TypeInfo)$3, lexer.getLine()); }
          | Identifier '=' Exp ';'
            { sem.validateAssignment($1, (TypeInfo)$3, lexer.getLine()); }
          | Identifier '[' Exp ']' '=' Exp ';'
            { yyerror("(sem) arrays não são tratados nesta versão da linguagem"); }
          ;

StatementList : StatementList Statement
              |
              ;

Exp : Exp AND Exp
      { $$ = sem.validateAnd((TypeInfo)$1, (TypeInfo)$3, lexer.getLine()); }
    | Exp '<' Exp
      { $$ = sem.validateLess((TypeInfo)$1, (TypeInfo)$3, lexer.getLine()); }
    | Exp EQ Exp
      { $$ = sem.validateEquality((TypeInfo)$1, (TypeInfo)$3, lexer.getLine()); }
    | Exp NEQ Exp
      { $$ = sem.validateEquality((TypeInfo)$1, (TypeInfo)$3, lexer.getLine()); }
    | Exp '+' Exp
      { $$ = sem.validateArithmetic("+", (TypeInfo)$1, (TypeInfo)$3, lexer.getLine()); }
    | Exp '-' Exp
      { $$ = sem.validateArithmetic("-", (TypeInfo)$1, (TypeInfo)$3, lexer.getLine()); }
    | Exp '/' Exp
      { $$ = sem.validateArithmetic("/", (TypeInfo)$1, (TypeInfo)$3, lexer.getLine()); }
    | Exp '*' Exp
      { $$ = sem.validateArithmetic("*", (TypeInfo)$1, (TypeInfo)$3, lexer.getLine()); }
    | Exp '[' Exp ']'
      {
        yyerror("(sem) arrays não são tratados nesta versão da linguagem");
        $$ = TypeInfo.ERROR;
      }
    | Exp '.' LENGTH
      {
        yyerror("(sem) arrays e .length não são tratados nesta versão da linguagem");
        $$ = TypeInfo.ERROR;
      }
    | Exp '.' Identifier '(' RealParamList ')'
      { $$ = sem.validateMethodCall((TypeInfo)$1, $3, $5, lexer.getLine()); }
    | Number
      { $$ = TypeInfo.INT; }
    | TRUE
      { $$ = TypeInfo.BOOLEAN; }
    | FALSE
      { $$ = TypeInfo.BOOLEAN; }
    | Identifier
      { $$ = sem.lookupVariableType($1, lexer.getLine()); }
    | THIS
      { $$ = sem.currentThisType(lexer.getLine()); }
    | NEW INT '[' Exp ']'
      {
        yyerror("(sem) arrays não são tratados nesta versão da linguagem");
        $$ = TypeInfo.ERROR;
      }
    | NEW Identifier '(' ')'
      { $$ = sem.validateNew($2, lexer.getLine()); }
    | '!' Exp
      { $$ = sem.validateNot((TypeInfo)$2, lexer.getLine()); }
    | '(' Exp ')'
      { $$ = $2; }
    ;

RealParamList : Exp RealParamListAux
                { $$ = sem.prependArg((TypeInfo)$1, $2); }
              |
                { $$ = new ArrayList<TypeInfo>(); }
              ;

RealParamListAux : ',' Exp RealParamListAux
                   { $$ = sem.prependArg((TypeInfo)$2, $3); }
                 |
                   { $$ = new ArrayList<TypeInfo>(); }
                 ;

%%

  private Yylex lexer;
  private Semantico sem;

  private int yylex() {
    int yyl_return = -1;
    try {
      yylval = new ParserVal(0);
      yyl_return = lexer.yylex();
    } catch (IOException e) {
      System.err.println("IO error: " + e.getMessage());
    }
    return yyl_return;
  }

  public void yyerror(String error) {
    int line = lexer != null ? lexer.getLine() : -1;
    if (sem != null) {
      sem.erro(line, error);
    } else {
      System.err.printf("Erro (linha: %2d) %s%n", line, error);
    }
  }

  public Parser(Reader r) {
    lexer = new Yylex(r, this);
    sem = new Semantico();
  }

  public void setDebug(boolean debug) {
    yydebug = debug;
  }

  public void listarTS() {
    sem.listarTS();
  }

  public static void main(String args[]) throws IOException {
    System.out.println("Verificador de MiniJava");

    Parser yyparser;
    if (args.length > 0) {
      yyparser = new Parser(new FileReader(args[0]));
    } else {
      System.out.println("[Quit with CTRL-D]");
      System.out.print("Programa de entrada:\n");
      yyparser = new Parser(new InputStreamReader(System.in));
    }

    int parseResult = yyparser.yyparse();
    yyparser.listarTS();

    if (!yyparser.sem.hasMain()) {
      yyparser.yyerror("(sem) programa não possui método main");
    }

    if (parseResult != 0 || yyparser.sem.getErrors() > 0) {
      System.err.println("\nAnálise concluída com erro(s).");
      System.exit(1);
    }

    System.out.println("\nAnálise concluída sem erros.");
  }
