package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	
	private int posn;
	
	public SourcePosition(int posn) {
		
		this.posn = posn;
	}
	
	public String toString() {
		
		return ""+posn;
	}
}
