package edu.columbia.cs.psl.ioclones.pojo;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;
import example.MyObject;

public class IORecord {
	
	private String methodKey;
	
	private int id;
	
	private LinkedList<Object> inputs = new LinkedList<Object>();
	
	private List<Object> outputs = new ArrayList<Object>();
	
	private transient Object dataLock = new Object();
	
	public IORecord(String methodKey) {
		this.methodKey = methodKey;
		this.id = GlobalInfoRecorder.getMethodIndex();
		//System.out.println("Instantiate io record: " + this.methodKey + " " + this.id);
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void registerInput(Object i, boolean ser) {
		if (i == null) {
			return ;
		}
		
		Object insert = null;
		if (ser) {
			insert = IOUtils.newObject(i);
		} else {
			insert = i;
		}
		
		//System.out.println("Register in: " + insert);
		synchronized(dataLock) {
			this.inputs.add(insert);
		}
	}
		
	public void registerAndReplace(Object o, boolean ser, int removeNum) {
		synchronized(dataLock) {
			this.removeObjs(removeNum);
			this.registerInput(o, ser);
		}
	}
	
	public void removeObjs(int removeNum) {
		synchronized(dataLock) {
			for (int i = 0; i < removeNum; i++) {
				this.inputs.removeLast();
			}
		}
	}
	
	public void pullOutput() {
		synchronized(dataLock) {
			Object lastInput = this.inputs.removeLast();
			this.outputs.add(lastInput);
		}
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
	
	public static void main(String[] args) {
		IORecord io = new IORecord("a.b.c.method()");
		int i = 5;
		io.registerInput(i, false);
		double d = 8;
		io.registerInput(d, false);
		char c = 'c';
		io.registerInput(c, false);
		short s = 3;
		io.registerInput(s, false);
		io.registerInput(0, false);
		io.registerInput(-1, false);
		long l = 8;
		io.registerInput(8l, false);
		float f = 3.0f;
		io.registerInput(2.0f, false);
		io.registerInput(32767, false);
		io.registerInput(32768, false);
		
		byte b = (byte)'1';
		io.registerInput(b, false);
		
		MyObject mo = new MyObject("test", 123);
		io.registerInput(mo, true);
		
		boolean bl = false;
		io.registerInput(bl, false);
	}

}
