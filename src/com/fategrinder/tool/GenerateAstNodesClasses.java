package com.fategrinder.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;



public class GenerateAstNodesClasses {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Gimme a SINGLE output directory");
			System.exit(64);
		}
		String outputDir = args[0];
		
	    defineAst(outputDir, "Expr", Arrays.asList(
	    		  "Assign   : Token name, Expr value",
	    	      "Binary   : Expr left, Token operator, Expr right",
	    	      "Call     : Expr callee, Token paren, List<Expr> arguments",
	    	      "Get      : Expr object, Token name",
	    	      "Grouping : Expr expression",
	    	      "Literal  : Object value, TokenType type",
	    	      "CommandExpression  : Token commandName, List<Expr> expressions, CommandPer commandPer",
	    	      "functionCallExpression  : Token functionName, List<Expr> arguments, Token closingParenthesis",
	    	      "Set      : Expr object, Token name, Expr value",
	    	      "Unary    : Token operator, Expr right",
	    	      "Variable : Token name"
	    	    ));
	    
	    defineAst(outputDir, "Stmt", Arrays.asList(
	    		  "Block      : List<Stmt> statements",
	    	      "Expression : Expr expression",
	    	      "If         : List<Stmt> condition, List<Stmt> thenBranch," +
	    	    		  		" Stmt.If elifBranch," +
	    	    		  		" List<Stmt> elseBranch",
                  "While      : List<Stmt> condition, List<Stmt> body",
                  "LogicalGrouping  :  Token operator, List<Stmt> operands",
	    	      "Print      : Expr expression",
	    	      "Var        : Token name, Expr initializer, TokenType type", //because declaration is a statement, while retrieving inside a statement is an expression
	    	      "Const      : Token name, Literal value",
	    	      "Break      : Token name",
	    	      "Continue   : Token name",
	    	      "DisableSelf   : Token name",
	    	      "LoadIf : boolean ifDefined, List<Stmt> statements, List<Stmt> elseStatements, Token symbol",
	    	      "ParamInitialization      : Token alias, Token actualParameterName",
	    	      "ParamAssignment : Token alias, Token actualParameterName",
	    	      "FunctionDefinition : Token name, Token returnType, Map<String,TokenType> arguments, Block body",
	    	      "Return   : Token name, Expr expression"
	    	    ));
	}
	
	private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");

		writer.println("package com.fategrinder.barracks;");
		writer.println();
		writer.println("import java.util.List;");
		writer.println("import java.util.Map;");
		writer.println("import com.fategrinder.barracks.Expr.Literal;");
		writer.println();
		writer.println("abstract class " + baseName + " {\n");
		defineVisitor(writer, baseName, types); //the visitor interface
		
	    //add the base accept() method.
	    writer.println("  abstract <R> R accept(Visitor<R> visitor);");
	    writer.println();

		// The AST classes.
		for (String type : types) {
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(writer, baseName, className, fields);
		}
		
		writer.println("}");
		writer.close();
	}
	
	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("  interface Visitor<R> {");

		for (String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
		}

		writer.println("  }");
	}
	
	private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
		writer.println("  static class " + className + " extends " + baseName + " {");

		// Constructor.
		writer.println("    " + className + "(" + fieldList + ") {");

		// Store parameters in fields.
		String[] fields = fieldList.split(", ");
		for (String field : fields) {
			String name = field.split(" ")[1];
			writer.println("      this." + name + " = " + name + ";");
		}

		writer.println("    }");

		// Fields.
		writer.println();
		for (String field : fields) {
			writer.println("    final " + field + ";");
		}
		
	    // Visitor pattern.
	    writer.println();
	    writer.println("    @Override");
	    writer.println("    <R> R accept(Visitor<R> visitor) {");
	    writer.println("      return visitor.visit" +
	        className + baseName + "(this);");
	    writer.println("    }");

		writer.println("  }\n");
	}
}