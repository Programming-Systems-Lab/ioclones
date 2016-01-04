package edu.columbia.cs.psl.ioclones.sim;

import java.util.Collection;

public interface SimAnalyzer {
	
	public static final int FLOAT_SCALE = 5;
	
	public Collection<Object> cleanCollection(Collection<Object> c);
	
	public double similarity(Collection<Object> c1, Collection<Object> c2);

}
