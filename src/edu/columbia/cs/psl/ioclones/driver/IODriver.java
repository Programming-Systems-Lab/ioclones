package edu.columbia.cs.psl.ioclones.driver;

import java.io.File;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class IODriver {
	
	private static final Logger logger = LogManager.getLogger(IODriver.class);
	
	private static final String iorepoDir = "./iorepo";
	
	public static void main(String args[]) {
		for (int i = 0; i < args.length; i++) {
			System.out.println(args[i]);
		}
		
		String className = args[0];
		String[] newArgs = new String[args.length - 1];
		for (int i = 1; i < args.length; i++) {
			newArgs[i - 1] = args[i];
		}
		
		try {
			logger.info("Executing: " + className);
			Class targetClass = Class.forName(className);
			Method mainMethod = targetClass.getMethod("main", String[].class);
			mainMethod.setAccessible(true);
			mainMethod.invoke(null, (Object)newArgs);
			
			File iorepo = new File(iorepoDir);
			if (!iorepo.exists()) {
				iorepo.mkdir();
			}
			
			logger.info("Reporting IOs");
			GlobalInfoRecorder.reportIOs(iorepo.getCanonicalPath());
		} catch (Exception ex) {
			
		}
	}

}
