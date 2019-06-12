package edu.columbia.cs.psl.ioclones.premain;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import edu.columbia.cs.psl.ioclones.instrument.FlowCloneTransformer;

public class PreMain {
	
	public static void premain(String args, Instrumentation inst) {
		ClassFileTransformer classTransformer = new FlowCloneTransformer();
		inst.addTransformer(classTransformer);
	}

}
