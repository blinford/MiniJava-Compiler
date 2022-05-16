package miniJava.ContextualAnalysis;

import java.util.HashMap;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;

public class IdTable {
	
	private Stack<HashMap<String,Declaration>> table;

	private ErrorReporter reporter;
	
	public ClassDecl currentClass;
	
	public IdTable(ErrorReporter reporter) {
		
		table = new Stack<HashMap<String,Declaration>>();
		
		this.reporter = reporter;
	}
	
	private IdTable(Stack<HashMap<String,Declaration>> table) {
		
		this.table = table;
	}
	
	public void openScope() {
		
		table.add(new HashMap<String,Declaration>());
	}
	
	public void closeScope() {
		
		table.pop();
	}
	
	public void enter(Declaration d) {
		
		// if local scope, check for duplicate variable names
		
		if(table.size() >= 4) {
			
			for(int i = 2; i < table.size(); i++) {
				
				if(table.get(i).containsKey(d.name)) {
					
					reporter.reportError("*** line "+d.posn+": duplicate variable name");
					
					break;
				}
			}
		}
		
		if(table.peek().containsKey(d.name)) {
			
			reporter.reportError("*** line "+d.posn+": duplicate variable name");
		}
		else {
			
			table.peek().put(d.name, d);
		}
	}
	
	public Declaration retrieve(String s) {
		
		for(int i = table.size() - 1; i >= 0; i--) {
			
			Declaration result = table.get(i).get(s);
			
			if(result != null) {
				
				return result;
			}
		}
		
		return null; // returns null if no declaration found
	}
	
	public IdTable copy() {
		
		Stack<HashMap<String,Declaration>> table = new Stack<HashMap<String,Declaration>>();
		
		table.addAll(this.table);
		
		return new IdTable(table);
	}
	
	public void print() {
		
		for(HashMap<String,Declaration> map : table) {
			map.entrySet().forEach(entry -> {
			    System.out.println(entry.getKey() + " " + entry.getValue());
			});
		}
	}
	
	public static HashMap<String,IdTable> getClassTables(ClassDeclList cdl, ErrorReporter reporter) {
		
		IdTable table = new IdTable(reporter);
		
		table.openScope();
		
		for(ClassDecl cd : cdl) {
			
			table.enter(cd);
		}
		
		HashMap<String,IdTable> classTables = new HashMap<String,IdTable>();
		
		for(ClassDecl cd : cdl) {
			
			IdTable classTable = table.copy();
			
			classTable.currentClass = cd;
			
			classTable.openScope();
			
			for(FieldDecl fd : cd.fieldDeclList) {
				classTable.enter(fd);
			}
			for(MethodDecl md : cd.methodDeclList) {
				classTable.enter(md);
			}
			
			classTables.put(cd.name, classTable);
		}
		
		return classTables;
	}
}
