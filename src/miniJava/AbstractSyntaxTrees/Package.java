/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Package extends AST {

  public Package(ClassDeclList cdl, SourcePosition posn) {
	  
    super(posn);
    
    classDeclList = new ClassDeclList();
    
    classDeclList.add(getSystem());
    classDeclList.add(getPrintStream());
    classDeclList.add(getString());
    
    for(ClassDecl cd : cdl) {
    	
    	classDeclList.add(cd);
    }
  }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitPackage(this, o);
    }

    public ClassDeclList classDeclList;
	
	private ClassDecl getSystem() {
		
		String className = "System";
		
		FieldDeclList fieldDecls = new FieldDeclList();
		MethodDeclList methodDecls = new MethodDeclList();
		
		Identifier id = new Identifier(new Token(TokenKind.ID, "_PrintStream", null));
		
		TypeDenoter type = new ClassType(id, null);
		
		FieldDecl fieldDecl = new FieldDecl(false, true, type, "out", null, null);
		fieldDecls.add(fieldDecl);
		
		return new ClassDecl(className, null, fieldDecls, methodDecls, null);
	}
	
	private ClassDecl getPrintStream() {
		
		String className = "_PrintStream";
		
		FieldDeclList fieldDecls = new FieldDeclList();
		MethodDeclList methodDecls = new MethodDeclList();
		
		TypeDenoter type = new BaseType(TypeKind.VOID, null);
		
		MemberDecl memberDecl = new FieldDecl(false, false, type, "println", null, null);
		
		ParameterDeclList parameterDecls = new ParameterDeclList();
		
		TypeDenoter parameterType = new BaseType(TypeKind.INT, null);
		
		ParameterDecl parameterDecl = new ParameterDecl(parameterType, "n", null); // int n
		parameterDecls.add(parameterDecl);
		
		StatementList statements = new StatementList();
		
		MethodDecl methodDecl = new MethodDecl(memberDecl, parameterDecls, statements, null);
		methodDecls.add(methodDecl);
		
		return new ClassDecl(className, null, fieldDecls, methodDecls, null);
	}
	
	private ClassDecl getString() {
		
		String className = "String";
		
		FieldDeclList fieldDecls = new FieldDeclList();
		MethodDeclList methodDecls = new MethodDeclList();
		
		return new ClassDecl(className, null, fieldDecls, methodDecls, null);
	} 
}
