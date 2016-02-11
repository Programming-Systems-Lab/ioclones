package edu.columbia.cs.psl.ioclones.sim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;
import edu.columbia.cs.psl.ioclones.utils.DeepHash;
import edu.columbia.cs.psl.ioclones.utils.Test.DupProj2;

public class FastAnalyzer extends AbstractSim {
	
	public static int[] genDeepHash(Collection<Object> collection) {
		int[] ret = new int[collection.size()];
		int counter = 0;
		for (Object o: collection) {
			int deepHash = -1;
			if (o instanceof XMLWrapper) {
				deepHash = ((XMLWrapper)o).deepHash;
			} else {
				deepHash = DeepHash.deepHash(o);
			}
			ret[counter++] = deepHash;
		}
		
		//Arrays.sort(ret);
		return ret;
	}
	
	@Override
	public double similarity(Collection<Object> c1, Collection<Object> c2) {
		// TODO Auto-generated method stub
		int[] deep1 = genDeepHash(c1);
		int[] deep2 = genDeepHash(c2);
		
		int[] simRecord = new int[deep2.length];
		double simSum = 0;
		for (int i = 0; i < deep1.length; i++) {
			int hash1 = deep1[i];
			
			double bestSim = 0.0;
			int bestMatch = -1;
			
			for (int j = 0; j < deep2.length; j++) {
				if (simRecord[j] == 1) {
					continue ;
				}
				
				int hash2 = deep2[j];
				
				if (hash1 == hash2) {
					bestSim = 1.0;
					bestMatch = j;
					break ;
				}
			}
			
			if (bestMatch != -1) {
				simRecord[bestMatch] = 1;
				simSum += bestSim;
			}
		}
		
		if (c1.size() == 0 && c2.size() == 0) {
			return 0;
		}
		
		//simSum is the intersection
		double jaccard = simSum/(c1.size() + c2.size() - simSum);
		return jaccard;
	}
	
	public static void main(String[] args) {
		/*List<Object> control = new ArrayList<Object>();
		control.add("2");
		control.add("abc");
		control.add(4);
		
		List<Object> test = new ArrayList<Object>();
		test.add(4);
		test.add("abc");
		test.add("^&%$");
		XMLWrapper w = new XMLWrapper("2");
		test.add(w);
		
		FastAnalyzer fa = new FastAnalyzer();
		System.out.println(fa.similarity(control, test));*/
		DupProj2 dup = new DupProj2();
		XMLWrapper w = new XMLWrapper(dup);
		boolean b = false;
		System.out.println(DeepHash.deepHash(w));
		System.out.println(DeepHash.deepHash(b));
		System.out.println(DeepHash.deepHash(0.732));
		System.out.println(new Double(0.731).hashCode());
		/*System.out.println(0.01 == 0.01000);
		Double d = new Double(0.01);
		System.out.println(d.hashCode());
		Double d2 = new Double(0.01);
		System.out.println(d2.hashCode());
		
		BigDecimal bd = new BigDecimal(0.003).setScale(2, BigDecimal.ROUND_HALF_UP);
		System.out.println(bd.hashCode());
		System.out.println(new Double(bd.doubleValue()).hashCode());
		Integer i = new Integer(15);
		System.out.println(i.hashCode());*/
		List<Double> list = new ArrayList<Double>();
		list.add(1.01);
		System.out.println(DeepHash.deepHash(list));
		System.out.println(DeepHash.deepHash(1.009));
		
		
	}

}
