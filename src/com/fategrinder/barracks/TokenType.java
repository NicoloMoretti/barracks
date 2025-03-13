package com.fategrinder.barracks;

enum TokenType {
	  // Surely single-character tokens.
	  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
	  COMMA, DOT, MINUS, PLUS, NEW_LINE, SLASH, STAR, MODULO,

	  // Tokens that could be made of either one or two character.
	  BANG, BANG_EQUAL,
	  ASSIGN, EQUAL_EQUAL,
	  GREATER, GREATER_EQUAL,
	  LESS, LESS_EQUAL, ROUNDED_DIVISION,
	  
	  FUNCTION_KEYWORD,

	  // Literals types
	  IDENTIFIER, STRING, NUMBER_INTEGER, NUMBER_FLOAT, BOOLEAN, VOID,
	  TIMER_OBJECT, POINT_OBJECT, NULL, PARAMATER_NATIVE_OBJECT, COMMAND_NATIVE_OBJECT,
	  //Variable status
	  NOT_INITIALZIED, INITIALIZED, X_INITIALIZED, Y_INITIALIZED,
	  
	  //Literal types are for the variable type itself, keyword types are the declration keywords themeselves

	  // Keywords types
	  AND, ELSE, FALSE, FUNC, FOR, IF, NIL, OR, NOT, CONST, ELIF,
	  PRINT, RETURN, TRUE, INT, FLOAT, WHILE, THEN, BREAK, CONTINUE, TIMER, POINT, DISABLE_SELF,
	  PARAM,
	  
	  // Keywords type load-ifs
	  LOAD_IF_DEFINED, LOAD_IF_NOT_DEFINED, LOAD_IF_ELSE, LOAD_IF_END_IF, LOAD_IF_NATIVE_SYMBOL,

	  EOF
	}
