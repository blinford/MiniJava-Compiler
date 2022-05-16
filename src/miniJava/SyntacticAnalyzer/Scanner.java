package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import miniJava.ErrorReporter;

public class Scanner {
	
	private InputStream inputStream;
	private ErrorReporter errorReporter;
	
	private char currentChar;
	private StringBuilder currentSpelling;
	
	private int lineNum;
	
	// true when end of input is found
	private boolean eot = false;
	
	public Scanner(InputStream inputStream, ErrorReporter errorReporter) {
		
		this.inputStream = inputStream;
		this.errorReporter = errorReporter;
		
		// initialize scanner state
		
		lineNum = 1;
		
		readChar();
	}

	public Token scan() {
		
		TokenKind kind = TokenKind.WHITESPACE;
		
		String spelling = null;
		
		while(kind == TokenKind.WHITESPACE) {
			
			currentSpelling = new StringBuilder();
			
			kind = scanToken();
			
			spelling = currentSpelling.toString();
		}
		
		// return new token
		return new Token(kind, spelling, new SourcePosition(lineNum));
	}
	
	private void take() {
		
		if(currentChar == '\n') {
			
			lineNum++;
		}
		
		currentSpelling.append(currentChar);
		nextChar();
	}
	
	private TokenKind scanToken() {
		
		if(eot) {
			
			return(TokenKind.EOT);
		}
		
		switch(currentChar) {
		
		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
			
			while(isDigit()) {
				take();
			}
		
			return(TokenKind.NUM);
		
		case '[':
			take();
			return(TokenKind.LBRACK);

		case ']':
			take();
			return(TokenKind.RBRACK);

		case '(':
			take();
			return(TokenKind.LPAREN);

		case ')':
			take();
			return(TokenKind.RPAREN);

		case '{':
			take();
			return(TokenKind.LBRACE);

		case '}':
			take();
			return(TokenKind.RBRACE);

		case ',':
			take();
			return(TokenKind.COMMA);

		case ';':
			take();
			return(TokenKind.SEMICOLON);

		case '=':
			
			take();
			
			switch(currentChar) {
			
			case '=':
				take();
				return(TokenKind.EQ);
				
			default:
				return(TokenKind.EQUALS);
			}

		case '.':
			take();
			return(TokenKind.PERIOD);

		case '>':
			
			take();
			
			switch(currentChar) {
			
			case '=':
				take();
				return(TokenKind.GTE);
				
			default:
				return(TokenKind.GT);
			}
			
		case '<':
			
			take();
			
			switch(currentChar) {
			
			case '=':
				take();
				return(TokenKind.LTE);
				
			default:
				return(TokenKind.LT);
			}
			
		case '!':
			
			take();
			
			switch(currentChar) {
			
			case '=':
				take();
				return(TokenKind.NEQ);
				
			default:
				return(TokenKind.NOT);
			}
			
		case '&':
			
			take();
			
			switch(currentChar) {
			
			case '&':
				take();
				return(TokenKind.AND);
				
			default:
				scanError("unexpected character: " + currentChar);
				return(TokenKind.ERROR);
			}
			
		case '|':
			
			take();
			
			switch(currentChar) {
			
			case '|':
				take();
				return(TokenKind.OR);
				
			default:
				scanError("unexpected character: " + currentChar);
				return(TokenKind.ERROR);
			}
			
		case '+':
			take();
			return(TokenKind.ADD);
			
		case '-':
			take();
			return(TokenKind.SUBTRACT);
			
		case '*':
			take();
			return(TokenKind.MULTIPLY);
			
		case '/':
			
			take();
			
			switch(currentChar) {
			
			case '/':

				take();
				
				while(currentChar != '\n' && !eot) {
					
					take();
				}
				
				return(TokenKind.WHITESPACE);
				
			case '*':

				take();

				boolean scanningComment = true;
				
				while(scanningComment) {

					if(eot) {

						return(TokenKind.ERROR);
					}
					
					if(currentChar == '*') {

						take();

						if(currentChar == '/') {

							take();
	
							scanningComment = false;
						}
					}
					else {
						
						take();
					}
				}
				
				return(TokenKind.WHITESPACE);
				
			default:

				return(TokenKind.DIVIDE);
			}
			
		case '\n': case '\t': case '\r': case ' ':
			take();
			return(TokenKind.WHITESPACE);
			
		default:
			
			if(isLetter()) {
				
				take();
				
				while(isLetter() || isDigit() || currentChar == '_') {
					
					take();
				}
				
				switch(currentSpelling.toString()) {
				
				case "boolean":
					return(TokenKind.BOOLEAN);
					
				case "class":
					return(TokenKind.CLASS);
					
				case "else":
					return(TokenKind.ELSE);
					
				case "extends":
					return(TokenKind.EXTENDS);
					
				case "if":
					return(TokenKind.IF);

				case "int":
					return(TokenKind.INT);
					
				case "new":
					return(TokenKind.NEW);
					
				case "null":
					return(TokenKind.NULL);
					
				case "private":
					return(TokenKind.PRIVATE);
					
				case "public":
					return(TokenKind.PUBLIC);
					
				case "return":
					return(TokenKind.RETURN);
					
				case "static":
					return(TokenKind.STATIC);

				case "this":
					return(TokenKind.THIS);
					
				case "void":
					return(TokenKind.VOID);
					
				case "while":
					return(TokenKind.WHILE);

				case "true":
					return(TokenKind.TRUE);

				case "false":
				return(TokenKind.FALSE);
					
				default:
					return(TokenKind.ID);
				}
			}
			
			scanError("unrecognized character: " + currentChar);
			return(TokenKind.ERROR);
		}
	}
	
	private void nextChar() {
		
		if(!eot) {
			
			readChar();
		}
	}
	
	private void readChar() {
		
		try {
			
			int c = inputStream.read();
			
			currentChar = (char) c;
			
			if(c == -1) {
				
				eot = true;
			}
		} 
		catch (IOException e) {
			
			scanError("I/O Exception!");
			eot = true;
		}
	}

	
	private boolean isLetter() {
		
		return ('a' <= currentChar && currentChar <= 'z')
				|| ('A' <= currentChar && currentChar <= 'Z');
	}
	
	
	private boolean isDigit() {
		
		return '0' <= currentChar && currentChar <= '9';
	}
	
	
	private void scanError(String m) {
		
		errorReporter.reportError("Scan Error:  " + m);
	}
}
