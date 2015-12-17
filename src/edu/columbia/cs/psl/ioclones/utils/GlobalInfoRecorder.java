package edu.columbia.cs.psl.ioclones.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class GlobalInfoRecorder {
	
	public static final AtomicInteger methodIndexer = new AtomicInteger();
	
	public static int getMethodIndex() {
		return methodIndexer.getAndIncrement();
	}

}
