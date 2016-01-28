package edu.columbia.cs.psl.ioclones.pojo;

import java.util.Map;
import java.util.TreeSet;

public class CalleeRecord {
	private String methodKey;
	
	//Facilitate query, Integer: callee param id, Integer: caller input param id
	private Map<Integer, Integer> potentialOutputs;
	
	//private Map<Integer, TreeSet<Integer>> potentialInputs;
	
	public boolean fixed = false;
	
	public CalleeRecord(String methodKey) {
		this.methodKey = methodKey;
	}
			
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public void setPotentialOutputs(Map<Integer, Integer> potentialOutputs) {
		this.potentialOutputs = potentialOutputs;
	}
	
	public Map<Integer, Integer> getPotentialOutputs() {
		return this.potentialOutputs;
	}
	
	/*public void setPotentialInputs(Map<Integer, TreeSet<Integer>> potentialInputs) {
		this.potentialInputs = potentialInputs;
	}
	
	public Map<Integer, TreeSet<Integer>> getPotentialInputs(){
		return this.potentialInputs;
	}*/
}
