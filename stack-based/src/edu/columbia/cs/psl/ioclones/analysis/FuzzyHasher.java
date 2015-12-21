package edu.columbia.cs.psl.ioclones.analysis;

import java.util.ArrayList;
import java.util.List;

import com.cedarsoftware.util.DeepEquals;

public class FuzzyHasher extends ComplexHasher {
	
	@Override
	public double computeIOSim(List l1, List l2) {
		// TODO Auto-generated method stub
		List<Object> l1Record = new ArrayList<Object>();
		List<Object> l2Record = new ArrayList<Object>();
		
		this.unrollObj(l1, l1Record);
		this.unrollObj(l2, l2Record);
		
		if (l1.size() == 0 && l2.size() == 0) {
			return 1.0;
		} else if (l1.size() == 0) {
			return 0.0;
		} else if (l2.size() == 0) {
			return 0.0;
		}
		
		if (l1Record.size() != l2Record.size()) {
			return 0.0;
		} else {
			int total = l1Record.size();
			int counter = 0;
			for (Object o1: l1) {
				int hash1 = DeepEquals.deepHashCode(o1);
				for (Object o2: l2) {
					int hash2 = DeepEquals.deepHashCode(o2);
					if (hash1 == hash2) {
						counter++;
					}
				}
			}
			
			double sim = (double)counter/total;
			return sim;
		}
	}

}
