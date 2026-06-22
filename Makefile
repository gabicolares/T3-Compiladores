# Fluxo no mesmo padrão dos arquivos do professor:
# lexico.flex  -> JFlex   -> Yylex.java
# miniJava.y   -> BYACC/J -> Parser.java
# Parser.java  -> javac   -> Parser.class

JFLEX  = jflex
BYACCJ = byaccj -tv -J
JAVAC  = javac
JAVA   = java

AUX = ParserVal.java TypeInfo.java ClasseID.java TS_entry.java TabSimb.java Semantico.java

all: Parser.class

run: Parser.class
	$(JAVA) Parser

build: clean Parser.class

test: Parser.class
	bash run-tests.sh

clean:
	rm -f *~ *.class *.o *.s Yylex.java Parser.java y.output

Parser.class: Yylex.java Parser.java $(AUX)
	$(JAVAC) Parser.java $(AUX)

Yylex.java: lexico.flex
	$(JFLEX) lexico.flex

Parser.java: miniJava.y Yylex.java
	$(BYACCJ) miniJava.y

.PHONY: all run build test clean
