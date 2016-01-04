package edu.columbia.cs.psl.ioclones.pojo;

public class XMLWrapper {
	
	public String data;
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof XMLWrapper)) {
			return false;
		}
		
		XMLWrapper tmp  = (XMLWrapper) o;
		return tmp.data.equals(this.data);
	}
	
	@Override
	public int hashCode() {
		return this.data.hashCode();
	}

}
