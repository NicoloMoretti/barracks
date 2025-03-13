package com.fategrinder.barracks;

public class test {

	
		public static void main(String[] args) {
			ATranslatedExpressionStatement a = new ATranslatedExpressionStatement(null, "oba1", null);
			System.out.println(a.getTranslatedCode());
			
			ATranslatedExpressionStatement b = new ATranslatedExpressionStatement(null, a.appendAndRetrieve("oba2"), null);
			System.out.println(b.getTranslatedCode());
			
			ATranslatedExpressionStatement c = new ATranslatedExpressionStatement(null, a.appendAndRetrieve("oba3"), null);
			System.out.println(c.getTranslatedCode());
			
			System.out.println(a.getTranslatedCode());
			
			ATranslatedExpressionStatement d = new ATranslatedExpressionStatement(null, b.mergeTranslatedExpressionStatements(c), null);
			System.out.println(d.getTranslatedCode());
		}
}
