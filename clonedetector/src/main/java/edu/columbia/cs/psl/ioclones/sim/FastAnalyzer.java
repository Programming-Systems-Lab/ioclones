package edu.columbia.cs.psl.ioclones.sim;

import java.util.Arrays;
import java.util.Collection;

import com.cedarsoftware.util.DeepEquals;

import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;

public class FastAnalyzer extends AbstractSim {
	
	public static int[] genDeepHash(Collection<Object> collection) {
		int[] ret = new int[collection.size()];
		int counter = 0;
		for (Object o: collection) {
			int deepHash = -1;
			if (o instanceof XMLWrapper) {
				deepHash = ((XMLWrapper)o).deepHash;
			} else {
				deepHash = DeepEquals.deepHashCode(o);
			}
			ret[counter++] = deepHash;
		}
		
		Arrays.sort(ret);
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
				}
			}
			
			if (bestMatch != -1) {
				simRecord[bestMatch] = 1;
				simSum += bestSim;
			}
		}
		
		int maxLen = Math.max(c1.size(), c2.size());
		
		if (maxLen == 0) {
			return 0.0;
		}
		
		double sim = simSum/maxLen;
		return sim;
	}

}
