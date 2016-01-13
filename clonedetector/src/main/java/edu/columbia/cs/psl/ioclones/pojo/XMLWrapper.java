package edu.columbia.cs.psl.ioclones.pojo;

public class XMLWrapper {
	
	public Object obj;
		
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
