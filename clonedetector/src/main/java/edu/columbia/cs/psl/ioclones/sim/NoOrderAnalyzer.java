package edu.columbia.cs.psl.ioclones.sim;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class NoOrderAnalyzer extends AbstractSim {
	
	private static final Logger logger = LogManager.getLogger(NoOrderAnalyzer.class);
	
	private static double THRESHOLD = 0.7;
	
	public Object cleanObject(Object o) {
		if (o == null) {
			return o;
		}
		
		Class clazz = o.getClass();
		if (String.class.isAssignableFrom(clazz) 
				|| Boolean.class.isAssignableFrom(clazz) 
				|| Character.class.isAssignableFrom(clazz) 
				|| Number.class.isAssignableFrom(clazz)) {
			return o;
		} else if (clazz.isArray()) {
			//For array, convert to list
			int arrayLength = Array.getLength(o);
			List<Object> ret = new ArrayList<Object>();
			for (int i = 0; i < arrayLength; i++) {
				Object curObj = Array.get(o, i);
				curObj = this.cleanObject(curObj);
				ret.add(curObj);
			}
			return ret;
		} else if (Collection.class.isAssignableFrom(clazz)) {
			try {
				Collection<Object> collection = (Collection<Object>) o;
				Collection<Object> ret = (Collection<Object>) clazz.newInstance();
				collection.forEach(element->{
					Object clean = this.cleanObject(element);
					ret.add(clean);
				});
				return ret;
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
			return o;
		} else if (Map.class.isAssignableFrom(clazz)) {
			Map<Object, Object> map = (Map<Object, Object>) o;
			Map ret = new HashMap();
			map.forEach((k, v)->{
				Object clean = this.cleanObject(v);
				ret.put(k, clean);
			});
			return ret;
		} else {
			//Convert every other object to xmlstring
			String xmlString = IOUtils.fromObj2XML(o);
			XMLWrapper wrapper = new XMLWrapper();
			wrapper.data = xmlString;
			return wrapper;
		}
	}
	
	public Set<Object> cleanCollection(Collection<Object> c) {
		Set<Object> ret = new HashSet<Object>();
		c.forEach(o->{
			Object clean = this.cleanObject(o);
			ret.add(clean);
		});
		
		return ret;
	}
	
	@Override
	public double similarity(Collection<Object> c1, Collection<Object> c2) {
		if (c1.size() == 0 && c2.size() == 0) {
			return 1;
		}
		
		if (c1.size() == 0) {
			return 0;
		}
		
		if (c2.size() == 0) {
			return 0;
		}
		
		Set<Object> clean1 = this.cleanCollection(c1);
		Set<Object> clean2 = this.cleanCollection(c2);
		
		return this.compareObject(clean1, clean2);
		
		/*while (it1.hasNext()) {
			Object o1 = it1.next();
			
			while (it2.hasNext()) {
				Object o2 = it2.next();
				
				if (this.compareObject(o1, o2)) {
					commonSet.add(o1);
					it2.remove();
					break ;
				}
			}
		}
		
		int intersect = commonSet.size();
		int union = clean1.size() + clean2.size() - intersect;
		double jaccard = ((double)intersect)/union;
		
		return jaccard;*/
	}
	
	public static void main(String[] args) {
		System.out.println(Math.pow(Math.E, -(2 * 0.4)));
		System.out.println((1.0 - 3 * Math.pow(Math.E, 0)) * (1.0 - 3 * Math.pow(Math.E, 0.038)));
		System.out.println((1.0 - 3 * Math.pow(Math.E, 1)) * (1.0 - 3 * Math.pow(Math.E, 1)));
		
		System.out.println(AbstractSim.expo.correlation(0.9, 0.9));
		
	}
}
