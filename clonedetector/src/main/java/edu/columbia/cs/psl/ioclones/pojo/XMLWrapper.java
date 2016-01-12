package edu.columbia.cs.psl.ioclones.pojo;

import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class XMLWrapper {
	
	public Object obj;
	
	@Override
	public String toString() {
		return IOUtils.fromObj2XML(this.obj);
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
		return this.obj.hashCode();
	}

}
