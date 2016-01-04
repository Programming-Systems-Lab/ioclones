package edu.columbia.cs.psl.ioclones.sim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class NoOrderAnalyzer implements SimAnalyzer {
	
	public Set<Object> cleanCollection(Collection<Object> c) {
		Set<Object> ret = new HashSet<Object>();
		c.forEach(o->{
			if (o == null) {
				ret.add(o);
			} else {
				Class clazz = o.getClass();
				if (Float.class.isAssignableFrom(clazz)) {
					Float val = (Float) o;
					BigDecimal bd = new BigDecimal(val.floatValue());
					bd.setScale(FLOAT_SCALE, RoundingMode.HALF_UP);
					ret.add(bd.floatValue());
				} else if (Double.class.isAssignableFrom(clazz)) {
					Double val = (Double) o;
					BigDecimal bd = new BigDecimal(val.doubleValue());
					bd.setScale(FLOAT_SCALE, RoundingMode.HALF_UP);
					ret.add(bd.doubleValue());
				} else if (Boolean.class.isAssignableFrom(clazz) 
						|| Character.class.isAssignableFrom(clazz) 
						|| Byte.class.isAssignableFrom(clazz) 
						|| Short.class.isAssignableFrom(clazz) 
						|| Integer.class.isAssignableFrom(clazz) 
						|| Long.class.isAssignableFrom(clazz) 
						|| String.class.isAssignableFrom(clazz)) {
					ret.add(o);
				} else {
					String xmlString = IOUtils.fromObj2XML(o);
					XMLWrapper wrapper = new XMLWrapper();
					wrapper.data = xmlString;
					ret.add(o);
				}
			}
		});
		
		return ret;
	}
	
	@Override
	public double similarity(Collection<Object> c1, Collection<Object> c2) {
		int common = 0;
		c1.forEach(obj1->{
			c2.forEach(obj2->{
				
			});
		});
		
		return 0.0;
	}

}
