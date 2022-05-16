package miniJava.SyntacticAnalyzer;

public enum TokenKind {

	// identifier
	
	ID,
	
	// num
	
	NUM,
	
	// boolean values
	
	TRUE,
	FALSE,
	
	// keywords
	
	BOOLEAN,
	CLASS,
	ELSE,
	EXTENDS,
	IF,
	INT,
	NEW,
	NULL,
	PRIVATE,
	PUBLIC,
	RETURN,
	STATIC,
	THIS,
	VOID,
	WHILE,
	
	// symbols
	
	LBRACK,
	RBRACK,
	LPAREN,
	RPAREN,
	LBRACE,
	RBRACE,
	COMMA,
	SEMICOLON,
	EQUALS,
	PERIOD,
	
	// relational operators
	
	GT,
	GTE,
	LT,
	LTE,
	EQ,
	NEQ,
	
	// logical operators
	
	AND,
	OR,
	NOT,
	
	// arithmetic operators
	
	ADD,
	SUBTRACT,
	MULTIPLY,
	DIVIDE,
	
	// eot
	
	EOT,
	
	// error
	
	ERROR,
	
	// other
	
	WHITESPACE;
}
