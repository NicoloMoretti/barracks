package com.fategrinder.barracks;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Barracks {
	
	private static final int barracksVersion = 11;
	
	public static void main(String[] args) throws IOException {
		translateSingleFile(args);
	}

	private static void translateSingleFile(String[] args) throws IOException {
		if (args.length != 1) { // to accept only one specified path
			System.out.println("Must specify as a single parameter the '.brk' file path.");
			System.exit(64);
		} else {
			if (!args[0].endsWith(".brk")) {
				if (args[0].equals("--version")) {
					System.out.println("Installed Barracks version is: " + barracksVersion + " .");
					System.exit(67);
				}
				System.out.println("Expected a .brk file but got -> " + args[0]);
				System.exit(67);
			}
			runFile(args[0]);
		}
	}

	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()), path);
		closeProgramIfErrors();
	}

	private static void closeProgramIfErrors() {
		if (hadError)
			System.exit(65);
		if (hadCompiletimeError)
			System.exit(70);
	}

	private static void run(String source, String path) throws IOException {
		final Scanner scanner = new Scanner(source);
		final List<Token> tokens = scanner.scanAllTokens();
		/*
		 * for (Token token : tokens) { System.out.println(token); }
		 */
		if (hadError)
			return; // return early to stop following phases if input is trash

		final Parser parser = new Parser(tokens);
		final List<Stmt> statements = parser.parse();
		/*
		 * for (Stmt statement : statements) { System.out.println(statement); }
		 */
		if (hadError)
			return;

		final Compiler compiler = new Compiler();
		compiler.compile(statements);

		if (hadCompiletimeError)
			return;

		String compiledText = compiler.elaborateFinalOutputPER();
		//System.out.println(compiledText);
		writeOutputFile(Paths.get(path), compiledText);
	}

	private static void writeOutputFile(Path inputPath, String content) throws IOException {
		String outputFileName = inputPath.getFileName().toString().replace(".brk", ".ai");
		Path outputPath = inputPath.getParent().resolve(outputFileName);
		Files.write(outputPath, content.getBytes(StandardCharsets.UTF_8));
		
		outputFileName = inputPath.getFileName().toString().replace(".brk", ".per");
		outputPath = inputPath.getParent().resolve(outputFileName);
		Files.write(outputPath, content.getBytes(StandardCharsets.UTF_8));
		System.out.println("Output written to: " + outputPath);
	}

	// -----------------error management-------------------------
	static boolean hadError = false;
	static boolean hadCompiletimeError = false; // to exit program with different error code

	private static void printGenericError(int line, String lexeme, String message) {
		System.err.println("[line " + line + "] Error" + lexeme + ": " + message);
		hadError = true; // because reporting an error means there's been an error
	}

	static void printScannerError(int line, String message) {
		printGenericError(line, "", message); // for wrapping lexeme-less errors from scanner
	}

	static void printParserError(Token token, String message) {
		if (token.type == TokenType.EOF) {
			printGenericError(token.line, " at end", message);
		} else {
			printGenericError(token.line, " at '" + token.lexeme + "'", message);
		}
	}

	static void compiletimeError(CompiletimeError error) { // to print costume compile time errors
		hadCompiletimeError = true;
		if (error.token != null) {
			System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
		} else {
			// despaired case without token
			System.err.println(error.getMessage() + "\n");
		}
		closeProgramIfErrors(); // this stops the compiling work after reporting the first error, semnthic is
								// complex and often means there's no way to synchronize, unlike syntax
	}

	static class CompiletimeError extends RuntimeException { // costum error to catch and just call support function to
																// print it while possibly continuing to compile
		final Token token;

		CompiletimeError(Token token, String message) {
			super(message);
			this.token = token;
		}
	}

	// -------------------------------------------------------------------------------
}
