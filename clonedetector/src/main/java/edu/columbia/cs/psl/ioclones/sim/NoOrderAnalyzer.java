package edu.columbia.cs.psl.ioclones.sim;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLClassLoader;
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

import edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver;
import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class NoOrderAnalyzer extends AbstractSim {
	
	private static final Logger logger = LogManager.getLogger(NoOrderAnalyzer.class);
	
	public static Object cleanObject(Object o) {
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
				curObj = cleanObject(curObj);
				ret.add(curObj);
			}
			return ret;
		} else if (Collection.class.isAssignableFrom(clazz)) {
			try {
				Collection<Object> collection = (Collection<Object>) o;
				Collection<Object> ret = (Collection<Object>) clazz.newInstance();
				collection.forEach(element->{
					Object clean = cleanObject(element);
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
				Object clean = cleanObject(v);
				ret.put(k, clean);
			});
			return ret;
		} else {
			//Convert every other object to xmlstring
			//String xmlString = IOUtils.fromObj2XML(o);
			XMLWrapper wrapper = new XMLWrapper();
			wrapper.obj = o;
			return wrapper;
		}
	}
	
	public static Set<Object> cleanCollection(Collection<Object> c) {
		Set<Object> ret = new HashSet<Object>();
		c.forEach(o->{
			Object clean = cleanObject(o);
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
				
		return this.compareObject(c1, c2);
		
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
		try {
			String codebase = "/Users/mikefhsu/Desktop/code_repos/hitoshi_container/code_repo/bin";
			File codebaseFile = new File(codebase);
			
			Class[] parameters = new Class[]{URL.class};
			
			URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			Class sysClass = URLClassLoader.class;
			Method method = sysClass.getDeclaredMethod("addURL", parameters);
			method.setAccessible(true);
			method.invoke(sysloader, new Object[]{codebaseFile.toURI().toURL()});
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		
		File xmlFile = new File("/Users/mikefhsu/Desktop/code_repos/hitoshi_container/iorepo/R5P1Y11.ogiekako/R5P1Y11.ogiekako.ProgramA-solve-414.xml");
		File xmlFile2 = new File("/Users/mikefhsu/Desktop/code_repos/hitoshi_container/iorepo/R5P1Y12.Kristofer/R5P1Y12.Kristofer.A$Solver$1-compare-225.xml");
		
		long t1Start = Runtime.getRuntime().freeMemory();
		IORecord test = (IORecord)IOUtils.fromXML2Obj(xmlFile);
		long t1End = Runtime.getRuntime().freeMemory();
		long t1Diff = t1End - t1Start;
		System.out.println("T1 size: " + (double)t1Diff/Math.pow(10, 6));
		
		long t2Start = Runtime.getRuntime().freeMemory();
		IORecord test2 = (IORecord)IOUtils.fromXML2Obj(xmlFile2);
		long t2End = Runtime.getRuntime().freeMemory();
		long t2Diff = t2End - t2Start;
		System.out.println("T2 size: " + (double)t2Diff/Math.pow(10, 6));
		
		long curMem = Runtime.getRuntime().freeMemory();
		System.out.println("Original input size: " + test.getInputs().size());
		NoOrderAnalyzer noa = new NoOrderAnalyzer();
		Set<Object> cleanInputs = NoOrderAnalyzer.cleanCollection(test.getInputs());
		Set<Object> cleanInput2 = NoOrderAnalyzer.cleanCollection(test2.getInputs());
		long afterCleanInputs = Runtime.getRuntime().freeMemory();
		long cleanDiff = afterCleanInputs - curMem;
		System.out.println("Clean inputs mem: " + (double)cleanDiff/Math.pow(10, 6));
		
		double simIn = noa.similarity(cleanInputs, cleanInput2);
		
		System.out.println("Similairty in: " + simIn);
		
		/*System.out.println("Clean size: " + cleanInputs.size());
		for (Object o: cleanInputs) {
			System.out.println(o);
		}*/
	}
}
