package com.fategrinder.barracks;

import java.util.List;

public class Elif {
	
	public Elif(List<Stmt> condition, List<Stmt> thenBranch) {
		this.condition = condition;
		this.thenBranch = thenBranch;
	}

	final List<Stmt> condition;
	final List<Stmt> thenBranch;
	
	
}
