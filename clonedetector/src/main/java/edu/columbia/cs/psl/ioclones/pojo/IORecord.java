package edu.columbia.cs.psl.ioclones.pojo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import edu.columbia.cs.psl.ioclones.sim.AbstractSim;
import edu.columbia.cs.psl.ioclones.sim.NoOrderAnalyzer;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class IORecord {
	
	private static final Logger logger = LogManager.getLogger(IORecord.class);
	
	private Comparator<Object> dhComparator = new AbstractSim.DHSComparator();
	
	private String methodKey;
	
	private int id = -1;
	
	private transient Set<Object> inputs = new HashSet<Object>();
	
	private transient Set<Object> outputs = new HashSet<Object>();
	
	public List<Object> sortedInputs;
	
	public List<Object> sortedOutputs;
		
	private transient boolean stopRecord = false;
	
	public transient Map<Integer, Object> preload = new HashMap<Integer, Object>();
	
	public transient List<Object> toSerialize = new ArrayList<Object>();
	
	public transient HashSet<Object> blackObject = new HashSet<Object>();
	
	public transient boolean isInput = true;
	
	public transient boolean show = false;
	
	public transient boolean removeShow = false;
	
	public IORecord(String methodKey, boolean isStatic) {
		this.methodKey = methodKey;
		if (GlobalInfoRecorder.stopRecord(methodKey)) {
			this.stopRecord = true;
			return ;
		}
		
		this.id = GlobalInfoRecorder.getMethodIndex();
		if (!isStatic) {
			//Instance method's this will never change...
			this.preload.put(0, null);
		}
		
		/*if (this.methodKey.equals("R5P1Y13.vot.A-getExp-(I+J)")) {
			//this.show = true;
			this.removeShow = true;
			System.out.println("IO: " + this.id);
		}*/
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public int getId() {
		return this.id;
	}
					
	public void preload(int paramIdx, Object o) {
		if (this.stopRecord) {
			return ;
		}
		this.preload.put(paramIdx, o);
		
		if (this.removeShow) {
			System.out.println("Preload: " + " "  + paramIdx + " " + this.preload);
		}
	}
	
	public void queueInWritten(Object o) {
		if (this.show) {
			System.out.println("Queue in written: " + o);
		}
		
		if (this.stopRecord) {
			return ;
		}
		this.toSerialize.add(o);
	}
	
	public void summarizeWrittens() {
		if (this.show) {
			System.out.println("Summarize writtens");
		}
		
		if (this.stopRecord) {
			return ;
		}
		
		this.toSerialize.forEach(o->{
			this.registerOutput(o, true);
		});
	}
	
	/**
	 * For primitive type and string
	 * @param paramId
	 * @param test
	 */
	public void attemptStopPrimParam(int paramId, Object test) {
		if (this.removeShow) {
			System.out.println("Attempt stop prim: " + paramId + " " + test);
		}
		
		if (this.stopRecord) {
			return ;
		}
		
		if (!this.preload.containsKey(paramId)) {
			return ;
		}
		
		//For iinc
		if (test == null) {
			this.preload.remove(paramId);
		}	
		
		Object preval = this.preload.get(paramId);
		if (preval == null) {
			if (this.removeShow) {
				logger.info("Already removed: " + this.methodKey + " " + paramId);
			}
			return ;
		}
		
		if (!preval.equals(test)) {
			this.preload.remove(paramId);
			if (this.removeShow) {
				System.out.println("Param removed: " + this.methodKey + " " + paramId);
			}
		}
	}
	
	public void blackObject(Object o) {
		if (this.show) {
			System.out.println("Black obj: " + o);
		}
		
		this.blackObject.add(o);
	}
	
	public void probeOwner(Object curObject) {
		if (this.show) {
			System.out.println("Probe: " + curObject);
		}
		
		if (this.stopRecord) {
			return ;
		}
		
		if (this.blackObject.contains(curObject)) {
			this.isInput = false;
		} else {
			this.isInput = true;
		}
	}
		
	public void registerValueFromInput(Object o, boolean ser) {
		if (this.show) {
			System.out.println("Registr val from input: " + o);
		}
		
		if (this.stopRecord) {
			return ;
		}
		
		if (this.isInput) {
			this.registerInput(o, ser);
		}
		
		this.isInput = true;
	}
	
	public void registerInput(Object o, int paramId, boolean ser) {
		if (this.show) {
			System.out.println("Register input with param: " + o + " " + paramId);
		}
		
		if (this.stopRecord) {
			return ;
		}
		
		if (this.preload.containsKey(paramId)) {
			this.registerInput(o, ser);
		}
	}
	
	public void registerInput(Object o, boolean ser) {
		if (this.show) {
			System.out.println("Register input: " + o);
		}
		
		if (this.stopRecord) {
			return ;
		}
		this.registerObj(o, ser, true);
	}
		
	public void registerOutput(Object o, boolean ser) {
		if (this.show) {
			System.out.println("Register output: " + o);
			System.out.println("Ending: " + this.id);
		}
		
		if (this.stopRecord) {
			return ;
		}
		this.registerObj(o, ser, false);
	}
	
	private void registerObj(Object o, boolean ser, boolean input) {
		if (this.show) {
			System.out.println("Register obj: " + input);
		}
		
		if (IOUtils.shouldRemove(o)) {
			return ;
		}
		
		if (this.show) {
			System.out.println("Pass removal check");
		}
		
		/*if (o == null) {
			ser = false;
		}*/
		
		Object insert = o;
		if (ser) {
			insert = IOUtils.newObject(o);
		}
		
		if (insert == null) {
			//The filter will filter out PrintWriter, Scanner, Reader, Writer in BlackConverter
			//logger.warn("Null new obj: " + insert);
			//logger.warn("Original obj: " + o);
			return ;
		}
		
		if (this.show) {
			System.out.println("New object: " + insert);
		}
		
		Object cleanObj = NoOrderAnalyzer.cleanObject(insert);
		//Except primitive types including string, others are serialized as xml wrapper
		if (input) {
			this.inputs.add(cleanObj);
		} else {
			this.outputs.add(cleanObj);
		}
	}
	
	public void finalizeIOs() {
		IOUtils.cleanNonSerializables(this.inputs);
		IOUtils.cleanNonSerializables(this.outputs);
		
		List<Object> sortedIn = new ArrayList<Object>(this.inputs);
		List<Object> sortedOut = new ArrayList<Object>(this.outputs);
		
		Collections.sort(sortedIn, this.dhComparator);
		Collections.sort(sortedOut, this.dhComparator);
		
		this.sortedInputs = sortedIn;
		this.sortedOutputs = sortedOut;
	}
				
	/*public Collection<Object> getInputs() {
		return this.inputs;
	}
	
	public Collection<Object> getOutputs() {
		return this.outputs;
	}*/
		
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Method: " + this.methodKey + "\n");
		sb.append("ID: " + this.id + "\n");
		sb.append("Inputs: " + this.sortedInputs + "\n");
		sb.append("Outputs: " + this.sortedOutputs + "\n");
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IORecord)) {
			return false;
		}
		
		IORecord tmp = (IORecord)obj;
		if (!tmp.getMethodKey().equals(this.methodKey)) {
			return false;
		}
		
		if (tmp.sortedInputs.size() != this.sortedInputs.size()) {
			return false;
		}
		
		if (tmp.sortedOutputs.size() != this.sortedOutputs.size()) {
			return false;
		}
		
		for (int i = 0; i < this.sortedInputs.size(); i++) {
			Object myObj = this.sortedInputs.get(i);
			Object tmpObj = tmp.sortedInputs.get(i);
			
			if (myObj == null) {
				System.out.println("Current method: " + this.methodKey);
				System.out.println("Something wrong: " + this.sortedInputs);
				System.out.println(this.inputs);
			}
			
			if (!myObj.equals(tmpObj)) {
				return false;
			}
		}
		
		for (int i = 0; i < this.sortedOutputs.size(); i++) {
			Object myObj = this.sortedOutputs.get(i);
			Object tmpObj = tmp.sortedOutputs.get(i);
			
			if (myObj == null) {
				System.out.println("Something wrong: " + this.sortedOutputs);
				System.out.println(this.outputs);
			}
			
			if (!myObj.equals(tmpObj)) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		
		int inHash = this.sortedInputs.hashCode();		
		int outHash = this.sortedOutputs.hashCode();
		
		//String inputString = IOUtils.fromObj2XML(this.inputs);
		//String outputString = IOUtils.fromObj2XML(this.outputs);
		
		result = 31 * result + this.methodKey.hashCode();
		result = 31 * result + inHash;
		result = 31 * result + outHash;
		return result;
	}
}
