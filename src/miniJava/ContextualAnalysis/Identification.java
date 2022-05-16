package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Identification implements Visitor<Object,Object> {

	private Environment env;
	private ErrorReporter reporter;
	
	public Identification(Package prog, Environment env, ErrorReporter reporter) {
		
		this.env = env;
		this.reporter = reporter;
		
		prog.visit(this, null);
	}
	
	@Override
	public Object visitPackage(Package prog, Object arg) {

		env.openScope();
		
		for(ClassDecl cd : prog.classDeclList) {
			
			env.enter(cd);
		}
		
		// handles inheritance and links extended classes
		
		for(ClassDecl cd : prog.classDeclList) {
			
			switch(cd.name) {
			
			case "System": case "_PrintStream": case "String":
				continue;
			
			default:
				if(cd.inheritedClassId != null) {
				
					Declaration decl = env.retrieve(cd.inheritedClassId, null);
					
					if(decl != null) {
						
						if(decl instanceof ClassDecl) {

							cd.inheritedClass = (ClassDecl) decl;
						}
						else {
							
							reporter.reportError("*** line "+decl.posn+": expected reference to class");
						}
					}
				}
			}
		}
		
		env.initializeMaps();
		
		for(ClassDecl cd : prog.classDeclList) { 
			
			cd.visit(this, null);
		}
		
		env.closeScope();
		
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {

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
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		
		fd.type.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		
		env.setMethod(md);
		
		md.type.visit(this, null);
		
		env.openScope();
		
		for(ParameterDecl pd : md.parameterDeclList) {
			
			pd.visit(this, null);
		}
		
		env.openScope();
		
		boolean returnFound = false;
		
		for(Statement st : md.statementList) {
			
			st.visit(this, null);
			
			returnFound = st instanceof ReturnStmt;
		}
		
		if(md.type.typeKind != TypeKind.VOID && !returnFound) {
			
			reporter.reportError("*** line "+md.posn+": non-void method should end with return statement");
		}
		
		env.closeScope();
		env.closeScope();
		
		env.setMethod(null);
		
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		
		pd.type.visit(this, null);
		
		env.enter(pd);
		
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		
		decl.type.visit(this, null);
		
		env.enter(decl);
		
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {

		if(env.classes.get(type.className.spelling) == null) {
			
			reporter.reportError("*** line "+type.posn+": class type not found");
		}
		
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		
		type.eltType.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		
		env.openScope();
		
		for(Statement s : stmt.sl) {
			
			s.visit(this, null);
		}
		
		env.closeScope();
		
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		
		stmt.varDecl.visit(this, null);
		
		stmt.initExp.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		
		stmt.val.visit(this, null);
		
		stmt.ref.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		
		stmt.ref.visit(this, null);
		
		stmt.ix.visit(this, null);
		
		stmt.exp.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		
		stmt.methodRef.visit(this, null);
		
		for(Expression expr : stmt.argList) {
			
			expr.visit(this, null);
		}
		
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		
		if(stmt.returnExpr != null) {
			
			stmt.returnExpr.visit(this, null);
		}
		
		return null;
	}

	private void checkConditional(Statement stmt) {
		
		if(stmt instanceof VarDeclStmt) {
			
			reporter.reportError("*** line "+stmt.posn+": solitary variable declaration in conditional statement branch");
		}
		
	}
	
	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {

		stmt.cond.visit(this, null);
		
		stmt.thenStmt.visit(this, null);
		
		checkConditional(stmt.thenStmt);
		
		if(stmt.elseStmt != null) {
			
			stmt.elseStmt.visit(this, null);
			
			checkConditional(stmt.elseStmt);
		}

		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		
		stmt.cond.visit(this, null);
		
		stmt.body.visit(this, null);
		
		checkConditional(stmt.body);
		
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		
		expr.expr.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		
		expr.left.visit(this, null);
		
		expr.right.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		
		expr.ref.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		
		expr.ixExpr.visit(this, null);
		
		expr.ref.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		
		expr.functionRef.visit(this, null);
		
		for(Expression ex : expr.argList) {
			
			ex.visit(this, null);
		}
		
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		
		expr.classtype.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		
		expr.eltType.visit(this, null);
		
		expr.sizeExpr.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		
		if(env.getCurrentMethod().isStatic) {
			
			reporter.reportError("*** line "+ref.posn+": cannot reference \'this\' in static context");
		}
		
		ref.className = env.getCurrentClass().name;
		
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		
		ref.decl = env.retrieve(ref.id, null);
		
		if(env.getCurrentMethod().isStatic && ref.decl instanceof FieldDecl && !((FieldDecl)ref.decl).isStatic) {
			
			if(ref.posn != null) {
				
				reporter.reportError("*** line "+ref.posn+": cannot reference instance variable in static context");
			}
		}

		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {

		ref.ref.visit(this, null);
		
		if(ref.decl != null) {
		}
		
		ref.decl = env.retrieve(ref.id, ref.ref);
		
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nul, Object arg) {
		return null;
	}

}
