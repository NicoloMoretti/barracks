package com.fategrinder.barracks;

import static com.fategrinder.barracks.TokenType.BOOLEAN;
import static com.fategrinder.barracks.TokenType.NOT_INITIALZIED;
import static com.fategrinder.barracks.TokenType.INITIALIZED;
import static com.fategrinder.barracks.TokenType.INT;
import static com.fategrinder.barracks.TokenType.NUMBER_INTEGER;
import static com.fategrinder.barracks.TokenType.POINT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fategrinder.barracks.Barracks.CompiletimeError;
import com.fategrinder.barracks.Environment.VariableInfo;
import com.fategrinder.barracks.Expr.Call;
import com.fategrinder.barracks.Expr.CommandExpression;
import com.fategrinder.barracks.Expr.Get;
import com.fategrinder.barracks.Expr.Set;
import com.fategrinder.barracks.Expr.functionCallExpression;
import com.fategrinder.barracks.Stmt.Break;
import com.fategrinder.barracks.Stmt.Const;
import com.fategrinder.barracks.Stmt.Continue;
import com.fategrinder.barracks.Stmt.DisableSelf;
import com.fategrinder.barracks.Stmt.FunctionDefinition;
import com.fategrinder.barracks.Stmt.LoadIf;
import com.fategrinder.barracks.Stmt.LogicalGrouping;
import com.fategrinder.barracks.Stmt.ParamAssignment;
import com.fategrinder.barracks.Stmt.ParamInitialization;
import com.fategrinder.barracks.Stmt.Return;
import com.fategrinder.barracks.Stmt.While;

public class Compiler implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	private static List<String> outputPER = new ArrayList<>();
	private static List<String> functionsBodiesPER = new ArrayList<>(); // function bodies are added on top to avoid
																		// loaded rules ids issues with jumps
	private CommandsContainer commandsContainer = new CommandsContainer();
	ParameterClassesContainer parametersContainer = new ParameterClassesContainer();
	DeclaredFunctionsInfo declaredFunctionsInfo = new DeclaredFunctionsInfo();

	private Environment environment = new Environment();
	private int goalIdBlockNestingDepthStack = 1;
	private int goalIdCallStack = 1001;
	private final int goalIdUsedToSplitDefrules = 2001;
	private final int goalIdTemporary = 2002;
	private final int goalIdPointerCallStack = 2003;
	private final int goalIdPointerBlockNestingDepthStack = 2004;
	private int goalIdWithBlockNestingDepth = 2005;
	private final int goalIdForElseBlocks = 2006;
	private int goalIdHowManyWorkRegistersToPushOnStack = 2007;
	private int goalIdTemporaryWorkRegistersPointer = 2008;
	private int goalIdLoopIPushAndPop = 2009;
	private int goalIdPointerVarsStack = 2010;
	private int goalIdFunctionToJumpToAfterPushId = 2011;
	private int goalIdHowManyGoalsToCopyForInputParameters = 2012;
	private int goalIdWorkRegistersPointerForCopyingParameters = 2013;
	private int returnValue1 = 2014;
	private int returnValue2 = 2015;
	private int goalIdStart = 2016 - 1; // the value of goalID at the start of the next statement, we will use this+1
	// there are also some goals allocated on the vars stack at start in background

	private final int ruleIdPush = 1;
	private final int ruleIdPop = 6;

	private int goalIdCurrent = goalIdStart;
	private int goalIdCurrentHighestValueEver = goalIdCurrent + 1;

	public void setGoalIdCurrent(int goalIdCurrent) {
		this.goalIdCurrent = goalIdCurrent;
		goalIdCurrentHighestValueEver = Math.max(goalIdCurrentHighestValueEver, goalIdCurrent);
	}

	private int currentNestingDepth = -1; // -1 because it's default initialization in .per
	private int timerId = 0;

	private int goalIdNumberOfFirstWorkRegister;

	private boolean someoneHasToOpenDefrule = true; // sometimes rules close themselves, sometimes not
	private boolean someoneHasToCloseDefrule = false;

	private boolean youHaveAnIfFather = false;
	private boolean firstSon = true; // only first son has to modify it's father
	private Integer ifSeedIndex = null; // to know where to modify father

	private boolean weAreInACondition = false; // temporary easy fix until bodies will accept booleans too
	private boolean weAreInLogicalGrouping = false;
	private static Map<Integer, List<ATranslatedExpressionStatement>> groupsOfperLinesToPutInLogicalGrouping = new HashMap<>(); // this
																																// is
																																// where
	// we
	// temporarily put
	// the codes to
	// build logical
	// grouping trees
	// off of.
	private int nestedLogicalFatherDepth = 0; // to navigate groupsOfperLinesToPutInLogicalGrouping
	private TokenType myLogicalFatherOperatorType = null;// to compare logical grouping type with it's father to see if
															// same type to decide if merging or not
	private boolean lastTranslationWasALogicTree = false;// to know how much code to extract, one line or a whole bunch
	private static List<String> trashToPutOnTopOFMainLogicTreeFather;

	public String elaborateFinalOutputPER() {
		postProcessRules(); // because if there are errors and we do it it breaks
		// java, but we never print
		// if there are errors, so here it's fine, we also only print once :)
		String result = String.join("", outputPER); // because it's currently a list of strings for every line of code
		result = DefaultConstants.PrintDefaultPerConstants() + result;
		return result;
	}

	int searchLocalTotalId;
	boolean isGhostGlobal; // so we can distinguish global variables that have been defined separately in
							// the very first compiling pass, so that we can throw error even if they are
							// defined, because they are defined BELOW. We need to do an extra fist pass for
							// them so that they are placed IMMEDIETLY in their final spots in the stack,
							// because a function may surprisingly call another that uses them EARLIER than
							// they appear in the main

	void compile(List<Stmt> statements) {
		environment.fillGlobalEnvironmentWithAllSN();
		environment.fillGlobalEnvironmentWithAllParameters(); // first fill with params and then commands because
																// commands can be params thanks to up-get-fact, so they
																// overwrite some, but it's ok, params has a support
																// structure for those
		environment.fillGlobalEnvironmentWithAllCommands();
		environment.fillGlobalEnvironmentWithAllLoadIfSymbols();
		searchLocalTotalId = environment.addSearchStatesVariablesToGlobalEnv(goalIdStart);
		declaredFunctionsInfo.declareNativeFunctions();
		goalIdStart += 4; // because moving the id start is like allocating space for 4 variables

		List<Stmt> statementsToCompileFirst = new ArrayList<Stmt>();
		for (int i = 0; i < statements.size(); i++) {
			if (statements.get(i) instanceof Stmt.Var) {
				// only global vars need reserved space on the goalArrayId, locations should be
				// detrmined at start
				Stmt stmt = statements.get(i);
				statementsToCompileFirst.add(stmt);
			}
		}

		isGhostGlobal = true;
		compileMainBlock(statementsToCompileFirst);
		isGhostGlobal = false;

		goalIdNumberOfFirstWorkRegister = goalIdStart + 1;

		for (int i = 0; i < statements.size(); i++) {
			if (statements.get(i) instanceof Stmt.FunctionDefinition) {
				Stmt stmt = statements.get(i);
				try {
					visitFunctionDefinitionStmtHeader((Stmt.FunctionDefinition) stmt);
				} catch (CompiletimeError error) {
					Barracks.compiletimeError(error);
				}
			}
		}
		compileMainBlock(statements);
		int highestAmountOfWorkRegistersEverInUseAtOnce = goalIdCurrentHighestValueEver
				- goalIdNumberOfFirstWorkRegister + 24; // +4 for the start search-list allocations + 20 extra to be
														// safe
		addPushAndPopBackgroundFunctions();
		outputPER.add(0, "$functionsDefinitionsOver\n");
		outputPER.addAll(0, functionsBodiesPER);
		List<String> firstDefrule = new ArrayList<String>(Arrays.asList("(defrule\n", "(true)\n", "=>\n",
				"(up-modify-goal " + goalIdPointerCallStack + " c:= " + (goalIdCallStack - 1) + ")\n",
				"(up-modify-goal " + goalIdPointerBlockNestingDepthStack + " c:= " + (goalIdBlockNestingDepthStack)
						+ ")\n",
				"(up-modify-goal " + goalIdPointerVarsStack + " c:= " + highestAmountOfWorkRegistersEverInUseAtOnce
						+ ")\n",
				"$skipOverAllFunctionsDefinitions\n", ")\n"));
		outputPER.addAll(0, firstDefrule);
	}

	void compileMainBlock(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				translate(statement);
			}
			closeDefruleIfNeeded();
		} catch (CompiletimeError error) {
			Barracks.compiletimeError(error);
		}

	}

	private void translate(Stmt stmt) {
		if (!weAreInLogicalGrouping) {
			setGoalIdCurrent(goalIdStart); // Not resetting goals inside conditions allows us to extract logic
		}
		stmt.accept(this);
	}

	// --------------------------------------------------------------------------------------------------------

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {

		openNewDefruleIfNeeded();
		ATranslatedExpressionStatement statement = evaluate(stmt.expression);
		addTranslatedStringToOutput(statement.getTranslatedCode());
		addTranslatedStringToOutput("(up-chat-data-to-self \"° %d\" g: " + statement.id + ")\n");
		someoneHasToCloseDefrule = true;
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		Environment previous = this.environment;
		try {
			this.environment = new Environment(environment);
			compileWhileStmt(stmt.condition, stmt.body);
		} finally {
			this.environment = previous;
		}
		return null;
	}

	int amountOfrulesToJumpBackWhile = 0;
	boolean weAreInALoop = false;
	boolean weHaveJustExitedALoopForDefconst = false;

	void compileWhileStmt(List<Stmt> head, List<Stmt> body) {
		try {
			boolean weAreInALoopAtStart = weAreInALoop;
			weAreInALoop = true;
			int goalIdAtStartOfBlock = goalIdStart;
			int nestingIdCurrentDepthAtStartOfBlock = currentNestingDepth;
			boolean youHaveAnIfFatherAtStartOfBlock = youHaveAnIfFather;

			int amountOfrulesToJumpBackWhileAtStart = amountOfrulesToJumpBackWhile;

			setupNewIfBlock();
			addTranslatedStringToOutput("$start_while\n"); // for post processing to modify jump if it splits loop
			someoneHasToOpenDefrule = false;
			weAreInACondition = true;
			nestingDepthOfMyAssociatedWhile = currentNestingDepth; // for break and continue
			int IndexWhereToAddTrash = outputPER.size();
			for (Stmt statement : head) {
				trashToPutOnTopOFMainLogicTreeFather = new ArrayList<String>();
				IndexWhereToAddTrash = outputPER.size();
				translate(statement);
				outputPER.addAll(IndexWhereToAddTrash, trashToPutOnTopOFMainLogicTreeFather);
			}
			currentNestingDepth++;
			addTranslatedStringToOutput("(up-modify-goal " + goalIdWithBlockNestingDepth + " c:+ 1)\n");
			weAreInACondition = false;

			addTranslatedStringToOutput("=>\n");
			youHaveAnIfFather = true;
			someoneHasToOpenDefrule = false;
			someoneHasToCloseDefrule = true; // because the => already happened, who wants to close, can
			firstSon = true; // my first born will be first son, else if i have no children, my else could
			IsFirstStatementInABlock = true;
			for (Stmt statement : body) {
				translate(statement);
				IsFirstStatementInABlock = false;
			}

			openNewDefruleIfNeeded();
			currentNestingDepth--;
			if (!thisBlockEndedWithAJump) {
				addTranslatedStringToOutput("(up-modify-goal " + goalIdWithBlockNestingDepth + " c:- 1)\n");

			}
			thisBlockEndedWithAJump = false;

			firstSon = false; // my brothers will not be my sons and neither my father's else

			if (someoneHasToOpenDefrule && !weJustJumped) {
				closeDefruleIfNeeded();
				if (outputPER.get(outputPER.size() - 1) != ")\n") {
					addTranslatedStringToOutput(")\n");
				}
				openNewDefruleIfNeeded(); // in case a while ends with an inner while it will create an extra defrule as
											// needed
			}
			youHaveAnIfFather = youHaveAnIfFatherAtStartOfBlock; // if I'm an IF who lives in main root, once closed, my
			// brother will be too.
			// If not, he won't be too.

			if (!weJustJumped) {
				addTranslatedStringToOutput(outputPER.size(),
						"$while(up-jump-rule " + "calculateInPostProcess" + ")\n");
			} else {
				addTranslatedStringToOutput(outputPER.size(), "(removedEndingJump)\n");
			}

			weJustJumped = false;
			closeDefruleIfNeeded();
			if (outputPER.get(outputPER.size() - 1) != ")\n") {
				addTranslatedStringToOutput(")\n");
			}

			someoneHasToOpenDefrule = true;

			goalIdStart = goalIdAtStartOfBlock;
			currentNestingDepth = nestingIdCurrentDepthAtStartOfBlock; // leaves nesting depth unchanged for brothers
			weAreInALoop = weAreInALoopAtStart;
			weHaveJustExitedALoopForDefconst = true;
			weJustJumped = false;
		} catch (CompiletimeError error) {
			Barracks.compiletimeError(error);
		}
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {

		Environment previous = this.environment;
		try {
			this.environment = new Environment(environment);
			compileIfBranch(stmt.condition, stmt.thenBranch, stmt.elifBranch, stmt.elseBranch);
		} finally {
			this.environment = previous;
			weHaveJustExitedALoopForDefconst = false;
			weJustJumped = false;
		}
		return null;
	}

	private boolean weHaveExaustedTheElifStreak = true; // to know when we are in elif father to efficiently reset depth
														// in one-shot when we close elif father

	void compileIfBranch(List<Stmt> head, List<Stmt> body, Stmt.If elif, List<Stmt> elseBody) {
		try {
			int goalIdAtStartOfBlock = goalIdStart;
			int nestingIdCurrentDepthAtStartOfBlock = currentNestingDepth;
			boolean youHaveAnIfFatherAtStartOfBlock = youHaveAnIfFather;

			if (elseBody != null || elif != null) {
				setupNewIfBlockWithElseLogicToo();
			} else {
				setupNewIfBlock();
			}
			someoneHasToOpenDefrule = false;
			weAreInACondition = true;
			int IndexWhereToAddTrash = outputPER.size();
			for (Stmt statement : head) {
				trashToPutOnTopOFMainLogicTreeFather = new ArrayList<String>();
				IndexWhereToAddTrash = outputPER.size();
				translate(statement);
				outputPER.addAll(IndexWhereToAddTrash, trashToPutOnTopOFMainLogicTreeFather);
			}
			currentNestingDepth++;
			addTranslatedStringToOutput("(up-modify-goal " + goalIdWithBlockNestingDepth + " c:+ 1)\n");
			weAreInACondition = false;

			addTranslatedStringToOutput("=>\n");
			youHaveAnIfFather = true;
			someoneHasToOpenDefrule = false;
			someoneHasToCloseDefrule = true; // because the => already happened, who wants to close, can
			firstSon = true; // my first born will be first son, else if i have no children, my else could
			IsFirstStatementInABlock = true;
			for (Stmt statement : body) {
				translate(statement);
				IsFirstStatementInABlock = false;
			}

			openNewDefruleIfNeeded();
			if (!thisBlockEndedWithAJump) {
				if (elseBody != null || elif != null) {
					addTranslatedStringToOutput("(up-modify-goal " + goalIdForElseBlocks + " c:= -1)\n");
				}

				currentNestingDepth--;
				addTranslatedStringToOutput("(up-modify-goal " + goalIdWithBlockNestingDepth + " c:- 1)\n");
			} else {
				currentNestingDepth--;

			}
			thisBlockEndedWithAJump = false;

			if (elif != null) {
				boolean weHaveExaustedTheElifStreakAtStart = weHaveExaustedTheElifStreak;
				weHaveExaustedTheElifStreak = false;

				setupNewElseBlock();

				addTranslatedStringToOutput("=>\n");
				youHaveAnIfFather = true;
				someoneHasToOpenDefrule = false;
				someoneHasToCloseDefrule = true; // because the => already happened, who wants to close, can
				firstSon = true; // my first born will be first son

				compileIfBranch(elif.condition, elif.thenBranch, elif.elifBranch, elif.elseBranch);

				weHaveExaustedTheElifStreak = weHaveExaustedTheElifStreakAtStart;
				if (weHaveExaustedTheElifStreak) {
					openNewDefruleIfNeededClosingElifStreak();
					currentNestingDepth--;
					addTranslatedStringToOutput(
							"(up-modify-goal " + goalIdWithBlockNestingDepth + " c:= " + currentNestingDepth + ")\n");
				}
			}

			if (elseBody != null) {
				setupNewElseBlock();

				addTranslatedStringToOutput("=>\n");
				youHaveAnIfFather = true;
				someoneHasToOpenDefrule = false;
				someoneHasToCloseDefrule = true; // because the => already happened, who wants to close, can
				firstSon = true; // my first born will be first son
				IsFirstStatementInABlock = true;
				for (Stmt statement : elseBody) {
					translate(statement);
					IsFirstStatementInABlock = false;
				}

				openNewDefruleIfNeeded();
				currentNestingDepth--;
				if (!thisBlockEndedWithAJump) {
					addTranslatedStringToOutput("(up-modify-goal " + goalIdWithBlockNestingDepth + " c:- 1)\n");
				}
				thisBlockEndedWithAJump = false;
			}
			firstSon = false; // my brothers will not be my sons and neither my father's else

			youHaveAnIfFather = youHaveAnIfFatherAtStartOfBlock; // if I'm an IF who lives in main root, once closed, my
																	// brother will be too.
																	// If not, he won't be too.
			closeDefruleIfNeeded();
			if (!youHaveAnIfFather && outputPER.get(outputPER.size() - 1) != ")\n") {
				addTranslatedStringToOutput(")\n");
			}
			someoneHasToOpenDefrule = true;

			goalIdStart = goalIdAtStartOfBlock;
			currentNestingDepth = nestingIdCurrentDepthAtStartOfBlock; // leaves nesting depth unchanged for brothers
		} catch (CompiletimeError error) {
			Barracks.compiletimeError(error);
		}
	}

	private void openNewDefruleIfNeededClosingElifStreak() {
		if (someoneHasToOpenDefrule) {
			addTranslatedStringToOutput("(defrule\n");
			amountOfrulesToJumpBackWhile += 1;
			if (youHaveAnIfFather) {
				addTranslatedStringToOutput(
						"(up-compare-goal " + goalIdWithBlockNestingDepth + " >= " + currentNestingDepth + ")\n");
			} else {
				addTranslatedStringToOutput("(true)\n");
			}
			addTranslatedStringToOutput("=>\n");
			someoneHasToOpenDefrule = false;
		}
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public Void visitLogicalGroupingStmt(LogicalGrouping stmt) {
		weAreInLogicalGrouping = true;
		TokenType myLogicalFatherOperatorTypeAtStartOfStatement = myLogicalFatherOperatorType; // for my brothers
		nestedLogicalFatherDepth++;

		myLogicalFatherOperatorType = stmt.operator.type; // for my children
		buildNestedLogicalStatementsTranslations(stmt); // to accumulate extra code on top, and the one needed to build
														// trees in a separate array

		if (doIHaveToCreateNewLogicalTree(stmt, myLogicalFatherOperatorTypeAtStartOfStatement)) { // only a
																									// logicalfather
																									// builds the
																									// condition
																									// tree. The
																									// code to put
																									// outside
			// logicalgrouping is already left outside on top
			switch (stmt.operator.type) {
			case AND:
				addTranslatedStringToOutput("(and\n");
				buildsLogicalNestedTreeTranslationStructure("and");
				break;
			case OR:
				addTranslatedStringToOutput("(or\n");
				buildsLogicalNestedTreeTranslationStructure("or");
				break;
			case NOT:
				addTranslatedStringToOutput("(not\n");
				// translate(stmt.operands.get(0));
				buildsLogicalNestedTreeTranslationStructure("not");
				addTranslatedStringToOutput(")\n");
				break;
			}
		} else {
			groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth - 1)
					.addAll(groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth));
			// because some father will have to print my stuff into his tree eventually
		}

		nestedLogicalFatherDepth--;
		myLogicalFatherOperatorType = myLogicalFatherOperatorTypeAtStartOfStatement; // for my brothers
		lastTranslationWasALogicTree = true;
		weAreInLogicalGrouping = false;
		return null;
	}

	private boolean doIHaveToCreateNewLogicalTree(LogicalGrouping stmt,
			TokenType myLogicalFatherOperatorTypeAtStartOfStatement) {
		if (myLogicalFatherOperatorTypeAtStartOfStatement == stmt.operator.type
				&& !(stmt.operator.type == TokenType.NOT)) { // because not doesnt ever merge with children
			return false;
		} else {
			return true;
		}
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		Environment previous = this.environment;
		int goalIdAtStartOfBlock = goalIdStart;
		try {
			this.environment = new Environment(environment);
			if (stmt.statements.get(0) instanceof Stmt.DisableSelf) {
				Stmt trueStatement = new Stmt.Expression(new Expr.Literal(true, BOOLEAN));
				List<Stmt> trueHead = new ArrayList<Stmt>();
				trueHead.add(trueStatement);
				compileIfBranch(trueHead, stmt.statements, null, null);
			} else {
				boolean iOpenedNewRule = openNewDefruleIfNeeded();
				int translatedSizeAtStart = outputPER.size();
				IsFirstStatementInABlock = true;
				for (Stmt statement : stmt.statements) {
					translate(statement);
					IsFirstStatementInABlock = false;
				}

				if (translatedSizeAtStart == outputPER.size() && iOpenedNewRule) {
					outputPER.add("(do-nothing)\n"); // to not leave it empty invalid
				}

				if (!outputPER.get(outputPER.size() - 1).equals(")\n")) {
					someoneHasToCloseDefrule = true;
				} // because this is too hard otherwise:
				/*
				 * (
				 * 
				 * (if 1 => 1 )
				 * 
				 * 1 ;try adding/removing!!!!!!! )
				 */
			}

		} catch (CompiletimeError error) {
			Barracks.compiletimeError(error);
		} finally {
			this.environment = previous;
			goalIdStart = goalIdAtStartOfBlock;
			weHaveJustExitedALoopForDefconst = false;
			weJustJumped = false;
		}
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		addTranslatedStringToOutput(evaluate(stmt.expression).getTranslatedCode());
		return null;
	}

	private ATranslatedExpressionStatement evaluate(Expr expr) {
		openNewDefruleIfNeeded();
		// someoneHasToCloseDefrule = true; // because otherwise blocks finishing inside
		// with closure of if/extra
		// expression after after the if, dont know if they should close or not
		return (ATranslatedExpressionStatement) expr.accept(this);
	}

	@Override
	public ATranslatedExpressionStatement visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		TokenType initializationStatus = NOT_INITIALZIED;
		TokenType variable_type = stmt.type;
		ATranslatedExpressionStatement value = null;
		if (stmt.initializer != null) {
			openNewDefruleIfNeeded();
			value = evaluate(stmt.initializer);
			if (value.type != variable_type) {
				throw new CompiletimeError(stmt.name, "Type mismatch in assignment. Trying to assign '" + value.type
						+ "' to '" + variable_type + "'.");
			}
			someoneHasToCloseDefrule = true;
			addTranslatedStringToOutput(value.getTranslatedCode());
			initializationStatus = INITIALIZED;
		}
		if (variable_type == TokenType.TIMER_OBJECT) {
			initializationStatus = INITIALIZED;
		}

		if (environment.isDefined(stmt.name) && environment.get(stmt.name).isConst) {
			throw new CompiletimeError(stmt.name,
					"Cannot create the variable '" + stmt.name.lexeme + "', the name is already used for a constant.");
		}
		if (environment.isDefined(stmt.name) && environment.get(stmt.name).isSN) {
			throw new CompiletimeError(stmt.name, "Cannot create the variable '" + stmt.name.lexeme
					+ "', the name is already used for a Strategic Number.");
		}
		if (environment.isDefined(stmt.name) && environment.get(stmt.name).isParam) {
			throw new CompiletimeError(stmt.name,
					"Cannot create the variable '" + stmt.name.lexeme + "', the name is already used for a Parameter.");
		}
		if (environment.isDefined(stmt.name) && environment.get(stmt.name).isCommand) {
			throw new CompiletimeError(stmt.name,
					"Cannot create the variable '" + stmt.name.lexeme + "', the name is already used for a Command.");
		}
		if (environment.isDefined(stmt.name) && environment.get(stmt.name).isReadOnly) {
			throw new CompiletimeError(stmt.name, "Cannot create the variable '" + stmt.name.lexeme
					+ "', the name is already used for a variable implemented by default.");
		}

		if (environment.isDefinedExactelyHere(stmt.name)) {
			if (environment.get(stmt.name).isGhostGlobal) {
				environment.get(stmt.name).isGhostGlobal = false;
				return null;
			}
			throw new CompiletimeError(stmt.name,
					"The variable '" + stmt.name.lexeme + "' is already defined at this EXACT scope level.");
		}

		if (variable_type == TokenType.TIMER_OBJECT) {
			timerId++;
			if (timerId > 50) {
				throw new CompiletimeError(stmt.name, "Cannot declare more than 50 different timers '"
						+ stmt.name.lexeme + "' is the " + timerId + "th.");
			}
			int myId = timerId;
			environment.define(stmt.name.lexeme, myId, variable_type, false, initializationStatus, isGhostGlobal);
			return null;
		}

		int myId = goalIdStart + 1;
		goalIdStart++;
		if (variable_type == TokenType.POINT_OBJECT) {
			goalIdStart++;
		}
		environment.define(stmt.name.lexeme, myId, variable_type, false, initializationStatus, isGhostGlobal);
		return null;
	}

	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		if (environment.isDefined(expr.name)) {
			if (environment.get(expr.name).isGhostGlobal) {
				throw new CompiletimeError(expr.name, "Global variable '" + expr.name.lexeme
						+ "' should be defined BEFORE the first usage mention in the code, for clarity reasons.\nIt's definition is currently placed somewhere below in the file, move it up.");
			}

			if (environment.get(expr.name).isConst) {
				throw new CompiletimeError(expr.name,
						"Cannot change the value of the constant '" + expr.name.lexeme + "'.");
			}
			if (environment.get(expr.name).isReadOnly) {
				throw new CompiletimeError(expr.name, "Cannot change the value of the variable '" + expr.name.lexeme
						+ "' directly. If possible use the appropriate command.");
			}
			openNewDefruleIfNeeded();
			ATranslatedExpressionStatement value = evaluate(expr.value);

			if (environment.get(expr.name).type != value.type) {
				throw new CompiletimeError(expr.name, "Type mismatch in assignment. Trying to assign '"
						+ environment.get(expr.name).type + "' to '" + value.type + "'.");
			}
			if (environment.get(expr.name).type == TokenType.TIMER_OBJECT) {
				throw new CompiletimeError(expr.name, "Cannot assign timers to other timers.");
			}

			someoneHasToCloseDefrule = true;
			environment.get(expr.name).type = value.type;
			environment.get(expr.name).initializationStatus = INITIALIZED;
			int myId = environment.get(expr.name).id;

			if (value.type == TokenType.POINT_OBJECT) {
				List<String> translationToReturn = value
						.appendAndRetrieve("(up-modify-goal " + myId + " g:= " + value.id + ")\n");
				translationToReturn.add("(up-modify-goal " + (myId + 1) + " g:= " + (value.id + 1) + ")\n");
				return new ATranslatedExpressionStatement(TokenType.POINT_OBJECT, translationToReturn, value.id);
			}
			if (environment.get(expr.name).isSN) {
				return new ATranslatedExpressionStatement(TokenType.NUMBER_INTEGER,
						value.appendAndRetrieve("(up-modify-sn " + expr.name.lexeme + " g:= " + value.id + ")\n"),
						value.id);
			}
			if (environment.get(expr.name).isParam) {
				throw new CompiletimeError(expr.name,
						"Cannot assign parameters inside a block.\nShould be done either in global scope or in a #load-if.");
			}
			if (environment.get(expr.name).isCommand) {
				throw new CompiletimeError(expr.name, "Cannot assign a value to a command.");
			}
			return new ATranslatedExpressionStatement(value.type,
					value.appendAndRetrieve("(up-modify-goal " + myId + " g:= " + value.id + ")\n"), value.id);
		}

		throw new CompiletimeError(expr.name, "Undefined variable '" + expr.name.lexeme + "'.");
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public ATranslatedExpressionStatement visitBinaryExpr(Expr.Binary expr) {
		ATranslatedExpressionStatement left = evaluate(expr.left);
		ATranslatedExpressionStatement right = evaluate(expr.right);
		switch (expr.operator.type) {
		case MINUS:
			return buildBinaryArithmeticTranlsatedExpression(expr, left, right, "-", true);
		case PLUS:
			return buildBinaryArithmeticTranlsatedExpression(expr, left, right, "+", true);
		case SLASH:
			return buildBinaryArithmeticTranlsatedExpression(expr, left, right, "/", true);
		case ROUNDED_DIVISION:
			return buildBinaryArithmeticTranlsatedExpression(expr, left, right, "~/", true);
		case MODULO:
			return buildBinaryArithmeticTranlsatedExpression(expr, left, right, "mod", false);
		case STAR:
			return buildBinaryArithmeticTranlsatedExpression(expr, left, right, "*", true);
		case GREATER:
			return buildComparisonTranlsatedExpression(expr, left, right, ">", false);
		case GREATER_EQUAL:
			return buildComparisonTranlsatedExpression(expr, left, right, ">=", false);
		case LESS:
			return buildComparisonTranlsatedExpression(expr, left, right, "<", false);
		case LESS_EQUAL:
			return buildComparisonTranlsatedExpression(expr, left, right, "<=", false);
		case EQUAL_EQUAL:
			return buildComparisonTranlsatedExpression(expr, left, right, "==", true);
		case BANG_EQUAL:
			return buildComparisonTranlsatedExpression(expr, left, right, "!=", true);
		}
		// Unreachable.
		return null;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public ATranslatedExpressionStatement visitUnaryExpr(Expr.Unary expr) {
		ATranslatedExpressionStatement right = evaluate(expr.right);

		switch (expr.operator.type) {
		case MINUS:
			if (right.type == TokenType.POINT_OBJECT) {
				List<String> translation = right.appendAndRetrieve("(up-modify-goal " + right.id + " c:* -1)\n");
				translation.add("(up-modify-goal " + (right.id + 1) + " c:* -1)\n");
				return new ATranslatedExpressionStatement(TokenType.POINT_OBJECT, translation, right.id);
			}
			checkNumberOperand(expr.operator, right);
			return new ATranslatedExpressionStatement(NUMBER_INTEGER,
					right.appendAndRetrieve("(up-modify-goal " + right.id + " c:* -1)\n"), right.id);
		// case BANG:
		// return !isTruthy(right);
		}
		// Unreachable.
		return null;
	}

	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		setGoalIdCurrent(goalIdCurrent + 1);
		int myId = goalIdCurrent;
		if (environment.get(expr.name).isGhostGlobal) {
			throw new CompiletimeError(expr.name, "Global variable '" + expr.name.lexeme
					+ "' should be defined BEFORE the first usage mention in the code, for clarity reasons.\nIt's definition is currently placed somewhere below in the file, move it up.");
		}
		if (environment.get(expr.name).initializationStatus != TokenType.INITIALIZED && !weAreInAFunctionDefinition) {
			throw new CompiletimeError(expr.name,
					"The variable '" + expr.name.lexeme + "' is being used without an initial value.");
		}

		if (environment.get(expr.name).isConst) {
			if (environment.get(expr.name).type == TokenType.STRING) {
				ATranslatedExpressionStatement returnedString = new ATranslatedExpressionStatement(
						environment.get(expr.name).type, "", -100);
				return returnedString;
			}
			return new ATranslatedExpressionStatement(environment.get(expr.name).type,
					"(up-modify-goal " + myId + " c:= " + expr.name.lexeme + ")\n", myId);
		}

		if (environment.get(expr.name).type == TokenType.POINT_OBJECT) {
			List<String> translationToReturn = new ArrayList<String>();
			translationToReturn.add("(up-modify-goal " + myId + " g:= " + environment.get(expr.name).id + ")\n");

			setGoalIdCurrent(goalIdCurrent + 1); // so that nobody will overwrite the y coordinate but we keep my id to
													// the x one
			// to keep everything similar to scalar integer math
			translationToReturn
					.add("(up-modify-goal " + (myId + 1) + " g:= " + (environment.get(expr.name).id + 1) + ")\n");
			return new ATranslatedExpressionStatement(environment.get(expr.name).type, translationToReturn, myId);
		}

		if (environment.get(expr.name).isSN) {
			return new ATranslatedExpressionStatement(environment.get(expr.name).type,
					"(up-modify-goal " + myId + " s:= " + expr.name.lexeme + ")\n", myId);
		}
		if (environment.get(expr.name).isParam) {
			return new ATranslatedExpressionStatement(environment.get(expr.name).type, "", myId);
		}

		if (environment.get(expr.name).isCommand) {
			throw new CompiletimeError(expr.name, "A command, like '" + expr.name.lexeme
					+ "', cannot be resolved to a value like that.\nCommands need to be enveloped in parenthesis.");
		}

		return new ATranslatedExpressionStatement(environment.get(expr.name).type,
				"(up-modify-goal " + myId + " g:= " + environment.get(expr.name).id + ")\n", myId);
	}

	@Override
	public ATranslatedExpressionStatement visitLiteralExpr(Expr.Literal expr) {
		if (expr.value instanceof Boolean) {
			// if we are not in a condition trhow error
			if (!weAreInACondition) {
				throw new CompiletimeError(null,
						"Cannot use boolean 'true'/'false' inside a body, only in conditions.");
			}
			if ((boolean) expr.value) {
				return new ATranslatedExpressionStatement(expr.type, "(true)\n", null);
			}
			return new ATranslatedExpressionStatement(expr.type, "(false)\n", null);
		}

		int myId;

		if (expr.type == TokenType.POINT_OBJECT) {
			myId = goalIdCurrent;
			ATranslatedExpressionStatement x = evaluate(((ArrayList<Expr>) expr.value).get(0));
			List<String> translatedDirectPoint = x.getTranslatedCode();
			setGoalIdCurrent(myId + 1);
			myId = goalIdCurrent;
			translatedDirectPoint.addAll(evaluate(((ArrayList<Expr>) expr.value).get(1)).getTranslatedCode());
			return new ATranslatedExpressionStatement(TokenType.POINT_OBJECT, translatedDirectPoint, myId);
		}

		if (expr.type == TokenType.STRING) {
			ATranslatedExpressionStatement returnedString = new ATranslatedExpressionStatement(expr.type, "", -100);
			returnedString.inlineStringValue = "\"" + expr.value + "\"";
			return returnedString;
		}
		setGoalIdCurrent(goalIdCurrent + 1);
		myId = goalIdCurrent;
		ATranslatedExpressionStatement integerIntoGoalTranslation = new ATranslatedExpressionStatement(expr.type,
				"(up-modify-goal " + myId + " c:= " + expr.value + ")\n", myId);
		integerIntoGoalTranslation.inlineIntValue = (Integer) expr.value;
		return integerIntoGoalTranslation;
	}

	boolean itWasAPointComparison = false;

	private void buildNestedLogicalStatementsTranslations(LogicalGrouping stmt) {

		groupsOfperLinesToPutInLogicalGrouping.put(nestedLogicalFatherDepth, new ArrayList<>());
		int startCycleIndex; // to be confronted with it's value later to know how many .per lines have been
								// added per translation
		for (int i = 0; i < stmt.operands.size(); i++) {
			startCycleIndex = outputPER.size() - 1;
			lastTranslationWasALogicTree = false;
			translate(stmt.operands.get(i));

			if (!addedZeroOneLinerStatements(startCycleIndex)) {// otherwise there's nothign to add
				// this is to do only if it was not a whole nested logic tree, if it was i
				// should pop and add the whole tree!!
				if (!lastTranslationWasALogicTree) {
					if (!itWasAPointComparison) {
						String lastTranslatedOneliner = popOutputPER();
						trashToPutOnTopOFMainLogicTreeFather
								.addAll(outputPER.subList(startCycleIndex + 1, outputPER.size()));
						outputPER = outputPER.subList(0, Math.max(0, startCycleIndex + 1));
						groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth)
								.add(new ATranslatedExpressionStatement(null, lastTranslatedOneliner, null));
					} else {
						itWasAPointComparison = false;
						if (myLogicalFatherOperatorType == TokenType.NOT) {
							throw new CompiletimeError(stmt.operator,
									"Please negate a point comparison directly using != or == without the NOT.");
						}
						String lastTranslatedOneliner = popOutputPER();
						groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth)
								.add(new ATranslatedExpressionStatement(null, lastTranslatedOneliner, null));
						lastTranslatedOneliner = popOutputPER();
						groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth)
								.add(new ATranslatedExpressionStatement(null, lastTranslatedOneliner, null));

						trashToPutOnTopOFMainLogicTreeFather
								.addAll(outputPER.subList(startCycleIndex + 1, outputPER.size()));
						outputPER = outputPER.subList(0, Math.max(0, startCycleIndex + 1));

					}
				} else {
					List<String> lastTranslatedOneliner = outputPER.subList(startCycleIndex + 1, outputPER.size());
					outputPER = outputPER.subList(0, Math.max(0, startCycleIndex + 1));
					trashToPutOnTopOFMainLogicTreeFather
							.addAll(outputPER.subList(startCycleIndex + 1, outputPER.size()));
					outputPER = outputPER.subList(0, Math.max(0, startCycleIndex + 1));
					groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth)
							.add(new ATranslatedExpressionStatement(null, lastTranslatedOneliner, null));
				}
			}
		}
	}

	private void buildsLogicalNestedTreeTranslationStructure(String operator) {

		for (int i = 0; i < groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth).size(); i++) {
			addTranslatedStringToOutput(
					groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth).get(i).getTranslatedCode());
			if (i < groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth).size() - 2) {
				addTranslatedStringToOutput("(" + operator + "\n");
			}
		}

		for (int i = 0; i < groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth).size() - 1; i++) {
			addTranslatedStringToOutput(")\n");
		}

		groupsOfperLinesToPutInLogicalGrouping.get(nestedLogicalFatherDepth).clear(); // so others dont find it filled
																						// with code
	}

	private String popOutputPER() {
		String lastTranslatedOneliner = outputPER.get(outputPER.size() - 1);
		outputPER.remove(outputPER.size() - 1);
		return lastTranslatedOneliner;
	}

	private boolean addedZeroOneLinerStatements(int startCycleIndex) {
		return startCycleIndex == (outputPER.size() - 1);
	}

	private void setupNewIfBlock() {
		closeDefruleIfNeeded();
		addTranslatedStringToOutput("(defrule\n");// because no rule opens a defrule head for another
		amountOfrulesToJumpBackWhile += 1;
		if (youHaveAnIfFather) {
			addTranslatedStringToOutput(
					"(up-compare-goal " + goalIdWithBlockNestingDepth + " == " + currentNestingDepth + ")\n");
		}
	}

	private void setupNewIfBlockWithElseLogicToo() {
		closeDefruleIfNeeded();
		addTranslatedStringToOutput("(defrule\n");// because no rule opens a defrule head for another
		amountOfrulesToJumpBackWhile += 1;
		if (youHaveAnIfFather) {
			addTranslatedStringToOutput(
					"(up-compare-goal " + goalIdWithBlockNestingDepth + " == " + currentNestingDepth + ")\n");
		}
		addTranslatedStringToOutput("(up-modify-goal " + goalIdForElseBlocks + " c:= 0)\n");
	}

	private void setupNewElseBlock() {
		closeDefruleIfNeeded();
		addTranslatedStringToOutput("(defrule\n");// because no rule opens a defrule head for another
		amountOfrulesToJumpBackWhile += 1;
		if (youHaveAnIfFather) {
			addTranslatedStringToOutput(
					"(up-compare-goal " + goalIdWithBlockNestingDepth + " == " + currentNestingDepth + ")\n");
			addTranslatedStringToOutput("(up-compare-goal " + goalIdForElseBlocks + " == 0)\n");
		}
		addTranslatedStringToOutput("(up-modify-goal " + goalIdForElseBlocks + " c:= -1)\n");
		currentNestingDepth++;
		addTranslatedStringToOutput("(up-modify-goal " + goalIdWithBlockNestingDepth + " c:+ 1)\n");
	}

	private void modifyMyNestedFatherIfImFirstSon() {
		if (firstSon) {
			outputPER.add(ifSeedIndex, "(up-modify-goal " + currentNestingDepth + " c:= 0)\n");
			addTranslatedStringToOutput("(up-modify-goal " + currentNestingDepth + " c:= 1)\n");
			firstSon = false;
		}
	}

	private void throwErrorIfNestedTooDeep() {
		if (goalIdWithBlockNestingDepth >= 1001) {
			throw new CompiletimeError(null, "Exceeded maximum nesting depth of " + 1000
					+ " for 'IF' or 'WHILE' blocks, please reorganize the code.\nIf after careful considerations you still belive you are correct, contact me and I'll see to it.");
		}
	}

	private void closeDefruleIfNeeded() {
		if (someoneHasToCloseDefrule) {
			addTranslatedStringToOutput(")\n");
			someoneHasToCloseDefrule = false;
		}
	}

	private ATranslatedExpressionStatement buildBinaryArithmeticTranlsatedExpression(Expr.Binary expr,
			ATranslatedExpressionStatement left, ATranslatedExpressionStatement right, String operator,
			boolean pointsMathMakesSense) {

		if (operator == "+" || operator == "-") {
			if (left.type == TokenType.POINT_OBJECT || right.type == TokenType.POINT_OBJECT) {
				checkPointOperands(expr.operator, left, right);
				List<String> translation = (right.mergeTranslatedExpressionStatements(left));
				translation.add("(up-modify-goal " + left.id + " g:" + operator + " " + right.id + ")\n");
				translation.add("(up-modify-goal " + (left.id + 1) + " g:" + operator + " " + (right.id + 1) + ")\n");
				return new ATranslatedExpressionStatement(TokenType.POINT_OBJECT, translation, left.id);
			} else {
				checkNumberOperands(expr.operator, left, right);
				List<String> translation = (right.mergeTranslatedExpressionStatements(left));
				translation.add("(up-modify-goal " + left.id + " g:" + operator + " " + right.id + ")\n");
				return new ATranslatedExpressionStatement(NUMBER_INTEGER, translation, left.id);
			}
		} else if (operator == "*" || operator == "/" || operator == "~/") {
			if ((operator == "/" || operator == "~/") && right.type == TokenType.POINT_OBJECT) {
				throw new CompiletimeError(expr.operator, "A point can only be divided, not be a divisor.");
			}
			if (operator == "/") {
				operator = "z/";
			} else if (operator == "~/") {
				operator = "/";
			}

			if (left.type == TokenType.POINT_OBJECT || right.type == TokenType.POINT_OBJECT) {
				if (left.type == NUMBER_INTEGER || right.type == NUMBER_INTEGER) {

					ATranslatedExpressionStatement point = left;
					ATranslatedExpressionStatement scalar = right;
					if (right.type == TokenType.POINT_OBJECT) {
						point = right;
						scalar = left;
					}

					// fix it by pretty printing correclty elavraging ont he fact fact you know
					// whats scalar and whats point
					List<String> translation = (right.mergeTranslatedExpressionStatements(left));

					translation.add("(up-modify-goal " + point.id + " g:" + operator + " " + scalar.id + ")\n");
					translation.add("(up-modify-goal " + (point.id + 1) + " g:" + operator + " " + scalar.id + ")\n");
					return new ATranslatedExpressionStatement(TokenType.POINT_OBJECT, translation, left.id);
				}
				throw new CompiletimeError(expr.operator, "A point can only be multiplied by an integer.");

			} else {
				checkNumberOperands(expr.operator, left, right);
				List<String> translation = (right.mergeTranslatedExpressionStatements(left));
				translation.add("(up-modify-goal " + left.id + " g:" + operator + " " + right.id + ")\n");
				return new ATranslatedExpressionStatement(NUMBER_INTEGER, translation, left.id);
			}
		}

		if (!pointsMathMakesSense) {
			checkNumberOperands(expr.operator, left, right);
			List<String> translation = (right.mergeTranslatedExpressionStatements(left));
			translation.add("(up-modify-goal " + left.id + " g:" + operator + " " + right.id + ")\n");
			return new ATranslatedExpressionStatement(NUMBER_INTEGER, translation, left.id);
		}
		// unreachable
		return null;
	}

	private ATranslatedExpressionStatement buildComparisonTranlsatedExpression(Expr.Binary expr,
			ATranslatedExpressionStatement left, ATranslatedExpressionStatement right, String comparisonString,
			boolean pointsMathMakesSense) {

		if (!weAreInACondition) {
			weAreInACondition = false;
			throw new CompiletimeError(expr.operator, "Comparison is only supported inside conditions.");
		}

		if (left.type == TokenType.POINT_OBJECT || right.type == TokenType.POINT_OBJECT) {
			checkPointOperands(expr.operator, left, right);
			if (!(comparisonString.equals("==") || comparisonString.equals("!="))) {
				throw new CompiletimeError(expr.operator, "Points can be compare only with '!=' or '=='.");
			}
			List<String> translation = (right.mergeTranslatedExpressionStatements(left));
			translation.add("(up-compare-goal " + left.id + " g:" + comparisonString + " " + right.id + ")\n");
			translation
					.add("(up-compare-goal " + (left.id + 1) + " g:" + comparisonString + " " + (right.id + 1) + ")\n");
			itWasAPointComparison = true;
			return new ATranslatedExpressionStatement(BOOLEAN, translation, null);
		} else {
			checkNumberOperands(expr.operator, left, right);
			List<String> translation = (right.mergeTranslatedExpressionStatements(left));
			translation.add("(up-compare-goal " + left.id + " g:" + comparisonString + " " + right.id + ")\n");
			return new ATranslatedExpressionStatement(BOOLEAN, translation, null);
		}
	}

	private boolean openNewDefruleIfNeeded() {
		if (someoneHasToOpenDefrule) {
			addTranslatedStringToOutput("(defrule\n");
			amountOfrulesToJumpBackWhile += 1;
			if (youHaveAnIfFather) {
				addTranslatedStringToOutput(
						"(up-compare-goal " + goalIdWithBlockNestingDepth + " == " + currentNestingDepth + ")\n");
			} else {
				addTranslatedStringToOutput("(true)\n");
			}
			addTranslatedStringToOutput("=>\n");
			someoneHasToOpenDefrule = false;
			return true;
		}
		return false;
	}

	private boolean isTruthy(Object object) {
		if (object == null)
			return false;
		if (object instanceof Boolean)
			return (boolean) object;
		return true;
	}

	private void checkNumberOperand(Token operator, ATranslatedExpressionStatement operand) {
		if (operand.type == NUMBER_INTEGER)
			return;
		throw new CompiletimeError(operator, "Operand must be an integer.");
	}

	private void checkNumberOperands(Token operator, ATranslatedExpressionStatement left,
			ATranslatedExpressionStatement right) {
		if (left.type == NUMBER_INTEGER && right.type == NUMBER_INTEGER)
			return;

		throw new CompiletimeError(operator, "Operands must be integers.");
	}

	private void checkPointOperands(Token operator, ATranslatedExpressionStatement left,
			ATranslatedExpressionStatement right) {
		if (left.type == TokenType.POINT_OBJECT && right.type == TokenType.POINT_OBJECT)
			return;

		throw new CompiletimeError(operator, "Operands must both be points.");
	}

	private static void addTranslatedStringToOutput(String text) { // for when we directly add a string
		outputPER.add(text);
	}

	private static void addTranslatedStringToOutput(int index, String text) { // for when we directly add a string
		outputPER.add(index, text);
	}

	private static void addTranslatedStringToOutput(List<String> texts) { // for when we unload
																			// ATranslatedExpressionStatement
		for (String text : texts) {
			outputPER.add(text);
		}
	}

	@Override
	public Void visitConstStmt(Const stmt) {
		TokenType variable_type = stmt.value.type; // the type of the value of a const is implied by the value put in it
													// itself
		Object value = stmt.value.value;

		closeDefruleIfNeeded();
		if (weHaveJustExitedALoopForDefconst) {
			addTranslatedStringToOutput("(defrule\n");
			addTranslatedStringToOutput("(false)\n");
			addTranslatedStringToOutput("=>\n");
			addTranslatedStringToOutput("(do-nothing)\n");
			addTranslatedStringToOutput(")\n");
			weHaveJustExitedALoopForDefconst = false;
		}

		if (variable_type == TokenType.STRING) {
			addTranslatedStringToOutput("(defconst " + stmt.name.lexeme + " \"" + value + "\")\n");
		} else {
			if (variable_type == TokenType.NUMBER_INTEGER && value instanceof Integer
					&& ((int) value < -32768 || (int) value > 32768)) {
				throw new CompiletimeError(stmt.name,
						"Constants in .per are limited between -32768 and 32767. Use a variable if you need a greater range.");
			}
			addTranslatedStringToOutput("(defconst " + stmt.name.lexeme + " " + value + ")\n");
		}
		someoneHasToOpenDefrule = true;

		if (environment.isDefined(stmt.name) /* && !environment.get(stmt.name).isConst */) {
			String forWhat = "";
			if (environment.get(stmt.name).isCommand)
				forWhat = "command";
			else if (environment.get(stmt.name).isParam)
				forWhat = "parameter";
			else if (environment.get(stmt.name).isSN)
				forWhat = "strategic number";
			else if (environment.get(stmt.name).isConst)
				forWhat = "previously defined constant";
			else if (environment.get(stmt.name).isReadOnly)
				forWhat = "native variable";
			else
				forWhat = "variable";
			throw new CompiletimeError(stmt.name, "The name '" + stmt.name.lexeme + "' is already used for a " + forWhat
					+ ".\nConstants cannot change value.");
		}
		environment.define(stmt.name.lexeme, -100, variable_type, true, INITIALIZED); // random impossible id, it's
																						// not a variable

		// anyway
		return null;
	}

	int totalRulesGenerated = 0;

	List<String> normalizedOutputPER = new ArrayList<>();

	int codeLinesCounterHeadAlreadyAdded = 0;

	int codeLinesCounterBodyAlreadyAdded = -1;

	boolean insideADefrule = false;

	boolean nowProcessingHead = true;

	List<String> supportLogicGroupingList = new ArrayList<>();

	boolean weArePostProcessingInsideLogicGroup = false;

	int layersOfLogicalGroupingToShreadOff = 0; // it's important to have it and not simply use only logicalGroupSize,

	// because if we close a nested logic group we shed layers but not size!
	int logicalGroupSize = 0;

	LinkedList<Integer> whileQueuePostProcessing = new LinkedList<>();

	LinkedList<LinkedList<Integer>> listOfBreaksForEachWhileGroupWithLineIndex = new LinkedList<>();

	class BreakInfo {
		public BreakInfo(int myLine) {
			this.myLine = myLine;
		}

		int myLine;
		int howManyDefrulesHaveBeenOpenedSince = 0;
	}

	LinkedList<LinkedList<BreakInfo>> allBreaksOfOpenWhiles = new LinkedList<LinkedList<BreakInfo>>();

	void increaseAllBreaksOfOpenWhiles() {
		for (LinkedList<BreakInfo> breaksList : allBreaksOfOpenWhiles) {
			for (BreakInfo breakInfo : breaksList) {
				breakInfo.howManyDefrulesHaveBeenOpenedSince++;
			}
		}
	}

	Map<String, Integer> functionsBodiesIds = new HashMap<String, Integer>();

	int indexOfSkipOverAllFunctionsDefinitionsString;

	private final int maxCommandsPerDefrule = 32 - 3; // 32 is the max, 3 is the amount of extra lines added when
														// maxCommandsPerDefrule is reached in a defrule

	private void postProcessRules() {

		List<String> OutputPerWithoutEmptyStrings = new ArrayList<>();
		for (int i = 0; i < outputPER.size(); i++) {
			if (!outputPER.get(i).equals("")) {
				OutputPerWithoutEmptyStrings.add(outputPER.get(i));
			}
		}
		outputPER = OutputPerWithoutEmptyStrings;

		for (int i = 1; i < outputPER.size(); i++) {
			if (outputPER.get(i).equals("(defrule\n") || outputPER.get(i).startsWith("#load-if-")
					|| outputPER.get(i).startsWith("#else") || outputPER.get(i).startsWith("(defconst")
					|| outputPER.get(i).equals("#end-if\n") || outputPER.get(i).equals("$functionsDefinitionsOver\n")) {
				if (!outputPER.get(i - 1).equals(")\n") && !outputPER.get(i - 1).startsWith("#load-if-")
						&& !outputPER.get(i - 1).startsWith("#else") && !outputPER.get(i - 1).equals("#end-if\n")
						&& !outputPER.get(i - 1).startsWith("(defconst")
						&& !outputPER.get(i - 1).equals("$functionsDefinitionsOver\n")) {
					outputPER.add(i, ")\n"); // easy bug fix for missing closing ) in translations
				}
			}
		}

		for (String line : outputPER) {
			if (line.equals("$start_while\n")) {
				whileQueuePostProcessing.addLast(1);
				listOfBreaksForEachWhileGroupWithLineIndex.add(new LinkedList<Integer>());
				allBreaksOfOpenWhiles.add(new LinkedList<BreakInfo>());
				continue;
			}

			if (line.startsWith("(removedEndingJump)\n")) {
				/*
				 * if (!listOfBreaksForEachWhileGroupWithLineIndex.isEmpty()) { for (Integer
				 * breakingLine : listOfBreaksForEachWhileGroupWithLineIndex.getLast()) {
				 * normalizedOutputPER.set(breakingLine, "(up-jump-direct c: " +
				 * (totalRulesGenerated + 1) + ")\n"); }
				 * listOfBreaksForEachWhileGroupWithLineIndex.removeLast();
				 * whileQueuePostProcessing.removeLast(); // because it's closed and that's what
				 * we do when we close a // rule normaly with the jump, but since there's the
				 * break // it doesnt get detected, but we still need to do it }
				 */
				if (!allBreaksOfOpenWhiles.isEmpty()) { // this happens whenever a while closes
					for (BreakInfo breakInfo : allBreaksOfOpenWhiles.getLast()) {
						normalizedOutputPER.set(breakInfo.myLine,
								"(up-jump-rule " + breakInfo.howManyDefrulesHaveBeenOpenedSince + ")\n");
					}
					allBreaksOfOpenWhiles.removeLast();
					whileQueuePostProcessing.removeLast();
				}
				continue;
			}

			if (line.startsWith("$iAm:")) {
				functionsBodiesIds.put(line.substring("$iAm:".length()), totalRulesGenerated);
				line = "(do-nothing)\n";
			}

			if (line.equals("$skipOverAllFunctionsDefinitions\n")) {
				indexOfSkipOverAllFunctionsDefinitionsString = normalizedOutputPER.size();
			}

			if (line.equals("$functionsDefinitionsOver\n")) {
				normalizedOutputPER.set(indexOfSkipOverAllFunctionsDefinitionsString,
						"(up-jump-direct c: " + totalRulesGenerated + ")\n");
				continue;
			}

			if (nowProcessingHead) {

				if (line.equals("(defrule\n")) {
					codeLinesCounterHeadAlreadyAdded = -1; // because it will be increased to 1 later, but we want it to
															// 0 still
					// after opening defrule
					insideADefrule = true;
				}

				if (line.equals("(and\n") || line.equals("(or\n") || line.equals("(not\n")) {
					if (!weArePostProcessingInsideLogicGroup) {
						logicalGroupSize = 0; // to know if it's illegally big, start at 0 because we increase it 1
												// below
						weArePostProcessingInsideLogicGroup = true;
					}
					layersOfLogicalGroupingToShreadOff += 1;
				}

				if (!line.equals(")\n")) { // we assume that unsplittalbe logic blocks close with that string
					if (weArePostProcessingInsideLogicGroup) {
						logicalGroupSize++;
					} else {
						codeLinesCounterHeadAlreadyAdded++;
					}
				}
				if (line.equals(")\n") && weArePostProcessingInsideLogicGroup) {
					layersOfLogicalGroupingToShreadOff -= 1;
					if (layersOfLogicalGroupingToShreadOff == 0) {
						weArePostProcessingInsideLogicGroup = false;

						dumpLogicGrouping();
					}
				}
				if (line.equals("=>\n")) {
					codeLinesCounterHeadAlreadyAdded -= 1; // because it's been increased but it shouldn't count
					nowProcessingHead = false;
				} else {
					insertAndSplitDefruleHead(line);
				}
			}
			if (!nowProcessingHead) { // !! don't change and turn it into an else, it's like this on purpose!
				codeLinesCounterBodyAlreadyAdded++;

				if (line.equals(")\n")) {
					codeLinesCounterBodyAlreadyAdded = 0;
					nowProcessingHead = true; // surely it closes the body
					totalRulesGenerated++;
					insideADefrule = false;
					normalizedOutputPER.add(line);
				} else {
					insertAndSplitDefruleBody(line);
				}
			}
		}

		outputPER = normalizedOutputPER;

		for (int i = 1; i < outputPER.size(); i++) {
			if (outputPER.get(i).startsWith("(up-jump")) {
				while (outputPER.get(i + 1).startsWith("(up-jump")) {
					outputPER.remove(i + 1);
				}
			}
		}

		for (int i = 1; i < outputPER.size(); i++) {
			if (functionsBodiesIds.containsKey(outputPER.get(i))) {
				outputPER.set(i, "(up-modify-goal " + goalIdFunctionToJumpToAfterPushId + " c:= "
						+ functionsBodiesIds.get(outputPER.get(i)) + ")\n");
			}
		}

		for (int i = 1; i < outputPER.size(); i++) {
			if (outputPER.get(i).startsWith("=>")) {
				if (outputPER.get(i + 1).startsWith(")")) {
					outputPER.add(i + 1, "(do-nothing)\n");
				}
			}
		}

		System.out.println("; Total rules generated : " + totalRulesGenerated + "/10000\n\n");
		outputPER.add(0, "; Total rules generated : " + totalRulesGenerated + "/10000\n\n");

	}

	private String fixLoopKeywordsStatementsAfterTheRuleCountHasBeenUpdateBySomeoneElse(String line) {
		if (line.startsWith("$while(up-jump-rule")) {
			int jumpAmount = whileQueuePostProcessing.removeLast();
			line = "(up-jump-rule " + -(jumpAmount) + ")\n";

			/*
			 * if (!listOfBreaksForEachWhileGroupWithLineIndex.isEmpty()) { for (Integer
			 * breakingLine : listOfBreaksForEachWhileGroupWithLineIndex.getLast()) {
			 * normalizedOutputPER.set(breakingLine, "(up-jump-direct c: " +
			 * (totalRulesGenerated + 1) + ")\n"); }
			 * listOfBreaksForEachWhileGroupWithLineIndex.removeLast(); }
			 */
			if (!allBreaksOfOpenWhiles.isEmpty()) { // this happens whenever a while closes
				for (BreakInfo breakInfo : allBreaksOfOpenWhiles.getLast()) {
					normalizedOutputPER.set(breakInfo.myLine,
							"(up-jump-rule " + breakInfo.howManyDefrulesHaveBeenOpenedSince + ")\n");
				}
				allBreaksOfOpenWhiles.removeLast();
			}
		}

		if (line.equals("continue\n")) {
			int jumpAmount = whileQueuePostProcessing.getLast();
			line = "(up-jump-rule " + -(jumpAmount) + ")\n";
		}
		return line;
	}

	private void increaseAmountOfJumpsPostProcessing() {
		increaseAllBreaksOfOpenWhiles();
		for (int i = 0; i < whileQueuePostProcessing.size(); i++) {
			// Get the current Integer value
			Integer currentValue = whileQueuePostProcessing.get(i);

			// Increase the value by 1
			currentValue = currentValue + 1; // Autoboxing creates a new Integer

			// Set the new value back to the list at the same position
			whileQueuePostProcessing.set(i, currentValue);
		}
	}

	private void dumpLogicGrouping() {
		if (logicalGroupSize + codeLinesCounterHeadAlreadyAdded < maxCommandsPerDefrule) {
			normalizedOutputPER.addAll(supportLogicGroupingList);
			supportLogicGroupingList = new ArrayList<>();
			codeLinesCounterHeadAlreadyAdded = logicalGroupSize;
			logicalGroupSize = 0;
		} else {
			if (codeLinesCounterHeadAlreadyAdded > 0) {
				// get rid of those, one new head, and try again
				postProcessingOpenNewDefruleByBreakingHead();
				codeLinesCounterHeadAlreadyAdded = 0;
				codeLinesCounterBodyAlreadyAdded = 0;
				nowProcessingHead = true;
				insideADefrule = true;
				totalRulesGenerated++;
				dumpLogicGrouping(); // to try again after starting a clean rule hoping the condition group will fit
			} else {
				throw new CompiletimeError(null,
						"Found Logic Group (and or not) too big to split defrule's condition in two, please reduce the un-nested logic group that has too many conditions inside of it.");
			}
		}

	}

	private void insertAndSplitDefruleHead(String line) {
		if (insideADefrule) {
			if (codeLinesCounterHeadAlreadyAdded < maxCommandsPerDefrule) {
				if (weArePostProcessingInsideLogicGroup) {
					supportLogicGroupingList.add(line);
				} else {
					normalizedOutputPER.add(line);
				}
				postProcessingOpenNewDefruleRegularly(line);
			} else {
				postProcessingOpenNewDefruleByBreakingHead();

				if (!weArePostProcessingInsideLogicGroup) {
					normalizedOutputPER.add(line);
					codeLinesCounterHeadAlreadyAdded = 1;
				} else {
					supportLogicGroupingList.add(line); // because we are clearly not ready to add it until the group is
														// closed
					logicalGroupSize++;
				}
				codeLinesCounterBodyAlreadyAdded = 0;
				nowProcessingHead = true;
				insideADefrule = true;
				totalRulesGenerated++;
			}
		} else {
			normalizedOutputPER.add(line); // it's neither body nor head, it's a const or something that lives ouside
											// rules
		}
	}

	private void postProcessingOpenNewDefruleByBreakingHead() {
		normalizedOutputPER.add("=>\n");
		normalizedOutputPER.add("(up-modify-goal " + goalIdUsedToSplitDefrules + " c:= 1)\n");
		normalizedOutputPER.add(")\n");
		normalizedOutputPER.add("(defrule\n");
		normalizedOutputPER.add("(up-compare-goal " + goalIdUsedToSplitDefrules + " == 1)\n");
		normalizedOutputPER.add("(up-modify-goal " + goalIdUsedToSplitDefrules + " c:= 0)\n");
		increaseAmountOfJumpsPostProcessing();
	}

	private void postProcessingOpenNewDefruleRegularly(String line) {
		if (line.equals("(defrule\n")) {
			normalizedOutputPER.add("(up-modify-goal " + goalIdUsedToSplitDefrules + " c:= 0)\n");
			increaseAmountOfJumpsPostProcessing();
		}
	}

	private void insertAndSplitDefruleBody(String line) {
		if (codeLinesCounterHeadAlreadyAdded + codeLinesCounterBodyAlreadyAdded < maxCommandsPerDefrule) {
			line = fixLoopKeywordsStatementsAfterTheRuleCountHasBeenUpdateBySomeoneElse(line);
			normalizedOutputPER.add(line);
		} else {
			String upGetRuleId = null; // for atomic splitting in function calls
			if (normalizedOutputPER.get(normalizedOutputPER.size() - 1).startsWith("(up-get-rule-id")) {
				upGetRuleId = normalizedOutputPER.remove(normalizedOutputPER.size() - 1);
			}
			totalRulesGenerated++;
			normalizedOutputPER.add("(up-modify-goal " + goalIdUsedToSplitDefrules + " c:= 1)\n");
			normalizedOutputPER.add(")\n");
			normalizedOutputPER.add("(defrule\n");
			normalizedOutputPER.add("(up-compare-goal " + goalIdUsedToSplitDefrules + " == 1)\n");
			normalizedOutputPER.add("(up-modify-goal " + goalIdUsedToSplitDefrules + " c:= 0)\n");
			normalizedOutputPER.add("=>\n");
			codeLinesCounterBodyAlreadyAdded = 0;
			codeLinesCounterHeadAlreadyAdded = 0;
			increaseAmountOfJumpsPostProcessing();
			line = fixLoopKeywordsStatementsAfterTheRuleCountHasBeenUpdateBySomeoneElse(line);
			if (upGetRuleId != null) {
				normalizedOutputPER.add(upGetRuleId);
				codeLinesCounterBodyAlreadyAdded++;
			}
			normalizedOutputPER.add(line);
		}

		if (line.equals("break\n")) {
			listOfBreaksForEachWhileGroupWithLineIndex.getLast().add(normalizedOutputPER.size() - 1);
			allBreaksOfOpenWhiles.getLast().add(new BreakInfo(normalizedOutputPER.size() - 1));
		}

	}

	boolean weJustJumped = false;
	boolean thisBlockEndedWithAJump = false;
	int nestingDepthOfMyAssociatedWhile = 0;

	@Override
	public Void visitBreakStmt(Break stmt) {
		if (!weAreInALoop) {
			throw new CompiletimeError(stmt.name, "There's no loop to break out of.");
		}
		openNewDefruleIfNeeded();
		weJustJumped = true;
		thisBlockEndedWithAJump = true;
		// in case we jump back here with break or continue, we need to get back to the
		// correct depth even before cheking conditions
		addTranslatedStringToOutput(
				"(up-modify-goal " + goalIdWithBlockNestingDepth + " c:= " + (nestingDepthOfMyAssociatedWhile) + ")\n");
		addTranslatedStringToOutput("break\n");
		return null;
	}

	@Override
	public Void visitContinueStmt(Continue stmt) {
		if (!weAreInALoop) {
			throw new CompiletimeError(stmt.name, "There's no loop to continue over.");
		}
		openNewDefruleIfNeeded();
		weJustJumped = true;
		thisBlockEndedWithAJump = true;
		// in case we jump back here with break or continue, we need to get back to the
		// correct depth even before cheking conditions
		addTranslatedStringToOutput(
				"(up-modify-goal " + goalIdWithBlockNestingDepth + " c:= " + nestingDepthOfMyAssociatedWhile + ")\n");
		addTranslatedStringToOutput("continue\n");
		return null;
	}

	@Override
	public Object visitCallExpr(Call expr) {
		// not implemented in the parser yet
		return null;
	}

	@Override
	public Object visitGetExpr(Get expr) {
		// if the expr calls on a point variable and looks either for x or y property
		// return the goal as if looking
		// up a variable normally
		// else return error

		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// case for functions Exprs too !!!!!!!!!!
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// case for functions Exprs too !!!!!!!!!!
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// case for functions Exprs too !!!!!!!!!!
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// case for functions Exprs too !!!!!!!!!!
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!we should add
		// case for functions Exprs too !!!!!!!!!!

		if (!(expr.object instanceof Expr.Variable) && !(expr.object instanceof Expr.CommandExpression)) {
			throw new CompiletimeError(expr.name, "Only point variables have fields (.x/.y).");
		}

		if (expr.object instanceof Expr.CommandExpression) {
			ATranslatedExpressionStatement compiledCommand = evaluate(expr.object);

			if (!compiledCommand.type.equals(TokenType.POINT_OBJECT)) {
				throw new CompiletimeError(expr.name, "Only point variables have fields (.x/.y).");
			}

			if (!expr.name.lexeme.equals("x") && !expr.name.lexeme.equals("y")) {
				throw new CompiletimeError(expr.name, "Only properties 'x' or 'y exist for points.");
			}

			setGoalIdCurrent(goalIdCurrent + 1);
			int myId = goalIdCurrent;
			ATranslatedExpressionStatement propertyAccess;
			if (expr.name.lexeme.equals("x")) {
				propertyAccess = new ATranslatedExpressionStatement(NUMBER_INTEGER,
						"(up-modify-goal " + myId + " g:= " + compiledCommand.id + ")\n", myId);
			} else {
				propertyAccess = new ATranslatedExpressionStatement(NUMBER_INTEGER,
						"(up-modify-goal " + myId + " g:= " + (compiledCommand.id + 1) + ")\n", myId);
			}

			List<String> translation = (compiledCommand.mergeTranslatedExpressionStatements(propertyAccess));
			return new ATranslatedExpressionStatement(NUMBER_INTEGER, translation, myId);
		}

		Expr.Variable callee = (Expr.Variable) expr.object;

		if (environment.get(callee.name).type != TokenType.POINT_OBJECT) {
			throw new CompiletimeError(expr.name, "Only point variables have fields. '" + callee.name.lexeme + "' is a "
					+ environment.get(callee.name).type + ".");
		}

		if (!expr.name.lexeme.equals("x") && !expr.name.lexeme.equals("y")) {
			throw new CompiletimeError(expr.name, "Only properties 'x' or 'y exist for points.");
		}

		if (environment.get(callee.name).initializationStatus != TokenType.INITIALIZED && !weAreInAFunctionDefinition) {
			throw new CompiletimeError(callee.name,
					"The point '" + callee.name.lexeme + "' is being used without an initial value.");
		}

		setGoalIdCurrent(goalIdCurrent + 1);
		int myId = goalIdCurrent;

		if (expr.name.lexeme.equals("x")) {
			return new ATranslatedExpressionStatement(NUMBER_INTEGER,
					"(up-modify-goal " + myId + " g:= " + environment.get(callee.name).id + ")\n", myId);
		} else {
			return new ATranslatedExpressionStatement(NUMBER_INTEGER,
					"(up-modify-goal " + myId + " g:= " + (environment.get(callee.name).id + 1) + ")\n", myId);
		}
	}

	@Override
	public Object visitSetExpr(Set expr) {
		// if the expr calls on a point variable and looks either for x or y property
		// translate the goal as if setting
		// a variable normally
		// and then change it by calling the usual translations parts
		if (!(expr.object instanceof Expr.Variable)) {
			throw new CompiletimeError(expr.name, "Only point variables have fields (.x/.y).");
		}
		Expr.Variable callee = (Expr.Variable) expr.object;

		if (environment.get(callee.name).type != TokenType.POINT_OBJECT) {
			throw new CompiletimeError(expr.name, "Only point variables have fields. '" + callee.name.lexeme + "' is a "
					+ environment.get(callee.name).type + ".");
		}

		if (!expr.name.lexeme.equals("x") && !expr.name.lexeme.equals("y")) {
			throw new CompiletimeError(expr.name, "Only properties 'x' or 'y exist for points.");
		}

		openNewDefruleIfNeeded();
		ATranslatedExpressionStatement value = evaluate(expr.value);
		if (NUMBER_INTEGER != value.type) {
			throw new CompiletimeError(expr.name, "Type mismatch in assignment. " + value.type);
		}
		someoneHasToCloseDefrule = true;
		updatePointInitializationStatus(callee.name, expr.name.lexeme);
		int myId = environment.get(callee.name).id;
		if (expr.name.lexeme.equals("x")) {
			return new ATranslatedExpressionStatement(null,
					value.appendAndRetrieve("(up-modify-goal " + myId + " g:= " + value.id + ")\n"), goalIdCurrent);
		} else {
			return new ATranslatedExpressionStatement(null,
					value.appendAndRetrieve("(up-modify-goal " + (myId + 1) + " g:= " + value.id + ")\n"),
					goalIdCurrent);
		}
	}

	void updatePointInitializationStatus(Token name, String property) {
		TokenType currentStatus = environment.get(name).initializationStatus;
		if (currentStatus != INITIALIZED) {
			if (currentStatus == NOT_INITIALZIED) {
				if (property.equals("x")) {
					environment.get(name).initializationStatus = TokenType.X_INITIALIZED;
				} else {
					environment.get(name).initializationStatus = TokenType.Y_INITIALIZED;
				}

			} else {
				if (property.equals("x") && environment.get(name).initializationStatus == TokenType.Y_INITIALIZED) {
					environment.get(name).initializationStatus = INITIALIZED;
				} else if (property.equals("y")
						&& environment.get(name).initializationStatus == TokenType.X_INITIALIZED) {
					environment.get(name).initializationStatus = INITIALIZED;
				}
			}

		}
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public Object visitCommandExpressionExpr(CommandExpression commandExpr) {
		int howMuchExtraSpaceIleaveForTheReturnType = 0;
		setGoalIdCurrent(goalIdCurrent + 1);
		howMuchExtraSpaceIleaveForTheReturnType++;
		int myId = goalIdCurrent;
		setGoalIdCurrent(goalIdCurrent + 1); // so that myide is frozen to the first free one if we initialize a
												// variable,
		// but also there's space for a point too
		howMuchExtraSpaceIleaveForTheReturnType++;
		List<String> totalCommandTranslation = new ArrayList<String>();

		String commandPerTranslatedName = commandExpr.commandName.lexeme;
		String finalCommandString = "(" + commandPerTranslatedName;
		int paramArgumentInsideCommandIndex = 0;
		int paramsInvisibleToUser = 0;
		String correctParamClassExpected = "";
		ParameterClass classOfSupposedParameterThatshouldGoThere;
		boolean theCommandReturnsAValue = false;
		boolean JustAddedAnInlineConstant = false;
		String comparisonOperatorString = "";
		boolean previousParamWasComparisonOp = false;
		String englishParameterStringWroteByUser = "";
		String genericEnglishStringParameterFromSameClassOfOneWroteByUser = "";
		boolean hasToAdd0ValueFactParameter = false;
		List<String> commandsThatDontAllowPlayerNumberStartingWithAny = new ArrayList<String>(
				Arrays.asList("player-number", "up-get-player-color", "up-get-player-fact", "up-get-upgrade-id",
						"up-set-placement-data", "up-store-player-chat", "up-store-player-name"));
		List<String> commandsThatDontAllowPlayerNumberStartingWithEvery = new ArrayList<String>(
				Arrays.asList("player-number", "up-get-fact-max", "up-get-fact-min", "up-get-fact-max",
						"up-get-fact-min", "up-get-fact-sum", "up-get-player-color", "up-get-player-fact",
						"up-get-upgrade-id", "up-set-placement-data", "up-store-player-chat", "up-store-player-name"));
		List<String> commandsThatDontAllowPlayerNumberStartingWithThisAny = new ArrayList<String>(
				Arrays.asList("up-find-player-flare", "up-get-fact-max", "up-get-fact-min", "up-get-fact-sum"));
		String[] removedCommands = { "goal", "set-goal", "up-modify-goal", "up-compare-goal", "set-strategic-number",
				"strategic-number", "up-modify-sn", "up-compare-sn", "up-modify-flag", "up-compare-flag",
				"up-add-point", "up-copy-point", "up-set-indirect-goal", "up-get-indirect-goal", "up-get-rule-id",
				"up-jump-direct", "up-jump-dynamic", "up-jump-rule", "xs-script-call", "up-get-search-state",
				"disable-self" };

		if (weAreInACondition && commandExpr.commandPer.commandType.equals("Action")) {
			throw new CompiletimeError(commandExpr.commandName,
					"Cannot use an 'Action' type command inside a condition.");
		}
		if (!weAreInACondition && commandExpr.commandPer.commandType.equals("Fact")) {
			throw new CompiletimeError(commandExpr.commandName,
					"Cannot use a 'Fact' type command outside a condition.\nCommand 'up-get-fact' might be useful here.");
		}

		if (commandPerTranslatedName.equals("up-get-threat-data")
				|| commandPerTranslatedName.equals("up-get-victory-data")
				|| commandPerTranslatedName.equals("set-shared-goal")
				|| commandPerTranslatedName.equals("shared-goal")) {
			throw new CompiletimeError(commandExpr.commandName, "Command '" + commandExpr.commandName.lexeme
					+ "' is unsupported in Barracks and there are no plans to add it.\nIf you believe it should be added, contact me.");
		}

		if (commandPerTranslatedName.equals("up-get-shared-goal")
				|| commandPerTranslatedName.equals("up-set-shared-goal")
				|| commandPerTranslatedName.equals("up-allied-goal")
				|| commandPerTranslatedName.equals("up-allied-sn")) {
			throw new CompiletimeError(commandExpr.commandName, "Command '" + commandExpr.commandName.lexeme
					+ "' is temporarly unsupported in Barracks.\nIf you have proposals on how the communication between AIs should work, let me know.");
		}

		if (Arrays.asList(removedCommands).contains(commandPerTranslatedName)) {
			String extraErorrInfo = "";
			if (commandPerTranslatedName.equals("up-get-search-state")) {
				extraErorrInfo = "\nUse '(update-search-state)' Action command.\nThe states are accessibile by using:\nsearch-local-total, search-local-last, search-remote-total, search-remote-last .";
			} else if (commandPerTranslatedName.equals("xs-script-call")) {
				extraErorrInfo = "\nXS is planned to be introduce natively. As of now it's unavailable.";
			} else if (commandPerTranslatedName.equals("up-add-point")) {
				extraErorrInfo = "\nJust add the points, like 'p1 + p2' or 'p1 + <10,15>' etc.";
			} else if (commandPerTranslatedName.equals("up-compare-goal")) {
				extraErorrInfo = "\nJust compare directly, like 'a > b' etc, no commands needed.";
			} else if (commandPerTranslatedName.equals("set-strategic-number")) {
				extraErorrInfo = "\nJust set the SN directly, like in 'sn-name := 12', no commands needed.";
			} else if (commandPerTranslatedName.equals("disable-self")) {
				extraErorrInfo = "\nUse 'disable-self' without paranthesis, it's now a keyword and not a command.";
			}
			throw new CompiletimeError(commandExpr.commandName,
					"Command '" + commandExpr.commandName.lexeme + "' has been removed in Barracks." + extraErorrInfo);
		}

		if (commandPerTranslatedName.equals("update-search-state")) {
			totalCommandTranslation.clear();
			totalCommandTranslation.add("(up-get-search-state " + searchLocalTotalId + ")\n");
			return new ATranslatedExpressionStatement(TokenType.NULL, totalCommandTranslation, myId);
		}

		for (Expr param : commandExpr.expressions) {
			correctParamClassExpected = commandExpr.commandPer.commandParameters.get(paramArgumentInsideCommandIndex);
			if (commandPerTranslatedName.equals("up-bound-point") && paramArgumentInsideCommandIndex == 0) {
				finalCommandString += " " + myId;
			}
			while (correctParamClassExpected.equals("mathOp") || correctParamClassExpected.equals("typeOp")) {
				paramArgumentInsideCommandIndex++;
				paramsInvisibleToUser++;
				correctParamClassExpected = commandExpr.commandPer.commandParameters
						.get(paramArgumentInsideCommandIndex);
			}
			if (correctParamClassExpected.equals("OutputGoalId")) {
				// we are in per command 'up-get-point-contains', can only happen in this one
				paramArgumentInsideCommandIndex++;
				paramsInvisibleToUser++;
				correctParamClassExpected = commandExpr.commandPer.commandParameters
						.get(paramArgumentInsideCommandIndex);
				while (correctParamClassExpected.equals("mathOp") || correctParamClassExpected.equals("typeOp")) {
					paramArgumentInsideCommandIndex++;
					paramsInvisibleToUser++;
					correctParamClassExpected = commandExpr.commandPer.commandParameters
							.get(paramArgumentInsideCommandIndex);
				}
				finalCommandString += " " + myId;
			}

			if (correctParamClassExpected.equals("FactId") && (param instanceof Expr.Variable)) {
				// we must report if there are too little parameters in the fact id related
				// command
				if (commandPerTranslatedName.equals("up-get-fact") || commandPerTranslatedName.equals("up-get-fact-max")
						|| commandPerTranslatedName.equals("up-get-fact-min")
						|| commandPerTranslatedName.equals("up-get-fact-sum")
						|| commandPerTranslatedName.equals("up-get-focus-fact")
						|| commandPerTranslatedName.equals("up-get-player-fact")
						|| commandPerTranslatedName.equals("up-get-target-fact")) {
					if (((Expr.Variable) param).name.lexeme.equals("allied-goal")
							|| ((Expr.Variable) param).name.lexeme.equals("allied-sn")) {
						throw new CompiletimeError(commandExpr.commandName,
								"Currently the compiler doesn't allow to communicate with other AIs using goals and SNs.\nPlanning to implement it in the future.");
					}
					if (!(parametersContainer.FactParameterAssociatedToFactId
							.containsKey(((Expr.Variable) param).name.lexeme))) {
						hasToAdd0ValueFactParameter = true;
						// because it's contained in the dictionary only if a fact parameter is needed
						// for this factif
						// so if it's not contained we need one less param than we supposed
						if (commandExpr.expressions
								.size() > commandExpr.commandPer.howManyParamsRequiredFromBarracksUser() - 1) {
							throw new CompiletimeError(commandExpr.commandName,
									"Command has too many parameters.\n(Also note: the parameter '0' for FactIDs that do not need any FactParameter is no longer needed).");
						}
					} else {
						if (commandExpr.expressions.size() < commandExpr.commandPer
								.howManyParamsRequiredFromBarracksUser()) {
							throw new CompiletimeError(commandExpr.commandName, "Command has too few parameters.");
						}
					}
				}
			}
			if (correctParamClassExpected.equals("FactParameter") && hasToAdd0ValueFactParameter) {
				hasToAdd0ValueFactParameter = false;
				paramArgumentInsideCommandIndex++;
				paramsInvisibleToUser++;
				correctParamClassExpected = commandExpr.commandPer.commandParameters
						.get(paramArgumentInsideCommandIndex);
				while (correctParamClassExpected.equals("mathOp") || correctParamClassExpected.equals("typeOp")) {
					paramArgumentInsideCommandIndex++;
					paramsInvisibleToUser++;
					correctParamClassExpected = commandExpr.commandPer.commandParameters
							.get(paramArgumentInsideCommandIndex);
				}
				finalCommandString += " 0";
			}
			/*
			 * if (paramClass.equals("FactParameter")) { if (paramArgumentInsideCommandIndex
			 * != 0) { if(commandExpr.commandPer.commandParameters.get(
			 * paramArgumentInsideCommandIndex-1).equals("FactId")) {
			 * System.out.println("yes"); } } }
			 */

			classOfSupposedParameterThatshouldGoThere = parametersContainer
					.getParameterClassObjectOfName(correctParamClassExpected);
			if ((param instanceof Expr.Variable && environment.get(((Expr.Variable) param).name).isParam)
					|| (param instanceof Expr.Literal && (((Expr.Literal) param).type.equals(TokenType.GREATER)
							|| ((Expr.Literal) param).type.equals(TokenType.GREATER_EQUAL)
							|| ((Expr.Literal) param).type.equals(TokenType.LESS)
							|| ((Expr.Literal) param).type.equals(TokenType.LESS_EQUAL)
							|| ((Expr.Literal) param).type.equals(TokenType.EQUAL_EQUAL)
							|| ((Expr.Literal) param).type.equals(TokenType.BANG_EQUAL)))) {

				// devo verificare che appartenga alla paramClass
				if (param instanceof Expr.Variable) {
					englishParameterStringWroteByUser = ((Expr.Variable) param).name.lexeme;
					genericEnglishStringParameterFromSameClassOfOneWroteByUser = englishParameterStringWroteByUser;
					if (environment.isDefined(((Expr.Variable) param).name)) {
						// for user defined parameters aliases
						if (environment.get(((Expr.Variable) param).name).associatedActualParameterName != null) {
							genericEnglishStringParameterFromSameClassOfOneWroteByUser = environment
									.get(((Expr.Variable) param).name).associatedActualParameterName;
						}
					}
				} else {
					englishParameterStringWroteByUser = ((Expr.Literal) param).type.toString();
					genericEnglishStringParameterFromSameClassOfOneWroteByUser = englishParameterStringWroteByUser;
				}
				if (!classOfSupposedParameterThatshouldGoThere
						.ParameterAllowedInParameterClass(genericEnglishStringParameterFromSameClassOfOneWroteByUser)) {
					if (correctParamClassExpected.equals("FactParameter")) {
						// check if the factparameter is allowed for the just inserted factid by looking
						// up the dictionary
						// if the corect paramclass here is a factparameter, then before this was a fact
						// id 100%
						String predecessorFactId = ((Expr.Variable) commandExpr.expressions
								.get(paramArgumentInsideCommandIndex - 1)).name.lexeme;
						if (parametersContainer.FactParameterAssociatedToFactId.containsKey(predecessorFactId)
								&& (predecessorFactId.equals("gaia-type-count-total")
										|| predecessorFactId.equals("gaia-type-count")
										|| predecessorFactId.equals("cc-gaia-type-count")
										|| parametersContainer.FactParameterAssociatedToFactId
												.get(predecessorFactId).valueList.contains(
														genericEnglishStringParameterFromSameClassOfOneWroteByUser))) {
							if (predecessorFactId.equals("gaia-type-count-total")
									|| predecessorFactId.equals("gaia-type-count")
									|| predecessorFactId.equals("cc-gaia-type-count")) {
								if (parametersContainer.pResource.valueList
										.contains(genericEnglishStringParameterFromSameClassOfOneWroteByUser)
										|| parametersContainer.pUnitId.valueList
												.contains(genericEnglishStringParameterFromSameClassOfOneWroteByUser)) {
									// then it's fine, no error
								} else {
									throw new CompiletimeError(commandExpr.commandName, "Parameter '"
											+ englishParameterStringWroteByUser + "' not allowed in "
											+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1)
											+ "th position of '" + commandPerTranslatedName
											+ "' command.\nExpected a valid parameter of parameterClass 'Resource' or 'UnitId' instead.");
								}
							}
						} else {
							throw new CompiletimeError(commandExpr.commandName,
									"Parameter '" + englishParameterStringWroteByUser + "' not allowed in "
											+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1)
											+ "th position of '" + commandPerTranslatedName
											+ "' command.\nExpected a valid parameter of parameterClass '"
											+ parametersContainer.FactParameterAssociatedToFactId
													.get(predecessorFactId).upParamName
											+ "' instead.");
						}
					} else {
						throw new CompiletimeError(commandExpr.commandName,
								"Parameter '" + englishParameterStringWroteByUser + "' not allowed in "
										+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1)
										+ "th position of '" + commandPerTranslatedName
										+ "' command.\nExpected a valid parameter of parameterClass '"
										+ correctParamClassExpected + "' instead.");
					}
				}
				if (commandPerTranslatedName.equals("can-afford-unit") || commandPerTranslatedName.equals("can-train")
						|| commandPerTranslatedName.equals("can-train-with-escrow")
						|| commandPerTranslatedName.equals("delete-unit")
						|| commandPerTranslatedName.equals("players-unit-type-count")
						|| commandPerTranslatedName.equals("train")
						|| commandPerTranslatedName.equals("up-train-site-ready")) {
					if (parametersContainer.pClassId.ParameterAllowedInParameterClass(
							genericEnglishStringParameterFromSameClassOfOneWroteByUser)) {
						throw new CompiletimeError(commandExpr.commandName,
								"Parameter '" + englishParameterStringWroteByUser + "' not allowed in "
										+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1)
										+ "th position of '" + commandPerTranslatedName
										+ "' command.\nExpected a valid parameter of parameterClass '"
										+ correctParamClassExpected
										+ "' instead.\nObject classes not allowed with this command.");
					}
				}
				if (commandPerTranslatedName.equals("up-train-site-ready")) {
					if (parametersContainer.pLineId.ParameterAllowedInParameterClass(
							genericEnglishStringParameterFromSameClassOfOneWroteByUser)) {
						throw new CompiletimeError(commandExpr.commandName,
								"Parameter '" + englishParameterStringWroteByUser + "' not allowed in "
										+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1)
										+ "th position of '" + commandPerTranslatedName
										+ "' command.\nExpected a valid parameter of parameterClass '"
										+ correctParamClassExpected
										+ "' instead.\nUnit lines not allowed with this command.");
					}
				}
				if (correctParamClassExpected.equals("PlayerNumber")) {
					if (englishParameterStringWroteByUser.startsWith("any-")) {
						if (commandsThatDontAllowPlayerNumberStartingWithAny.contains(commandExpr.commandName.lexeme)) {
							throw new CompiletimeError(commandExpr.commandName, "Command '" + commandPerTranslatedName
									+ "' doesn't allow the 'PlayerNumber' parameter to start with 'any-'.");
						}
						if ((commandExpr.commandName.lexeme).equals("up-allied-goal")
								|| (commandExpr.commandName.lexeme).equals("up-allied-resource-amount")
								|| (commandExpr.commandName.lexeme).equals("up-allied-resource-percent")
								|| (commandExpr.commandName.lexeme).equals("up-allied-sn")) {
							if (!englishParameterStringWroteByUser.contains("ally")) {
								throw new CompiletimeError(commandExpr.commandName, "Command '"
										+ commandPerTranslatedName
										+ "' allows the 'PlayerNumber' parameter to start with 'any-' ONLY FOR ally players ones.");
							}
						}
					} else if (englishParameterStringWroteByUser.startsWith("every-")) {
						if (commandsThatDontAllowPlayerNumberStartingWithEvery
								.contains(commandExpr.commandName.lexeme)) {
							throw new CompiletimeError(commandExpr.commandName, "Command '" + commandPerTranslatedName
									+ "' doesn't allow the 'PlayerNumber' parameter to start with 'every-'.");
						}
						if ((commandExpr.commandName.lexeme).equals("up-allied-goal")
								|| (commandExpr.commandName.lexeme).equals("up-allied-resource-amount")
								|| (commandExpr.commandName.lexeme).equals("up-allied-resource-percent")
								|| (commandExpr.commandName.lexeme).equals("up-allied-sn")) {
							if (!englishParameterStringWroteByUser.contains("ally")) {
								throw new CompiletimeError(commandExpr.commandName, "Command '"
										+ commandPerTranslatedName
										+ "' allows the 'PlayerNumber' parameter to start with 'every-' ONLY FOR ally players ones.");
							}
						}
					} else if (englishParameterStringWroteByUser.startsWith("this-any-")) {
						if (commandExpr.commandPer.commandType.equals("Fact")
								|| commandsThatDontAllowPlayerNumberStartingWithThisAny
										.contains(commandExpr.commandName.lexeme)) {
							throw new CompiletimeError(commandExpr.commandName, "Command '" + commandPerTranslatedName
									+ "' doesn't allow the 'PlayerNumber' parameter to start with 'this-any-'.");
						}
						if ((commandExpr.commandName.lexeme).equals("up-set-placement-data")) {
							if (!englishParameterStringWroteByUser.contains("ally")) {
								throw new CompiletimeError(commandExpr.commandName, "Command '"
										+ commandPerTranslatedName
										+ "' allows the 'PlayerNumber' parameter to start with 'this-any-' ONLY FOR ally players ones.");
							}
						}
					}
				}
				if (param instanceof Expr.Literal && (((Expr.Literal) param).type.equals(TokenType.GREATER)
						|| ((Expr.Literal) param).type.equals(TokenType.GREATER_EQUAL)
						|| ((Expr.Literal) param).type.equals(TokenType.LESS)
						|| ((Expr.Literal) param).type.equals(TokenType.LESS_EQUAL)
						|| ((Expr.Literal) param).type.equals(TokenType.EQUAL_EQUAL)
						|| ((Expr.Literal) param).type.equals(TokenType.BANG_EQUAL))) {
					englishParameterStringWroteByUser = ((Expr.Literal) param).type.toString();
					switch (englishParameterStringWroteByUser) {
					case "GREATER_EQUAL": {
						comparisonOperatorString = ">=";
						break;
					}
					case "GREATER": {
						comparisonOperatorString = ">";
						break;
					}
					case "LESS_EQUAL": {
						comparisonOperatorString = "<=";
						break;
					}
					case "LESS": {
						comparisonOperatorString = "<";
						break;
					}
					case "EQUAL_EQUAL": {
						comparisonOperatorString = "==";
						break;
					}
					case "BANG_EQUAL": {
						comparisonOperatorString = "!=";
						break;
					}

					}
				}

				if (paramArgumentInsideCommandIndex > 0 && commandExpr.commandPer.commandParameters
						.get(paramArgumentInsideCommandIndex - 1).equals("typeOp")) {
					finalCommandString += " c:";
				}

				if (previousParamWasComparisonOp) {
					finalCommandString += " c:" + comparisonOperatorString + " " + englishParameterStringWroteByUser; // because
																														// space
																														// between
																														// g:
																														// and
																														// <
																														// breaks
																														// it
					previousParamWasComparisonOp = false;
				} else if (!commandExpr.commandPer.commandParameters.get(paramArgumentInsideCommandIndex)
						.equals("compareOp")) {
					finalCommandString += " " + englishParameterStringWroteByUser; // because native params write
																					// themselves in directly with no g:
				}

				if (commandExpr.commandPer.commandParameters.get(paramArgumentInsideCommandIndex).equals("compareOp")) {
					previousParamWasComparisonOp = true;
				}

				paramArgumentInsideCommandIndex++;
				continue;
			}
			// devo verificare che la paramClass acetti un oggetto di tipo inserito
			// dall'utente int/timer/point
			// server che ogni ParameterClass abbia delle spunte sul fatto che può accettare
			// int/timer/point as well, oltre che eventuali parametri a parole
			ATranslatedExpressionStatement paramTranslation = evaluate(param);
			if (paramTranslation.type == TokenType.NUMBER_INTEGER
					&& classOfSupposedParameterThatshouldGoThere.allowsInteger) {

			} else if (paramTranslation.type == TokenType.POINT_OBJECT
					&& classOfSupposedParameterThatshouldGoThere.allowsPoint) {

			} else if (paramTranslation.type == TokenType.TIMER_OBJECT
					&& classOfSupposedParameterThatshouldGoThere.allowsTimer) {

			} else if (paramTranslation.type == TokenType.STRING
					&& classOfSupposedParameterThatshouldGoThere.allowsString) {

			} else {
				if ((paramArgumentInsideCommandIndex - 1) >= 0 && correctParamClassExpected.equals("FactParameter")) {
					String predecessorFactId = ((Expr.Variable) commandExpr.expressions
							.get(paramArgumentInsideCommandIndex - 1)).name.lexeme;
					if (parametersContainer.FactParameterAssociatedToFactId.get(predecessorFactId) == null) {
						throw new CompiletimeError(commandExpr.commandName, "Parameter '"
								+ englishParameterStringWroteByUser + "' not allowed in "
								+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1) + "th position of '"
								+ commandPerTranslatedName
								+ "' command.\nExpected a valid parameter of parameterClass 'Resource' or 'UnitId' instead.");
					}
					throw new CompiletimeError(commandExpr.commandName, "Parameter '"
							+ englishParameterStringWroteByUser + "' not allowed in "
							+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1) + "th position of '"
							+ commandPerTranslatedName + "' command.\nExpected a valid parameter of parameterClass '"
							+ parametersContainer.FactParameterAssociatedToFactId.get(predecessorFactId).upParamName
							+ "' instead.");
				}
				throw new CompiletimeError(commandExpr.commandName,
						"Value of type '" + paramTranslation.type + "' not allowed in "
								+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1) + "th position of '"
								+ commandPerTranslatedName
								+ "' command.\nExpected a valid parameter of parameterClass '"
								+ correctParamClassExpected + "' instead.");
			}
			if (paramTranslation.type == TokenType.STRING) {
				if (param instanceof Expr.Variable && environment.get(((Expr.Variable) param).name).isConst) {
					String englishParameterStringWroteByUser2 = ((Expr.Variable) param).name.lexeme;
					finalCommandString += " " + englishParameterStringWroteByUser2;
				} else {
					finalCommandString += " " + paramTranslation.inlineStringValue;
				}
			} else if (paramTranslation.type == TokenType.POINT_OBJECT) {
				finalCommandString += " " + paramTranslation.id;
			} else {
				if (paramArgumentInsideCommandIndex > 0 && (commandExpr.commandPer.commandParameters
						.get(paramArgumentInsideCommandIndex - 1).equals("typeOp")
						|| commandExpr.commandPer.commandParameters.get(paramArgumentInsideCommandIndex - 1)
								.equals("compareOp"))) {
					if (commandExpr.commandPer.commandParameters.get(paramArgumentInsideCommandIndex - 1)
							.equals("compareOp")) {
						if (previousParamWasComparisonOp) {
							finalCommandString += " g:" + comparisonOperatorString;
							previousParamWasComparisonOp = false;
						}
						finalCommandString += " " + paramTranslation.id;
					} else {
						finalCommandString += " g: " + paramTranslation.id; // because if precceeded by typeof it needes
																			// to add the g, else if preceeded by a
																			// comparisonop, it already there before the
																			// < > etcc
					}
				} else {
					while (param instanceof Expr.Grouping) {
						param = ((Expr.Grouping) param).expression;
					}
					boolean shouldNegateInlineValue = false;
					if (param instanceof Expr.Unary) {
						Expr.Unary expr = ((Expr.Unary) param);
						param = expr.right;
						paramTranslation = evaluate(param);
						checkNumberOperand(expr.operator, paramTranslation);
						switch (expr.operator.type) {

						case MINUS:

							shouldNegateInlineValue = true;
						}
					}
					if (param instanceof Expr.Literal) {
						finalCommandString += " ";
						if (shouldNegateInlineValue) {
							finalCommandString += "-";
						}
						finalCommandString += paramTranslation.inlineIntValue;
						JustAddedAnInlineConstant = true;
					} else if (param instanceof Expr.Variable
							&& environment.get(((Expr.Variable) param).name).isConst) {
						String englishParameterStringWroteByUser3 = ((Expr.Variable) param).name.lexeme;
						if (shouldNegateInlineValue) {
							finalCommandString += "-";
						}
						finalCommandString += englishParameterStringWroteByUser3;
						JustAddedAnInlineConstant = true;
					} else if (param instanceof Expr.Variable
							&& environment.get(((Expr.Variable) param).name).type == TokenType.TIMER_OBJECT) {
						finalCommandString += " ";
						if (shouldNegateInlineValue) {
							finalCommandString += "-";
						}
						finalCommandString += environment.get(((Expr.Variable) param).name).id;
						JustAddedAnInlineConstant = true;
					} else {
						throw new CompiletimeError(commandExpr.commandName, "Variable/SN/expression not allowed in "
								+ (paramArgumentInsideCommandIndex - paramsInvisibleToUser + 1) + "th position of '"
								+ commandPerTranslatedName + "' command.\nRequiered a number/value directly instead.");
					}
				}
			}
			if (!JustAddedAnInlineConstant) {
				totalCommandTranslation.addAll(paramTranslation.getTranslatedCode());
			}
			JustAddedAnInlineConstant = false; // for next cycle
			paramArgumentInsideCommandIndex++;
		}

		if (commandPerTranslatedName.equals("up-bound-point")) {
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation, myId);
		}
		if (commandPerTranslatedName.equals("up-bound-precise-point")) {
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation,
					myId + howMuchExtraSpaceIleaveForTheReturnType);
		}
		if (commandPerTranslatedName.equals("up-cross-tiles")) {
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation,
					myId + howMuchExtraSpaceIleaveForTheReturnType);
		}
		if (commandPerTranslatedName.equals("up-find-flare")) {
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation, myId);
		}
		if (commandPerTranslatedName.equals("up-find-player-flare")) {
			finalCommandString += " " + myId;
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation, myId);
		}
		if (commandPerTranslatedName.equals("up-get-point")) {
			finalCommandString += " " + myId;
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation, myId);
		}
		if (commandPerTranslatedName.equals("up-lerp-percent")) {
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation,
					myId + howMuchExtraSpaceIleaveForTheReturnType);
		}
		if (commandPerTranslatedName.equals("up-lerp-tiles")) {
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation,
					myId + howMuchExtraSpaceIleaveForTheReturnType);
		}
		if (commandPerTranslatedName.equals("up-get-point-contains")) {
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation, myId);
		}

		if (commandExpr.commandPer.commandParameters.size() >= paramArgumentInsideCommandIndex + 1) {
			// the only
			// parameterClass
			// the parser
			// asks to skip
			// that can be
			// found at the
			// end of a
			// command is
			// the
			// OutputGoalID
			// or
			// up-get-attacker-class

			if (hasToAdd0ValueFactParameter) {
				hasToAdd0ValueFactParameter = false;
				paramArgumentInsideCommandIndex++;
				paramsInvisibleToUser++;
				correctParamClassExpected = commandExpr.commandPer.commandParameters
						.get(paramArgumentInsideCommandIndex);
				while (correctParamClassExpected.equals("mathOp") || correctParamClassExpected.equals("typeOp")) {
					paramArgumentInsideCommandIndex++;
					paramsInvisibleToUser++;
					correctParamClassExpected = commandExpr.commandPer.commandParameters
							.get(paramArgumentInsideCommandIndex);
				}
				finalCommandString += " 0";
			}
			totalCommandTranslation.add(finalCommandString + " " + myId + ")\n"); // since the earliest goals is
																					// overritten, if someone
																					// declared an int var in this
																					// line, it's instantly
																					// allocated too. It assumes
																					// only integers are returned
																					// and conditions that freeze
																					// goals dont allow declaration
																					// of new variables inside
																					// logical grouping
			return new ATranslatedExpressionStatement(commandExpr.commandPer.returnType, totalCommandTranslation, myId);
		} else {
			totalCommandTranslation.add(finalCommandString + ")\n");
			return new ATranslatedExpressionStatement(TokenType.NULL, totalCommandTranslation, myId);
		}

	}

	boolean IsFirstStatementInABlock = true;

	@Override
	public Void visitDisableSelfStmt(DisableSelf stmt) {
		if (!IsFirstStatementInABlock) {
			throw new CompiletimeError(stmt.name,
					"'disable-self' must be the first statement of the block's body. Move it up.");
		}
		IsFirstStatementInABlock = false;
		// !!!!!!!!! if inside an unconditional block then we should also add logic in
		// that
		addTranslatedStringToOutput("(disable-self)\n");
		return null;
	}

	@Override
	public Void visitLoadIfStmt(LoadIf stmt) {
		closeDefruleIfNeeded();
		if (stmt.ifDefined) {
			addTranslatedStringToOutput("#load-if-defined " + stmt.symbol.lexeme + "\n");
		} else {
			addTranslatedStringToOutput("#load-if-not-defined " + stmt.symbol.lexeme + "\n");
		}

		compileLoadIfBlockStatements(stmt.statements);

		closeDefruleIfNeeded();
		if (stmt.elseStatements.size() > 0) {
			addTranslatedStringToOutput("#else\n");
			someoneHasToOpenDefrule = true;
			compileLoadIfBlockStatements(stmt.elseStatements);
			closeDefruleIfNeeded();
			addTranslatedStringToOutput("#end-if\n");
			someoneHasToOpenDefrule = true;
		} else {
			addTranslatedStringToOutput("#end-if\n");
			someoneHasToOpenDefrule = true;
		}
		return null;
	}

	private void compileLoadIfBlockStatements(List<Stmt> stmts) {
		try {
			for (Stmt statement : stmts) {
				translate(statement);
			}
			closeDefruleIfNeeded();
		} catch (CompiletimeError error) {
			Barracks.compiletimeError(error);
		}
	}

	@Override
	public Void visitParamInitializationStmt(ParamInitialization stmt) {

		if (environment.isDefined(stmt.actualParameterName)) {
			if (environment.get(stmt.actualParameterName).isParam) {
			} else {
				throw new CompiletimeError(stmt.actualParameterName, "The suggested '" + stmt.actualParameterName.lexeme
						+ "' parameter doesn't actually exist, such IDENTIFIER is known but it's not a parameter.");
			}
		} else {
			throw new CompiletimeError(stmt.actualParameterName, "The suggested '" + stmt.actualParameterName.lexeme
					+ "' native parameter doesn't actually exist, so it's impossible to assign it.");
		}

		closeDefruleIfNeeded();
		if (weHaveJustExitedALoopForDefconst) {
			addTranslatedStringToOutput("(defrule\n");
			addTranslatedStringToOutput("(false)\n");
			addTranslatedStringToOutput("=>\n");
			addTranslatedStringToOutput("(do-nothing)\n");
			addTranslatedStringToOutput(")\n");
			weHaveJustExitedALoopForDefconst = false;
		}

		addTranslatedStringToOutput("(defconst " + stmt.alias.lexeme + " " + stmt.actualParameterName.lexeme + ")\n");

		someoneHasToOpenDefrule = true;

		if (environment.isDefined(stmt.alias) /* && !environment.get(stmt.name).isConst */) {
			String forWhat = "";
			if (environment.get(stmt.alias).isCommand)
				forWhat = "command";
			else if (environment.get(stmt.alias).isParam)
				forWhat = "parameter";
			else if (environment.get(stmt.alias).isSN)
				forWhat = "strategic number";
			else if (environment.get(stmt.alias).isConst)
				forWhat = "previously defined constant";
			else if (environment.get(stmt.alias).isReadOnly)
				forWhat = "native variable";
			else
				forWhat = "variable";
			throw new CompiletimeError(stmt.alias,
					"The name '" + stmt.alias.lexeme + "' is already used for a " + forWhat + ".");
		}

		// System.out.println(parametersContainer.getParameterClassOfAParamater(stmt.actualParameterName.lexeme).upParamName);

		// String parameterClass =
		// parametersContainer.getParameterClassObjectOfName(stmt.actualParameterName.lexeme).upParamName;

		if (environment.get(stmt.actualParameterName).associatedActualParameterName == null) {
			environment.define(stmt.alias.lexeme, 0, TokenType.PARAMATER_NATIVE_OBJECT, false, TokenType.INITIALIZED,
					false, true, false, false, stmt.actualParameterName.lexeme);
		} else {
			environment.define(stmt.alias.lexeme, 0, TokenType.PARAMATER_NATIVE_OBJECT, false, TokenType.INITIALIZED,
					false, true, false, false, environment.get(stmt.actualParameterName).associatedActualParameterName);
		}

		// anyway
		return null;
	}

	@Override
	public Void visitParamAssignmentStmt(ParamAssignment stmt) {
		closeDefruleIfNeeded();
		if (weHaveJustExitedALoopForDefconst) {
			addTranslatedStringToOutput("(defrule\n");
			addTranslatedStringToOutput("(false)\n");
			addTranslatedStringToOutput("=>\n");
			addTranslatedStringToOutput("(do-nothing)\n");
			addTranslatedStringToOutput(")\n");
			weHaveJustExitedALoopForDefconst = false;
		}

		addTranslatedStringToOutput("(defconst " + stmt.alias.lexeme + " " + stmt.actualParameterName.lexeme + ")\n");

		someoneHasToOpenDefrule = true;

		if (!environment.get(stmt.alias).isParam) {
			throw new CompiletimeError(stmt.alias, "Only custom parameters can be assigned to in the global scope.\n'"
					+ stmt.alias.lexeme
					+ "' is not a previously declared custom parameter. Use keyword 'param' to declare it if needed.");
		}
		if (environment.get(stmt.alias).associatedActualParameterName == null) {
			throw new CompiletimeError(stmt.alias, "Only custom parameters can be assigned to in the global scope.\n'"
					+ stmt.alias.lexeme + "' is a native parameter and therefore it cannot be reassigned.");
		}

		String currentClassOfParameterOfMyAlias = parametersContainer
				.getParameterClassOfAParamater(environment.get(stmt.alias).associatedActualParameterName).upParamName;
		String classOfParameterOfParameterImTryingToAssign = parametersContainer
				.getParameterClassOfAParamater(stmt.actualParameterName.lexeme).upParamName;

		if (!currentClassOfParameterOfMyAlias.equals(classOfParameterOfParameterImTryingToAssign)) {
			throw new CompiletimeError(stmt.actualParameterName,
					"'" + stmt.alias.lexeme + "' has been previously associated with a parameter of class '"
							+ currentClassOfParameterOfMyAlias + "', however '" + stmt.actualParameterName.lexeme
							+ "' is of parameter class '" + classOfParameterOfParameterImTryingToAssign
							+ "'.\nA param variable can never change parameter class.");
		}

		if (environment.get(stmt.actualParameterName).associatedActualParameterName == null) {
			environment.define(stmt.alias.lexeme, 0, TokenType.PARAMATER_NATIVE_OBJECT, false, TokenType.INITIALIZED,
					false, true, false, false, stmt.actualParameterName.lexeme);
		} else {
			environment.define(stmt.alias.lexeme, 0, TokenType.PARAMATER_NATIVE_OBJECT, false, TokenType.INITIALIZED,
					false, true, false, false, environment.get(stmt.actualParameterName).associatedActualParameterName);
		}

		// anyway
		return null;
	}

	boolean weAreInAFunction = false;

	@Override
	public Object visitfunctionCallExpressionExpr(functionCallExpression expr) {
		if (!declaredFunctionsInfo.isExistingFunction(expr.functionName.lexeme)) {
			throw new CompiletimeError(expr.functionName, "Unknown function name '" + expr.functionName.lexeme + "'.");
		}

		if (declaredFunctionsInfo.getArity(expr.functionName.lexeme) != expr.arguments.size()) {
			throw new CompiletimeError(expr.functionName,
					expr.functionName.lexeme + "\nExpected " + declaredFunctionsInfo.getArity(expr.functionName.lexeme)
							+ " arguments, got " + expr.arguments.size() + " arguments in function call '"
							+ expr.functionName.lexeme + "'.");
		}

		ATranslatedExpressionStatement compiledArguments = null;
		ATranslatedExpressionStatement evaluatedArgument;
		int whichInputAregumentHaveWeInsertedIndex = 0;
		int goalIdCurrentBeforeProcessingArguments = goalIdCurrent; // so that later we can know our offset in the
																	// expression we've be called in
		for (Expr argument : expr.arguments) {
			if (compiledArguments == null) {
				compiledArguments = evaluate(argument);
			} else {
				evaluatedArgument = evaluate(argument);
				compiledArguments = new ATranslatedExpressionStatement(evaluatedArgument.type,
						compiledArguments.mergeTranslatedExpressionStatements(evaluatedArgument), evaluatedArgument.id);
			}

			setGoalIdCurrent(compiledArguments.id);
			if (compiledArguments.type.equals(TokenType.POINT_OBJECT)) {
				setGoalIdCurrent(goalIdCurrent + 1);
			}

			if (!declaredFunctionsInfo
					.getExpectedArgumentType(expr.functionName.lexeme, whichInputAregumentHaveWeInsertedIndex)
					.equals(compiledArguments.type)) {
				throw new CompiletimeError(expr.functionName,
						expr.functionName.lexeme + "\nExpected a "
								+ declaredFunctionsInfo.getExpectedArgumentType(expr.functionName.lexeme,
										whichInputAregumentHaveWeInsertedIndex)
								+ " in " + whichInputAregumentHaveWeInsertedIndex + "th position, got a "
								+ compiledArguments.type + " instead.");
			}
			whichInputAregumentHaveWeInsertedIndex++;
		}

		int myId = goalIdStart + 1 + (goalIdCurrentBeforeProcessingArguments - goalIdStart);
		if (expr.functionName.lexeme.equals("max(")) {
			setGoalIdCurrent(goalIdCurrent + 1); // we did work
			return new ATranslatedExpressionStatement(declaredFunctionsInfo.getReturnType(expr.functionName.lexeme),
					compiledArguments.appendAndRetrieve("(up-modify-goal " + (myId) + " g:max " + (myId + 1) + ")\n"),
					(myId));
		} else if (expr.functionName.lexeme.equals("min(")) {
			setGoalIdCurrent(goalIdCurrent + 1); // we did work
			return new ATranslatedExpressionStatement(declaredFunctionsInfo.getReturnType(expr.functionName.lexeme),
					compiledArguments.appendAndRetrieve("(up-modify-goal " + (myId) + " g:min " + (myId + 1) + ")\n"),
					(myId));
		} else if (expr.functionName.lexeme.equals("abs(")) {
			compiledArguments = new ATranslatedExpressionStatement(null, compiledArguments
					.appendAndRetrieve("(up-modify-goal " + (goalIdCurrent + 1) + " g:= " + (myId) + ")\n"), null);
			compiledArguments = new ATranslatedExpressionStatement(null,
					compiledArguments.appendAndRetrieve("(up-modify-goal " + (goalIdCurrent + 1) + " c:* -1)\n"), null);
			setGoalIdCurrent(goalIdCurrent + 1); // we did work
			return new ATranslatedExpressionStatement(declaredFunctionsInfo.getReturnType(expr.functionName.lexeme),
					compiledArguments.appendAndRetrieve(
							"(up-modify-goal " + (myId) + " g:max " + (goalIdCurrent) + ")\n"),
					(myId));
		}

		if (weAreInACondition) {
			throw new CompiletimeError(expr.functionName,
					"Only calls to native functions such as 'min()', 'max()' and 'abs()' are allowed inside conditions.");
		}

		boolean weAreInAFunctionAtStart = weAreInAFunction;
		weAreInAFunction = true;

		if (compiledArguments != null) {
			addTranslatedStringToOutput(compiledArguments.getTranslatedCode());
		}
		addTranslatedStringToOutput("(up-modify-goal " + goalIdHowManyGoalsToCopyForInputParameters + " c:= "
				+ declaredFunctionsInfo.getHowManyGoalsToCopyForInputParameters(expr.functionName.lexeme) + ")\n");

		addTranslatedStringToOutput("(up-modify-goal " + goalIdHowManyWorkRegistersToPushOnStack + " c:= "
				+ (goalIdCurrent - goalIdNumberOfFirstWorkRegister + 1
						- declaredFunctionsInfo.getHowManyGoalsToCopyForInputParameters(expr.functionName.lexeme))
				+ ")\n"); // after these we expect to find the inputs
		addTranslatedStringToOutput(expr.functionName.lexeme + "\n"); // to know to substitute at postprocessing time,
																		// the function id we will want to jump after
																		// the push
		addTranslatedStringToOutput("(up-get-rule-id " + goalIdTemporary + ")\n"); // to know where to jump back
		// eventually
		addTranslatedStringToOutput("(up-jump-direct c: " + ruleIdPush + ")\n");
		closeDefruleIfNeeded();
		someoneHasToOpenDefrule = true;
		openNewDefruleIfNeeded();
		// once the func jumps back here, here's what we want to do with the return
		// value:
		weAreInAFunction = weAreInAFunctionAtStart;
		List<String> codeForFixingPointersAfterComingBackFromFunctionCall = new ArrayList<>();
		codeForFixingPointersAfterComingBackFromFunctionCall
				.add("(up-modify-goal " + goalIdPointerCallStack + " c:- 1)\n");
		myId = goalIdStart + 1 + (goalIdCurrentBeforeProcessingArguments - goalIdStart); // we need to save both in one
																							// more than the start in
		// case it's being assigned directly to a variable like
		// int a:= func()....we also need to offset by whichever
		// amount of temporary goals might have been temporarly
		// put there, like in int a := 4+2+func()+2 it's 2 extra
		// goals, which is found by difference of goals
		codeForFixingPointersAfterComingBackFromFunctionCall
				.add("(up-modify-goal " + myId + " g:= " + returnValue1 + ")\n");
		if (declaredFunctionsInfo.getReturnType(expr.functionName.lexeme).equals(TokenType.POINT_OBJECT)) {
			myId++;
			codeForFixingPointersAfterComingBackFromFunctionCall
					.add("(up-modify-goal " + myId + " g:= " + returnValue2 + ")\n");
			myId--;// we just want to write there, not to shift where we belive start of point is
					// saved
		}
		setGoalIdCurrent(goalIdCurrent + 1); // we did work
		addTranslatedStringToOutput(codeForFixingPointersAfterComingBackFromFunctionCall);
		return new ATranslatedExpressionStatement(declaredFunctionsInfo.getReturnType(expr.functionName.lexeme), "",
				myId);
	}

	boolean weAreInAFunctionDefinition = false;
	TokenType returnKeywordIsExpectedToReturnThisType;

	@Override
	public Void visitFunctionDefinitionStmt(FunctionDefinition stmt) {

		// compile the body too
		// it appeared now, so it must be compiled now, so we also know if eventual
		// global variables have been used without being declared first!
		weAreInAFunctionDefinition = true;
		returnKeywordIsExpectedToReturnThisType = convertFormalTypeToActualType(stmt.returnType.type);
		closeDefruleIfNeeded();
		someoneHasToOpenDefrule = true;
		int indexBeforeAddingFunction = outputPER.size();
		openNewDefruleIfNeeded();
		addTranslatedStringToOutput("$iAm:" + stmt.name.lexeme + "\n");
		// before visiting the block statements we should add the declaration of the
		// parameters so that they initialize themeselves with
		// parameters found on the stack too
		List<String> keys = new ArrayList<>(stmt.arguments.keySet());
		Collections.reverse(keys);
		for (String argumentName : keys) {
			Expr initializer = null;
			Token fakeToken = new Token(TokenType.IDENTIFIER, argumentName, argumentName, -99999999);
			stmt.body.statements.add(0, (new Stmt.Var(fakeToken, initializer,
					convertFormalTypeToActualType(stmt.arguments.get(argumentName)))));
		}
		if (!(stmt.body.statements.get(stmt.body.statements.size() - 1) instanceof Stmt.Return)) {
			if (returnKeywordIsExpectedToReturnThisType.equals(TokenType.NULL)) {
				stmt.body.statements.add(new Stmt.Return(null, null));
			} else {
				throw new CompiletimeError(stmt.name,
						"Missing 'return' statement for the " + stmt.name.lexeme + " function.");
			}
		}
		visitBlockStmt(stmt.body);
		closeDefruleIfNeeded();
		someoneHasToOpenDefrule = true;
		functionsBodiesPER.addAll(outputPER.subList(indexBeforeAddingFunction, outputPER.size()));
		outputPER = outputPER.subList(0, indexBeforeAddingFunction);
		// now we rob this .per code and move it somewhere else !!!!!!
		// we should put it in a block with all other functions, the block gets appended
		// at the top of outputPER once we are done compiling the script, before
		// preprocessing
		weAreInAFunctionDefinition = false;
		return null;
	}

	public Void visitFunctionDefinitionStmtHeader(FunctionDefinition stmt) {
		if (declaredFunctionsInfo.isExistingFunction(stmt.name.lexeme)) {
			throw new CompiletimeError(stmt.name,
					"Trying to redefine function '" + stmt.name.lexeme + "', but it already exists.");
		}
		List<TokenType> argumentsTypes = new ArrayList<TokenType>();
		for (String argumentName : stmt.arguments.keySet()) {
			argumentsTypes.add(convertFormalTypeToActualType(stmt.arguments.get(argumentName)));
		}

		declaredFunctionsInfo.declareFunction(stmt.name.lexeme, stmt.arguments.size(), argumentsTypes,
				convertFormalTypeToActualType(stmt.returnType.type));
		return null;
	}

	private void addPushAndPopBackgroundFunctions() {
		int indexBeforeAddingFunction = outputPER.size();

		// push
		addTranslatedStringToOutput("(defrule\n");
		addTranslatedStringToOutput("(true)\n");
		addTranslatedStringToOutput("=>\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdTemporary + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerCallStack + " c:+ 1)\n");
		addTranslatedStringToOutput(
				"(up-set-indirect-goal g: " + goalIdPointerCallStack + " g: " + goalIdTemporary + ")\n");

		addTranslatedStringToOutput("(up-modify-goal " + goalIdTemporaryWorkRegistersPointer + " c:= "
				+ goalIdNumberOfFirstWorkRegister + ")\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdLoopIPushAndPop + " c:= 0)\n");
		addTranslatedStringToOutput(")\n");

		addTranslatedStringToOutput("(defrule\n");
		addTranslatedStringToOutput("(up-compare-goal " + goalIdLoopIPushAndPop + " g:< "
				+ goalIdHowManyWorkRegistersToPushOnStack + ")\n");
		addTranslatedStringToOutput("=>\n");
		addTranslatedStringToOutput(
				"(up-get-indirect-goal g: " + goalIdTemporaryWorkRegistersPointer + " " + goalIdTemporary + ")\n");
		addTranslatedStringToOutput(
				"(up-set-indirect-goal g: " + goalIdPointerVarsStack + " g: " + goalIdTemporary + ")\n");

		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerVarsStack + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdTemporaryWorkRegistersPointer + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdLoopIPushAndPop + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-jump-rule -1)\n");
		addTranslatedStringToOutput(")\n");

		addTranslatedStringToOutput("(defrule\n");
		addTranslatedStringToOutput("(true)\n");
		addTranslatedStringToOutput("=>\n");
		addTranslatedStringToOutput("(up-set-indirect-goal g: " + goalIdPointerVarsStack + " g: "
				+ goalIdHowManyWorkRegistersToPushOnStack + ")\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerVarsStack + " c:+ 1)\n");

		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerBlockNestingDepthStack + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-set-indirect-goal g: " + goalIdPointerBlockNestingDepthStack + " g: "
				+ goalIdWithBlockNestingDepth + ")\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdWithBlockNestingDepth + " c:= -1)\n");

		addTranslatedStringToOutput("(up-modify-goal " + goalIdLoopIPushAndPop + " c:= 0)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdWorkRegistersPointerForCopyingParameters + " c:= "
				+ goalIdNumberOfFirstWorkRegister + ")\n");
		addTranslatedStringToOutput(")\n");
		// part of the push that copies inputs:
		addTranslatedStringToOutput("(defrule\n");
		addTranslatedStringToOutput("(up-compare-goal " + goalIdLoopIPushAndPop + " g:< "
				+ goalIdHowManyGoalsToCopyForInputParameters + ")\n");
		addTranslatedStringToOutput("=>\n");
		addTranslatedStringToOutput(
				"(up-get-indirect-goal g: " + goalIdTemporaryWorkRegistersPointer + " " + goalIdTemporary + ")\n");
		addTranslatedStringToOutput("(up-set-indirect-goal g: " + goalIdWorkRegistersPointerForCopyingParameters
				+ " g: " + goalIdTemporary + ")\n");

		addTranslatedStringToOutput("(up-modify-goal " + goalIdTemporaryWorkRegistersPointer + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdWorkRegistersPointerForCopyingParameters + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdLoopIPushAndPop + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-jump-rule -1)\n");
		addTranslatedStringToOutput(")\n");

		addTranslatedStringToOutput("(defrule\n");
		addTranslatedStringToOutput("(true)\n");
		addTranslatedStringToOutput("=>\n");
		// rather than jumping directly to goalIdFunctionToJumpToAfterPushId, it would
		// now be appropriate to copy inputs' goalsIds of the function from the bottom
		// work-registers to the top work-registers
		addTranslatedStringToOutput("(up-jump-direct g: " + goalIdFunctionToJumpToAfterPushId + ")\n");
		addTranslatedStringToOutput(")\n");

		// pop
		addTranslatedStringToOutput("(defrule\n");
		addTranslatedStringToOutput("(true)\n");
		addTranslatedStringToOutput("=>\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerVarsStack + " c:- 1)\n");
		addTranslatedStringToOutput("(up-get-indirect-goal g: " + goalIdPointerVarsStack + " "
				+ goalIdHowManyWorkRegistersToPushOnStack + ")\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerVarsStack + " c:- 1)\n");

		addTranslatedStringToOutput("(up-modify-goal " + goalIdTemporaryWorkRegistersPointer + " g:= "
				+ goalIdHowManyWorkRegistersToPushOnStack + ")\n");
		// addTranslatedStringToOutput("(up-modify-goal " +
		// goalIdTemporaryWorkRegistersPointer + " c:+
		// "+(goalIdFirstWorkRegister)+")\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdTemporaryWorkRegistersPointer + " c:+ "
				+ (goalIdNumberOfFirstWorkRegister - 1) + ")\n");
		// copy the last two goals
		/*
		 * addTranslatedStringToOutput("(up-set-indirect-goal g: " +
		 * goalIdTemporaryWorkRegistersPointer + " g: " + returnValue1 + ")\n");
		 * addTranslatedStringToOutput("(up-modify-goal " +
		 * goalIdTemporaryWorkRegistersPointer + " c:+ 1)\n");
		 * addTranslatedStringToOutput("(up-set-indirect-goal g: " +
		 * goalIdTemporaryWorkRegistersPointer + " g: " + returnValue2 + ")\n");
		 * addTranslatedStringToOutput("(up-modify-goal " +
		 * goalIdTemporaryWorkRegistersPointer + " c:- 2)\n");
		 */

		addTranslatedStringToOutput("(up-modify-goal " + goalIdLoopIPushAndPop + " c:= 0)\n");
		addTranslatedStringToOutput(")\n");

		addTranslatedStringToOutput("(defrule\n");
		addTranslatedStringToOutput("(up-compare-goal " + goalIdLoopIPushAndPop + " g:< "
				+ goalIdHowManyWorkRegistersToPushOnStack + ")\n");
		addTranslatedStringToOutput("=>\n");
		addTranslatedStringToOutput(
				"(up-get-indirect-goal g: " + goalIdPointerVarsStack + " " + goalIdTemporary + ")\n");
		addTranslatedStringToOutput(
				"(up-set-indirect-goal g: " + goalIdTemporaryWorkRegistersPointer + " g: " + goalIdTemporary + ")\n");

		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerVarsStack + " c:- 1)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdTemporaryWorkRegistersPointer + " c:- 1)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdLoopIPushAndPop + " c:+ 1)\n");
		addTranslatedStringToOutput("(up-jump-rule -1)\n");
		addTranslatedStringToOutput(")\n");

		addTranslatedStringToOutput("(defrule\n");
		addTranslatedStringToOutput("(true)\n");
		addTranslatedStringToOutput("=>\n");
		addTranslatedStringToOutput("(up-get-indirect-goal g: " + goalIdPointerBlockNestingDepthStack + " "
				+ goalIdWithBlockNestingDepth + ")\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerBlockNestingDepthStack + " c:- 1)\n");
		addTranslatedStringToOutput("(up-modify-goal " + goalIdPointerVarsStack + " c:+ 1)\n");
		addTranslatedStringToOutput(
				"(up-get-indirect-goal g: " + goalIdPointerCallStack + " " + goalIdTemporary + ")\n");
		addTranslatedStringToOutput("(up-jump-direct g: " + goalIdTemporary + ")\n");
		addTranslatedStringToOutput(")\n");
		functionsBodiesPER.addAll(0, outputPER.subList(indexBeforeAddingFunction, outputPER.size()));
		outputPER = outputPER.subList(0, indexBeforeAddingFunction);
	}

	private TokenType convertFormalTypeToActualType(TokenType formalType) {
		if (formalType.equals(INT)) {
			return NUMBER_INTEGER;
		}
		if (formalType.equals(TokenType.TIMER)) {
			return TokenType.TIMER_OBJECT;
		}
		if (formalType.equals(POINT)) {
			return TokenType.POINT_OBJECT;
		}
		if (formalType.equals(TokenType.VOID)) {
			return TokenType.NULL;
		}
		// it's already fine
		return formalType;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		if (!weAreInAFunctionDefinition) {
			throw new CompiletimeError(stmt.name, "'return' statements may appear only inside bodies of functions.");
		}
		if (stmt.expression == null && returnKeywordIsExpectedToReturnThisType != TokenType.NULL) {
			throw new CompiletimeError(stmt.name, "'return' statement requiers an expression of type "
					+ returnKeywordIsExpectedToReturnThisType + " to be returned.");
		}
		if (stmt.expression != null && returnKeywordIsExpectedToReturnThisType == TokenType.NULL) {
			throw new CompiletimeError(stmt.name,
					"'return' statement should be left empty without argument, since it's function returns '"
							+ returnKeywordIsExpectedToReturnThisType + ".");
		}

		openNewDefruleIfNeeded();
		if (returnKeywordIsExpectedToReturnThisType != TokenType.NULL) {
			ATranslatedExpressionStatement returnedArgument = evaluate(stmt.expression);
			if (!returnedArgument.type.equals(returnKeywordIsExpectedToReturnThisType)) {
				throw new CompiletimeError(stmt.name,
						"'return' statement type mismatch.\nGot a " + returnedArgument.type + ", expeted a "
								+ returnKeywordIsExpectedToReturnThisType + " to be returned.");
			}
			addTranslatedStringToOutput(returnedArgument.getTranslatedCode());
			addTranslatedStringToOutput("(up-modify-goal " + returnValue1 + " g:= " + (returnedArgument.id) + ")\n");
			if (returnKeywordIsExpectedToReturnThisType.equals(TokenType.POINT_OBJECT)) {
				addTranslatedStringToOutput(
						"(up-modify-goal " + returnValue2 + " g:= " + (returnedArgument.id + 1) + ")\n");
			}
		}
		addTranslatedStringToOutput("(up-jump-direct c: " + ruleIdPop + ")\n");
		closeDefruleIfNeeded();
		someoneHasToOpenDefrule = true;
		return null;

	}

}
