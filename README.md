# T3 Compiladores — Verificação semântica para MiniJava

Este repositório contém uma adaptação do exemplo de verificação semântica visto em aula para uma versão simplificada da linguagem MiniJava.

O foco da implementação é:

- controle de escopo;
- declaração antes do uso;
- validação de tipos;
- validação de chamadas de métodos;
- suporte a polimorfismo por herança;
- rejeição de arrays, conforme simplificação do enunciado.

A implementação principal está em Java puro, com um analisador léxico e sintático recursivo-descendente. Também foi incluída uma pasta `gramatica/` com uma versão adaptada dos arquivos `.y` e `.flex` como referência da gramática original usada no trabalho.

## Estrutura dos arquivos

```text
.
├── Makefile
├── Parser.java
├── Lexer.java
├── Token.java
├── TokenType.java
├── TypeInfo.java
├── ClasseID.java
├── TS_entry.java
├── TabSimb.java
├── run-tests.sh
├── testes/
│   ├── 01-correto-basico.mjava
│   ├── 02-correto-polimorfismo.mjava
│   ├── 03-erro-classe-nao-declarada.mjava
│   ├── 04-erro-variavel-nao-declarada.mjava
│   ├── 05-erro-tipo-atribuicao.mjava
│   ├── 06-erro-if-nao-boolean.mjava
│   ├── 07-erro-metodo-inexistente.mjava
│   ├── 08-erro-parametros.mjava
│   ├── 09-erro-polimorfismo-invalido.mjava
│   └── 10-erro-array-nao-suportado.mjava
└── gramatica/
    ├── miniJava.y
    └── lexico.flex
```

## Como compilar

```bash
make
```

Esse comando compila todos os arquivos Java necessários para executar o analisador.

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

## Estrutura da tabela de símbolos

A tabela de símbolos foi implementada na classe `TabSimb`.

Cada entrada da tabela é representada por um objeto `TS_entry`, que armazena:

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

A linguagem foi tratada com a simplificação de que classes, atributos e métodos devem ser declarados antes do uso. Isso permite fazer a análise em uma única passagem.

Para variáveis usadas dentro de métodos, a busca segue esta ordem:

1. variáveis locais do método;
2. parâmetros do método;
3. atributos da classe atual;
4. atributos das superclasses, se houver herança.

Para métodos, a busca começa na classe do objeto e sobe pela cadeia de superclasses.

Exemplo:

```java
class Animal {
    public int idade() {
        return 3;
    }
}

class Cachorro extends Animal {
    public int late() {
        return 1;
    }
}
```

Nesse caso, uma chamada a `idade()` em um objeto do tipo `Cachorro` é válida, pois o método foi herdado de `Animal`.

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

Esse caso é válido porque `Cachorro extends Animal`.

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
4. método duplicado na mesma classe;
5. parâmetro duplicado no mesmo método;
6. variável local duplicada no mesmo método;
7. uso de classe não declarada como tipo;
8. uso de classe não declarada em `new`;
9. uso de variável não declarada;
10. compatibilidade de tipos em atribuições;
11. compatibilidade de tipos em expressões aritméticas;
12. compatibilidade de tipos em expressões booleanas;
13. condição de `if` obrigatoriamente booleana;
14. condição de `while` obrigatoriamente booleana;
15. compatibilidade do tipo retornado por métodos;
16. existência de método chamado;
17. quantidade correta de argumentos em chamadas de métodos;
18. tipo correto dos argumentos em chamadas de métodos;
19. compatibilidade polimórfica entre classes e subclasses;
20. rejeição de arrays.

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

## Observação sobre a gramática

A pasta `gramatica/` contém uma versão adaptada dos arquivos `miniJava.y` e `lexico.flex`, baseada na gramática inicial fornecida pelo professor. Esses arquivos documentam a gramática considerada, incluindo a remoção do tratamento de arrays e a inclusão de `extends`.

A versão executável deste repositório usa as classes Java diretamente, para facilitar a compilação e execução dos testes com `javac`, sem depender da instalação local de `jflex` e `byaccj`.
