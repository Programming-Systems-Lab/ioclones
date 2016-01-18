package edu.columbia.cs.psl.ioclones.sim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import edu.columbia.cs.psl.ioclones.config.IOCloneConfig;
import edu.columbia.cs.psl.ioclones.driver.SimAnalysisDriver;
import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;
import edu.columbia.cs.psl.ioclones.utils.DeepHash;
import edu.columbia.cs.psl.ioclones.utils.XMLDiffer;

public abstract class AbstractSim implements SimAnalyzer {
	
	private static final Logger logger = LogManager.getLogger(AbstractSim.class);
	
	public static String DHASH = "deepHash";
	
	public static String XML_DIFF = "xmlDiff";
	
	private static final double CONSTANT = 1.0;
	
	public static final double EXP_CONSTANT = 2;
	
	public static final double WEIGHT = 0.5;
	
	public static String XML_ALG = null;
	
	public static final Correlation linear = new Correlation() {
		public double correlation(double sim1, double sim2) {
			if (Math.abs(sim1 - 0) < TOLERANCE && Math.abs(sim2 - 0) < TOLERANCE) {
				return 0;
			}
			return 1.0 - Math.abs(sim1 - sim2);
		}
	};
	
	public static final Correlation expo = new Correlation() {
		
		public double cons = 3.0;
		
		public double normalizer = Math.pow((1.0 - cons * Math.pow(Math.E, 1)), 2); 
		
		public double correlation(double sim1, double sim2) {
			if (Math.abs(sim1 - 0) < TOLERANCE && Math.abs(sim2 - 0) < TOLERANCE) {
				return 0;
			}
			
			double numerator = (1 - cons * Math.pow(Math.E, sim1)) * (1 - cons * Math.pow(Math.E, sim2));
			
			return numerator/normalizer;
		}
	};
	
	@Override
	public double compareObject(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return 1.0;
		}
		
		if (o1 == null) {
			return 0.0;
		}
		
		if (o2 == null) {
			return 0.0;
		}
		
		if (o1 == o2) {
			return 1.0;
		}
		
		if (o1.equals(o2)) {
			//char, string, boolean should be handle here
			return 1.0;
		}
		
		//System.out.println("O1 class: " + o1.getClass());
		//System.out.println("O2 class: " + o2.getClass());
		
		//Try floatint point
		if (Number.class.isAssignableFrom(o1.getClass()) 
				&& Number.class.isAssignableFrom(o2.getClass())) {
			Number n1 = (Number) o1;
			Number n2 = (Number) o2;
			double d1 = n1.doubleValue();
			double d2 = n2.doubleValue();
			
			if (Math.abs(d1 - d2) < TOLERANCE) {
				return 1.0;
			} else {
				return 0.0;
			}
		}
		
		Class clazz1 = o1.getClass();
		Class clazz2 = o2.getClass();
		if (Collection.class.isAssignableFrom(clazz1) 
				&& Collection.class.isAssignableFrom(clazz2)) {
			//No array, since all arrays are converted to list
			long before = Runtime.getRuntime().freeMemory();
			Collection<Object> c1 = (Collection<Object>) o1;
			Collection<Object> c2 = (Collection<Object>) o2;
			
			if (c1.size() == 0 || c2.size() == 0) {
				return 0.0;
			}
			
			int min = Math.min(c1.size(), c2.size());
			int max = Math.max(c1.size(), c2.size());
			double maxSim = ((double)min)/max;
			if (maxSim - SimAnalysisDriver.simThresh < SimAnalyzer.TOLERANCE) {
				return 0.0;
			}
			
			/*Iterator<Object> c1IT = c1.iterator();
			double simSum = 0.0;
			while (c1IT.hasNext()) {
				Object co1 = c1IT.next();
				
				Iterator<Object> c2IT = c2.iterator();
				while (c2IT.hasNext()) {
					Object co2 = c2IT.next();
					simSum += this.compareObject(co1, co2);
				}
			}
			System.out.println("Simsum: " + simSum);
			double simRank = (CONSTANT * simSum)/(c1.size() * c2.size());
			return simRank;*/
			
			//Start from greedy algorithm
			List<Object> c1Copy = new ArrayList<Object>(c1);
			List<Object> c2Copy = new ArrayList<Object>(c2);
			int[] simRecord = new int[c2Copy.size()];
			
			if (c1Copy.size() > 1000 || c2Copy.size() > 1000) {
				logger.info("Detect large I/O: " + c1Copy.size() + " " + c2Copy.size());
			}
			long afterCopy = Runtime.getRuntime().freeMemory();
			
			double diff = ((double)(afterCopy - before))/Math.pow(10, 6);
			if (diff > 100) {
				logger.info("Large copy: " + c1Copy.size() + " " + c2Copy.size());
			}
			
			double simSum = 0;
			for (int i = 0; i < c1Copy.size(); i++) {
				Object c1Obj = c1Copy.get(i);
				double bestSim = 0.0;
				int bestMatch = -1;
				
				for (int j = 0; j < c2Copy.size(); j++) {
					if (simRecord[j] == 1) {
						continue ;
					}
					
					Object c2Obj = c2Copy.get(j);
					double curSim = this.compareObject(c1Obj, c2Obj);
					//System.out.println(c1Obj + " " + c2Obj + " " + curSim);
					
					BigDecimal cur = new BigDecimal(curSim);
					BigDecimal best = new BigDecimal(bestSim);
					if (cur.compareTo(best) == 1) {
						bestSim = curSim;
						bestMatch = j;
					}
				}
				//System.out.println("Best match: " + bestMatch);
				//System.out.println("Best sim: " + bestSim);
				
				if (bestMatch != -1) {
					simRecord[bestMatch] = 1;
					simSum += bestSim;
				}
			}
			
			long afterCompare = Runtime.getRuntime().freeMemory();
			double compareDiff = ((double)(afterCompare - afterCopy))/Math.pow(10, 6);
			if (compareDiff > 1000) {
				logger.info("Large compare: " + c1.size() + " " + c2.size());
			}
			
			//System.out.println("Sim sum: " + simSum);
			int maxLen = Math.max(c1.size(), c2.size());
			double sim = simSum/maxLen;
			//System.out.println("Similarity: " + sim);
			return sim;
		} else if (Map.class.isAssignableFrom(clazz1) 
				&& Map.class.isAssignableFrom(clazz2)) {
			Map c1 = (Map) o1;
			Map c2 = (Map) o2;
			
			if (c1.size() == 0 || c2.size() == 0) {
				return 0.0;
			}
			
			int min = Math.min(c1.size(), c2.size());
			int max = Math.max(c1.size(), c2.size());
			double maxSim = ((double)min)/max;
			if (maxSim - SimAnalysisDriver.simThresh < SimAnalyzer.TOLERANCE) {
				return 0.0;
			}
			
			double simSum = 0.0;
			for (Object key1: c1.keySet()) {
				Object val1 = c1.get(key1);
				
				for (Object key2: c2.keySet()) {
					Object val2 = c2.get(key2);
					simSum += this.compareObject(val1, val2);
				}
			}
			
			double simRank = (CONSTANT * simSum)/(c1.size() * c2.size());
			return simRank;
		} else if ((o1 instanceof XMLWrapper) && (o2 instanceof XMLWrapper)) {
			XMLWrapper xml1 = (XMLWrapper) o1;
			XMLWrapper xml2 = (XMLWrapper) o2;
			
			long before = Runtime.getRuntime().freeMemory();
			
			if (XML_ALG.equals(DHASH)) {
				if (xml1.deepHash == XMLWrapper.UNINITIALIZED) {
					xml1.deepHash = DeepHash.deepHash(xml1.obj);
				}
				
				if (xml2.deepHash == XMLWrapper.UNINITIALIZED) {
					xml2.deepHash = DeepHash.deepHash(xml2.obj);
				}
				
				long after = Runtime.getRuntime().freeMemory();
				double diff = ((double)(after - before))/Math.pow(10, 6);
				if (diff > 1000) {
					logger.info("Large xml compare: " + xml1.obj + " " + xml2.obj);
				}
				
				if (xml1.deepHash == xml2.deepHash) {
					return 1.0;
				} else {
					return 0.0;
				}
			} else if (XML_ALG.equals(XML_DIFF)) {
				Diff xmlDiff = XMLDiffer.xmlDiff(xml1.obj, xml2.obj);
				Iterator<Difference> diffIT = xmlDiff.getDifferences().iterator();
				while (diffIT.hasNext()) {
					Difference diff = diffIT.next();
					
					if (diff.getResult() == ComparisonResult.DIFFERENT) {
						return 0.0;
					}
				}
				return 1.0;
			} else {
				logger.error("Invalid xml algorithm: " + XML_ALG);
				return 0.0;
			}
		} else {
			//logger.error("Unidentified obj type: " + clazz1.getName() + " " + clazz2.getName());
			return 0.0;
		}
	}
	
	public interface Correlation {
		
		public double correlation(double sim1, double sim2);
	}
}
