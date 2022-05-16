package miniJava.CodeGenerator;

import java.util.HashMap;
import java.util.Map;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import mJAM.ObjectFile;
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
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Expression;
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
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntacticAnalyzer.TokenKind;

public class CodeGeneration implements Visitor<Object, Object> {
	
	private ErrorReporter reporter;

	// runtime entity descriptions managed by these maps
	
	private Map<Declaration, Integer> descriptions;
	private Map<MethodDecl, Integer> indexDescriptions;
	
	// class descriptions
	
	private Map<String, ClassDecl> classes;
	
	private Map<String, Map<String, FieldDecl>> fields;
	private Map<String, Map<String, MethodDecl>> methods;

	private Map<String, Map<String, FieldDecl>> instanceFields;
	
	private Map<String, Integer> classSizes;
	
	public CodeGeneration(Package prog, String fileName, ErrorReporter reporter) {
		
		this.reporter = reporter;

		// check for unique main class
		
		MethodDecl main = null;
		
		for(ClassDecl cd : prog.classDeclList) {
			
			for(MethodDecl md : cd.methodDeclList) {
				
				if(md.name.equals("main")) {
					
					if(main != null) {
						
						reporter.reportError("*** code generation error: multiple main methods found");
					}
					else if(!md.isPrivate && md.isStatic && md.type.typeKind == TypeKind.VOID){
						
						if(md.parameterDeclList.size() == 1) {
							
							ParameterDecl pd = md.parameterDeclList.get(0);
							
							if(pd.type instanceof ArrayType) {
								
								ArrayType arrayType = (ArrayType) pd.type;
								
								if(arrayType.eltType instanceof ClassType) {
									
									ClassType classType = (ClassType) arrayType.eltType;
									
									if(classType.className.spelling.equals("String")) {
										
										main = md;
										
										continue;
									}
								}
							}
						}
					}
				}
			}
		}
		
		if(main == null) { 
			
			reporter.reportError("*** code generation error: no main method found");
		}
		else {
			
			descriptions = new HashMap<Declaration, Integer>();
			indexDescriptions = new HashMap<MethodDecl, Integer>();
			
			classes = new HashMap<String, ClassDecl>();
			
			fields = new HashMap<String, Map<String, FieldDecl>>();
			methods = new HashMap<String, Map<String, MethodDecl>>();
			
			instanceFields = new HashMap<String, Map<String, FieldDecl>>();
			
			classSizes = new HashMap<String, Integer>();
			
			// initialize code generation
			
			Machine.initCodeGen();

			//  generate runtime entity descriptions for class declarations and field declarations
			
			for(ClassDecl cd : prog.classDeclList) {

				classes.put(cd.name, cd);
				
				int size = 0;
				
				for(FieldDecl fd : cd.fieldDeclList) {
					
					size += fd.isStatic ? 0 : 1;
				}
				
				classSizes.put(cd.name, size);
			}
			
			Map<String, Integer> newClassSizes = new HashMap<String, Integer>();
			
			for(ClassDecl cd : prog.classDeclList) {
				
				switch(cd.name) {
				
				case "System": case "_PrintStream": case "String":
					continue;
					
				default:
					int classSize = classSizes.get(cd.name);
					
					ClassDecl decl = cd;
					
					while(decl.inheritedClass != null) {
						
						decl = decl.inheritedClass;
						
						classSize += classSizes.get(decl.name);
					}
					
					newClassSizes.put(cd.name, classSize);
				}
			}
			
			classSizes = newClassSizes;
			
			SB_offset = 0;
			
			for(ClassDecl cd : prog.classDeclList) {
				
				switch(cd.name) {
				
				case "System": case "_PrintStream": case "String":
					continue;
				
				default:
					
					// displacement for static fields
					
					Map<String, FieldDecl> classFields = getFields(cd);
					Map<String, FieldDecl> classInstanceFields = getInstanceFields(cd);
					
					for(FieldDecl fd : classFields.values()) {
						
						if(fd.isStatic) {
							
							descriptions.put(fd, SB_offset++);
							
							if(fd.initExp != null) {
								
								fd.initExp.visit(this, null);
							}
							else {
								
								Machine.emit(Op.LOADL, 0);
							}
						}
					}
					
					fields.put(cd.name, classFields);
					instanceFields.put(cd.name, classInstanceFields);
					
					// displacement for class descriptors
					
					descriptions.put(cd, SB_offset++);
					
					int sizeAddr = Machine.nextInstrAddr();
					Machine.emit(Op.LOADL, -1);
					
					Map<String, MethodDecl> classMethods = getMethods(cd);
					
					int index = -1;
					
					for(MethodDecl md : classMethods.values()) {

						indexDescriptions.put(md, index++);
						Machine.emit(Op.LOADL, getOffset(md));
						SB_offset++;
					}
					
					Machine.patch(sizeAddr, classMethods.size());
					
					methods.put(cd.name, classMethods);
				}
			}

			Machine.emit(Op.LOADL, 0);
			Machine.emit(Prim.newarr);
			
			main.callAddrList.add(Machine.nextInstrAddr());        
			Machine.emit(Op.CALL, Reg.CB, -1);
			
			Machine.emit(Op.HALT, 0, 0, 0);
			
			prog.visit(this, null);
			
			descriptions.forEach((decl, desc) -> {
				if(desc == null) {
					reporter.reportError("*** code generation error: runtime entity description not found for decl "+decl.name);
				}
				else {
					for(Integer addr : decl.callAddrList) {
						Machine.patch(addr, desc);
					}
				}
			});
			
			indexDescriptions.forEach((decl, desc) -> {
				if(desc == null) {
					reporter.reportError("*** code generation error: runtime entity description not found for decl "+decl.name);
				}
				else {
					for(Integer addr : decl.dynamicCallAddrList) {
						Machine.patch(addr, desc);
					}
				}
			});
			
			/*
			for(int i = 0; i < Machine.code.length; i++) {
				Instruction in = Machine.code[i];
				if(in == null)
					continue;
				System.out.println(Machine.intToOp[in.op]+" "+in.n+" "+in.r+" "+in.d);
			}
			*/
		}
	}
	
	// java -> mJAM
	public void generateCode(String fileName) {
		
		String mJAM = fileName.replace(".java", ".mJAM");
		String asm = fileName.replace(".java", ".asm");
		
		ObjectFile objF = new ObjectFile(mJAM);
		
		System.out.print("writing object code file " + mJAM + " ... ");
		
		if (objF.write()) {
			System.out.println("FAILED!");
			return;
		}
		else
			System.out.println("SUCCEEDED");
		
        Disassembler d = new Disassembler(mJAM);

        System.out.print("writing assembly file " + asm + " ... ");
        
        if (d.disassemble()) {
                System.out.println("FAILED!");
                return;
        }
        else
                System.out.println("SUCCEEDED");
        /*
        Interpreter.debug(fileName.replace(".java", ".mJAM"), fileName.replace(".java", ".asm"));

        System.out.println("*** mJAM execution completed");
        */
	}
	
	// offset relative to LB for local variables and parameter variables
	// offset relative to SB for static fields and class descriptors
	// offset relative to OB for instance variables
	private int LB_offset, SB_offset, OB_offset;
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		
		for(ClassDecl cd : prog.classDeclList) {
			
			switch(cd.name) {
			
			case "System": case "_PrintStream": case "String":
				continue;
				
			default:
				cd.visit(this, null);
			}
		}
		
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		
		OB_offset = 0;
		
		for(FieldDecl fd : cd.fieldDeclList) {
			
			fd.visit(this, null);
		}
		
		for(MethodDecl md : cd.methodDeclList) {
			
			md.visit(this, null);
		}
		
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {

		if(fd.isStatic) {
			
		}
		else {
			
			descriptions.put(fd, OB_offset++);
		}
		
		return null;
	}

	private int callerArg;
	private boolean voidReturn;
	
	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		
		descriptions.put(md, Machine.nextInstrAddr());
		
		LB_offset = -md.parameterDeclList.size();
		
		for(ParameterDecl pd : md.parameterDeclList) {
			
			pd.visit(this, null);
		}

		LB_offset += 3;
		
		callerArg = md.parameterDeclList.size();
		voidReturn = md.type.typeKind == TypeKind.VOID;
		
		boolean endsWithReturn = false;
		
		for(Statement st : md.statementList) {
			
			st.visit(this, null);
			
			endsWithReturn = st instanceof ReturnStmt;
		}
		
		if(!endsWithReturn) {
			
			if(!voidReturn) {
				
				reporter.reportError("*** code generation error: method body should end with return statement");
			}
			else {
				
				Machine.emit(Op.RETURN, 0, 0, callerArg);
			}
		}
		
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {

		descriptions.put(pd, LB_offset++);
		
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		
		descriptions.put(decl, LB_offset++);
		
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		
		int start_LB_offset = LB_offset;
		
		for(Statement s : stmt.sl) {
			
			s.visit(this, null);
		}
		
		Machine.emit(Op.POP, LB_offset - start_LB_offset);
		
		LB_offset = start_LB_offset;
			
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
		
		if(stmt.ref.decl instanceof FieldDecl) {

			if(!((FieldDecl) stmt.ref.decl).isStatic) {
				
				if(stmt.ref instanceof QualRef && ((QualRef) stmt.ref).ref.decl != null
						&& ((QualRef) stmt.ref).ref.decl.type != null
						&& ((QualRef) stmt.ref).ref.decl.type instanceof ArrayType) {
					
					reporter.reportError("*** code generation error: cannot assign to array field \'length\'");
				}
				
				stmt.ref.visit(this, null);
				stmt.val.visit(this, null);
				
				Machine.emit(Prim.fieldupd);
			}
			else {
				
				stmt.val.visit(this, null);
				Machine.emit(Op.STORE, Reg.SB, getOffset(stmt.ref));
			}
		}
		else if(stmt.ref.decl instanceof VarDecl || stmt.ref.decl instanceof ParameterDecl) {

			stmt.val.visit(this, null);
			Machine.emit(Op.STORE, Reg.LB, getOffset(stmt.ref));
		}
		
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		
		stmt.ref.visit(this, null);
		
		if(stmt.ref.decl instanceof FieldDecl) {
			
			Machine.emit(Prim.fieldref);
		}
		
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		
		Machine.emit(Prim.arrayupd);
		
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		
		for(Expression expr : stmt.argList) {

			expr.visit(this, null);
		}
		
		stmt.methodRef.visit(this, null);
		
		if(((MethodDecl) stmt.methodRef.decl).type.typeKind != TypeKind.VOID) {
			
			//Machine.emit(Op.POP, 1);
		}
		
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		
		stmt.returnExpr.visit(this, null);
		
		Machine.emit(Op.RETURN, voidReturn ? 0 : 1, 0, callerArg);

		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		
		stmt.cond.visit(this, null);
		
		int addr1 = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Reg.CB, -1);
		
		stmt.thenStmt.visit(this, null);
		
		int addr2 = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, -1);
		
		Machine.patch(addr1, Machine.nextInstrAddr());
		
		if(stmt.elseStmt != null) {
			
			stmt.elseStmt.visit(this, null);
		}
		
		Machine.patch(addr2, Machine.nextInstrAddr());
		
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {

		int addr1 = Machine.nextInstrAddr();
		stmt.cond.visit(this, null);

		int addr2 = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);
		
		stmt.body.visit(this, null);

		int addr3 = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, addr1);
		
		Machine.patch(addr2, addr3 + 1);
		 
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		
		if(expr.operator.kind == TokenKind.SUBTRACT) {
			
			Machine.emit(Op.LOADL, 0);
		}
		
		expr.expr.visit(this, null);
		
		expr.operator.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		
		expr.left.visit(this, null);
		
		// short-circuit if necessary
		
		if(expr.operator.kind == TokenKind.AND) {
			
			Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, Machine.nextInstrAddr() + 1); // jump over next jump if true
			
			Machine.emit(Op.LOADL, Machine.falseRep);
			
			int addr = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, -1); // jump to end if false
			
			expr.right.visit(this, null);
			
			Machine.patch(addr, Machine.nextInstrAddr());
		}
		else if(expr.operator.kind == TokenKind.OR) {
			
			Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, Machine.nextInstrAddr() + 1); // jump over next jump if false
			
			Machine.emit(Op.LOADL, Machine.trueRep);
			
			int addr = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, -1); // jump to end if false
			
			expr.right.visit(this, null);
			
			Machine.patch(addr, Machine.nextInstrAddr());
		}
		else {
			
			expr.right.visit(this, null);
			
			expr.operator.visit(this, null);
		}
		
		return null;
	}

	// returns the value of the reference
	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		
		// a.b.c ...
		// this.a -> b -> c

		expr.ref.visit(this, null);
		
		if(expr.ref.decl instanceof FieldDecl) {
			
			if(expr.ref instanceof QualRef && ((QualRef) expr.ref).ref.decl != null
					&& ((QualRef) expr.ref).ref.decl.type != null
					&& ((QualRef) expr.ref).ref.decl.type instanceof ArrayType) {
				
			}
			else {
				
				if(!((FieldDecl) expr.ref.decl).isStatic) {
					
					Machine.emit(Prim.fieldref);
				}
			}
		}
		
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		
		expr.ref.visit(this, null); // load addr of array

		if(expr.ref.decl instanceof FieldDecl) {
			
			if(!((FieldDecl) expr.ref.decl).isStatic) {
				
				Machine.emit(Prim.fieldref);
			};
		}
		
		expr.ixExpr.visit(this, null); // load element index
		
		Machine.emit(Prim.arrayref);
		
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		
		for(Expression e : expr.argList) {

			e.visit(this, null);
		}
		
		expr.functionRef.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		
		expr.lit.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		String cn = expr.classtype.className.spelling;
		Machine.emit(Op.LOADL, getOffset(classes.get(cn)));
		Machine.emit(Op.LOADL, classSizes.get(cn));
		Machine.emit(Prim.newobj); // ..., addr of class object a, size (# of fields) of object n → ..., addr of new obj
		
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr); // ..., number of elts n → ..., addr of new array in heap
		
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		
		Machine.emit(Op.LOADA, Reg.OB, 0);
		
		return null;
	}
	
	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		
		if(ref.decl instanceof ClassDecl) {
			
			// static reference
		}
		else if(ref.decl instanceof FieldDecl) {
			
			if(((FieldDecl) ref.decl).isStatic) {

				Machine.emit(Op.LOAD, Reg.SB, getOffset(ref));
			}
			else {

				Machine.emit(Op.LOADA, Reg.OB, 0);
				Machine.emit(Op.LOADL, getOffset(ref));
			}
		}
		else if(ref.decl instanceof MethodDecl) {
			
			if(((MethodDecl) ref.decl).isStatic) {

				Machine.emit(Op.CALL, Reg.SB, getOffset(ref));
			}
			else {

				Machine.emit(Op.LOADA, Reg.OB, 0);
				Machine.emit(Op.CALLD, getIndex((MethodDecl) ref.decl), 0, 0);
			}
		}
		else if(ref.decl instanceof VarDecl || ref.decl instanceof ParameterDecl) {
			
			Machine.emit(Op.LOAD, Reg.LB, getOffset(ref));
		}
		
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		
		if(ref.decl.name.equals("println") && ref.ref.decl.name.equals("out") 
				&& ref.ref instanceof QualRef
				&& ((QualRef) ref.ref).ref.decl instanceof ClassDecl
				&& ((ClassDecl) ((QualRef) ref.ref).ref.decl).name.equals("System")) {
			
			
			Machine.emit(Prim.putintnl);
		}
		else if(ref.ref instanceof ThisRef) {
			
			Machine.emit(Op.LOADA, Reg.OB, 0);
			
			if(ref.decl instanceof FieldDecl) {
				
				Machine.emit(Op.LOADL, getOffset(ref));
			}
			else if(ref.decl instanceof MethodDecl) {
				
				Machine.emit(Op.CALLD, getIndex((MethodDecl) ref.decl), 0, 0);
			}
			else {
				
				reporter.reportError("*** code generation error: expected member decl");
			}
		}
		else if(ref.ref.decl != null && ref.ref.decl instanceof ClassDecl) {
			
			// static reference
			
			if(ref.decl instanceof FieldDecl) {
				
				Machine.emit(Op.LOAD, Reg.SB, getOffset(ref));
			}
			else if(ref.decl instanceof MethodDecl) {
				
				Machine.emit(Op.CALL, Reg.SB, getOffset(ref));
			}
		}
		else {
			
			ref.ref.visit(this, null);
			
			if(ref.decl instanceof FieldDecl) {
				
				if(ref.ref.decl instanceof FieldDecl) {
					
					Machine.emit(Prim.fieldref);
				}
				
				if(ref.ref.decl.type != null && ref.ref.decl.type instanceof ArrayType) {
					
					if(ref.decl.name.equals("length")) {
						
						Machine.emit(Prim.arraylen);
					}
					else {
						
						reporter.reportError("*** code generation error: expected length field");
					}
				}
				else {
					
					Machine.emit(Op.LOADL, getOffset(ref));
				}
			}
			else if(ref.decl instanceof MethodDecl) {
				
				if(ref.ref.decl instanceof FieldDecl) {
					
					if(!((FieldDecl) ref.ref.decl).isStatic) {
						
						Machine.emit(Prim.fieldref);
					}
				}
				
				Machine.emit(Op.CALLD, getIndex((MethodDecl) ref.decl), 0, 0);
			}
		}
		
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		
		switch(op.kind) {
		
		case GT:
			Machine.emit(Prim.gt);
			break;
		case GTE:
			Machine.emit(Prim.ge);
			break;
		case LT:
			Machine.emit(Prim.lt);
			break;
		case LTE:
			Machine.emit(Prim.le);
			break;
		case EQ:
			Machine.emit(Prim.eq);
			break;
		case NEQ:
			Machine.emit(Prim.ne);
			break;
		case AND:
			Machine.emit(Prim.and);
			break;
		case OR:
			Machine.emit(Prim.or);
			break;
		case NOT:
			Machine.emit(Prim.not);
			break;
		case ADD:
			Machine.emit(Prim.add);
			break;
		case SUBTRACT:
			Machine.emit(Prim.sub);
			break;
		case MULTIPLY:
			Machine.emit(Prim.mult);
			break;
		case DIVIDE:
			Machine.emit(Prim.div);
			break;
		default:
			reporter.reportError("*** code generation error: unexpected operator");
		}
		
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		
		Machine.emit(Op.LOADL, Integer.parseInt(num.spelling));
		
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		
		if(bool.spelling.equals("true")) {

			Machine.emit(Op.LOADL, Machine.trueRep);
		}
		else if(bool.spelling.equals("false")) {
			
			Machine.emit(Op.LOADL, Machine.falseRep);
		}
		else {
			
			reporter.reportError("*** code generation error: unexpected boolean spelling");
		}
		
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nul, Object arg) {
		
		Machine.emit(Op.LOADL, Machine.nullRep);
		
		return null;
	}
	
	private int getOffset(Reference ref) {
		
		Integer offset = descriptions.get(ref.decl);
		
		if(offset == null) {
			
			ref.decl.callAddrList.add(Machine.nextInstrAddr());
			return -1;
		}
		else {
			
			return offset;
		}
	}
	
	private int getOffset(Declaration decl) {
		
		Integer offset = descriptions.get(decl);

		if(offset == null) {
			
			decl.callAddrList.add(Machine.nextInstrAddr());
			return -1;
		}
		else {
			
			return offset;
		}
	}
	
	private int getIndex(MethodDecl decl) {
		
		Integer index = indexDescriptions.get(decl);

		if(index == null) {
			decl.dynamicCallAddrList.add(Machine.nextInstrAddr());
			return -1;
		}
		else {
			
			return index;
		}
	}
	
	public int getClassSize(String cn) {
		
		return instanceFields.get(cn).size();
	}

	public int getClassSize(ClassDecl cd) {
		
		return instanceFields.get(cd.name).size();
	}
	
	private Map<String, FieldDecl> getInheritedInstanceFields(ClassDecl cd) {
		
		cd = cd.inheritedClass;
		
		if(cd == null) {
			
			return new HashMap<String, FieldDecl>();
		}
		else {

			Map<String, FieldDecl> inheritedFields = getInheritedInstanceFields(cd);
			
			for(FieldDecl fd : cd.fieldDeclList) {
				
				if(!fd.isPrivate && !fd.isStatic) {
					
					inheritedFields.put(fd.name, fd);
				}
			}
			
			return inheritedFields;
		}
	}
	
	private Map<String, FieldDecl> getInstanceFields(ClassDecl cd) {
		
		Map<String, FieldDecl> fields = getInheritedInstanceFields(cd);
		
		for(FieldDecl fd : cd.fieldDeclList) {
			
			if(!fd.isStatic) {
			
				fields.put(fd.name, fd);
			}
		}
		
		return fields;
	}
	
	private Map<String, FieldDecl> getInheritedFields(ClassDecl cd) {
		
		cd = cd.inheritedClass;
		
		if(cd == null) {
			
			return new HashMap<String, FieldDecl>();
		}
		else {

			Map<String, FieldDecl> inheritedFields = getInheritedInstanceFields(cd);
			
			for(FieldDecl fd : cd.fieldDeclList) {
				
				if(!fd.isPrivate) {
					
					inheritedFields.put(fd.name, fd);
				}
			}
			
			return inheritedFields;
		}
	}
	
	private Map<String, FieldDecl> getFields(ClassDecl cd) {
		
		Map<String, FieldDecl> fields = getInheritedFields(cd);
		
		for(FieldDecl fd : cd.fieldDeclList) {
			
			fields.put(fd.name, fd);
		}
		
		return fields;
	}
		
	private Map<String, MethodDecl> getInheritedMethods(ClassDecl cd) {
		
		cd = cd.inheritedClass;
		
		if(cd == null) {
			
			return new HashMap<String, MethodDecl>();
		}
		else {

			Map<String, MethodDecl> inheritedMethods = getInheritedMethods(cd);
			
			for(MethodDecl md : cd.methodDeclList) {
				
				if(!md.isPrivate) {
					
					inheritedMethods.put(md.name, md);
				}
			}
			
			return inheritedMethods;
		}
	}
	
	private Map<String, MethodDecl> getMethods(ClassDecl cd) {
		
		Map<String, MethodDecl> methods = getInheritedMethods(cd);
		
		for(MethodDecl md : cd.methodDeclList) {
			
			methods.put(md.name, md);
		}
		
		return methods;
	}
}