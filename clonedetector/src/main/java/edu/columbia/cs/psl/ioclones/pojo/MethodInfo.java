package edu.columbia.cs.psl.ioclones.pojo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.analysis.DependentValueInterpreter;

public class MethodInfo {
	
	public static final int NO_CHECK = 0;
	
	public static final int PUBLIC = 1;
	
	public static final int PROTECTED = 2;
	
	public static final int DEFAULT = 3;
	
	public static final int PRIVATE = 4;
	
	public transient boolean stabelized = false;
	
	private String methodKey;
	
	private TreeSet<Integer> writeParams = new TreeSet<Integer>();
	
	private Set<CalleeRecord> fixedCallees = new HashSet<CalleeRecord>();
	
	private Set<CalleeRecord> floatingCallees = new HashSet<CalleeRecord>();
	
	public transient InsnList insts;
	
	public transient Frame[] frames;
	
	public transient DependentValueInterpreter dvi;
	
	private int level = -1;
	
	private boolean isFinal = false;
	
	public MethodInfo(String methodKey) {
		this.methodKey = methodKey;
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public void addWriteParams(int paramId) {			
		this.writeParams.add(paramId);
	}
	
	public TreeSet<Integer> getWriteParams() {
		return this.writeParams;
	}
	
	public void addFixedCallee(CalleeRecord fixedCallee) {
		this.fixedCallees.add(fixedCallee);
	}
	
	public Set<CalleeRecord> getFixedCallees() {
		return this.fixedCallees;
	}
	
	public void addFloatingCallee(CalleeRecord floatingCallee) {
		this.floatingCallees.add(floatingCallee);
	}
	
	public Set<CalleeRecord> getFloatingCallees() {
		return this.floatingCallees;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public int getLevel() {
		return this.level;
	}
	
	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}
	
	public boolean isFinal() {
		return isFinal;
	}
}
