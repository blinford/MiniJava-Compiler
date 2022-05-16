package miniJava.ContextualAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.Declaration;

public class IdentificationTable {
	
	private Stack<HashMap<String, Declaration>> table;
	
	private ErrorReporter reporter;
	
	public IdentificationTable(ErrorReporter reporter) {
		
		this.table = new Stack<HashMap<String, Declaration>>();
		
		this.reporter = reporter;
	}
	
	public void openScope() {
		
		table.add(new HashMap<String, Declaration>());
	}
	
	public void closeScope() {
		
		table.pop();
	}

	public void enter(Declaration d) {
		
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
		
		return null;
	}
	
	public int getIndex(String s) {
		
		for(int i = table.size() - 1; i >= 0; i--) {
			
			Declaration result = table.get(i).get(s);
			
			if(result != null) {
				
				return i;
			}
		}
		
		return -1;
	}
	
	public void print() {
		
		for(HashMap<String, Declaration> scope : table) {
			scope.entrySet().forEach(entry -> {
			    System.out.println(entry.getKey() + " " + entry.getValue());
			});
		}
	}
}
