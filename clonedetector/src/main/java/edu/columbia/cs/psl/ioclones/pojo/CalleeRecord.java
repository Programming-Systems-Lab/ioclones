package edu.columbia.cs.psl.ioclones.pojo;

import java.util.Map;

import edu.columbia.cs.psl.ioclones.analysis.DependentValue;

public class CalleeRecord {
	private String methodKey;
	
	//Facilitate query, Integer: callee param idnex, DV: a value from input param
	private Map<Integer, DependentValue> calleeCallerBridge;
	
	public CalleeRecord(String methodKey) {
		this.methodKey = methodKey;
	}
			
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public void setCalleeCallerBridge(Map<Integer, DependentValue> calleeCallerBridge) {
		this.calleeCallerBridge = calleeCallerBridge;
	}
	
	public Map<Integer, DependentValue> getCalleeCallerBridge() {
		return this.calleeCallerBridge;
	}
}
