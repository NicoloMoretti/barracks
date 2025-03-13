package com.fategrinder.barracks;

import static com.fategrinder.barracks.TokenType.AND;
import static com.fategrinder.barracks.TokenType.ASSIGN;
import static com.fategrinder.barracks.TokenType.BANG;
import static com.fategrinder.barracks.TokenType.BANG_EQUAL;
import static com.fategrinder.barracks.TokenType.CONST;
import static com.fategrinder.barracks.TokenType.ELIF;
import static com.fategrinder.barracks.TokenType.ELSE;
import static com.fategrinder.barracks.TokenType.EOF;
import static com.fategrinder.barracks.TokenType.EQUAL_EQUAL;
import static com.fategrinder.barracks.TokenType.FALSE;
import static com.fategrinder.barracks.TokenType.GREATER;
import static com.fategrinder.barracks.TokenType.GREATER_EQUAL;
import static com.fategrinder.barracks.TokenType.IDENTIFIER;
import static com.fategrinder.barracks.TokenType.IF;
import static com.fategrinder.barracks.TokenType.INT;
import static com.fategrinder.barracks.TokenType.LEFT_PAREN;
import static com.fategrinder.barracks.TokenType.LESS;
import static com.fategrinder.barracks.TokenType.LESS_EQUAL;
import static com.fategrinder.barracks.TokenType.MINUS;
import static com.fategrinder.barracks.TokenType.MODULO;
import static com.fategrinder.barracks.TokenType.NEW_LINE;
import static com.fategrinder.barracks.TokenType.NIL;
import static com.fategrinder.barracks.TokenType.NOT;
import static com.fategrinder.barracks.TokenType.NUMBER_FLOAT;
import static com.fategrinder.barracks.TokenType.NUMBER_INTEGER;
import static com.fategrinder.barracks.TokenType.OR;
import static com.fategrinder.barracks.TokenType.PLUS;
import static com.fategrinder.barracks.TokenType.PRINT;
import static com.fategrinder.barracks.TokenType.RIGHT_PAREN;
import static com.fategrinder.barracks.TokenType.SLASH;
import static com.fategrinder.barracks.TokenType.STAR;
import static com.fategrinder.barracks.TokenType.STRING;
import static com.fategrinder.barracks.TokenType.THEN;
import static com.fategrinder.barracks.TokenType.TRUE;
import static com.fategrinder.barracks.TokenType.WHILE;
import static com.fategrinder.barracks.TokenType.BREAK;
import static com.fategrinder.barracks.TokenType.CONTINUE;
import static com.fategrinder.barracks.TokenType.BOOLEAN;
import static com.fategrinder.barracks.TokenType.TIMER;
import static com.fategrinder.barracks.TokenType.POINT;
import static com.fategrinder.barracks.TokenType.TIMER_OBJECT;
import static com.fategrinder.barracks.TokenType.COMMA;
import static com.fategrinder.barracks.TokenType.ROUNDED_DIVISION;
import static com.fategrinder.barracks.TokenType.DISABLE_SELF;
import static com.fategrinder.barracks.TokenType.LOAD_IF_DEFINED;
import static com.fategrinder.barracks.TokenType.LOAD_IF_NOT_DEFINED;
import static com.fategrinder.barracks.TokenType.LOAD_IF_ELSE;
import static com.fategrinder.barracks.TokenType.LOAD_IF_END_IF;
import static com.fategrinder.barracks.TokenType.PARAM;
import static com.fategrinder.barracks.TokenType.FUNC;
import static com.fategrinder.barracks.TokenType.VOID;
import static com.fategrinder.barracks.TokenType.RETURN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Parser {

	private final List<Token> tokens;
	private int current = 0; // for pointing at current token to process

	private CommandsContainer commandsContainer = new CommandsContainer();
	private LoadIfSymbolsContainer loadIfSymbolsContainer = new LoadIfSymbolsContainer();

	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>(); // The parser must return a list of AST, known as statements, each
													// statement is the root of a sub tree
		try {
			while (!isAtEnd()) {
				statements.add(allowedOutside());
			}
		} catch (ParseError error) {
			return null;
		}

		return statements;
	}

	private Stmt allowedOutside() {
		try {
			if (match(LEFT_PAREN)) {
				if (match(IF)) {
					consume(NEW_LINE, "Missing 'newline' after the 'IF' keyword.");
					return ifStatement();
				}
				if (match(WHILE)) {
					consume(NEW_LINE, "Missing 'newline' after the 'WHILE' keyword.");
					return whileStatement();
				}
				consume(NEW_LINE, "Missing 'newline' after opening new block.");
				return new Stmt.Block(block());
			}
			if (existingType()) {
				return rawDeclaration();
			}
			if (match(CONST)) {
				return constStatement();
			}
			if (match(PARAM)) {
				return paramInitializationStatement();
			}
			if (match(LOAD_IF_DEFINED) || match(LOAD_IF_NOT_DEFINED)) {
				return loadIfStatement();
			}
			if (match(IDENTIFIER)) {
				return paramAssignmentStatement();
			}
			if (match(FUNC)) {
				return functionDeclaration();
			}

		} catch (ParseError error) {
			synchronize();
			return null;
		}
		throw error(peek(),
				"Only if/while/blocks, or functions declarations (func) or variable declarations (without initialization), or constants declarations or param aliases definitions or #load-if- blocks are allowed in the main body.");
	}

	private Stmt functionDeclaration() {
		Token returnType;
		if (check(INT) || check(VOID) || check(POINT) || check(TIMER)) {
			returnType = advance();
			if (returnType.type.equals(TIMER)) {
				throw error(returnType, "Barracks doesn't yet support returning timer objects from functions.\nIf you believe it's an urgently needed, contact me.");
			}
		} else {
			throw error(peek(), "Missing a return type for the function declaration. Use 'void' for none.");
		}

		Token functionName = consume(TokenType.FUNCTION_KEYWORD, "Missing a valid function name.");

		Map<String, TokenType> arguments = new LinkedHashMap<String, TokenType>(); // LinkedHashMap to have arguments in order too
		if (!check(RIGHT_PAREN)) {
			do {
				if (check(INT) || check(POINT) || check(TIMER)) {
					TokenType type = advance().type;
					if (type.equals(TIMER)) {
						throw error(returnType, "Barracks doesn't yet support passing timer objects to functions.\nIf you believe it's an urgently needed feature, contact me.");
					}
					arguments.put(consume(IDENTIFIER,
							"Missing a valid name for the parameter of type '" + type + "'.").lexeme, type);
				} else {
					throw error(peek(), "Expected a type before an argument's name.");
				}

			} while (match(COMMA));
		}
		consume(RIGHT_PAREN, "Missing closing parenthesis ')' of the function.\nIf you inteded to add multiple arguments, separate them with commas ','.");
		//consume(NEW_LINE, "Missing 'newline' after function declaration header.");
		Stmt.Block functionDeclarationBody;
		if (match(LEFT_PAREN)) {
			consume(NEW_LINE, "Missing 'newline' after opening new block.");
			functionDeclarationBody = new Stmt.Block(block());
		} else {
			throw error(peek(), "Expected a '(' that would open the function declaration's body.");
		}

		return new Stmt.FunctionDefinition(functionName, returnType, arguments, functionDeclarationBody);
	}

	private boolean existingType() {
		if (check(INT) || check(TIMER) || check(POINT)) {
			return true;
		}
		return false;
	}

	private Stmt constStatement() {
		Token name = consume(IDENTIFIER, "Missing the constant's name.");
		Expr.Literal value = null;
		int constantSignMultiplier = 1;
		if (match(TokenType.MINUS)) {
			constantSignMultiplier = -1;
		}
		if (match(NUMBER_INTEGER)) {
			value = new Expr.Literal((int) previous().literal * constantSignMultiplier, previous().type);
		} else if (match(STRING)) {
			if (constantSignMultiplier == -1) {
				throw error(peek(), "Negating a string is unreasonable.");
			}
			value = new Expr.Literal(previous().literal, previous().type);
		} else {
			throw error(peek(), "Constant definition contains an invalid value");
		}
		consume(NEW_LINE, "Missing 'newline' after constant declaration.");
		return new Stmt.Const(name, value);
	}

	private Stmt paramInitializationStatement() {
		Token alias = consume(IDENTIFIER, "Missing the parameter's new alias name.");
		if (!match(ASSIGN)) {
			throw error(peek(),
					"Missing ':='. It's necessary to bind a Parameter to the newly declared alias immediately, can be reassigned later.");
		}
		Token actualParameterName = consume(IDENTIFIER,
				"Missing the actual Parameter to associate to '" + alias.lexeme + "'.");
		consume(NEW_LINE, "Missing 'newline' after parameter alias definition.");
		return new Stmt.ParamInitialization(alias, actualParameterName);
	}

	private Stmt rawDeclaration() {
		if (match(INT)) {
			Token name = consume(IDENTIFIER, "Missing a variable name.");
			consume(NEW_LINE, "Expected a 'newline' after declaring an uninitialized variable.");
			Expr initializer = null;
			return new Stmt.Var(name, initializer, NUMBER_INTEGER);
		}
		if (match(TIMER)) {
			Token name = consume(IDENTIFIER, "Missing a variable name.");
			consume(NEW_LINE, "Expected a 'newline' after declaring an uninitialized variable.");
			Expr initializer = null;
			return new Stmt.Var(name, initializer, TIMER_OBJECT);
		}
		if (match(POINT)) {
			Token name = consume(IDENTIFIER, "Missing a variable name.");
			consume(NEW_LINE, "Expected a 'newline' after declaring an uninitialized variable.");
			Expr initializer = null;
			return new Stmt.Var(name, initializer, TokenType.POINT_OBJECT);
		}
		// unreachable
		return null;
	}

	private Stmt loadIfStatement() {
		boolean ifDefined = true;
		if (!previous().type.equals(LOAD_IF_DEFINED)) {
			ifDefined = false;
		}
		Token symbol = null;
		if (match(IDENTIFIER)) {
			if (loadIfSymbolsContainer.isValidLoadIfSymbol(previous().lexeme)) {
				symbol = previous();
				consume(NEW_LINE, "Missing 'newline' after the 'Load-If' Symbol.");
			} else {
				throw error(previous(), "Unknown System Defined 'Load-If' Symbol.");
			}
		} else {
			throw error(peek(), "Missing System Defined 'Load-If' Symbol.");
		}

		List<Stmt> statements = new ArrayList<>();

		while (!check(LOAD_IF_END_IF) && !check(LOAD_IF_ELSE) && !isAtEnd()) {
			if (check(BREAK) || check(CONTINUE)) {
				throw error(peek(), "'break' and 'continue' cannot appear inside a #load-if statment.");
			}
			// :
			if (match(LOAD_IF_DEFINED) || match(LOAD_IF_NOT_DEFINED)) {
				statements.add(loadIfStatement());
			} else {
				if (match(LEFT_PAREN)) {
					if (match(IF)) {
						consume(NEW_LINE, "Missing 'newline' after the 'IF' keyword.");
						statements.add(ifStatement());
					}
					if (match(WHILE)) {
						consume(NEW_LINE, "Missing 'newline' after the 'WHILE' keyword.");
						statements.add(whileStatement());
					}
					consume(NEW_LINE, "Missing 'newline' after opening new block.");
					statements.add(new Stmt.Block(block()));
				} else if (match(LOAD_IF_DEFINED) || match(LOAD_IF_NOT_DEFINED)) {
					statements.add(loadIfStatement());
				} else if (match(IDENTIFIER)) {
					statements.add(paramAssignmentStatement());
				} else {
					throw error(peek(),
							"Only if/while/blocks, or param aliases assignments or #load-if- blocks are allowed inside #load-if- .");
				}
			}
		}
		/*
		 * commented out, a #load-if cannot be inside a loop anyway Stmt jumpStatment =
		 * jump(); if (jumpStatment != null) { statements.add(jumpStatment); }
		 */
		if (statements.size() == 0) {
			throw error(peek(), "Body of 'load-if' cannot be empty");
		}

		boolean thereWasElseBranch = false;
		List<Stmt> elseStatements = new ArrayList<>();
		if (match(LOAD_IF_ELSE)) {
			thereWasElseBranch = true;
			consume(NEW_LINE, "Missing 'newline' after the '#else' statement.");
			while (!check(LOAD_IF_END_IF) && !check(LOAD_IF_ELSE) && !isAtEnd()) {
				if (check(BREAK) || check(CONTINUE)) {
					throw error(peek(), "'break' and 'continue' cannot appear inside a #load-if statment.");
				}
				if (match(LOAD_IF_DEFINED) || match(LOAD_IF_NOT_DEFINED)) {
					elseStatements.add(loadIfStatement());
				} else {
					if (match(LEFT_PAREN)) {
						if (match(IF)) {
							consume(NEW_LINE, "Missing 'newline' after the 'IF' keyword.");
							elseStatements.add(ifStatement());
						}
						if (match(WHILE)) {
							consume(NEW_LINE, "Missing 'newline' after the 'WHILE' keyword.");
							elseStatements.add(whileStatement());
						}
						consume(NEW_LINE, "Missing 'newline' after opening new block.");
						elseStatements.add(new Stmt.Block(block()));
					} else if (match(LOAD_IF_DEFINED) || match(LOAD_IF_NOT_DEFINED)) {
						elseStatements.add(loadIfStatement());
					} else if (match(IDENTIFIER)) {
						elseStatements.add(paramAssignmentStatement());
					} else {
						throw error(peek(),
								"Only if/while/blocks, or param aliases assignments or #load-if- blocks are allowed inside #load-if- .");
					}
				}
			}
		}

		if (thereWasElseBranch && elseStatements.size() == 0) {
			throw error(peek(), "Body of 'else' of 'load-if' cannot be empty");
		}

		consume(LOAD_IF_END_IF, "Missing '#end-if', unclosed 'load-if' block.");
		consume(NEW_LINE, "Missing 'newline' after closing the 'load-if'.");
		return new Stmt.LoadIf(ifDefined, statements, elseStatements, symbol);
	}

	private Stmt ifStatement() {
		List<Stmt> condition = new ArrayList<>();
		while (!check(THEN) && !isAtEnd()) {
			condition.add(oneLiner());
		}
		if (condition.size() == 0) {
			throw error(peek(), "Condition of 'if' cannot be empty");
		}
		consume(THEN, "Expected '=>' after the condition of the IF.");
		consume(NEW_LINE, "Missing 'newline' after '=>' .");

		List<Stmt> thenBranchStatements = new ArrayList<>();
		Stmt jumpStatment = null;
		while (!check(RIGHT_PAREN) && !isAtEnd()) {
			if (check(RETURN)) {
				thenBranchStatements.add(statement());
				if (!check(RIGHT_PAREN)) {
					throw error(peek(), "'return' must be the last statement in its block.");
				}
				continue;
			}
			if (check(BREAK) || check(CONTINUE)) {
				jumpStatment = jump();
				if (!check(RIGHT_PAREN)) {
					throw error(peek(), "'break' and 'continue' must be the last statement of their block.");
				}
				break;
			}
			thenBranchStatements.add(statement());
		}

		if (jumpStatment != null) {
			thenBranchStatements.add(jumpStatment);
		}
		if (thenBranchStatements.size() == 0) {
			throw error(peek(), "Body of 'if' cannot be empty");
		}
		consume(RIGHT_PAREN, "Missing ')', unclosed if block.");
		consume(NEW_LINE, "Missing 'newline' after closing the IF block.");

		Stmt.If elifBranch = null;
		elifBranch = parseElifBranch(elifBranch);
		List<Stmt> elseBranch = null;
		if (elifBranch == null) {
			elseBranch = parseElseBranch(elseBranch);
		}
		return new Stmt.If(condition, thenBranchStatements, elifBranch, elseBranch);
	}

	private Stmt whileStatement() {
		List<Stmt> condition = new ArrayList<>();
		while (!check(THEN) && !isAtEnd()) {
			condition.add(oneLiner());
		}
		if (condition.size() == 0) {
			throw error(peek(), "Condition of 'while' cannot be empty");
		}
		consume(THEN, "Expected '=>' after the condition of the WHILE.");
		consume(NEW_LINE, "Missing 'newline' after '=>' .");

		List<Stmt> body = new ArrayList<>();
		Stmt jumpStatment = null;
		while (!check(RIGHT_PAREN) && !isAtEnd()) {
			if (check(RETURN)) {
				body.add(statement());
				if (!check(RIGHT_PAREN)) {
					throw error(peek(), "'return' must be the last statement in its block.");
				}
				continue;
			}
			if (check(BREAK) || check(CONTINUE)) {
				jumpStatment = jump();
				if (!check(RIGHT_PAREN)) {
					throw error(peek(), "'break' and 'continue' must be the last statement of their block.");
				}
				break;
			}
			body.add(statement());
		}

		if (jumpStatment != null) {
			body.add(jumpStatment);
		}
		/*
		 * if (body.size() == 0) { throw error(peek(),
		 * "Body of 'while' cannot be empty"); }
		 */
		consume(RIGHT_PAREN, "Missing ')', unclosed while block.");
		consume(NEW_LINE, "Missing 'newline' after closing the WHILE block.");

		return new Stmt.While(condition, body);
	}

	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();

		Stmt jumpStatment = null;
		while (!check(RIGHT_PAREN) && !isAtEnd()) {
			if (check(RETURN)) {
				statements.add(statement());
				if (!check(RIGHT_PAREN)) {
					throw error(peek(), "'return' must be the last statement in its block.");
				}
				continue;
			}
			if (check(BREAK) || check(CONTINUE)) {
				jumpStatment = jump();
				if (!check(RIGHT_PAREN)) {
					throw error(peek(), "'break' and 'continue' must be the last statement of their block.");
				}
				break;
			}
			statements.add(statement());
		}

		if (jumpStatment != null) {
			statements.add(jumpStatment);
		}
		if (statements.size() == 0) {
			throw error(peek(), "Body of block cannot be empty");
		}

		consume(RIGHT_PAREN, "Missing ')', unclosed code block.");
		consume(NEW_LINE, "Missing 'newline' after closing the block.");
		return statements;
	}

	private Stmt varDeclaration() {
		if (match(INT)) {
			Token name = consume(IDENTIFIER, "Missing a variable name.");
			Expr initializer = null;
			if (match(ASSIGN)) {
				initializer = expression();
			}
			consume(NEW_LINE, "Missing 'newline' after variable declaration.");
			return new Stmt.Var(name, initializer, NUMBER_INTEGER);
		}
		/*
		 * Commented because we want timers to be declared only globally if
		 * (match(TIMER)) { Token name = consume(IDENTIFIER,
		 * "Missing a variable name."); Expr initializer = null; if (match(ASSIGN)) {
		 * Barracks.printParserError(name,
		 * "Cannot assign a value to a timer directly, use the proper timer related commands."
		 * ); } consume(NEW_LINE, "Missing 'newline' after variable declaration.");
		 * return new Stmt.Var(name, initializer, TIMER_OBJECT); }
		 */
		if (match(POINT)) {
			Token name = consume(IDENTIFIER, "Missing a variable name.");
			Expr initializer = null;
			if (match(ASSIGN)) {
				initializer = expression();
			}
			consume(NEW_LINE, "Missing 'newline' after variable declaration.");
			return new Stmt.Var(name, initializer, TokenType.POINT_OBJECT);
		}
		if (match(TIMER)) {
			throw error(peek(), "Timers can only be defined globally, not inside blocks.");
		}
		// unreachable
		return null;
	}

	private Stmt statement() {
		try {
			if (match(DISABLE_SELF))
				return disableSelfStatement();
			if (match(PRINT))
				return printStatement();
			if (match(RETURN)) {
				return returnStatement();
			}
			if (check(LEFT_PAREN) && !thereIsACommandNameAfterCurrentPar() && match(LEFT_PAREN)) {
				if (match(IF)) {
					consume(NEW_LINE, "Missing 'newline' after the 'IF' keyword.");
					return ifStatement();
				}
				if (match(WHILE)) {
					consume(NEW_LINE, "Missing 'newline' after the 'WHILE' keyword.");
					return whileStatement();
				}
				consume(NEW_LINE, "Missing 'newline' after opening new block.");
				return new Stmt.Block(block());
			}
			return oneLiner();

		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}

	private Stmt returnStatement() {
		Token returnKeywordToken = previous();
		Expr expr = null;
		if (match(NEW_LINE)) {
			return new Stmt.Return(returnKeywordToken, expr);
		}
		expr = term();
		consume(NEW_LINE, "Expected a 'newline' after 'return' statement.");
		return new Stmt.Return(returnKeywordToken, expr);
	}

	private Stmt jump() {
		if (match(BREAK)) {
			consume(NEW_LINE, "Expected a 'newline' after 'break' statement.");
			return new Stmt.Break(previous());
		}
		if (match(CONTINUE)) {
			consume(NEW_LINE, "Expected a 'newline' after 'continue' statement.");
			return new Stmt.Continue(previous());
		}
		return null;
	}

	boolean ComingFromAllowedInLogicalGrouping = false;

	private Stmt oneLiner() {
		if (existingType()) {
			return varDeclaration();
		}
		if (weArePointingAtOpeningParenthesisOfALogicalStatement()) {
			List<Stmt> allowedInLogicalGrouping = new ArrayList<>();
			Token operator = null;
			advance();
			boolean ComingFromAllowedInLogicalGroupingAtStart = ComingFromAllowedInLogicalGrouping;
			ComingFromAllowedInLogicalGrouping = true;
			if (check(AND)) {
				operator = parseVaridicalAnd(allowedInLogicalGrouping);
			} else if (check(OR)) {
				operator = parseVaridicalOr(allowedInLogicalGrouping);
			} else if (check(NOT)) {
				operator = parseNot(allowedInLogicalGrouping);
			}
			ComingFromAllowedInLogicalGrouping = ComingFromAllowedInLogicalGroupingAtStart;
			return new Stmt.LogicalGrouping(operator, allowedInLogicalGrouping);
		}
		return expressionStatement();
	}

	private Stmt allowedInLogicalGrouping() {
		if (weArePointingAtOpeningParenthesisOfALogicalStatement()) {
			List<Stmt> allowedInLogicalGrouping = new ArrayList<>();
			Token operator = null;
			advance();
			if (check(AND)) {
				operator = parseVaridicalAnd(allowedInLogicalGrouping);
			} else if (check(OR)) {
				operator = parseVaridicalOr(allowedInLogicalGrouping);
			} else if (check(NOT)) {
				operator = parseNot(allowedInLogicalGrouping);
			}

			return new Stmt.LogicalGrouping(operator, allowedInLogicalGrouping);
		}
		Expr expr = equality();
		consume(NEW_LINE, "I expected a 'newline' before it.");
		return new Stmt.Expression(expr);
	}

	private Stmt printStatement() {
		Expr value = expression();
		consume(NEW_LINE, "I require a newline after the value.");
		return new Stmt.Print(value);
	}

	private Stmt disableSelfStatement() {
		consume(NEW_LINE, "Expected a 'newline' after 'disable-self' statement.");
		return new Stmt.DisableSelf(previous());
	}

	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(NEW_LINE, "I expected a 'newline' before it.");
		return new Stmt.Expression(expr);
	}

	private Expr expression() {
		return assignment(); // because an expression could first be an assignment
	}

	private Stmt paramAssignmentStatement() {
		Token alias = previous();
		if (!match(ASSIGN)) {
			throw error(peek(), "Missing ':='.");
		}
		Token actualParameterName = consume(IDENTIFIER,
				"Missing the actual Parameter to associate to '" + alias.lexeme + "'.");
		consume(NEW_LINE, "Missing 'newline' after parameter alias definition.");
		return new Stmt.ParamAssignment(alias, actualParameterName);
	}

	private Expr assignment() {
		Expr expr = equality(); // because it's surely at least an equality, and maybe then an assignment

		if (match(ASSIGN)) {
			Token assign = previous();
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value);

			} else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get) expr;
				return new Expr.Set(get.object, get.name, value);
			}

			error(assign, "Invalid assignment target.");
		}

		return expr;
	}

	private Expr equality() {
		Expr expr = comparison();

		while (!(weAreInsideCommandPer) && match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr comparison() {
		Expr expr = term();
		// if we are inside a command and the last term was a point we cannot enter the
		// while! it's not a comparision of points but two different points
		// additionally let us ban the internal comparisons directly inside of
		// commands... our boolean doesnt really work yet and also it's not really
		// useful at all inside commands......if it ever becomes let's come up with
		// conventions to allow it
		while (!(weAreInsideCommandPer) && match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr term() {
		Expr expr = factor();

		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr factor() {
		Expr expr = unary();

		while (match(SLASH, STAR, MODULO, ROUNDED_DIVISION)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr unary() {
		if (match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return propertyCall();
	}

	private Expr propertyCall() {
		Expr expr = functionCall();

		while (true) {
			if (match(TokenType.DOT)) {
				Token name = consume(IDENTIFIER, "Expected something after '.' (no spaces).");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}

		return expr; // either we return the functionCall expression or the get expression if there
						// was a
						// dot,
		// which contains the functionCall and the identifier of the property
	}

	private Expr functionCall() {
		if (match(TokenType.FUNCTION_KEYWORD)) {
			Token functionName = previous();
			List<Expr> arguments = new ArrayList<>();
			if (!check(RIGHT_PAREN)) {
				do {
					arguments.add(term());
				} while (match(COMMA));
			}
			Token closingParenthesis = consume(RIGHT_PAREN, "Missing closing parenthesis ')' of the function.");
			return new Expr.functionCallExpression(functionName, arguments, closingParenthesis);
		}

		return primary(); // either we return the primary expression or the functionCall expression if
							// there was a
							// function call,
		// which contains the function name and arguments
	}

	boolean weAreInsideCommandPer = false;

	private Expr primary() {
		if (match(FALSE))
			return new Expr.Literal(false, BOOLEAN);
		if (match(TRUE))
			return new Expr.Literal(true, BOOLEAN);
		if (match(NIL))
			return new Expr.Literal(null, previous().type);
		if (match(NUMBER_INTEGER, NUMBER_FLOAT, STRING)) {
			return new Expr.Literal(previous().literal, previous().type);
		}

		if (match(LESS)) { // identifies a point literal
			boolean ComingFromAllowedInLogicalGroupingAtStart = ComingFromAllowedInLogicalGrouping;
			ComingFromAllowedInLogicalGrouping = false; // to be sure we dont declare shit in points
			Expr firstArgumentOfPoint = term();
			consume(COMMA, "Expected separating ',' inside the point.");
			Expr secondArgumentOfPoint = term();
			consume(GREATER, "Expected closing '>' for the point.");
			ArrayList<Expr> termsOfPoint = new ArrayList<Expr>();
			termsOfPoint.add(firstArgumentOfPoint);
			termsOfPoint.add(secondArgumentOfPoint);
			ComingFromAllowedInLogicalGrouping = ComingFromAllowedInLogicalGroupingAtStart;
			return new Expr.Literal(termsOfPoint, TokenType.POINT_OBJECT);
		}

		if (match(IDENTIFIER)) {
			if (previous().lexeme.equals("GREATER_EQUAL") || previous().lexeme.equals("GREATER")
					|| previous().lexeme.equals("LESS_EQUAL") || previous().lexeme.equals("LESS")
					|| previous().lexeme.equals("EQUAL_EQUAL") || previous().lexeme.equals("BANG_EQUAL")) {
				throw error(previous(), "Forbidden word '" + previous().lexeme + "'. Use another word.");
			}
			return new Expr.Variable(previous());
		}

		if (match(LEFT_PAREN)) {
			if (pointingAtCommandName()) {
				boolean weAreInsideCommandPerAtStart = weAreInsideCommandPer;
				weAreInsideCommandPer = true;
				advance();
				Token commandName = previous();

				List<Expr> commandParameters = new ArrayList<Expr>();
				boolean ComingFromAllowedInLogicalGroupingAtStart = ComingFromAllowedInLogicalGrouping;
				ComingFromAllowedInLogicalGrouping = true; // to only take the equality path if we meet a grouping
															// paranthesis, we dont want declarations inside commands
															// haha
				while (!isAtEnd() && commandParameters
						.size() < commandsContainer.getCommandOfNanem(commandName.lexeme).commandParameters.size()
						&& !check(RIGHT_PAREN)) {
					if (commandParameters.size() < commandsContainer.getCommandOfNanem(commandName.lexeme)
							.getCommandParametersListWithoutParemetersNotRequiredFromBarracksProgrammer().size()
							&& commandsContainer.getCommandOfNanem(commandName.lexeme)
									.getCommandParametersListWithoutParemetersNotRequiredFromBarracksProgrammer()
									.get(commandParameters.size()).equals("compareOp")
							&& match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, EQUAL_EQUAL, BANG_EQUAL)) {
						commandParameters.add(new Expr.Literal(null, previous().type));
					}
					commandParameters.add(equality());
				}
				ComingFromAllowedInLogicalGrouping = ComingFromAllowedInLogicalGroupingAtStart;
				weAreInsideCommandPer = weAreInsideCommandPerAtStart;

				int howManyParamsRequiredFromBarracksUser = commandsContainer.getCommandOfNanem(commandName.lexeme)
						.howManyParamsRequiredFromBarracksUser();
				if (commandParameters.size() > howManyParamsRequiredFromBarracksUser) {
					throw error(commandName, "Command has too many parameters.");
				} else if (commandParameters.size() < howManyParamsRequiredFromBarracksUser
						&& !(commandName.lexeme.equals("up-get-fact") || commandName.lexeme.equals("up-get-fact-max")
								|| commandName.lexeme.equals("up-get-fact-min")
								|| commandName.lexeme.equals("up-get-fact-sum")
								|| commandName.lexeme.equals("up-get-focus-fact")
								|| commandName.lexeme.equals("up-get-player-fact")
								|| commandName.lexeme.equals("up-get-target-fact"))) {
					throw error(commandName, "Command is missing "
							+ (howManyParamsRequiredFromBarracksUser - commandParameters.size()) + " parameters.");
				}
				if ((commandName.lexeme.equals("up-get-fact") || commandName.lexeme.equals("up-get-fact-max")
						|| commandName.lexeme.equals("up-get-fact-min") || commandName.lexeme.equals("up-get-fact-sum")
						|| commandName.lexeme.equals("up-get-focus-fact")
						|| commandName.lexeme.equals("up-get-player-fact")
						|| commandName.lexeme.equals("up-get-target-fact"))) {
					if (commandParameters.size() < howManyParamsRequiredFromBarracksUser - 1) {
						throw error(commandName,
								"Command is missing "
										+ (howManyParamsRequiredFromBarracksUser - 1 - commandParameters.size())
										+ " parameters.");
						// because we at elast give error of missing params if there's not even one
						// param.
						// if there's at least one param, the compiler can catch it if it's not a factid
						// in the position where it should be
						// it instead there is and it's the factid, then it knows how to proceed
					}
				}

				consume(RIGHT_PAREN, "Expected closing ')' after command.");

				return new Expr.CommandExpression(commandName, commandParameters,
						commandsContainer.getCommandOfNanem(commandName.lexeme));
			}

			Expr expr;
			if (!ComingFromAllowedInLogicalGrouping) {
				expr = expression();
			} else {
				expr = equality();
			}
			consume(RIGHT_PAREN, "Expected closing ')' after expression.");
			return new Expr.Grouping(expr);
		}

		throw error(peek(), "Missing some expression here, or attempting something illegal.");
	}

	// ----------------------------------------------------------------------------------------------

	private boolean pointingAtCommandName() {
		return commandsContainer.isACommandName(peek().lexeme);
	}

	private boolean thereIsACommandNameAfterCurrentPar() {
		for (CommandPer command : commandsContainer.commandsArray) {
			if (peekNext().lexeme.equals(command.commandName)) {
				return true;
			}
		}
		return false;
	}

	private List<Stmt> parseElseBranch(List<Stmt> elseBranch) {
		if (check(LEFT_PAREN) && checkNext(ELSE)) {
			advance();
			match(ELSE);
			consume(NEW_LINE, "Missing 'newline' after the ELSE keyword.");
			elseBranch = new ArrayList<>();
			Stmt jumpStatment = null;
			while (!check(RIGHT_PAREN) && !isAtEnd()) {
				if (check(RETURN)) {
					elseBranch.add(statement());
					if (!check(RIGHT_PAREN)) {
						throw error(peek(), "'return' must be the last statement in its block.");
					}
					continue;
				}
				if (check(BREAK) || check(CONTINUE)) {
					jumpStatment = jump();
					if (!check(RIGHT_PAREN)) {
						throw error(peek(), "'break' and 'continue' must be the last statement of their block.");
					}
					break;
				}
				elseBranch.add(statement());
			}

			if (jumpStatment != null) {
				elseBranch.add(jumpStatment);
			}
			if (elseBranch.size() == 0) {
				throw error(peek(), "Body of 'else' cannot be empty");
			}
			consume(RIGHT_PAREN, "Missing ')', unclosed ELSE block.");
			consume(NEW_LINE, "Missing 'newline' after closing the ELSE block.");
		}
		return elseBranch;
	}

	private Stmt.If parseElifBranch(Stmt.If elifBranch) {

		if (check(LEFT_PAREN) && checkNext(ELIF)) {
			advance();
			match(ELIF);
			consume(NEW_LINE, "Missing 'newline' after the 'elif' keyword.");
			return (Stmt.If) ifStatement();
		}
		return elifBranch;
	}

	private Token parseNot(List<Stmt> oneLiners) {
		Token operator;
		operator = consume(NOT, "Missing 'not'.");
		consume(NEW_LINE, "Missing 'newline' after 'not'.");
		while (!check(RIGHT_PAREN) && !isAtEnd()) {
			oneLiners.add(allowedInLogicalGrouping());
		}
		if (oneLiners.size() != 1) {
			throw error(peek(), "A (not...) has to have exactley one argument, I counted " + oneLiners.size() + ".");
		}
		consume(RIGHT_PAREN, "Missing ')', unclosed (not...).");
		consume(NEW_LINE, "Missing 'newline' after closing the (not...).");
		return operator;
	}

	private Token parseVaridicalOr(List<Stmt> oneLiners) {
		Token operator;
		operator = consume(OR, "Missing 'or'.");
		consume(NEW_LINE, "Missing 'newline' after 'or'.");
		while (!check(RIGHT_PAREN) && !isAtEnd()) {
			oneLiners.add(allowedInLogicalGrouping());
		}
		if (oneLiners.size() < 2) {
			throw error(peek(), "Too few statements for an (or...).");
		}
		consume(RIGHT_PAREN, "Missing ')', unclosed (or...).");
		consume(NEW_LINE, "Missing 'newline' after closing the (or...).");
		return operator;
	}

	private Token parseVaridicalAnd(List<Stmt> oneLiners) {
		Token operator;
		operator = consume(AND, "Missing 'and'.");
		consume(NEW_LINE, "Missing 'newline' after 'and'.");
		while (!check(RIGHT_PAREN) && !isAtEnd()) {
			oneLiners.add(allowedInLogicalGrouping());
		}
		if (oneLiners.size() < 2) {
			throw error(peek(), "Too few statements for an (and...).");
		}
		consume(RIGHT_PAREN, "Missing ')', unclosed (and...).");
		consume(NEW_LINE, "Missing 'newline' after closing the (and...).");
		return operator;
	}

	private boolean weArePointingAtOpeningParenthesisOfALogicalStatement() {
		return check(LEFT_PAREN) && (checkNext(AND) || checkNext(OR) || checkNext(NOT));
	}

	// ----------------------------------------------------------------------------------------------

	private Token consume(TokenType type, String message) {
		if (check(type))
			return advance();

		throw error(peek(), message);
	}

	private class ParseError extends RuntimeException {
	} // a costum error type to catch and eventually call synchronize() on to parse
		// more code regardless

	private ParseError error(Token token, String message) {
		Barracks.printParserError(token, message);
		return new ParseError();
	}

	private void synchronize() {
		advance(); // to skip the current faulty

		while (!isAtEnd()) {
			if (previous().type == NEW_LINE)
				return; // since NEW_LINE TOKENS surely separates statements, it means a new healthy
						// statement
			advance();
		}
	}

	// ----------------------------------------------------------------------------------------------

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}

		return false;
	}

	private boolean check(TokenType type) {
		if (isAtEnd())
			return false;
		return peek().type == type;
	}

	private boolean checkNext(TokenType type) {
		if (isAtEnd())
			return false;
		return peekNext().type == type;
	}

	private Token advance() {
		if (!isAtEnd())
			current++;
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == EOF;
	}

	private Token peek() {
		return tokens.get(current);
	}

	private Token peekNext() {
		return tokens.get(current + 1);
	}

	private Token previous() {
		return tokens.get(current - 1);
	}

}
