package edu.columbia.cs.psl.ioclones.sim;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;
import edu.columbia.cs.psl.ioclones.utils.XMLDiffer;

public abstract class AbstractSim implements SimAnalyzer {
	
	private static final Logger logger = LogManager.getLogger(AbstractSim.class);
	
	@Override
	public boolean compareObject(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return true;
		}
		
		if (o1 == null) {
			return false;
		}
		
		if (o2 == null) {
			return false;
		}
		
		if (o1 == o2) {
			return true;
		}
		
		if (o1.equals(o2)) {
			//char, string, boolean should be handle here
			return true;
		}
		
		//Try floatint point
		try {
			double d1 = Double.parseDouble(o1.toString());
			double d2 = Double.parseDouble(o2.toString());
			
			if (Math.abs(d1 - d2) < TOLERANCE) {
				return true;
			} else {
				return false;
			}
		} catch (NumberFormatException ex) {
			//do nothing
		}
		
		Class clazz1 = o1.getClass();
		Class clazz2 = o2.getClass();
		//No array, since all arrays are converted to list
		if (Collection.class.isAssignableFrom(clazz1) 
				&& Collection.class.isAssignableFrom(clazz2)) {
			Collection<Object> c1 = (Collection<Object>) o1;
			Collection<Object> c2 = (Collection<Object>) o2;
			if (c1.size() != c2.size()) {
				return false;
			}
			
			Iterator<Object> c1IT = c1.iterator();
			Iterator<Object> c2IT = c2.iterator();
			while (c1IT.hasNext()) {
				Object co1 = c1IT.next();
				Object co2 = c2IT.next();
				
				if (!this.compareObject(co1, co2)) {
					return false;
				}
			}
			
			return true;
		} else if (Map.class.isAssignableFrom(clazz1) 
				&& Map.class.isAssignableFrom(clazz2)) {
			Map c1 = (Map) o1;
			Map c2 = (Map) o2;
			
			if (c1.size() != c2.size()) {
				return false;
			}
			
			for (Object key: c1.keySet()) {
				Object val1 = c1.get(key);
				Object val2 = c2.get(key);
				
				if (!this.compareObject(val1, val2)) {
					return false;
				}
			}
			
			return true;
		} else if ((o1 instanceof XMLWrapper) && (o2 instanceof XMLWrapper)) {
			XMLWrapper xml1 = (XMLWrapper) o1;
			XMLWrapper xml2 = (XMLWrapper) o2;
			
			Diff xmlDiff = XMLDiffer.xmlDiff(xml1.data, xml2.data);
			
			Iterator<Difference> diffIT = xmlDiff.getDifferences().iterator();
			while (diffIT.hasNext()) {
				Difference diff = diffIT.next();
				
				if (diff.getResult() == ComparisonResult.DIFFERENT) {
					return false;
				}
			}
			return true;
		} else {
			logger.error("Unidentified obj type: " + clazz1.getName() + " " + clazz2.getName());
			return false;
		}
	}
}
