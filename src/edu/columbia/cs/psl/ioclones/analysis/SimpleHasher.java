package edu.columbia.cs.psl.ioclones.analysis;

import java.util.List;

import com.cedarsoftware.util.DeepEquals;

public class SimpleHasher extends AbstractSim {
	
	@Override
	public double computeIOSim(List l1, List l2) {
		// TODO Auto-generated method stub
		int hash1 = DeepEquals.deepHashCode(l1);
		int hash2 = DeepEquals.deepHashCode(l2);
		
		if (hash1 == hash2) {
			return 1.0;
		} else {
			return 0.0;
		}
	}
}
