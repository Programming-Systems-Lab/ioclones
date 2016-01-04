package edu.columbia.cs.psl.ioclones.sim;

import java.util.Collection;

public interface SimAnalyzer {
	
	//public static final int FLOAT_SCALE = 5;
	
	public static final double TOLERANCE = Math.pow(10, -5);
	
	public boolean compareObject(Object o1, Object o2);
	
	public Collection<Object> cleanCollection(Collection<Object> c);
	
	public double similarity(Collection<Object> c1, Collection<Object> c2);

}
