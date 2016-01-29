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
	
	private Map<String, MethodInfo> methodInfo = new HashMap<String, MethodInfo>();
	
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
	
	public void addMethodInfo(String nameArgs, MethodInfo mi) {
		this.methodInfo.put(nameArgs, mi);
	}
	
	public MethodInfo getMethodInfo(String nameArgs) {
		return this.methodInfo.get(nameArgs);
	}
	
	public Map<String, MethodInfo> getMethodInfo() {
		return this.methodInfo;
	}
}
