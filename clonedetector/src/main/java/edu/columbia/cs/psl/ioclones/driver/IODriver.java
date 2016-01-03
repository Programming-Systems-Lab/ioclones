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
import edu.columbia.cs.psl.ioclones.utils.ShutdownLogger;

public class IODriver {
	
	private static final Logger logger = LogManager.getLogger(IODriver.class);
	
	private static final String iorepoDir = "iorepo";
	
	public static void main(String args[]) {
		/*for (int i = 0; i < args.length; i++) {
			System.out.println(args[i]);
		}*/
		IOCloneConfig config = IOCloneConfig.getInstance();
		logger.info("Configuration: ");
		logger.info(config);
		
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
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					logger.info("Reporting IOs");
					ShutdownLogger.appendMessage("Reporting IOs");
					try {
						int recordCount = GlobalInfoRecorder.getRecordCounter();
						int exactCount = GlobalInfoRecorder.reportIOs(iorepo.getCanonicalPath(), targetClass.getName());
						logger.info("Record count: " + recordCount);
						logger.info("Exact count: " + exactCount);
					} catch (Exception ex) {
						logger.error("Error: ", ex);
					}
					logger.info("Finish io clone reporting");
				}
			});
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
}