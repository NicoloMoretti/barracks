package com.fategrinder.barracks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fategrinder.barracks.Barracks.CompiletimeError;

class Environment {

	final Environment enclosing;

	Environment() { // this constructor is only for the global environment which isn't enclosed
		enclosing = null;
	}

	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	final Map<String, VariableInfo> variablesGoals = new HashMap<>();

	class VariableInfo { // environment will store objects of this type in it's map because they allow to
							// easily know goals IDs and do type checking
		final Integer id;
		TokenType type;
		final boolean isConst;
		TokenType initializationStatus;
		boolean isSN = false;
		boolean isParam = false;
		boolean isCommand = false;
		boolean isReadOnly = false;
		String associatedActualParameterName = null;
		boolean isGhostGlobal = false;

		public VariableInfo(Integer id, TokenType type, boolean isConst, TokenType initializationStatus) {
			this.id = id;
			this.type = type;
			this.isConst = isConst;
			this.initializationStatus = initializationStatus;
		}
		
		public VariableInfo(Integer id, TokenType type, boolean isConst, TokenType initializationStatus, boolean isGhostGlobal) {
			this.id = id;
			this.type = type;
			this.isConst = isConst;
			this.initializationStatus = initializationStatus;
			this.isGhostGlobal = isGhostGlobal;
		}

		public VariableInfo(Integer id, TokenType type, boolean isConst, TokenType initializationStatus, boolean isSN,
				boolean isParam, boolean isCommand, boolean isReadOnly) {
			this(id, type, isConst, initializationStatus);
			this.isSN = isSN;
			this.isParam = isParam;
			this.isCommand = isCommand;
			this.isReadOnly = isReadOnly;
		}
		
		public VariableInfo(Integer id, TokenType type, boolean isConst, TokenType initializationStatus, boolean isSN,
				boolean isParam, boolean isCommand, boolean isReadOnly, String associatedActualParameterName) {
			this(id, type, isConst, initializationStatus, isSN, isParam, isCommand, isReadOnly);
			this.associatedActualParameterName = associatedActualParameterName;
		}
	}

	void define(String name, int id, TokenType type, boolean isConst, TokenType initializationStatus, boolean isGhostGlobal) {
		variablesGoals.put(name, new VariableInfo(id, type, isConst, initializationStatus, isGhostGlobal)); // will overwrite, that's
																								// why we launch define
																								// even when
		// assigning
		// should probably create an "assign" wrapper method that throws error if never
		// defined, ahh...
	}
	
	void define(String name, int id, TokenType type, boolean isConst, TokenType initializationStatus) {
		variablesGoals.put(name, new VariableInfo(id, type, isConst, initializationStatus)); // will overwrite, that's
																								// why we launch define
																								// even when
		// assigning
		// should probably create an "assign" wrapper method that throws error if never
		// defined, ahh...
	}

	void define(String name, int id, TokenType type, boolean isConst, TokenType initializationStatus, boolean isSN,
			boolean isParam, boolean isCommand, boolean isReadOnly) {
		variablesGoals.put(name, new VariableInfo(id, type, isConst, initializationStatus, isSN, isParam, isCommand, isReadOnly)); // will
																											// overwrite,
		// that's why we
		// launch define
		// even when
		// assigning
		// should probably create an "assign" wrapper method that throws error if never
		// defined, ahh...
	}
	
	void define(String name, int id, TokenType type, boolean isConst, TokenType initializationStatus, boolean isSN,
			boolean isParam, boolean isCommand, boolean isReadOnly, String associatedActualParameterName) {
		variablesGoals.put(name, new VariableInfo(id, type, isConst, initializationStatus, isSN, isParam, isCommand, isReadOnly, associatedActualParameterName));
	}

	boolean isDefined(Token name) {
		if (variablesGoals.containsKey(name.lexeme)) {
			return true;
		}

		if (enclosing != null) {
			return enclosing.isDefined(name);
		}

		return false;
	}

	boolean isDefinedExactelyHere(Token name) { // to check for redefinition in same scope
		if (variablesGoals.containsKey(name.lexeme)) {
			return true;
		}

		return false;
	}

	VariableInfo get(Token name) {
		if (variablesGoals.containsKey(name.lexeme)) {
			return variablesGoals.get(name.lexeme);
		}

		if (enclosing != null)
			return enclosing.get(name);
		throw new CompiletimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	void fillGlobalEnvironmentWithAllSN() {

		StrategicNumbersContainer strategicNumberContainer = new StrategicNumbersContainer();
		for (StrategicNumber sn : strategicNumberContainer.snArrayById) {
			if (sn.de == 1) {
				this.define(sn.snName, 0, TokenType.NUMBER_INTEGER, false, TokenType.INITIALIZED, true, false, false, false);
			}
		}
	}

	public static final List<String> parameterNamesOverwrittenByCommandsNames = new ArrayList<>();
	void fillGlobalEnvironmentWithAllParameters() {

		ParameterClassesContainer parametersContainer = new ParameterClassesContainer();
		CommandsContainer commandsContainer = new CommandsContainer();
		for (ParameterClass paramClass : parametersContainer.parametersArray) {
			for (String param : paramClass.valueList) {
				if (commandsContainer.isACommandName(param)) {
					parameterNamesOverwrittenByCommandsNames.add(param);
				}
				this.define(param, 0, TokenType.PARAMATER_NATIVE_OBJECT, false, TokenType.INITIALIZED, false, true, false, false);
			}
		}
	}
	
	void fillGlobalEnvironmentWithAllLoadIfSymbols() {
		LoadIfSymbolsContainer loadIfSymbolsContainer = new LoadIfSymbolsContainer();
		for (String symbol : loadIfSymbolsContainer.loadIfSymbolsList) {
			this.define(symbol, 0, TokenType.LOAD_IF_NATIVE_SYMBOL, false, TokenType.INITIALIZED, false, false, false, false);
		}
	}

	void fillGlobalEnvironmentWithAllCommands() {
		// so that commands may not be used as variables
		CommandsContainer commandsContainer = new CommandsContainer();
		for (CommandPer commandPer : commandsContainer.commandsArray) {
			boolean isAlsoParam = false;
			if (parameterNamesOverwrittenByCommandsNames.contains(commandPer.commandName)) {
				isAlsoParam = true;
			}
			this.define(commandPer.commandName, 0, TokenType.COMMAND_NATIVE_OBJECT, false, TokenType.INITIALIZED,
					false, isAlsoParam, true, false);
		}
	}
	
	int addSearchStatesVariablesToGlobalEnv(int goalIdStart) {
		this.define("search-local-total", goalIdStart+1, TokenType.NUMBER_INTEGER, false, TokenType.INITIALIZED,
				false, false, false, true);
		this.define("search-local-last", goalIdStart+2, TokenType.NUMBER_INTEGER, false, TokenType.INITIALIZED,
				false, false, false, true);
		this.define("search-remote-total", goalIdStart+3, TokenType.NUMBER_INTEGER, false, TokenType.INITIALIZED,
				false, false, false, true);
		this.define("search-remote-last", goalIdStart+4, TokenType.NUMBER_INTEGER, false, TokenType.INITIALIZED,
				false, false, false, true);
		return goalIdStart+1; //so we know where it was saved the first of the 4 consecutive values
	}

}
