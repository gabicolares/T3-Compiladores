import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String input;
    private final List<Token> buffer = new ArrayList<>();
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("class", TokenType.CLASS);
        KEYWORDS.put("extends", TokenType.EXTENDS);
        KEYWORDS.put("public", TokenType.PUBLIC);
        KEYWORDS.put("static", TokenType.STATIC);
        KEYWORDS.put("void", TokenType.VOID);
        KEYWORDS.put("main", TokenType.MAIN);
        KEYWORDS.put("String", TokenType.STRING);
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("boolean", TokenType.BOOLEAN);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("new", TokenType.NEW);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("this", TokenType.THIS);
        KEYWORDS.put("return", TokenType.RETURN);
    }

    public Lexer(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] data = new char[4096];
        int read;
        while ((read = reader.read(data)) != -1) {
            sb.append(data, 0, read);
        }
        this.input = sb.toString();
    }

    public Token peek() {
        return peek(0);
    }

    public Token peek(int offset) {
        while (buffer.size() <= offset) {
            buffer.add(readNextToken());
        }
        return buffer.get(offset);
    }

    public Token next() {
        Token token = peek(0);
        buffer.remove(0);
        return token;
    }

    private Token readNextToken() {
        skipIgnored();

        int startLine = line;
        int startColumn = column;

        if (isAtEnd()) {
            return new Token(TokenType.EOF, "", startLine, startColumn);
        }

        if (startsWith("System.out.println")) {
            advanceBy("System.out.println".length());
            return new Token(TokenType.SOUT, "System.out.println", startLine, startColumn);
        }

        char c = current();

        if (Character.isLetter(c) || c == '_') {
            StringBuilder sb = new StringBuilder();
            while (!isAtEnd() && (Character.isLetterOrDigit(current()) || current() == '_')) {
                sb.append(current());
                advance();
            }
            String lexeme = sb.toString();
            TokenType keyword = KEYWORDS.get(lexeme);
            return new Token(keyword != null ? keyword : TokenType.IDENTIFIER, lexeme, startLine, startColumn);
        }

        if (Character.isDigit(c)) {
            StringBuilder sb = new StringBuilder();
            while (!isAtEnd() && Character.isDigit(current())) {
                sb.append(current());
                advance();
            }
            return new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn);
        }

        if (startsWith("&&")) {
            advanceBy(2);
            return new Token(TokenType.AND, "&&", startLine, startColumn);
        }
        if (startsWith("==")) {
            advanceBy(2);
            return new Token(TokenType.EQ, "==", startLine, startColumn);
        }
        if (startsWith("!=")) {
            advanceBy(2);
            return new Token(TokenType.NEQ, "!=", startLine, startColumn);
        }

        advance();
        switch (c) {
            case '<': return new Token(TokenType.LT, "<", startLine, startColumn);
            case '+': return new Token(TokenType.PLUS, "+", startLine, startColumn);
            case '-': return new Token(TokenType.MINUS, "-", startLine, startColumn);
            case '*': return new Token(TokenType.STAR, "*", startLine, startColumn);
            case '/': return new Token(TokenType.SLASH, "/", startLine, startColumn);
            case '!': return new Token(TokenType.NOT, "!", startLine, startColumn);
            case '=': return new Token(TokenType.ASSIGN, "=", startLine, startColumn);
            case '.': return new Token(TokenType.DOT, ".", startLine, startColumn);
            case ',': return new Token(TokenType.COMMA, ",", startLine, startColumn);
            case ';': return new Token(TokenType.SEMICOLON, ";", startLine, startColumn);
            case '(': return new Token(TokenType.LPAREN, "(", startLine, startColumn);
            case ')': return new Token(TokenType.RPAREN, ")", startLine, startColumn);
            case '{': return new Token(TokenType.LBRACE, "{", startLine, startColumn);
            case '}': return new Token(TokenType.RBRACE, "}", startLine, startColumn);
            case '[': return new Token(TokenType.LBRACKET, "[", startLine, startColumn);
            case ']': return new Token(TokenType.RBRACKET, "]", startLine, startColumn);
            default:
                throw new RuntimeException("Caractere inesperado '" + c + "' na linha " + startLine + ", coluna " + startColumn);
        }
    }

    private void skipIgnored() {
        boolean changed;
        do {
            changed = false;

            while (!isAtEnd() && Character.isWhitespace(current())) {
                advance();
                changed = true;
            }

            if (startsWith("//")) {
                while (!isAtEnd() && current() != '\n' && current() != '\r') {
                    advance();
                }
                changed = true;
            }

            if (startsWith("/*")) {
                advanceBy(2);
                while (!isAtEnd() && !startsWith("*/")) {
                    advance();
                }
                if (!isAtEnd()) {
                    advanceBy(2);
                }
                changed = true;
            }

            if (startsWith("$TRACE_ON") || startsWith("$TRACE_OFF")) {
                while (!isAtEnd() && current() != '\n' && current() != '\r') {
                    advance();
                }
                changed = true;
            }
        } while (changed);
    }

    private boolean startsWith(String text) {
        return input.startsWith(text, pos);
    }

    private boolean isAtEnd() {
        return pos >= input.length();
    }

    private char current() {
        return input.charAt(pos);
    }

    private void advanceBy(int amount) {
        for (int i = 0; i < amount; i++) {
            advance();
        }
    }

    private void advance() {
        if (isAtEnd()) {
            return;
        }
        char c = input.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else if (c == '\r') {
            if (!isAtEnd() && input.charAt(pos) == '\n') {
                pos++;
            }
            line++;
            column = 1;
        } else {
            column++;
        }
    }
}
