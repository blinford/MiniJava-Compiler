package miniJava.SyntacticAnalyzer;

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
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.IxAssignStmt;
import miniJava.AbstractSyntaxTrees.IxExpr;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.NullLiteral;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.ReturnStmt;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class Parser {
	
	private Scanner scanner;
	
	private ErrorReporter reporter;
	
	private Token token;
	
	private final boolean TRACE = false;

	public Parser(Scanner scanner, ErrorReporter reporter) {
		
		this.scanner = scanner;
		this.reporter = reporter;
	}
	
	class SyntaxError extends Error {
		
		private static final long serialVersionUID = 1L;
	}
	
	private void accept() throws SyntaxError {
		
		accept(token.kind);
	}
	
	private void accept(TokenKind expectedTokenKind) throws SyntaxError {
		
		if (token.kind == expectedTokenKind) {
			
			if (TRACE) {
				
				pTrace();
			}
			
			token = scanner.scan();
		}
		else
			parseError("expecting '" + expectedTokenKind +"' but found '" + token.kind + "'");
	}
	
	private void parseError(String e) throws SyntaxError {
		
		reporter.reportError("Parse Error: " + e);
		
		throw new SyntaxError();
	}
	
	private void pTrace() {
		
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		
		for (int i = stl.length - 1; i > 0 ; i--) {
			
			if(stl[i].toString().contains("parse")) {
				
				System.out.println(stl[i]);
			}
		}
		
		System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
		System.out.println();
	}

	public Package parse() {
		
		Package program = null; 
		
		token = scanner.scan();
		
		try {
			
			program = parseProgram();
		}
		catch (SyntaxError e) {
			
		}
		
		return program;
	}
	
	// (ClassDeclaration)* EOT
	
	private Package parseProgram() throws SyntaxError {
		
		ClassDeclList classDeclList = new ClassDeclList();
		
		while(token.kind != TokenKind.EOT) {
			
			classDeclList.add(parseClassDeclaration());
		}
		
		accept();
		
		return new Package(classDeclList, null);
	}

	// CLASS ID LBRACE (FieldDeclaration | MethodDeclaration)* RBRACE
	
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		
		SourcePosition posn = token.posn;
		
		accept(TokenKind.CLASS);
		
		String cn = token.spelling;
		
		accept(TokenKind.ID);
		
		Identifier extendsId = null; // super class name
		
		if(token.kind == TokenKind.EXTENDS) {
			
			accept();
			
			extendsId = new Identifier(token);
			
			accept(TokenKind.ID);
		}
		
		accept(TokenKind.LBRACE);
		
		FieldDeclList fieldDeclList = new FieldDeclList();
		MethodDeclList methodDeclList = new MethodDeclList();
		
		while(token.kind != TokenKind.RBRACE) {
			
			MemberDecl decl = parseMemberDeclaration();
			
			if(decl instanceof FieldDecl) {
				
				fieldDeclList.add((FieldDecl)decl);
			}
			else if(decl instanceof MethodDecl) {

				methodDeclList.add((MethodDecl)decl);
			}
			else {
				
				System.out.println("error");
			}
		}
		
		accept(/*RBRACE*/);
		
		return new ClassDecl(cn, extendsId, fieldDeclList, methodDeclList, posn);
	}
	
	// FieldDeclaration | MethodDeclaration
	
	// Visibility Access
	//
	//		Type ID SEMICOLON
	//		  or
	//		Type ID LPAREN ParameterList? RPAREN LBRACE Statement* RBRACE
	//		  or
	//		VOID ID LPAREN ParameterList? RPAREN LBRACE Statement* RBRACE
	
	private MemberDecl parseMemberDeclaration() throws SyntaxError {
		
		SourcePosition posn = token.posn;
		
		// Visibility Access
		
		boolean isPrivate = parseVisibility();
		
		boolean isStatic = parseAccess();
		
		TypeDenoter typeDenoter = null;
		
		String id = null;
		
		// Type ID SEMICOLON (field)
		// Type ID (method)
		// VOID ID (method)
		
		switch(token.kind) {
		
		case INT:
		case BOOLEAN:
		case ID:

			typeDenoter = parseType();
			
			id = token.spelling;
			
			accept(TokenKind.ID);
			
			if(token.kind == TokenKind.SEMICOLON) {
				
				accept(/*SEMICOLON*/);
				
				return new FieldDecl(isPrivate, isStatic, typeDenoter, id, null, posn);
			}
			else if(token.kind == TokenKind.EQUALS) {
				
				if(isStatic) {
					accept(/*EQUALS*/);
					
					Expression expr = parseExpression();

					accept(TokenKind.SEMICOLON);
					
					return new FieldDecl(isPrivate, isStatic, typeDenoter, id, expr, posn);
				}
				else {
					
					parseError("cannot initialize non-static fields");
				}
			}
			
			break;
			
		case VOID:
			
			typeDenoter = new BaseType(TypeKind.VOID, null);
			
			accept();
			
			id = token.spelling;
			
			accept(TokenKind.ID);
			
			break;
			
		default:
			
			parseError("expected type or void, found " + token.kind);
		}
		
		MemberDecl memberDecl = new FieldDecl(isPrivate, isStatic, typeDenoter, id, null, posn);
		
		// LPAREN ParameterList? RPAREN LBRACE Statement* RBRACE
		
		accept(TokenKind.LPAREN);
		
		ParameterDeclList parameterList = new ParameterDeclList(); // null or empty list?
		
		if(token.kind != TokenKind.RPAREN) {
			
			parameterList = parseParameterList();
		}

		accept(TokenKind.RPAREN);
		
		accept(TokenKind.LBRACE);
		
		StatementList statementList = new StatementList();
		
		while(token.kind != TokenKind.RBRACE) {
			
			statementList.add(parseStatement());
		}
		
		accept(TokenKind.RBRACE);
		
		return new MethodDecl(memberDecl, parameterList, statementList, posn);
	}
	
	// (PUBLIC | PRIVATE)?
	
	private boolean parseVisibility() throws SyntaxError {
		
		boolean isPrivate = (token.kind == TokenKind.PRIVATE);
		boolean isPublic = (token.kind == TokenKind.PUBLIC);
		
		if(isPrivate || isPublic) {
			
			accept();
		}
		
		return isPrivate;
	}

	// STATIC?
	
	private boolean parseAccess() throws SyntaxError {
		
		boolean isStatic = (token.kind == TokenKind.STATIC);
		
		if(isStatic) {
			
			accept();
		}
		
		return isStatic;
	}
	
	// INT | BOOLEAN | ID | (INT | ID) LBRACK RBRACK
	
	private TypeDenoter parseType() throws SyntaxError {
		
		SourcePosition posn = token.posn;
		
		TypeDenoter typeDenoter = null;
		
		switch(token.kind) {
		
		case BOOLEAN:
			
			typeDenoter = new BaseType(TypeKind.BOOLEAN, posn);
			
			accept();
			
			if(token.kind == TokenKind.LBRACK) {
				
				accept();
				accept(TokenKind.RBRACK);
				
				return new ArrayType(typeDenoter, posn);
			}
			else {
				
				return typeDenoter;
			}
			
		case INT:
			
			typeDenoter = new BaseType(TypeKind.INT, posn);
			
			accept();
			
			if(token.kind == TokenKind.LBRACK) {
				
				accept();
				accept(TokenKind.RBRACK);
				
				return new ArrayType(typeDenoter, posn);
			}
			else {
				
				return typeDenoter;
			}

		case ID:
			
			Identifier id = new Identifier(token);
			
			typeDenoter = new ClassType(id, posn);
			
			accept();
			
			if(token.kind == TokenKind.LBRACK) {
				
				accept();
				accept(TokenKind.RBRACK);
				
				return new ArrayType(typeDenoter, posn);
			}
			else {
				
				return typeDenoter;
			}
			
		default:
			
			parseError("expected type, found "+token.kind);
		}
		
		return typeDenoter;
	}
	
	//  Type ID (COMMA Type ID)*
	
	private ParameterDeclList parseParameterList() throws SyntaxError {
		
		SourcePosition posn = token.posn;
		
		ParameterDeclList paramDeclList = new ParameterDeclList();
		
		ParameterDecl parameterDecl = new ParameterDecl(parseType(), token.spelling, posn);
		
		accept();
		
		paramDeclList.add(parameterDecl);
		
		while(token.kind == TokenKind.COMMA) {
			
			accept();

			parameterDecl = new ParameterDecl(parseType(), token.spelling, posn);
			
			accept(TokenKind.ID);
			
			paramDeclList.add(parameterDecl);
		}
		
		return paramDeclList;
	}
	
	// Expression (COMMA Expression)*
	
	private ExprList parseArgumentList() throws SyntaxError {
		
		ExprList argList = new ExprList();
		
		argList.add(parseExpression());
		
		while(token.kind == TokenKind.COMMA) {
			
			accept();
			
			argList.add(parseExpression());
		}
		
		return argList;
	}
	
	// ID | THIS | (Reference PERIOD ID)
	// (ID | THIS) (PERIOD ID)*
	// LPAREN ID RPAREN Reference
	
	private Reference parseReference() throws SyntaxError {
		
		SourcePosition posn = token.posn;
		
		Reference ref = null;
		
		switch(token.kind) {
			
		case ID:
			
			ref = new IdRef(new Identifier(token), posn);
			
			accept();
			
			break;
			
		case THIS:

			ref = new ThisRef(posn);
			
			accept();
			
			break;

		default:
			
			parseError("expected reference, found " + token.kind);
		}
		
		return parseReferenceID(ref);
	}
	
	private Reference parseReferenceID(Reference ref) throws SyntaxError {
		
		SourcePosition posn = token.posn;
		
		while(token.kind == TokenKind.PERIOD) {
			
			accept();
			
			Identifier id = new Identifier(token);
			
			accept(TokenKind.ID);
			
			ref = new QualRef(ref, id, posn);
		}
		
		return ref;
	}
	
	// LBRACE Statement* RBRACE
	//  |	Type ID EQUALS Expression SEMICOLON
	//  |	Reference EQUALS Expression SEMICOLON
	//  |	Reference LBRACK Expression RBRACK EQUALS Expression SEMICOLON
	//  |	Reference LPAREN ArgumentList? RPAREN SEMICOLON
	//  |	RETURN Expression? SEMICOLON
	//  |	IF LPAREN Expression RPAREN Statement (ELSE Statement)?
	//  |	WHILE LPAREN Expression RPAREN Statement
	//
	private Statement parseStatement() throws SyntaxError {
		
		SourcePosition posn = token.posn;
		
		TypeDenoter type = null;
		
		switch(token.kind) {
		
		case LBRACE:
			
			accept();
			
			StatementList statementList = new StatementList();
			
			while(token.kind != TokenKind.RBRACE) {
				
				statementList.add(parseStatement());
			}
			
			accept();
			
			return new BlockStmt(statementList, posn);
			
		case INT:
		case BOOLEAN:
			
			type = parseType();
		
			return parseVarDeclStmt(new VarDecl(type, token.spelling, posn));
			
		case ID:
			
			Identifier id = new Identifier(token);
			Reference ref = new IdRef(id, null);
			
			accept();
			
			if(token.kind == TokenKind.PERIOD) { 
				
				return parseReferenceStmt(ref);
			}
			
			if(token.kind == TokenKind.ID) {
				 
				type = new ClassType(id, null);
				
				return parseVarDeclStmt(new VarDecl(type, token.spelling, posn));
			}
			
			if(token.kind == TokenKind.LBRACK) {
				
				accept();
				
				if(token.kind == TokenKind.RBRACK) {
					
					accept();
					
					type = new ArrayType(new ClassType(id, posn), posn);
					
					return parseVarDeclStmt(new VarDecl(type, token.spelling, posn));
				}
				else {
					
					Expression ixAssignStmtExpr1 = parseExpression();
					
					return parseIxAssignStmt(ref, ixAssignStmtExpr1);
				}
			}
			
			return parseReferenceStmt(ref);
			
		case THIS:
			
			accept();
			
			return parseReferenceStmt(new ThisRef(posn));
			
		case RETURN:
			
			accept();
			
			Expression returnStmtExpr = null;
			
			if(token.kind != TokenKind.SEMICOLON) {
				
				returnStmtExpr = parseExpression();
			}
			
			accept(TokenKind.SEMICOLON);
			
			return new ReturnStmt(returnStmtExpr, posn);
			
		case IF:
			
			accept();
			
			accept(TokenKind.LPAREN);
			
			Expression ifStmtExpr = parseExpression();
			
			accept(TokenKind.RPAREN);
			
			Statement ifStatement = parseStatement();
			
			if(token.kind == TokenKind.ELSE) {
				
				accept();
				
				return new IfStmt(ifStmtExpr, ifStatement, parseStatement(), posn);
			}
			
			return new IfStmt(ifStmtExpr, ifStatement, posn);
		
		case WHILE:
			
			accept();
			
			accept(TokenKind.LPAREN);
			
			Expression whileStmtExpr = parseExpression();
			
			accept(TokenKind.RPAREN);
			
			Statement whileStatement = parseStatement();
			
			return new WhileStmt(whileStmtExpr, whileStatement, posn);
			
		default:
			
			parseError("unexpected token, found " + token.kind);
		}
		
		return null;
	}
	
	private VarDeclStmt parseVarDeclStmt(VarDecl varDecl) throws SyntaxError {
		
		SourcePosition posn = token.posn;
		
		accept(TokenKind.ID);
		
		accept(TokenKind.EQUALS);
		
		Expression expr = parseExpression();
		
		accept(TokenKind.SEMICOLON);
		
		return new VarDeclStmt(varDecl, expr, posn);
	}
	
	private Statement parseReferenceStmt(Reference ref) {
		
		ref = parseReferenceID(ref);
		
		switch(token.kind) {
		
		case EQUALS:
			return parseAssignStmt(ref);
			
		case LBRACK:
			return parseIxAssignStmt(ref);
		
		case LPAREN:
			return parseCallStmt(ref);
		
		default:
			parseError("expected equals, lbrack, or lparen, found " + token.kind);
		}
		
		return null;
	}
	
	private AssignStmt parseAssignStmt(Reference ref) {
		
		SourcePosition posn = token.posn;
		
		accept();
		
		Expression assignStmtExpr = parseExpression();
		
		accept(TokenKind.SEMICOLON);
		
		return new AssignStmt(ref, assignStmtExpr, posn);
	}
	
	private IxAssignStmt parseIxAssignStmt(Reference ref) {
	
		accept();
		
		Expression ixAssignStmtExpr1 = parseExpression();
		
		return parseIxAssignStmt(ref, ixAssignStmtExpr1);
	}
	
	private IxAssignStmt parseIxAssignStmt(Reference ref, Expression ixAssignStmtExpr1) {
		
		SourcePosition posn = token.posn;
		
		accept(TokenKind.RBRACK);
		
		accept(TokenKind.EQUALS);
		
		Expression ixAssignStmtExpr2 = parseExpression();
		
		accept(TokenKind.SEMICOLON); 
		
		return new IxAssignStmt(ref, ixAssignStmtExpr1, ixAssignStmtExpr2, posn);
	}

	private CallStmt parseCallStmt(Reference ref) { 
		
		SourcePosition posn = token.posn;
		
		accept();
		
		ExprList argList = new ExprList();

		if(token.kind != TokenKind.RPAREN) {

			argList = parseArgumentList();
		}

		accept(TokenKind.RPAREN);

		accept(TokenKind.SEMICOLON); 
		
		return new CallStmt(ref, argList, posn);
	}
	
	private Expression parseExpression() {
		
		return parseExpressionA();
	}
	
	// A ::= B (|| B)*
	private Expression parseExpressionA() {
		
		SourcePosition posn = token.posn;
		
		Expression e1 = parseExpressionB();
		
		while(isDisjunction()) {
			
			Operator op = new Operator(token);
		
			accept();
			
			Expression e2 = parseExpressionB();
			
			e1 = new BinaryExpr(op, e1, e2, posn);
		}
		
		return e1;
	}
	
	// B ::= C (&& C)*
	private Expression parseExpressionB() {
		
		SourcePosition posn = token.posn;
		
		Expression e1 = parseExpressionC();
		
		while(isConjunction()) {
			
			Operator op = new Operator(token);
		
			accept();
			
			Expression e2 = parseExpressionC();
			
			e1 = new BinaryExpr(op, e1, e2, posn);
		}
		
		return e1;
	}

	// C ::= D ((=|!) = D)*
	private Expression parseExpressionC() {
		
		SourcePosition posn = token.posn;
		
		Expression e1 = parseExpressionD();
		
		while(isEquality()) {
			
			Operator op = new Operator(token);
		
			accept();
			
			Expression e2 = parseExpressionD();
			
			e1 = new BinaryExpr(op, e1, e2, posn);
		}
		
		return e1;
	}

	// D ::= E ((<|>) =? E)*
	private Expression parseExpressionD() {
		
		SourcePosition posn = token.posn;
		
		Expression e1 = parseExpressionE();
		
		while(isRelational()) {
			
			Operator op = new Operator(token);
		
			accept();
			
			Expression e2 = parseExpressionE();
			
			e1 = new BinaryExpr(op, e1, e2, posn);
		}
		
		return e1;
	}

	// E ::= F ((+|-) F)*
	private Expression parseExpressionE() {
		
		SourcePosition posn = token.posn;
		
		Expression e1 = parseExpressionF();
		
		while(isAdditive()) {
			
			Operator op = new Operator(token);
		
			accept();
			
			Expression e2 = parseExpressionF();
			
			e1 = new BinaryExpr(op, e1, e2, posn);
		}
		
		return e1;
	}

	// F ::= G ((*|/) G)*
	private Expression parseExpressionF() {
		
		SourcePosition posn = token.posn;
		
		Expression e1 = parseExpressionG();
		
		while(isMultiplicative()) {
			
			Operator op = new Operator(token);
		
			accept();
			
			Expression e2 = parseExpressionG();
			
			e1 = new BinaryExpr(op, e1, e2, posn);
		}
		
		return e1;
	}

	// G ::= (-|!)* H
	private Expression parseExpressionG() {
		
		SourcePosition posn = token.posn;
		
		Expression expr;
		
		if(isUnary()) {
			
			Operator op = new Operator(token);
		
			accept();
			
			expr = parseExpressionG();
			
			expr = new UnaryExpr(op, expr, posn);
		}
		else {
			
			expr = parseExpressionH();
		}
		
		return expr;
	}
	
	// H ::= LPAREN Expr RPAREN | NUM | ...
	private Expression parseExpressionH() {
		
		SourcePosition posn = token.posn;
		
		Expression expr = null;
		
		switch(token.kind) {
		
		case ID:
		case THIS:
			
			Reference ref = parseReference();
		
			switch(token.kind) {
			
			case LBRACK:
				
				accept();
				
				expr = parseExpression();
				
				accept(TokenKind.RBRACK);
				
				return new IxExpr(ref, expr, posn);
				
			case LPAREN:
				
				accept();
				
				ExprList exprList = new ExprList();
				
				if(token.kind != TokenKind.RPAREN) {
					
					exprList = parseArgumentList();
				}
				
				accept(TokenKind.RPAREN);
				
				return new CallExpr(ref, exprList, posn);
				
			default:
				
				return new RefExpr(ref, posn);
			}
			
		case LPAREN:
			
			accept();
			
			expr = parseExpression();
			
			accept(TokenKind.RPAREN);
			
			return expr;
			
		case NUM:
			
			IntLiteral intLit = new IntLiteral(token);
			
			accept();
			
			return new LiteralExpr(intLit, posn);
			
		case TRUE:
		case FALSE:
			
			BooleanLiteral booleanLit = new BooleanLiteral(token);
			
			accept();
		
			return new LiteralExpr(booleanLit, posn);
			
		case NEW:
			
			accept();
			
			switch(token.kind) {
			
			case ID:
				
				Identifier id = new Identifier(token);
				
				accept();
				
				switch(token.kind) {
					
				case LPAREN:
					
					ClassType classType = new ClassType(id, posn);
					
					accept();
					
					accept(TokenKind.RPAREN);
					
					return new NewObjectExpr(classType, posn);
					
				case LBRACK:
					
					TypeDenoter typeDenoter = new ClassType(id, posn);
					
					accept();
					
					expr = parseExpression();
					
					accept(TokenKind.RBRACK);
					
					return new NewArrayExpr(typeDenoter, expr, posn);
					
				default:
					
					parseError("expected LPAREN or LBRACK, found " + token.kind);
				}
				
				break;
				
			case INT:
				
				TypeDenoter intType = new BaseType(TypeKind.INT, posn);
				
				accept();
				
				accept(TokenKind.LBRACK);
				
				expr = parseExpression();
				
				accept(TokenKind.RBRACK);
				
				return new NewArrayExpr(intType, expr, posn);
				
			case BOOLEAN: 
				
				TypeDenoter booleanType = new BaseType(TypeKind.BOOLEAN, posn);
				
				accept();
				
				accept(TokenKind.LBRACK);
				
				expr = parseExpression();
				
				accept(TokenKind.RBRACK);
				
				return new NewArrayExpr(booleanType, expr, posn);
				
			default:
				
				parseError("unexpected token, found " + token.kind);
			}
			
			break;
			
		case NULL:
			
			NullLiteral nullLit = new NullLiteral(token);
			
			accept();
			
			return new LiteralExpr(nullLit, posn);
			
		default:

			parseError("expected binop, found " + token.kind);
		}
		
		return expr;
	}

	private boolean isDisjunction() {
		
		return token.kind == TokenKind.OR;
	}
	
	private boolean isConjunction() {

		return token.kind == TokenKind.AND;
	}

	private boolean isEquality() {
		
		return token.kind == TokenKind.EQ || token.kind == TokenKind.NEQ;
	}
	
	private boolean isRelational() {
		return token.kind == TokenKind.LTE || token.kind == TokenKind.LT
				|| token.kind == TokenKind.GTE || token.kind == TokenKind.GT;
	}
	
	private boolean isAdditive() {
		
		return token.kind == TokenKind.ADD || token.kind == TokenKind.SUBTRACT;
	}
	
	private boolean isMultiplicative() {
		
		return token.kind == TokenKind.MULTIPLY || token.kind == TokenKind.DIVIDE;
	}
	
	private boolean isUnary() {
		
		return token.kind == TokenKind.SUBTRACT || token.kind == TokenKind.NOT;
	}
}
