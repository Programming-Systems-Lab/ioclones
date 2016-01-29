package edu.columbia.cs.psl.ioclones.pojo;

import java.util.Map;
import java.util.TreeSet;

public class MethodInfo {
	
	public static final int NO_CHECK = 0;
	
	public static final int PUBLIC = 1;
	
	public static final int PROTECTED = 2;
	
	public static final int DEFAULT = 3;
	
	public static final int PRIVATE = 4;
	
	public transient boolean leaf = false;
	
	private Map<Integer, TreeSet<Integer>> writtenParams;
	
	private int level = -1;
	
	private boolean isFinal = false;
		
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
	
	public void setWrittenParams(Map<Integer, TreeSet<Integer>> writtenParams) {
		this.writtenParams = writtenParams;
	}
	
	public Map<Integer, TreeSet<Integer>> getWrittenParams() {
		return this.writtenParams;
	}
}
