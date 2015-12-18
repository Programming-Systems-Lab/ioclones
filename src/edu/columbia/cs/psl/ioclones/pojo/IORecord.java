package edu.columbia.cs.psl.ioclones.pojo;

import java.util.List;
import java.util.ArrayList;

import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class IORecord {
	
	private String methodKey;
	
	private int id;
	
	List<Object> inputs = new ArrayList<Object>();
	
	List<Object> outputs = new ArrayList<Object>();
	
	public IORecord(String methodKey) {
		this.methodKey = methodKey;
		this.id = GlobalInfoRecorder.getMethodIndex();
		System.out.println("Instantiate io record: " + this.methodKey + " " + this.id);
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void registerInput(Object i) {
		System.out.println("Register: " + i);
		this.inputs.add(i);
	}
	
	public void registerOutput(Object o) {
		this.outputs.add(o);
	}
	
	public List<Object> getInputs() {
		return this.inputs;
	}
	
	public List<Object> getOutputs() {
		return this.outputs;
	}
	
	public static void main(String[] args) {
		IORecord io = new IORecord("a.b.c.method()");
		int i = 5;
		io.registerInput(i);
		double d = 8;
		io.registerInput(d);
		char c = 'c';
		io.registerInput(c);
		short s = 3;
		io.registerInput(s);
		io.registerInput(0);
		io.registerInput(-1);
		long l = 8;
		io.registerInput(8l);
		float f = 3.0f;
		io.registerInput(2.0f);
		io.registerInput(32767);
		
		byte b = (byte)'1';
		io.registerInput(b);
	}

}
