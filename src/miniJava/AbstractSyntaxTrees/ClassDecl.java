/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import  miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassDecl extends Declaration {

  public ClassDecl(String cn, Identifier inheritedClassId, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  super(cn, null, posn);
	  
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
	  
	  this.inheritedClassId = inheritedClassId;
	  
	  inheritedClass = null;
  }
  
  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }
  
  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
  
  public Identifier inheritedClassId;
  public ClassDecl inheritedClass;
}