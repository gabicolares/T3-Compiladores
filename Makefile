JAVAC = javac
JAVA = java
SOURCES = TokenType.java Token.java Lexer.java TypeInfo.java ClasseID.java TS_entry.java TabSimb.java SemanticParser.java

all: SemanticParser.class

SemanticParser.class: $(SOURCES)
	$(JAVAC) $(SOURCES)

run: SemanticParser.class
	$(JAVA) SemanticParser

test: SemanticParser.class
	bash run-tests.sh

clean:
	rm -f *.class

.PHONY: all run test clean
