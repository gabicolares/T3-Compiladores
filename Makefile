JAVAC = javac
JAVA = java
SOURCES = TokenType.java Token.java Lexer.java TypeInfo.java ClasseID.java TS_entry.java TabSimb.java Parser.java

all: Parser.class

Parser.class: $(SOURCES)
	$(JAVAC) $(SOURCES)

run: Parser.class
	$(JAVA) Parser

test: Parser.class
	bash run-tests.sh

clean:
	rm -f *.class

.PHONY: all run test clean
