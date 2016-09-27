package edu.columbia.cs.psl.ioclones.driver;

import java.io.File;
import java.lang.reflect.Method;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.config.IOCloneConfig;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;
import edu.columbia.cs.psl.ioclones.utils.ShutdownLogger;

public class IODriver {
	
	private static final Logger logger = LogManager.getLogger(IODriver.class);
	
	public static final String iorepoDir = "iorepo";
	
	public static final String profileDir = "classinfo";
	
	private static long timeLimit;
	
	public static long startTime;
	
	public static String className;
	
	public static boolean timeOut = false;
	
	static {
		startTime = System.currentTimeMillis();
		timeLimit = IOCloneConfig.getInstance().getTimeLimit() * 60 * 1000;
		logger.info("Start time: " + startTime);
		logger.info("Time limit: " + timeLimit);
	}
	
	public static boolean isTimeOut() {
		if (timeOut) {
			return true;
		}
		
		long diff = System.currentTimeMillis() - startTime;
		if (diff > timeLimit) {
			logger.info("Time out " + className + " " + diff);
			logger.info("Threshold: " + timeLimit);
			timeOut = true;
		}
		
		return timeOut;
	}
	
	public static void main(String args[]) {
		/*for (int i = 0; i < args.length; i++) {
			System.out.println(args[i]);
		}*/
		IOCloneConfig config = IOCloneConfig.getInstance();
		logger.info("Configuration: ");
		logger.info(config);
		
		if (!IOCloneConfig.getInstance().isDynamic()) {
			logger.info("Loading class info");
			//IOUtils.unzipClassInfo();
			IOUtils.loadMethodIODeps("cb");
		}
				
		className = args[0];
		String[] newArgs = new String[args.length - 1];
		for (int i = 1; i < args.length; i++) {
			newArgs[i - 1] = args[i];
		}
		
		try {
			logger.info("Executing: " + className);
			Class targetClass = Class.forName(className);
			
			File iorepo = new File(iorepoDir);
			if (!iorepo.exists()) {
				iorepo.mkdir();
			}
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					ShutdownLogger.appendMessage("Reporting IOs");
					try {
						int recordCount = GlobalInfoRecorder.getRecordCounter();
						int exactCount = GlobalInfoRecorder.reportIOs(iorepo.getCanonicalPath(), targetClass.getName());
						
						ShutdownLogger.appendMessage("Record count: " + recordCount);
						ShutdownLogger.appendMessage("Exact count: " + exactCount);
					} catch (Exception ex) {
						ShutdownLogger.appendException(ex);
					}
					ShutdownLogger.appendMessage("Finish io clone reporting");
					ShutdownLogger.finalFlush();
				}
			});
			
			Method mainMethod = targetClass.getMethod("main", String[].class);
			mainMethod.setAccessible(true);
			mainMethod.invoke(null, (Object)newArgs);
			long endTime = System.currentTimeMillis();
			long elapsed = endTime - startTime;
			logger.info("Execution time: " + className + " " + elapsed + " ms");
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
}