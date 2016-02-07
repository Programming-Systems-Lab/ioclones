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

import edu.columbia.cs.psl.ioclones.sim.NoOrderAnalyzer;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class IORecord {
	
	private static final Logger logger = LogManager.getLogger(IORecord.class);
	
	private Comparator<Object> dhComparator = new Comparator<Object>(){
		public int compare(Object o1, Object o2) {
			return o1.hashCode() > o2.hashCode()?1:(o1.hashCode() < o2.hashCode()?-1:0);
		}
	};
	
	private String methodKey;
	
	private int id = -1;
	
	private Set<Object> inputs = new HashSet<Object>();
	
	private Set<Object> outputs = new HashSet<Object>();
	
	private transient boolean stopRecord = false;
	
	public transient Map<Integer, Object> preload = new HashMap<Integer, Object>();
	
	public transient List<Object> toSerialize = new ArrayList<Object>();
	
	public transient HashSet<Object> blackObject = new HashSet<Object>();
	
	public transient boolean isInput = true;
	
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
		//System.out.println("Instantiate io record: " + this.methodKey + " " + this.id);
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
	}
	
	public void queueInWritten(Object o) {
		if (this.stopRecord) {
			return ;
		}
		this.toSerialize.add(o);
	}
	
	public void summarizeWrittens() {
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
		if (!preval.equals(test)) {
			this.preload.remove(paramId);
		}
	}
	
	public void blackObject(Object o) {
		this.blackObject.add(o);
	}
	
	public void probeOwner(Object curObject) {
		if (this.stopRecord) {
			return ;
		}
		
		if (this.blackObject.contains(curObject)) {
			this.isInput = false;
		}
	}
		
	public void registerValueFromInput(Object o, boolean ser) {
		if (this.stopRecord) {
			return ;
		}
		
		if (!this.isInput) {
			return ;
		}
		
		this.isInput = true;
		this.registerInput(o, ser);
	}
	
	public void registerInput(Object o, int paramId, boolean ser) {
		if (this.stopRecord) {
			return ;
		}
		
		if (this.preload.containsKey(paramId)) {
			this.registerInput(o, ser);
		}
	}
	
	public void registerInput(Object o, boolean ser) {
		if (this.stopRecord) {
			return ;
		}
		this.registerObj(o, ser, true);
	}
		
	public void registerOutput(Object o, boolean ser) {
		if (this.stopRecord) {
			return ;
		}
		this.registerObj(o, ser, false);
	}
	
	private void registerObj(Object o, boolean ser, boolean input) {
		if (IOUtils.shouldRemove(o)) {
			return ;
		}
		
		if (o == null) {
			ser = false;
		}
		
		Object insert = o;
		if (ser) {
			insert = IOUtils.newObject(o);
		}
		
		Object cleanObj = NoOrderAnalyzer.cleanObject(insert);
		if (input) {
			this.inputs.add(cleanObj);
		} else {
			this.outputs.add(cleanObj);
		}
	}
				
	public Collection<Object> getInputs() {
		return this.inputs;
	}
	
	public Collection<Object> getOutputs() {
		return this.outputs;
	}
		
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Method: " + this.methodKey + "\n");
		sb.append("ID: " + this.id + "\n");
		sb.append("Inputs: " + this.inputs + "\n");
		sb.append("Outputs: " + this.outputs + "\n");
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
		
		/*String tmpInput = IOUtils.fromObj2XML(tmp.getInputs());
		String thisInput = IOUtils.fromObj2XML(this.inputs);
		if (!tmpInput.equals(thisInput)) {
			return false;
		}*/
		
		if (tmp.getInputs().size() != this.inputs.size()) {
			return false;
		}
		
		if (tmp.getOutputs().size() != this.outputs.size()) {
			return false;
		}
		
		List<Object> myInput = new ArrayList<Object>(this.inputs);
		List<Object> tmpInput = new ArrayList<Object>(tmp.getInputs());
		
		Collections.sort(myInput, this.dhComparator);
		Collections.sort(tmpInput, this.dhComparator);
		
		for (int i = 0; i < myInput.size(); i++) {
			Object myObj = myInput.get(i);
			Object tmpObj = tmpInput.get(i);
			
			if (!myObj.equals(tmpObj)) {
				return false;
			}
		}
		
		List<Object> myOutput = new ArrayList<Object>(this.outputs);
		List<Object> tmpOutput = new ArrayList<Object>(tmp.getOutputs());
		Collections.sort(myOutput, this.dhComparator);
		Collections.sort(tmpOutput, this.dhComparator);
		
		for (int i = 0; i < myOutput.size(); i++) {
			Object myObj = myOutput.get(i);
			Object tmpObj = tmpOutput.get(i);
			
			if (!myObj.equals(tmpObj)) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		
		int inHash = this.inputs.hashCode();		
		int outHash = this.outputs.hashCode();
		
		//String inputString = IOUtils.fromObj2XML(this.inputs);
		//String outputString = IOUtils.fromObj2XML(this.outputs);
		
		result = 31 * this.methodKey.hashCode();
		result = 31 * result + inHash;
		result = 31 * result + outHash;
		return result;
	}
}
