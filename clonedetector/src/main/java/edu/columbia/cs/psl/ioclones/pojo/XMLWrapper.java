package edu.columbia.cs.psl.ioclones.pojo;

import edu.columbia.cs.psl.ioclones.utils.DeepHash;

public class XMLWrapper {
	
	public static final int UNINITIALIZED = Integer.MIN_VALUE;
	
	public Object obj;
	
	public int deepHash;
	
	public XMLWrapper(Object obj) {
		this.obj = obj;
		this.deepHash = DeepHash.deepHash(this.obj);
	}
		
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof XMLWrapper)) {
			return false;
		}
		
		XMLWrapper tmp  = (XMLWrapper) o;
		return tmp.obj.equals(this.obj);
	}
	
	@Override
	public int hashCode() {
		//return this.obj.hashCode();
		return this.deepHash;
	}

}
