package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.CodeGeneration;
import miniJava.ContextualAnalysis.Environment;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.TypeChecking;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {

	private static final String TEST_FILE = null; // manually provide test file, use args[0] if null
	
	public static void main(String[] args) {

		InputStream inputStream = null;
		
		String filePath = TEST_FILE == null ? args[0] : TEST_FILE;
		
		try {
			
			inputStream = new FileInputStream(filePath);
		}
		catch(FileNotFoundException e) {
			
			System.out.println("Input file " + args[0] + " not found");
			System.exit(3);
		}
		
		ErrorReporter reporter = new ErrorReporter();
		
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);
		
		AST ast = parser.parse();
		
		// new ASTDisplay().showTree(ast);
		checkErrors(reporter);
		
		Environment env = new Environment((Package) ast, reporter);
		
		new Identification((Package) ast, env, reporter);

		checkErrors(reporter);
		
		new TypeChecking((Package) ast, env, reporter);
		
		checkErrors(reporter);
		
		CodeGeneration code = new CodeGeneration((Package) ast, filePath, reporter);
		
		checkErrors(reporter);
		
		code.generateCode(filePath);
	}
	
	private static void checkErrors(ErrorReporter reporter) {
		
		if (reporter.hasErrors()) {
			
			for(String error : reporter.errors) {
				
				System.out.println(error);
			}
			
			System.exit(4);
		}
	}
	
	
	/*
		InputStream inputStream = new FileInputStream("")
		
		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);
	
		System.out.println("Syntactic analysis ... ");
		parser.parse();
		System.out.print("Syntactic analysis complete:  ");
		
		if (reporter.hasErrors()) {
			System.out.println("INVALID arithmetic expression");
			// return code for invalid input
			System.exit(4);
		}
		else {
			System.out.println("valid arithmetic expression");
			// return code for valid input
			System.exit(0);
		}
	*/
}
