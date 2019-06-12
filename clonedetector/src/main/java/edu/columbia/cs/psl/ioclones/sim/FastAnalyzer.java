package edu.columbia.cs.psl.ioclones.sim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.pojo.XMLWrapper;
import edu.columbia.cs.psl.ioclones.utils.DeepHash;

public class FastAnalyzer extends AbstractSim {
	/**
	 * Generate deep hash
	 */
	private static final Logger logger = LogManager.getLogger(FastAnalyzer.class);
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

	/**
	 * This method is used to get the similarity coefficient between two methods. 
	 * 
	 * <p>User guide: this method provide four options of the similarity index: 
	 * <a href="https://en.wikipedia.org/wiki/Cosine_similarity">Cosine Coefficient</a>, 
	 * <a href="http://www.daylight.com/dayhtml/doc/theory/theory.finger.html">Euclid Coefficient</a>, 
	 * <a href="https://www.ibm.com/support/knowledgecenter/en/SSLVMB_22.0.0/com.ibm.spss.statistics.algorithms/alg_proximities_hamann.htm">Hamman Coefficient</a>, 
	 * <a href="https://en.wikipedia.org/wiki/Simple_matching_coefficient">Simple Matching Coefficient</a>, 
	 * <a href="https://en.wikipedia.org/wiki/Jaccard_index">Jaccard Index</a>, 
	 * <a href="https://en.wikipedia.org/wiki/S%C3%B8rensen%E2%80%93Dice_coefficient">Sørensen–Dice coefficient</a> 
	 * and <a href="https://en.wikipedia.org/wiki/Tversky_index">Tversky index</a>.
	 * 
	 * You can change the indexType to choose difference coefficient. 
	 * The possible values of indexType include "cosine", "euclid", "hamman", "simple", "jaccard", "tversky", and "dice": 
	 * "cosine" refers to Cosine Coefficient,
	 * "euclid" refers to Euclide Coefficient,
	 * "hamann" refers to Hamman Coefficient, 
	 * "simple" refers to Simple Matching Coefficient,
	 * "jaccard" refers to Jaccard index,
	 * "dice" refers to Sørensen–Dice coefficient,
	 * "tversky" refers to Tversky index.
	 * If you choose "tversky", you can change alpha or beta to set the Tversky index parameters.
	 * 
	 * @author Zhijian
	 * @returns {double}
	 */
	@Override
	public synchronized double similarity(Collection<Object> c1, Collection<Object> c2) {
		// TODO Auto-generated method stub
		String indexType = "jaccard"; // change this to choose different coefficients
		double alpha = 0.75, beta = 0.75; // tversky index parameter

		if (c1.size() == 0 && c2.size() == 0) {
			return 0;
		}

		Set<Integer> deep1 = new HashSet<>();
		Set<Integer> deep2 = new HashSet<>();
		int[] deepArray1 = genDeepHash(c1);
		int[] deepArray2 = genDeepHash(c2);
		
		for(int hash: genDeepHash(c1)){
			deep1.add(hash);
		}
		for(int hash: genDeepHash(c2)){
			deep2.add(hash);
		}
		
		// intersection of deep1 and deep2
		double intersection = 0.0;
		for(int hash1: deep1){
			if(deep2.contains(hash1)){
				intersection++;
			}
		}

		// deep1 - deep2
		Set<Integer> deep1MinusDeep2 = new HashSet<Integer>(deep1);
		deep1MinusDeep2.removeAll(deep2);

		// deep2 - deep1
		Set<Integer> deep2MinusDeep1 = new HashSet<Integer>(deep2);
		deep2MinusDeep1.removeAll(deep1);
		
		Map<String, Double> indexMap = new HashMap<>();

		// euclid coefficient
		if(deepArray1.length == deepArray2.length){
			double sum = 0.0;
			for(int i = 0; i < deep1.size(); i++){
				sum += Math.pow(deepArray1[i] - deepArray2[i], 2);
			}
			indexMap.put("euclid", 1 / (Math.sqrt(sum) + 1));
			// logger.info("sum = " + sum + " distance = " + Math.sqrt(sum) + " euclid = " + Math.min(1 / Math.sqrt(sum), 1.0));
		}else{
			indexMap.put("euclid", 0.0);
		}

		// cosine coefficient
		if(deepArray1.length == deepArray2.length){
			double sum1 = 0.0;
			double sum2 = 0.0;
			double sum3 = 0.0;
			for(int i = 0; i < deepArray1.length; i++){
				sum1 += deepArray1[i] * deepArray2[i];
				sum2 += Math.pow(deepArray1[i], 2);
				sum3 += Math.pow(deepArray2[i], 2);
			}
			double cosine = sum1 / (Math.sqrt(sum2) * Math.sqrt(sum3));

			indexMap.put("cosine", Double.isNaN(cosine) ? -1: cosine);
		}else{
			indexMap.put("cosine", 0.0);
		}

		indexMap.put("hamann", (intersection - deep1MinusDeep2.size() - deep2MinusDeep1.size())
			/ (intersection + deep1MinusDeep2.size() + deep2MinusDeep1.size()));

		double jaccard = intersection/(c1.size() + c2.size() - intersection);
// 		System.out.println("c1Size: " + c1.size() + "\n" +
// 			"c2Size: " + c2.size() + "\n" +
// 			"intersection: " +intersection + "\n" +
// 			Arrays.toString(c1.toArray())+ "\n" +
// 			Arrays.toString(c2.toArray())); 
		
		indexMap.put("simple", intersection / (deep1.size() + deep2.size()));

		indexMap.put("jaccard", jaccard);

		indexMap.put("tversky", intersection / (intersection + alpha * (double)deep1MinusDeep2.size() + beta * (double)deep2MinusDeep1.size())); 

		indexMap.put("dice", 2 * intersection / (deep1.size() + deep2.size()));

		return indexMap.get(indexType);
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
