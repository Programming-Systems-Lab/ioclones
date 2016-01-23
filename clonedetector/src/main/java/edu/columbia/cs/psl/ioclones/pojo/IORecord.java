package edu.columbia.cs.psl.ioclones.pojo;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class IORecord {
	
	private static final Logger logger = LogManager.getLogger(IORecord.class);
	
	private String methodKey;
	
	private int id = -1;
	
	//private Set<Integer> stopVar = new HashSet<Integer>();
	
	private List<Object> inputs = new ArrayList<Object>();
	
	private List<Object> outputs = new ArrayList<Object>();
	
	public transient Collection<Object> cleanInputs;
	
	public transient Collection<Object> cleanOutputs;
	
	private transient Object retVal = null;
	
	private transient List<Object> controls = new ArrayList<Object>();
	
	private transient List<Object> sideEffects = new ArrayList<Object>();
	
	private boolean stopRecord = false;
	
	public IORecord(String methodKey) {
		this.methodKey = methodKey;
		if (GlobalInfoRecorder.stopRecord(methodKey)) {
			this.stopRecord = true;
			return ;
		}
		
		this.id = GlobalInfoRecorder.getMethodIndex();
		//System.out.println("Instantiate io record: " + this.methodKey + " " + this.id);
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setRetVal(Object retVal, boolean ser) {
		if (this.stopRecord) {
			return ;
		}
		
		if (retVal == null) {
			ser = false;
		}
		
		Object insert = null;
		if (ser) {
			insert = IOUtils.newObject(retVal);
		} else {
			insert = retVal;
		}
		
		this.retVal = insert;
	}
		
	public Object getRetVal() {
		return this.retVal;
	}
	
	public void registerControl(Object i, boolean ser) {
		if (this.stopRecord) {
			return ;
		}
				
		if (i == null) {
			ser = false;
		}
		
		Object insert = null;
		if (ser) {
			insert = IOUtils.newObject(i);
		} else {
			insert = i;
		}
		
		this.controls.add(insert);
	}
			
	public void swapLastTwo() {
		if (this.stopRecord) {
			return ;
		}
		
		//System.out.println("Before swap2: " + this.inputs);
		Object last = this.controls.get(this.inputs.size() - 1);
		Object last2 = this.controls.get(this.inputs.size() - 2);
		this.controls.set(this.controls.size() - 2, last);
		this.controls.set(this.controls.size() - 1, last2);
		//System.out.println("After swap2: " + this.inputs);
	}
		
	public void registerInput(Object o, boolean ser) {
		this.registerObj(o, ser, true);
	}
	
	public void registerSideEffect(Object o) {
		this.registerObj(o, true, true);
	}
	
	private void registerObj(Object o, boolean ser, boolean input) {
		if (o == null) {
			ser = false;
		}
		
		Object insert = o;
		if (ser) {
			insert = IOUtils.newObject(o);
		}
		
		if (input) {
			this.inputs.add(insert);
		} else {
			this.sideEffects.add(insert);
		}
	}
	
	public void summarizeIO() {
		//Summarize inputs
		this.inputs.addAll(this.controls);
		this.outputs.addAll(this.sideEffects);
		this.outputs.add(this.retVal);
	}
				
	public List<Object> getInputs() {
		return this.inputs;
	}
	
	public List<Object> getOutputs() {
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
		
		for (int i = 0; i < this.inputs.size(); i++) {
			Object myObj = this.inputs.get(i);
			Object tmpObj = tmp.getInputs().get(i);
			
			if (!myObj.equals(tmpObj)) {
				return false;
			}
		}
		
		for (int i = 0; i < this.outputs.size(); i++) {
			Object myObj = this.outputs.get(i);
			Object tmpObj = tmp.getOutputs().get(i);
			
			if (!myObj.equals(tmpObj)) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		
		int inHash = 0;
		for (Object obj: this.inputs) {
			inHash += obj.hashCode();
		}
		
		int outHash = 0;
		for (Object obj: this.outputs) {
			outHash += obj.hashCode();
		}
		
		//String inputString = IOUtils.fromObj2XML(this.inputs);
		//String outputString = IOUtils.fromObj2XML(this.outputs);
		
		result = 31 * this.methodKey.hashCode();
		result = 31 * result + inHash;
		result = 31 * result + outHash;
		return result;
	}
}
