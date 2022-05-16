package miniJava.ContextualAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Environment {
	
	private IdentificationTable table;
	
	private ErrorReporter reporter;
	
	private ClassDecl currentClass;
	private MethodDecl currentMethod;

	private Package prog;
	
	public Environment(Package prog, ErrorReporter reporter) {
		
		table = new IdentificationTable(reporter);
		
		this.reporter = reporter;
		this.prog = prog;
		
		classes = new HashMap<String, ClassDecl>();
		
		fields = new HashMap<String, Map<String, FieldDecl>>();
		methods = new HashMap<String, Map<String, MethodDecl>>();
		
		for(ClassDecl cd : prog.classDeclList) {
			
			switch(cd.name) {
				
			default:
				classes.put(cd.name, cd);
			}
		}
	}
	
	public void initializeMaps() {
		
		for(ClassDecl cd : prog.classDeclList) {
			
			switch(cd.name) {
				
			default:
				classes.put(cd.name, cd);
				
				fields.put(cd.name, getFields(cd));
				methods.put(cd.name, getMethods(cd));
			}
		}
	}
	
	public void openScope() {
		
		table.openScope();
	}
	
	public void closeScope() {
		
		table.closeScope();
	}
	
	public void enter(Declaration d) {
		
		table.enter(d);
	}
	
	public Declaration retrieve(Identifier id, Reference ref) {
		
		Declaration decl = null;
		
		if(ref == null || ref.decl == null) {
			
			String name = id.spelling;
			
			decl = table.retrieve(name);
			
			if(decl == null) {
				
				if(currentFields.containsKey(name)) {
					
					return currentFields.get(name);
				}
				else if(currentMethods.containsKey(name)) {
					
					return currentMethods.get(name);
				}
			}
		}
		else if(ref.decl.type instanceof ArrayType) {
			
			if(id.spelling.equals("length")) {
				
				return new FieldDecl(false, false, new BaseType(TypeKind.INT, null), id.spelling, null, null);
			}
		}
		else if(ref instanceof ThisRef) {
			
			decl = table.retrieve(id.spelling);
		}
		else {
			
			if(ref.decl instanceof ClassDecl) {
				
				Map<String, FieldDecl> classFields = fields.get(ref.decl.name);
				
				FieldDecl fd = classFields.get(id.spelling);
				
				Map<String, MethodDecl> classMethods = methods.get(ref.decl.name);
				
				MethodDecl md = classMethods.get(id.spelling);
				
				if(fd != null) {
					
					return fd;
				}
				else if(md != null) {
					
					return md;
				}
				else {
					
					reporter.reportError("*** line "+id.posn+": "+id.spelling+" not found");
					
					return null;
				}
			}
			else {
				
				if(ref.decl != null && ref.decl.type != null && ref.decl.type instanceof ClassType) {
					
					Map<String, FieldDecl> classFields = fields.get(((ClassType) ref.decl.type).className.spelling);
					
					FieldDecl fd = classFields.get(id.spelling);
					
					Map<String, MethodDecl> classMethods = methods.get(((ClassType) ref.decl.type).className.spelling);
					
					MethodDecl md = classMethods.get(id.spelling);
					
					if(fd != null) {
						
						return fd;
					}
					else if(md != null) {
						
						return md;
					}
					else {
						
						reporter.reportError("*** line "+id.posn+": "+id.spelling+" not found");
						
						return null;
					}
				}
				else if(ref.decl != null && ref.decl.type != null && ref.decl.type instanceof ArrayType){
					
					return null;
				}
				else {
					
					reporter.reportError("");
				}
			}
		}
		
		if(decl == null) {
			
			reporter.reportError("*** line "+id.posn+": "+id.spelling+" not found");
		}
		
		return decl;
	}
	
	public void setClass(ClassDecl cd) {
		
		currentClass = cd;
		
		currentFields = fields.get(currentClass.name);
		currentMethods = methods.get(currentClass.name);
	}
	
	public ClassDecl getCurrentClass() {
		
		return currentClass;
	}
	
	public void setMethod(MethodDecl md) {
		
		currentMethod = md;
	}
	
	public MethodDecl getCurrentMethod() {
		
		return currentMethod;
	}
	
	public Map<String, ClassDecl> classes;
	
	public Map<String, Map<String, FieldDecl>> fields;
	public Map<String, Map<String, MethodDecl>> methods;
	
	public Map<String, FieldDecl> currentFields;
	public Map<String, MethodDecl> currentMethods;
	
	public int getClassSize(String cn) {
		
		return fields.get(cn).size();
	}

	public int getClassSize(ClassDecl cd) {
		
		return fields.get(cd.name).size();
	}
	
	public List<String> getInheritedClasses(String cn) {
		
		return getInheritedClasses(classes.get(cn));
	}

	public List<String> getInheritedClasses(ClassDecl cd) {
		
		List<String> inheritedClasses = new ArrayList<String>();
		
		while(cd != null) {

			if(cd instanceof ClassDecl) {
				
				inheritedClasses.add(cd.name);
				
				cd = ((ClassDecl) cd).inheritedClass;
			}
			else {
				
				reporter.reportError("expected class decl");
			}
		}
		
		return inheritedClasses;
	}
	
	private Map<String, FieldDecl> getInheritedFields(ClassDecl cd) {
		
		cd = cd.inheritedClass;
		
		if(cd == null) {
			
			return new HashMap<String, FieldDecl>();
		}
		else {

			Map<String, FieldDecl> inheritedFields = getInheritedFields(cd);
			
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
