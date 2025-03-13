package com.fategrinder.barracks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.fategrinder.barracks.TokenType.*;

class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0; // to copy literal token text from (start to current) every cycle
	private int current = 0; // always points to the char that will interest us at the start of the next
								// iterative analysis
	private int line = 1; // for reporting errors at correct line

	Scanner(String source) {
		String normalized = source.replace("\r\n", "\n");
		this.source = normalized + "\n";
	}

	List<Token> scanAllTokens() {
		while (!isAtEnd()) {
			start = current;
			scanToken();
		}

		removeExtraInitialNewlinesTokens();
		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}

	private void removeExtraInitialNewlinesTokens() {
		while (!tokens.isEmpty() && tokens.get(0).type == NEW_LINE) {
			tokens.remove(0);
		}
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}

	private static final Map<String, TokenType> keywords; // to know reserved keywords
	static {
		keywords = new HashMap<>();
		keywords.put("and", AND);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		keywords.put("for", FOR);
		keywords.put("func", FUNC);
		keywords.put("if", IF);
		keywords.put("nil", NIL);
		keywords.put("or", OR);
		keywords.put("not", NOT);
		keywords.put("print", PRINT);
		keywords.put("return", RETURN);
		keywords.put("true", TRUE);
		keywords.put("int", INT);
		keywords.put("float", FLOAT);
		keywords.put("elif", ELIF);
		keywords.put("while", WHILE);
		keywords.put("const", CONST);
		keywords.put("break", BREAK);
		keywords.put("continue", CONTINUE);
		keywords.put("timer", TIMER);
		keywords.put("point", POINT);
		keywords.put("disable-self", DISABLE_SELF);
		keywords.put("#load-if-defined", LOAD_IF_DEFINED);
		keywords.put("#load-if-not-defined", LOAD_IF_NOT_DEFINED);
		keywords.put("#else", LOAD_IF_ELSE);
		keywords.put("#end-if", LOAD_IF_END_IF);
		keywords.put("param", PARAM);
		keywords.put("void", VOID);
	}

	private void scanToken() {
		discardWhitespaces();
		char c = advance();
		switch (c) {
		case '(':
			addToken(LEFT_PAREN);
			break;
		case ')':
			addToken(RIGHT_PAREN);
			break;
		case ',':
			addToken(COMMA);
			break;
		case '.':
			addToken(DOT);
			break;
		case '-':
			addToken(MINUS);
			break;
		case '+':
			addToken(PLUS);
			break;
		case '*':
			addToken(STAR);
			break;
		case '%':
			addToken(MODULO);
			break;
		case '!':
			if (match('=')) {
				addToken(BANG_EQUAL);
				break;
			}
			Barracks.printScannerError(line, "Unexpected character.");
			break;
		case '=':
			if (match('=')) {
				addToken(EQUAL_EQUAL);
				break;
			}
			if (match('>')) {
				addToken(THEN);
				break;
			}
			Barracks.printScannerError(line, "Unexpected character.");
			break;
		case ':':
			if (match('=')) {
				addToken(ASSIGN);
				break;
			}
			Barracks.printScannerError(line, "Unexpected character.");
			break;
		case '<':
			addToken(match('=') ? LESS_EQUAL : LESS);
			break;
		case '>':
			addToken(match('=') ? GREATER_EQUAL : GREATER);
			break;
		case ';':
			// A comment goes until the end of the line.
			discardRemainderOfLine();
			break;
		case '/':
			if (match('*')) {
				// A comment goes on until a */
				c = source.charAt(current); // because current is already pointing at the char after '*'
				discardMultilineCommentFromCharUntilClosure(c);
			} else {
				addToken(SLASH);
			}
			break;
		case '~':
			if (match('/')) {
				addToken(ROUNDED_DIVISION);
				break;
			}
			Barracks.printScannerError(line, "Unexpected character.");
			break;
		case '\n':
			addToken(NEW_LINE);
			line++;
			discardWhitespaces();
			while ((peek() == '\n' || peek() == ';' || (peek() == '/' && peekNext() == '*')) && !isAtEnd()) {
				if (peek() == ';') {
					discardRemainderOfLine();
				} else if (peek() == '/' && peekNext() == '*') {
					advance();
					match('*');
					c = source.charAt(current); // current is already pointing at the char after *
					discardMultilineCommentFromCharUntilClosureWithoutAddingNewlineToken(c);
				} else if (peek() == '\n') {
					advance();
					line++;
				}
				discardWhitespaces();
			}
			break;
		case '"':
			string();
			break;
		default:
			if (isDigit(c)) {
				number();
			} else if (isAlpha(c)) {
				identifier();
			} else {
				Barracks.printScannerError(line, "Unexpected character.");
			}
			break;
		}
	}

	private void discardMultilineCommentFromCharUntilClosureWithoutAddingNewlineToken(char c) {
		while (!isAtEnd()) {
			if (c == '\n') {
				line++;
			}
			if (c == '*' && match('/')) {
				break;
			}
			c = advance();
		}
	}

	private void discardWhitespaces() {
		while (match(' ') || match('\r') || match('\t'))
			start = current; // because maybe someone else works after this and they end up inglobing the
								// whitespaces in the text, which is bad for variables names....
	}

	private void discardMultilineCommentFromCharUntilClosure(char c) {
		boolean should_add_new_line_token = false; // to only add one newline at most
		while (!isAtEnd()) {
			if (c == '\n') {
				should_add_new_line_token = true;
				line++;
			}
			if (c == '*' && match('/')) {
				break;
			}
			c = advance();
		}
		if (peek() != '\n' && !isAtEnd() && should_add_new_line_token) { // if there's at least a following '\n' it will
																			// be added later by someone else anyway
			discardWhitespaces();

			if (peek() != ';' && peek() != '\n') {
				if (peek() != '/' || (peekNext() != '*')) {
					addToken(NEW_LINE);
				}
			}
		}
	}

	private void discardRemainderOfLine() {
		while (peek() != '\n' && !isAtEnd())
			advance();
	}

	private char advance() {
		return source.charAt(current++); // returns next interesting character, and then moves pointer to the next for
											// later
	}

	private boolean match(char expected) { // to eat correct matching characters, and leave current pointer still if not
											// matching, with boolean feedback

		if (isAtEnd())
			return false;
		if (source.charAt(current) != expected)
			return false;
		current++;
		return true;
	}

	private char peek() { // like an advance but that doesn't consume a char
		if (isAtEnd())
			return '\0';
		return source.charAt(current);
	}

	private char peekNext() {
		if (current + 1 >= source.length())
			return '\0';
		return source.charAt(current + 1);
	}

	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') {
				Barracks.printScannerError(line, "Unterminated string.");
				return;
			}
			advance();
		}

		if (isAtEnd()) {
			Barracks.printScannerError(line, "Unterminated string.");
			return;
		}

		// The closing ".
		advance();

		// Trim the surrounding quotes.
		String value = source.substring(start + 1, current - 1);
		addToken(STRING, value);
	}

	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-' || c == '#';
	}

	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	private void identifier() {
		while (isAlphaNumeric(peek()))
			advance();

		boolean isFunctionKeyword = false;
		if (peek() == '(') {
			isFunctionKeyword = true;
			advance();
		}
		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		if (isFunctionKeyword) {
			type = FUNCTION_KEYWORD;
		} else if (type == null)
			type = IDENTIFIER;
		addToken(type);
	}

	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private void number() {
		while (isDigit(peek()))
			advance();

		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the "."
			advance();

			while (isDigit(peek()))
				advance();

			addToken(NUMBER_FLOAT, Double.parseDouble(source.substring(start, current)));
			return;
		}

		addToken(NUMBER_INTEGER, (int) Double.parseDouble(source.substring(start, current)));
	}

	private void addToken(TokenType type) { // for adding tokens without a literal value
		addToken(type, null);
	}

	private void addToken(TokenType type, Object literal) { // for adding tokens with a literal value
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}
}
