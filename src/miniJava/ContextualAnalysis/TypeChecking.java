package miniJava.ContextualAnalysis;

import java.util.List;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class TypeChecking implements Visitor<Object,TypeDenoter> {
	
	private Environment env;
	private ErrorReporter reporter;
	
	public TypeChecking(Package prog, Environment env, ErrorReporter reporter) {
		
		this.env = env;
		this.reporter = reporter;
		
		prog.visit(this, null);
	}
	
	private boolean checkClassInheritance(ClassType a, ClassType b) {

		// checks if b inherits a
		// a = A
		// b = B, A
		// everything in a should be found in b
		
		List<String> aExtends = env.getInheritedClasses(a.className.spelling);
		List<String> bExtends = env.getInheritedClasses(b.className.spelling);

		for(int i = 0; i < aExtends.size(); i++) {
			
			if(!bExtends.contains(aExtends.get(i))) {
				
				return false;
			}
		}
		
		return true;
	}
	
	private boolean checkClassEquality(ClassType a, ClassType b) {

		List<String> aExtends = env.getInheritedClasses(a.className.spelling);
		List<String> bExtends = env.getInheritedClasses(b.className.spelling);
		
		for(int i = 0; i < aExtends.size(); i++) {
			
			for(int j = 0; j < bExtends.size(); j++) {
				
				if(aExtends.get(i).equals(bExtends.get(j))) {
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean checkEquality(TypeDenoter a, TypeDenoter b) {
		
		if(a.typeKind == b.typeKind) {
			
			if(a instanceof ClassType && b instanceof ClassType) {

				return checkClassInheritance((ClassType) a, (ClassType) b);
			}
			else if(a instanceof ArrayType && b instanceof ArrayType) {
				
				return checkEquality(((ArrayType) a).eltType, ((ArrayType) b).eltType);
			}
			else {
				
				return true;
			}
		}
		else {
			
			if(a.typeKind == TypeKind.ERROR || b.typeKind == TypeKind.ERROR) {
				
				return true;
			}
			else if((a.typeKind == TypeKind.NULL || b.typeKind == TypeKind.NULL)
					&& (a.typeKind == TypeKind.CLASS || b.typeKind == TypeKind.CLASS)) {
				
				return true;
			}
			else if((a.typeKind == TypeKind.NULL || b.typeKind == TypeKind.NULL)
					&& (a.typeKind == TypeKind.ARRAY || b.typeKind == TypeKind.ARRAY)) {
				
				return true;
			}
			else {
				
				return false;
			}
		}
	}
	
	private boolean checkEquality(TypeDenoter type, TypeKind kind) {
		
		return type.typeKind == kind || type.typeKind == TypeKind.ERROR;
	}

	@Override
	public TypeDenoter visitPackage(Package prog, Object arg) {

		env.openScope();
		
		for(ClassDecl cd : prog.classDeclList) {
			
			env.enter(cd);
		}
		
		for(ClassDecl cd : prog.classDeclList) { 
			
			cd.visit(this, null);
		}
		
		env.closeScope();
		
		return null;
	}

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {

		env.openScope();
		
		for(FieldDecl fd : cd.fieldDeclList) {
			
			env.enter(fd);
		}
		
		for(MethodDecl md : cd.methodDeclList) {
			
			env.enter(md);
		}
		
		env.setClass(cd);
		
		for(FieldDecl fd : cd.fieldDeclList) {
			
			fd.visit(this, null);
		}
		
		for(MethodDecl md : cd.methodDeclList) {
			
			md.visit(this, null);
		}
		
		env.closeScope();
		
		return null;
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
		
		return fd.type;
	}

	private TypeDenoter returnType;
	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
		
		returnType = md.type;
		
		for(Statement stmt : md.statementList) {
			stmt.visit(this, null);
		}
		
		return md.type;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
		
		return pd.type;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
		
		return decl.type;
	}

	@Override
	public TypeDenoter visitBaseType(BaseType type, Object arg) {
		
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, Object arg) {
		
		return type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, Object arg) {
		
		return type;
	}

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
		
		for(Statement s : stmt.sl) {
			
			s.visit(this, null);
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		
		TypeDenoter initExpType = stmt.initExp.visit(this, null);
		
		if(!checkEquality(stmt.varDecl.visit(this, null), initExpType)) {
			
			reporter.reportError("*** line "+stmt.posn+": type mismatch");
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
		TypeDenoter valType = stmt.val.visit(this, null);

		if(valType == null) {
			
			reporter.reportError("*** line "+stmt.posn+": val type null");
		}
		else if(!checkEquality(stmt.ref.visit(this, null), stmt.val.visit(this, null))) {
			
			reporter.reportError("*** line "+stmt.posn+": type mismatch");
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		
		if(!checkEquality(stmt.ref.visit(this, null), TypeKind.ARRAY)) {
			
			reporter.reportError("*** line "+stmt.posn+": expected reference of type ARRAY");
		}

		if(!checkEquality(stmt.ix.visit(this, null), TypeKind.INT)) {
			
			reporter.reportError("*** line "+stmt.posn+": expected index of type INT");
		}
		
		if(!checkEquality(((ArrayType) stmt.ref.visit(this, null)).eltType, stmt.exp.visit(this, null))) {
			
			reporter.reportError("*** line "+stmt.posn+": type mismatch");
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
		
		if(!(stmt.methodRef.decl instanceof MethodDecl)) {
			
			reporter.reportError("*** line "+stmt.posn+": expected method decl");
			
			return new BaseType(TypeKind.ERROR, null);
		}

		MethodDecl methodCalled = (MethodDecl) stmt.methodRef.decl;
		
		ParameterDeclList parameterList = methodCalled.parameterDeclList;
		
		if(stmt.argList.size() != parameterList.size()) {
			
			reporter.reportError("*** line "+stmt.posn+": expected "+parameterList.size()+" arguments, found "+stmt.argList.size());
			
			return new BaseType(TypeKind.ERROR, null);
		}
		
		for(int i = 0; i < stmt.argList.size(); i++) {
			
			if(!checkEquality(parameterList.get(i).visit(this, null), stmt.argList.get(i).visit(this, null))) {
				
				reporter.reportError("*** line "+stmt.posn+": argument type mismatch");
				
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		
		return methodCalled.type;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
		
		if(stmt.returnExpr == null) { 
			
			if(!checkEquality(returnType, TypeKind.VOID)) {
				
				reporter.reportError("*** line "+stmt.posn+": expected VOID return type");
			}
		}
		else {
			
			if(!checkEquality(returnType, stmt.returnExpr.visit(this, null))) {
				
				reporter.reportError("*** line "+stmt.posn+": expected "+returnType.typeKind+" return type");
			}
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
		
		if(!checkEquality(stmt.cond.visit(this, null), TypeKind.BOOLEAN)) {
			
			reporter.reportError("*** line "+stmt.posn+": expected type BOOLEAN");
		}
		
		stmt.thenStmt.visit(this, null);
		
		if(stmt.elseStmt != null) {
			
			stmt.elseStmt.visit(this, null);
		}
		
		return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
		
		if(!checkEquality(stmt.cond.visit(this, null), TypeKind.BOOLEAN)) {
			
			reporter.reportError("*** line "+stmt.posn+": expected type BOOLEAN");
		}
		
		stmt.body.visit(this, null);
	
		return null;
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
		
		TypeDenoter type = expr.expr.visit(this, null);
		
		TypeDenoter returnType = new BaseType(TypeKind.ERROR, null);
		
		switch(expr.operator.kind) {
		
		case NOT:
			if(checkEquality(type, TypeKind.BOOLEAN)) {
				
				returnType = new BaseType(TypeKind.BOOLEAN, null);
			}
			else {
				
				reporter.reportError("*** line "+expr.posn+": expected type BOOLEAN"+type.typeKind);
			}
			
			break;
			
		case SUBTRACT:
			if(checkEquality(type, TypeKind.INT)) {
				
				returnType = new BaseType(TypeKind.INT, null);
			}
			else {
				
				reporter.reportError("*** line "+expr.posn+": expected type INT, found "+type.typeKind);
			}
			
			break;
			
		default:
			break;
		}
		
		return returnType;
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
		
		TypeDenoter left = expr.left.visit(this, null);
		TypeDenoter right = expr.right.visit(this, null);
		
		TypeDenoter returnType = new BaseType(TypeKind.ERROR, null);
		
		switch(expr.operator.kind) {
		
		case ADD: case SUBTRACT: case MULTIPLY: case DIVIDE:
			if(checkEquality(left, TypeKind.INT) && checkEquality(right, TypeKind.INT)) {
				
				returnType = new BaseType(TypeKind.INT, null);
			}
			else {
				
				reporter.reportError("*** line "+expr.posn+": expected INT"+expr.operator.spelling+"INT, found "+left.typeKind+expr.operator.spelling+right.typeKind);
			}
			break;
		case GT: case GTE: case LT: case LTE:
			if(checkEquality(left, TypeKind.INT) && checkEquality(right, TypeKind.INT)) {
				
				returnType = new BaseType(TypeKind.BOOLEAN, null);
			}
			else {
				
				reporter.reportError("*** line "+expr.posn+": expected INT"+expr.operator.spelling+"INT, found "+left.typeKind+expr.operator.spelling+right.typeKind);
			}
			
			break;
		case EQ: case NEQ:
			if(checkEquality(left, right)) {
				
				returnType = new BaseType(TypeKind.BOOLEAN, null);
			}
			else {
				
				reporter.reportError("*** line "+expr.posn+": expected Type"+expr.operator.spelling+"Type, found "+left.typeKind+expr.operator.spelling+right.typeKind);
			}
			break;
		case AND: case OR:
			if(checkEquality(left, TypeKind.BOOLEAN) && checkEquality(right, TypeKind.BOOLEAN)) {
				
				returnType = new BaseType(TypeKind.BOOLEAN, null);
			}
			else {
				
				reporter.reportError("*** line "+expr.posn+": expected BOOLEAN"+expr.operator.spelling+"BOOLEAN, found "+left.typeKind+expr.operator.spelling+right.typeKind);
			}
			break;
		default:
			reporter.reportError("*** line "+expr.posn+": unexpected error");
		}
		
		return returnType;
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
		
		TypeDenoter resultType = expr.ref.visit(this, null);
		
		if(expr.ref instanceof ThisRef) {
			
			return resultType;
		}

		/*
		if(env.getIndex(expr.ref.decl.name) == 0) {
			
			reporter.reportError("*** line "+expr.posn+": expected variable");
			
			return new BaseType(TypeKind.ERROR, null);
		}
		*/
		
		if(resultType != null && resultType.typeKind == TypeKind.CLASS && ((ClassType) resultType).className.spelling.equals("String")) {
			
			reporter.reportError("*** line "+expr.posn+": unsupported type found");
			
			return new BaseType(TypeKind.ERROR, null);
		}
			
		return resultType;
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
		
		if(expr.ref.decl.type.typeKind != TypeKind.ARRAY) {
			
			reporter.reportError("*** line "+expr.posn+": expected reference of type ARRAY");
			
			return new BaseType(TypeKind.ERROR, null);
		}
		
		if(!checkEquality(expr.ixExpr.visit(this, null), TypeKind.INT)) {
			
			reporter.reportError("*** line "+expr.posn+": expected index of type INT");
			
			return new BaseType(TypeKind.ERROR, null);
		}
		
		TypeDenoter resultType = ((ArrayType) expr.ref.visit(this, null)).eltType;
		
		if(resultType.typeKind == TypeKind.CLASS && ((ClassType) resultType).className.spelling.equals("String")) {
			
			reporter.reportError("*** line "+expr.posn+": unsupported type found");
		}
		
		return resultType;
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {

		MethodDecl methodCalled = (MethodDecl) expr.functionRef.decl;
		
		ParameterDeclList parameterList = methodCalled.parameterDeclList;
		
		if(expr.argList.size() != parameterList.size()) {
			
			reporter.reportError("*** line "+expr.posn+": expected "+parameterList.size()+" arguments, found "+expr.argList.size());
			
			return new BaseType(TypeKind.ERROR, null);
		}
		
		for(int i = 0; i < expr.argList.size(); i++) {
			
			if(!checkEquality(expr.argList.get(i).visit(this, null), parameterList.get(i).visit(this, null))) {
				
				reporter.reportError("*** line "+expr.posn+": argument type mismatch");
				
				return new BaseType(TypeKind.ERROR, null);
			}
		}
		
		return methodCalled.type;
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
		
		return expr.lit.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		
		return expr.classtype;
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {

		if(!checkEquality(expr.sizeExpr.visit(this, null), TypeKind.INT)) {
			
			reporter.reportError("*** line "+expr.posn+": expected int expression");
			
			return new BaseType(TypeKind.ERROR, null);
		}
		
		return new ArrayType(expr.eltType, null);
	}

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
		
		Token token = new Token(TokenKind.CLASS, ref.className, null);
		Identifier id = new Identifier(token);
		TypeDenoter result = new ClassType(id, null);
		return result;
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, Object arg) {
		
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, Object arg) {
		
		if(ref.ref.decl != null && ref.ref.decl.type != null && ref.ref.decl.type instanceof ArrayType) {
			
			if(ref.decl != null && ref.decl.name.equals("length")) {
				
				return new BaseType(TypeKind.INT, null);
			}
		}

		if(ref.decl == null || ref.decl.type == null) {
			
			return new BaseType(TypeKind.ERROR, null);
		}
		
		return ref.decl.type;
	}

	@Override
	public TypeDenoter visitIdentifier(Identifier id, Object arg) {
		
		return null;
	}

	@Override
	public TypeDenoter visitOperator(Operator op, Object arg) {
		
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
		
		return new BaseType(TypeKind.INT, null);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {

		return new BaseType(TypeKind.BOOLEAN, null);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral nul, Object arg) {
		
		return new BaseType(TypeKind.NULL, null);
	}

}
