package edu.columbia.cs.psl.ioclones.analysis;

import java.util.List;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;

public interface ISimilarity {
	
	public double computeElementSim(Object o1, Object o2);
	
	public double computeIOSim(List l1, List l2);
	
	public double computeTotalSim(IORecord io1, IORecord io2);

}
