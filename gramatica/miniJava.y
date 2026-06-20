%{
  import java.io.*;
%}

%token CLASS, EXTENDS, PUBLIC, STATIC, VOID, MAIN
%token STRING, INT, BOOLEAN, IF, ELSE, WHILE, SOUT
%token NEW, TRUE, FALSE, THIS, RETURN
%token AND, EQ, NEQ
%token Identifier, Number

%nonassoc '<' EQ NEQ
%left AND
%left '+' '-'
%left '*' '/'
%right '!'
%left '.'

%type <sval> Identifier
%type <obj> Type BaseType Exp RealParamList

%%

Program : ClassDeclarationList
        ;

ClassDeclarationList : ClassDeclarationList ClassDeclaration
                     |
                     ;

ClassDeclaration : CLASS Identifier ClassExtendsOpt
                  '{' VarDeclarationList MethodDeclarationList MainMethodOpt '}'
                  {
                    /* Inserir classe no escopo global, validar duplicidade e guardar superclasse. */
                  }
                 ;

ClassExtendsOpt : EXTENDS Identifier
                  {
                    /* Validar se a superclasse já foi declarada antes do uso. */
                  }
                |
                ;

MainMethodOpt : PUBLIC STATIC VOID MAIN '(' STRING '[' ']' Identifier ')' '{' StatementList '}'
                {
                  /* Inserir método main no escopo da classe. String[] é aceito apenas aqui. */
                }
              |
              ;

MethodDeclarationList : MethodDeclarationList MethodDeclaration
                      |
                      ;

MethodDeclaration : PUBLIC Type Identifier '(' ParamList ')' '{' VarDeclarationList StatementList RETURN Exp ';' '}'
                    {
                      /* Inserir método, parâmetros e validar tipo do return. */
                    }
                  ;

VarDeclarationList : VarDeclarationList VarDeclaration
                   |
                   ;

VarDeclaration : Type Identifier ';'
                 {
                   /* Inserir atributo ou variável local conforme escopo corrente. */
                 }
               ;

ParamList : Type Identifier ParamListAux
          |
          ;

ParamListAux : ',' Type Identifier ParamListAux
             |
             ;

BaseType : BOOLEAN
         | INT
         ;

Type : BaseType
     | Identifier
       {
         /* Validar se a classe usada como tipo já foi declarada. */
       }
     ;

StatementList : StatementList Statement
              |
              ;

Statement : '{' StatementList '}'
          | IF '(' Exp ')' Statement ELSE Statement
            {
              /* Exp deve ser boolean. */
            }
          | WHILE '(' Exp ')' Statement
            {
              /* Exp deve ser boolean. */
            }
          | SOUT '(' Exp ')' ';'
          | Identifier '=' Exp ';'
            {
              /* Validar declaração e compatibilidade de tipos, incluindo subtipagem. */
            }
          ;

Exp : Exp AND Exp
    | Exp '<' Exp
    | Exp EQ Exp
    | Exp NEQ Exp
    | Exp '+' Exp
    | Exp '-' Exp
    | Exp '/' Exp
    | Exp '*' Exp
    | Exp '.' Identifier '(' RealParamList ')'
      {
        /* Buscar método na classe da expressão e nas superclasses. */
      }
    | Number
    | TRUE
    | FALSE
    | Identifier
      {
        /* Buscar variável em local, parâmetro, atributo e superclasses. */
      }
    | THIS
    | NEW Identifier '(' ')'
      {
        /* Validar se a classe foi declarada antes do uso. */
      }
    | '!' Exp
      {
        /* Exp deve ser boolean. */
      }
    | '(' Exp ')'
    ;

RealParamList : Exp RealParamListAux
              |
              ;

RealParamListAux : ',' Exp RealParamListAux
                 |
                 ;

%%

/*
 * Este arquivo documenta a gramática adaptada para o trabalho.
 * A implementação executável do repositório está nos arquivos Java da raiz.
 */
