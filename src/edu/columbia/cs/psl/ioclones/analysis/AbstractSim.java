package edu.columbia.cs.psl.ioclones.analysis;

import java.util.List;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;

public abstract class AbstractSim implements ISimilarity {
	
	@Override
	public double computeElementSim(Object o1, Object o2) {
		return 0.0;
	}
	
	@Override
	public double computeTotalSim(IORecord io1, IORecord io2) {
		// TODO Auto-generated method stub
		double iSim = this.computeIOSim(io1.getInputs(), io2.getInputs());
		double oSim = this.computeIOSim(io1.getOutputs(), io2.getOutputs());
		
		return iSim * oSim;
	}

}
