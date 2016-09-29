package edu.columbia.cs.psl.ioclones.premain;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import edu.columbia.cs.psl.ioclones.instrument.FlowCloneTransformer;

public class PreMain {
	
	public static Instrumentation instPtr;
	
	public static void premain(String args, Instrumentation inst) {
		ClassFileTransformer classTransformer = new FlowCloneTransformer();
		inst.addTransformer(classTransformer);
		
		//instPtr = inst;
		//observeLoadedClasses();
	}
	
	public static void observeLoadedClasses() {
		Class[] classes = instPtr.getAllLoadedClasses();
	    List<Class> candidates = new ArrayList<Class>();
	    for (Class c : classes) {
	        if (instPtr.isModifiableClass(c)){
	        	if (c.getName().startsWith("R5P1Y13"))
	        		candidates.add(c);
	        }
	    }
	    System.out.println("There are "+candidates.size()+" classes");
	    try {
	        // if we have matching candidates, then
	        // retransform those classes so that we
	        // will get callback to transform.
	        if (!candidates.isEmpty()) {
	            Iterator it = candidates.iterator();
	            while(it.hasNext()){
	                Class c = (Class)it.next();
	                System.out.println(" ========================> In Progress:"+c.getName());
	            }
	        }else{
	            System.out.println("candidates.isEmpty()");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

}
