package edu.columbia.cs.psl.ioclones.pojo;

import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class IORecord {
	
	private String methodKey;
	
	private int id;
	
	public IORecord(String methodKey) {
		this.methodKey = methodKey;
		this.id = GlobalInfoRecorder.getMethodIndex();
	}

}
