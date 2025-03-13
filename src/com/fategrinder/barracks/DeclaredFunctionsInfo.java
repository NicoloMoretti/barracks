package com.fategrinder.barracks;

import static com.fategrinder.barracks.TokenType.NUMBER_INTEGER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeclaredFunctionsInfo {
	static List<String> existingFunctions = new ArrayList<String>();
	static Map<String, Integer> functionsArity = new HashMap<>();
	static Map<String, List<TokenType>> functionsExpectedArgumentsTypes = new HashMap<>();
	static Map<String, TokenType> functionsReturnType = new HashMap<>();
	static Map<String, Integer> howManyGoalsToCopyForInputParameters = new HashMap<>();
	//maybe we'll need to make a field to save the functions' code too, who knows
	
	public void declareFunction(String name, int arity, List<TokenType> expectedArgumentsTypes, TokenType returnType) {
		existingFunctions.add(name);
		functionsArity.put(name, (Integer)arity);
		functionsExpectedArgumentsTypes.put(name, expectedArgumentsTypes);
		functionsReturnType.put(name, returnType);
		
		int howManyGoalsToCopyForInputParameters = 0;
		for (TokenType inputType : expectedArgumentsTypes) {
			if (inputType.equals(NUMBER_INTEGER)) {
				howManyGoalsToCopyForInputParameters++;
			}
			if (inputType.equals(TokenType.TIMER_OBJECT)) {
				howManyGoalsToCopyForInputParameters++;
			}
			if (inputType.equals(TokenType.POINT_OBJECT)) {
				howManyGoalsToCopyForInputParameters++;
				howManyGoalsToCopyForInputParameters++;
			}
		}
		this.howManyGoalsToCopyForInputParameters.put(name, (Integer)howManyGoalsToCopyForInputParameters);
	}
	
	public boolean isExistingFunction(String name) {
		return existingFunctions.contains(name);
	}

	public int getArity(String name) {
		return functionsArity.get(name);
	}
	
	public TokenType getExpectedArgumentType(String name, int index) {
		return functionsExpectedArgumentsTypes.get(name).get(index);
	}
	
	public TokenType getReturnType(String name) {
		return functionsReturnType.get(name);
	}
	
	public int getHowManyGoalsToCopyForInputParameters(String name) {
		return howManyGoalsToCopyForInputParameters.get(name);
	}
	
	public void declareNativeFunctions() {
		declareFunction("max(", 2, new ArrayList<TokenType>(Arrays.asList(TokenType.NUMBER_INTEGER, TokenType.NUMBER_INTEGER)), TokenType.NUMBER_INTEGER);
		declareFunction("min(", 2, new ArrayList<TokenType>(Arrays.asList(TokenType.NUMBER_INTEGER, TokenType.NUMBER_INTEGER)), TokenType.NUMBER_INTEGER);
		declareFunction("abs(", 1, new ArrayList<TokenType>(Arrays.asList(TokenType.NUMBER_INTEGER)), TokenType.NUMBER_INTEGER);
	}
}
