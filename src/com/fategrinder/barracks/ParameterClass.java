package com.fategrinder.barracks;

import java.util.ArrayList;
import java.util.List;

public class ParameterClass {
	public String upParamName;	//interesting
	public Object paramVersion;
	public Object esParamName;
	public String description;
	public String shortDescription;
	public String range;
	public List<String> valueList;		//interesting         I merge all possible values here, wildcard and rule variables deprecated
	public List<Object> wildcardParam;	//interesting   -> deprecated
	public List<Object> relatedParams;
	public List<Object> prefixTypes;
	public List<Object> operatorTypes;
	public List<Object> ruleVariables;	//interesting   -> deprecated
	
	public boolean allowsInteger = false;
	public boolean allowsPoint = false;
	public boolean allowsTimer = false;
	public boolean allowsString = false;

	ParameterClass(String upParamName, String paramVersion, String esParamName) {
		this.upParamName = upParamName;
		this.paramVersion = paramVersion;
		this.esParamName = esParamName;
		this.description = "";
		this.shortDescription = "";
		this.range = "";
		
		valueList = new ArrayList<String>();
	}
	
	ParameterClass(String upParamName, String paramVersion) {
		this(upParamName, paramVersion, null);
	}
	
	public boolean ParameterAllowedInParameterClass(String par) {
		for (String parameter : valueList) {
			if (par.equals(parameter)) {
				return true;
			}
		}
		return false;
	}
}