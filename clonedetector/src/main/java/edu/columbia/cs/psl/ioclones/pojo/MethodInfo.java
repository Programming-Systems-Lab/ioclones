package edu.columbia.cs.psl.ioclones.pojo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	
	public TreeSet<Integer> flowToRet;
	
	public TreeSet<Integer> flowToStatic;
	
	public Set<Integer> writtenInputs;
	
	private String methodKey;
	
	private Set<CalleeRecord> callees = new HashSet<CalleeRecord>();
	
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
	
	public void addCallee(CalleeRecord callee) {
		this.callees.add(callee);
	}
	
	public Set<CalleeRecord> getCallees() {
		return this.callees;
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
