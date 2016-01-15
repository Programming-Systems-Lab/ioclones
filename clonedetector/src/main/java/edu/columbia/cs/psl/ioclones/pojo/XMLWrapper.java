package edu.columbia.cs.psl.ioclones.pojo;

public class XMLWrapper {
	
	public static final int UNINITIALIZED = Integer.MIN_VALUE;
	
	public Object obj;
	
	public transient int deepHash = Integer.MIN_VALUE;
		
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
		return this.obj.hashCode();
	}

}
