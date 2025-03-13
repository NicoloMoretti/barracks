package com.fategrinder.barracks;

import java.util.ArrayList;
import java.util.List;

public class CommandPer {
	public String commandName; // interesting
	public String commandType; // interesting
	public String commandVersion;
	public String shortDescription;
	public String description;
	public List<String> commandParameters; // interesting
	public Object commandExample;
	public List<Object> commandLinks;
	public List<Object> relatedCommands;
	public List<Object> relatedSNs;
	public List<Object> commandCategory;
	public List<Object> complexity;
	public String example;
	public TokenType returnType = TokenType.NULL;

	CommandPer(String commandName, String commandType, String commandVersion) {
		this.commandName = commandName;
		this.commandType = commandType;
		this.commandVersion = commandVersion;
		this.shortDescription = "";
		this.description = "";
		this.commandExample = "";
		this.example = "";

		this.commandParameters = new ArrayList<String>();
	}

	public int howManyParamsRequiredFromBarracksUser() {
		// some commands are special cases and accept one point less (output) etc
		if (commandName.equals("up-bound-point")) {
			return 1;
		}
		if (commandName.equals("up-find-flare")) {
			return 0;
		}
		if (commandName.equals("up-find-player-flare")) {
			return 1;
		}
		if (commandName.equals("up-get-point")) {
			return 1;
		}

		int i = 0;
		for (String param : commandParameters) {
			if (param.equals("mathOp") || param.equals("typeOp") || param.equals("OutputGoalId")
					|| param.equals("ColorId") || param.equals("ThreatSource")) {
				continue;
			}
			i++;
		}
		return i;
	}

	public List<String> getCommandParametersListWithoutParemetersNotRequiredFromBarracksProgrammer() {
		List<String> parametersListWithoutParemetersNotRequiredFromBarracksProgrammer = new ArrayList<String>(
				commandParameters);
		parametersListWithoutParemetersNotRequiredFromBarracksProgrammer
				.removeIf(parameter -> parameter.equals("mathOp") || parameter.equals("typeOp")
						|| parameter.equals("OutputGoalId") || parameter.equals("ColorId")
						|| parameter.equals("ThreatSource"));
		return parametersListWithoutParemetersNotRequiredFromBarracksProgrammer;
	}

}