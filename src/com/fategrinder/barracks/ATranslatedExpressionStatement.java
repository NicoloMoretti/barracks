package com.fategrinder.barracks;

import java.util.ArrayList;
import java.util.List;

class ATranslatedExpressionStatement { // to accumulate all the code relative to a single 'expression'
										// statement, and knowing it's type as well, and the goals ID that
										// contains the value of the result of all previous mathematical
										// calculations done by the statement
	final Integer id; // where is the result stored
	final TokenType type;// integer or what
	private final List<String> translatedCode = new ArrayList<>();
	public String inlineStringValue;
	public Integer inlineIntValue;

	ATranslatedExpressionStatement(TokenType type, List<String> translatedCode, Integer id) {
		this.type = type;
		this.translatedCode.addAll(translatedCode);
		this.id = id;
	}

	ATranslatedExpressionStatement(TokenType type, String translatedCode, Integer id) { // for literals and resolved
																						// variables that produce 1
																						// .per line at most,
		this.type = type;
		this.translatedCode.add(translatedCode);
		this.id = id;
	}

	List<String> appendAndRetrieve(String perCodeLine) { // so we can generate a new ATranslatedExpressionStatement
															// from another one with one added string

		List<String> newTranslatedCode = new ArrayList<>(translatedCode); // so every time we work with a new list and
																			// not
																			// always passing the same list between
																			// multiple objects
		newTranslatedCode.add(perCodeLine);
		return newTranslatedCode;
	}

	public List<String> getTranslatedCode() {
		List<String> newTranslatedCode = new ArrayList<>(translatedCode); // so every time we work a new list and not
		// always passing the same list between
		// multiple objects
		return newTranslatedCode;
	}

	public List<String> mergeTranslatedExpressionStatements(ATranslatedExpressionStatement... subarrays) {
		List<String> newTranslatedCode = new ArrayList<>(translatedCode);

		for (ATranslatedExpressionStatement array : subarrays) {
			newTranslatedCode.addAll(array.translatedCode);
		}

		return newTranslatedCode;
	}
	
	public String removeLast() {
		return translatedCode.remove(translatedCode.size()-1);
	}

}