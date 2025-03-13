package com.fategrinder.barracks;

class Token {
	final TokenType type;
	final String lexeme;
	final Object literal;
	final int line;

	Token(TokenType type, String lexeme, Object literal, int line) {
		this.type = type; // the token type, numerical integer, numerical float, and and, a for, a while,
							// etc...
		this.lexeme = lexeme; // what the user wrote
		this.literal = literal; // the java value, if you wanted to compute before finishing compiling I
								// suppose...
		this.line = line; // to report errors
	}

	public String toString() {
		return type + " " + lexeme + " " + literal;
	}
}
