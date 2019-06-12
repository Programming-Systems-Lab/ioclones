package edu.columbia.cs.psl.ioclones.sim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
	
	public static final double WEIGHT = 0.6;
	
	public static String XML_ALG = null;
	
	public static boolean lenFilter = false;
	
	public static final Correlation linear = new Correlation() {
		public double correlation(double sim1, double sim2) {
			if (Math.abs(sim1 - 0) < TOLERANCE && Math.abs(sim2 - 0) < TOLERANCE) {
				return 0;
			}
			
			return (1 - WEIGHT) * sim1 + WEIGHT * sim2;
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

	/**
	 * The method returns an expotential model with variable base
	 * 
	 * <p>User guide: you can change the base by base value
	 * 
	 * @author Zhijian Jiang
	 */
	public static final Correlation expo2 = new Correlation() {
		public double base = Math.E;

		public double cons = 3.0;
		
		public double normalizer = Math.pow((1.0 - cons * Math.pow(base, 1)), 2); 
		
		public double correlation(double sim1, double sim2) {
			if (Math.abs(sim1 - 0) < TOLERANCE && Math.abs(sim2 - 0) < TOLERANCE) {
				return 0;
			}
			
			double numerator = (1 - cons * Math.pow(base, sim1)) * (1 - cons * Math.pow(base, sim2));
			
			return numerator/normalizer;
		}
	};
	
	public static boolean filter(int size1, int size2) {
		int max = Math.max(size1, size2);
		int min = Math.min(size1, size2);
		
		double val = ((double)min)/max;
		BigDecimal bdVal = new BigDecimal(val);
		int result = bdVal.compareTo(FILTER_THRESH);
		if (result < 0) {
			return true;
		} else {
			return false;
		}
	}
	
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
			//No array, not other collection, since all arrays/collections are converted to list
			Collection<Object> c1 = (Collection<Object>) o1;
			Collection<Object> c2 = (Collection<Object>) o2;
						
			if (c1.size() == 0 || c2.size() == 0) {
				return 0.0;
			}
			
			if (AbstractSim.lenFilter) {
				if (filter(c1.size(), c2.size())) {
					return 0.0;
				}
			}
			
			//Start from greedy algorithm
			DHSComparator sorter = new DHSComparator();
			List<Object> c1Copy = new ArrayList<Object>(c1);
			List<Object> c2Copy = new ArrayList<Object>(c2);
			
			Collections.sort(c1Copy, sorter);
			Collections.sort(c2Copy, sorter);
			
			int[] simRecord = new int[c2Copy.size()];
			
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
					
					BigDecimal cur = new BigDecimal(curSim);
					BigDecimal best = new BigDecimal(bestSim);
					if (cur.compareTo(best) == 1) {
						bestSim = curSim;
						bestMatch = j;
					}
				}
				
				if (bestMatch != -1) {
					simRecord[bestMatch] = 1;
					simSum += bestSim;
				}
			}
			
			int maxLen = Math.max(c1.size(), c2.size());
			double sim = simSum/maxLen;
			return sim;
		} else if (Map.class.isAssignableFrom(clazz1) 
				&& Map.class.isAssignableFrom(clazz2)) {
			Map c1 = (Map) o1;
			Map c2 = (Map) o2;
			
			if (c1.size() == 0 || c2.size() == 0) {
				return 0.0;
			}
			
			if (lenFilter) {
				if (filter(c1.size(), c2.size())) {
					return 0.0;
				}
			}
			
			double simSum = 0.0;
			for (Object key1: c1.keySet()) {
				Object val1 = c1.get(key1);
				Object val2 = c2.get(key1);
				
				simSum += this.compareObject(val1, val2);
			}
			
			double sim = simSum/Math.max(c1.size(), c2.size());
			return sim;
		} else if ((o1 instanceof XMLWrapper) && (o2 instanceof XMLWrapper)) {
			XMLWrapper xml1 = (XMLWrapper) o1;
			XMLWrapper xml2 = (XMLWrapper) o2;
			
			if (XML_ALG.equals(DHASH)) {				
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
	
	public static class DHSComparator implements Comparator<Object> {

		@Override
		public int compare(Object o1, Object o2) {
			int o1Hash, o2Hash;
			o1Hash = o2Hash = -1;
			if (o1 == null) {
				o1Hash = 0;
			} else {
				o1Hash = o1.hashCode();
			}
			
			if (o2 == null) {
				o2Hash = 0;
			} else {
				o2Hash = o2.hashCode();
			}
			return o1Hash > o2Hash?1:(o1Hash < o2Hash?-1:0);
		}
		
	}
}
