package miniJava.CodeGenerator;

import mJAM.Machine.Reg;

public class Description {
	
	// offset relative to LB for local variables and parameter variables
	// offset relative to SB for static fields
	// offset relative to OB for instance variables
	// offset relative to CB for methods

	public int size, offset;
	public Reg register;
	
	public Description(int offset, Reg register) {
		
		this.offset = offset;
		this.register = register;
	}
	
	public String toString() {
		switch(register) {
		case LB:
			return "LB "+offset;
		case SB:
			return "SB "+offset;
		case OB:
			return "OB "+offset;
		case CB:
			return "CB "+offset;
		default:
			return "";
		}
	}
}
