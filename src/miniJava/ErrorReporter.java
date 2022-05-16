package miniJava;

import java.util.ArrayList;
import java.util.List;

public class ErrorReporter {

	List<String> errors;
	
	public ErrorReporter() {
		
		errors = new ArrayList<String>();
	}
	
	public void reportError(String string) {
		
		errors.add(string);
	}

	public boolean hasErrors() {
		
		return !errors.isEmpty();
	}

	public void printErrors() {
		
		for(String error : errors) {
			
			System.out.println(error);
		}
	}
}
