package edu.columbia.cs.psl.ioclones.sim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;
import edu.columbia.cs.psl.ioclones.utils.DeepHash;

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
		if (c1.size() == 0 && c2.size() == 0) {
			return 0;
		}
		
		int[] deep1 = genDeepHash(c1);
		int[] deep2 = genDeepHash(c2);
		
		int[] simRecord = new int[deep2.length];
		double sameCounter = 0;
		for (int i = 0; i < deep1.length; i++) {
			int hash1 = deep1[i];
						
			for (int j = 0; j < deep2.length; j++) {
				if (simRecord[j] == 1) {
					continue ;
				}
				
				int hash2 = deep2[j];
				
				if (hash1 == hash2) {
					simRecord[j] = 1;
					sameCounter++;
					break ;
				}
			}
		}
		
		//simSum is the intersection
		double jaccard = sameCounter/(c1.size() + c2.size() - sameCounter);
		return jaccard;
	}
	
	public static void main(String[] args) {
		List<Object> control = new ArrayList<Object>();
		control.add("2");
		control.add("abc");
		control.add(2.915);
		
		List<Object> test = new ArrayList<Object>();
		test.add(2.92);
		test.add("abc");
		test.add("^&%$");
		XMLWrapper w = new XMLWrapper("2");
		test.add(w);
		
		FastAnalyzer fa = new FastAnalyzer();
		System.out.println(fa.similarity(control, test));
		
		System.out.println(DeepHash.deepHash(2.915));
		System.out.println(DeepHash.deepHash(2.92));
		System.out.println(DeepHash.deepHash(0.9));
		System.out.println(DeepHash.deepHash(0.899));
		System.out.println(DeepHash.deepHash(0.0));
		
		Set<Object> cSet = new HashSet<Object>();
		cSet.add(false);
		
		Set<Object> tSet = new HashSet<Object>();
		long tmp = 1237L;
		tSet.add(tmp);
		System.out.println(fa.similarity(cSet, tSet));
	}

}
