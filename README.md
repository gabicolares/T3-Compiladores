# T3 Compiladores — Verificação semântica para MiniJava

Este repositório contém uma adaptação do exemplo de verificação semântica visto em aula para a linguagem MiniJava. A entrega segue o mesmo fluxo dos arquivos disponibilizados pelo professor:

```text
lexico.flex  --JFlex-->   Yylex.java
miniJava.y   --BYACC/J--> Parser.java
Parser.java  --javac-->   Parser.class
```

O foco da implementação é:

- controle de escopo;
- declaração antes do uso;
- validação de tipos;
- validação de chamadas de métodos;
- suporte a polimorfismo por herança;
- rejeição de arrays, conforme simplificação do enunciado.

## Estrutura dos arquivos

```text
.
├── Makefile
├── ParserVal.java
├── lexico.flex
├── miniJava.y
├── Semantico.java
├── TypeInfo.java
├── ClasseID.java
├── TS_entry.java
├── TabSimb.java
├── run-tests.sh
└── testes/
    ├── 01-correto-basico.mjava
    ├── 02-correto-polimorfismo.mjava
    ├── 03-erro-classe-nao-declarada.mjava
    ├── 04-erro-variavel-nao-declarada.mjava
    ├── 05-erro-tipo-atribuicao.mjava
    ├── 06-erro-if-nao-boolean.mjava
    ├── 07-erro-metodo-inexistente.mjava
    ├── 08-erro-parametros.mjava
    ├── 09-erro-polimorfismo-invalido.mjava
    └── 10-erro-array-nao-suportado.mjava
```

Os arquivos `Parser.java` e `Yylex.java` não ficam versionados, pois são gerados automaticamente pelo BYACC/J e pelo JFlex.

## Dependências

No Codespaces, instale as dependências com:

```bash
sudo apt update
sudo apt install -y default-jdk make jflex byacc-j
```

Para conferir:

```bash
java -version
javac -version
jflex --version
byaccj -V
```

Caso `byaccj -V` não funcione, use:

```bash
byaccj -v
```

## Como compilar

```bash
make clean
make
```

O `Makefile` executa automaticamente:

```bash
jflex lexico.flex
byaccj -tv -J miniJava.y
javac Parser.java ParserVal.java TypeInfo.java ClasseID.java TS_entry.java TabSimb.java Semantico.java
```

## Como executar um programa MiniJava

```bash
java Parser testes/01-correto-basico.mjava
```

Quando o programa é aceito, o analisador imprime a tabela de símbolos e finaliza com código `0`.

Quando há erro sintático ou semântico, o analisador imprime os erros encontrados e finaliza com código diferente de `0`.

## Como executar os testes

```bash
make test
```

O script `run-tests.sh` executa todos os arquivos da pasta `testes/`.

Os testes `01` e `02` devem ser aceitos. Os demais testes possuem erros semânticos esperados.

## Papel de cada arquivo

### `lexico.flex`

Contém as regras léxicas da linguagem. Ele reconhece palavras reservadas, identificadores, números, operadores e símbolos da linguagem MiniJava. Esse arquivo gera o `Yylex.java`.

### `miniJava.y`

Contém a gramática usada pelo BYACC/J e as ações semânticas associadas às produções. Esse arquivo gera o `Parser.java`.

As ações semânticas chamam métodos da classe `Semantico`, como:

- inserção de classes, atributos, métodos, parâmetros e variáveis locais;
- validação de tipos;
- validação de escopo;
- validação de chamadas de métodos;
- validação de compatibilidade polimórfica.

### `ParserVal.java`

Classe auxiliar usada pelo BYACC/J para transportar valores semânticos entre regras da gramática. Ela permite usar valores como `$1`, `$2` e `$$` dentro das ações do `.y`.

### `Semantico.java`

Centraliza a lógica semântica do trabalho. Foi criada para deixar o `miniJava.y` mais organizado, mantendo as regras da gramática separadas das funções auxiliares de validação.

### `TabSimb.java` e `TS_entry.java`

Implementam a tabela de símbolos. A tabela guarda objetos `TS_entry`, que representam símbolos declarados no programa.

Cada entrada da tabela armazena:

- `id`: nome do identificador;
- `tipo`: tipo associado ao símbolo;
- `categoria`: categoria do símbolo;
- `escopo`: escopo em que o símbolo foi declarado;
- `superClasse`: referência para a superclasse, quando o símbolo representa uma classe com herança;
- `parametros`: lista de parâmetros, quando o símbolo representa um método.

As categorias possíveis estão no enum `ClasseID`:

```java
TipoBase,
NomeClasse,
Atributo,
NomeMetodo,
NomeParam,
VarLocal
```

## Estrutura dos escopos

Os escopos foram representados como strings:

- `global`: usado para tipos base e nomes de classes;
- `NomeDaClasse`: usado para atributos e métodos de uma classe;
- `NomeDaClasse.nomeDoMetodo`: usado para parâmetros e variáveis locais de métodos.

Exemplo:

```text
Animal                         -> classe no escopo global
idade                          -> método no escopo Animal
recebeAnimal                   -> método no escopo Abrigo
a                              -> parâmetro no escopo Abrigo.recebeAnimal
```

## Controle de escopo

A linguagem foi tratada com a simplificação de que classes, atributos e métodos devem ser declarados antes do uso. Isso permite fazer a análise semântica em uma passagem.

Para variáveis usadas dentro de métodos, a busca segue esta ordem:

1. variáveis locais do método;
2. parâmetros do método;
3. atributos da classe atual;
4. atributos das superclasses, se houver herança.

Para métodos, a busca começa na classe do objeto e sobe pela cadeia de superclasses.

## Polimorfismo

O polimorfismo é tratado pela relação de subtipagem entre classes.

Uma atribuição entre objetos é válida quando o tipo da expressão é igual ao tipo esperado ou quando o tipo da expressão é uma subclasse do tipo esperado.

Exemplo válido:

```java
Animal a;
Cachorro c;
c = new Cachorro();
a = c;
```

Esse caso é válido se `Cachorro extends Animal`.

Exemplo inválido:

```java
Animal a;
Cachorro c;
a = new Animal();
c = a;
```

Esse caso é inválido porque nem todo `Animal` é necessariamente um `Cachorro`.

A mesma regra de compatibilidade é usada em:

- atribuições;
- argumentos de métodos;
- retorno de métodos.

## Validações semânticas executadas

O analisador verifica:

1. classe duplicada;
2. superclasse usada antes de ser declarada;
3. atributo duplicado no mesmo escopo;
4. atributo com mesmo nome de atributo herdado;
5. método duplicado na mesma classe;
6. parâmetro duplicado no mesmo método;
7. variável local duplicada no mesmo método;
8. uso de classe não declarada como tipo;
9. uso de classe não declarada em `new`;
10. uso de variável não declarada;
11. compatibilidade de tipos em atribuições;
12. compatibilidade de tipos em expressões aritméticas;
13. compatibilidade de tipos em expressões booleanas;
14. condição de `if` obrigatoriamente booleana;
15. condição de `while` obrigatoriamente booleana;
16. compatibilidade do tipo retornado por métodos;
17. existência de método chamado;
18. quantidade correta de argumentos em chamadas de métodos;
19. tipo correto dos argumentos em chamadas de métodos;
20. compatibilidade polimórfica entre classes e subclasses;
21. rejeição de arrays.

## Simplificações adotadas

Conforme o enunciado, arrays não são tratados. Portanto, o analisador acusa erro para construções como:

```java
int[] v;
v[0] = 1;
new int[10];
v.length;
```

A única ocorrência aceita de colchetes é na assinatura fixa do método principal:

```java
public static void main(String[] args)
```
