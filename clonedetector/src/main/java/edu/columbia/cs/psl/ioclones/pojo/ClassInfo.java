package edu.columbia.cs.psl.ioclones.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassInfo {
	
	private String className;
	
	private String parent;
	
	private List<String> interfaces = new ArrayList<String>();
	
	private List<String> children = new ArrayList<String>();
	
	private Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
	
	public ClassInfo(String className) {
		this.className = className;
	}
	
	public String getClassName() {
		return this.className;
	}
	
	public void setParent(String parent) {
		this.parent = parent;
	}
	
	public String getParent() {
		return this.parent;
	}
	
	public void addInterface(String inter) {
		this.interfaces.add(inter);
	}
	
	public List<String> getInterfaces() {
		return this.interfaces;
	}
	
	public void addChild(String child) {
		this.children.add(child);
	}
	
	public List<String> getChildren() {
		return this.children;
	}
	
	public void addMethod(MethodInfo method) {
		this.methods.put(method.getMethodKey(), method);
	}
	
	public Map<String, MethodInfo> getMethods() {
		return methods;
	}
}
