package edu.columbia.cs.psl.ioclones.driver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.config.IOCloneConfig;

public class ScriptsGenerator {
	
	private static final Logger logger = LogManager.getLogger(ScriptsGenerator.class);
	
	private static final String HEADER = "#!/bin/bash";
	
	private static final String NORMAL_PRE = "normal-";
	
	private static final String SCRIPTS_PRE = "taint-";
	
	private static final String PRE_COMMAND = "jre-inst/bin/java -Xbootclasspath/a:Phosphor-0.0.2-SNAPSHOT.jar -javaagent:Phosphor-0.0.2-SNAPSHOT.jar -noverify";
	
	private static final String IO_DRIVER = "edu.columbia.cs.psl.ioclones.driver.IODriver";
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	private static final Class[] mainParameters = new Class[]{String[].class};
	
	public static void main(String[] args) throws Exception {
		String codebase = args[0];
		String transformed = args[1];
		String problemSet = args[2];
		
		System.out.println("Codebase: " + codebase);
		System.out.println("Transformed codebase: " + transformed);
		System.out.println("Problem set: " + problemSet);
		
		File codebaseFile = new File(codebase);
		if (!codebaseFile.exists()) {
			logger.error("Invalid codebase: " + codebaseFile.getAbsolutePath());
			System.exit(-1);
		}
		
		File transformedFile = new File(transformed);
		if (!transformedFile.exists()) {
			logger.error("Invalid transformed codebase: " + transformedFile.getAbsolutePath());
			System.exit(-1);
		}
		
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Class sysClass = URLClassLoader.class;
		Method method = sysClass.getDeclaredMethod("addURL", parameters);
		method.setAccessible(true);
		method.invoke(sysloader, new Object[]{codebaseFile.toURI().toURL()});
		/*for (URL u: sysloader.getURLs()) {
			System.out.println(u);
		}*/
		
		logger.info("Confirm codebase: " + codebaseFile.getAbsolutePath());		
		File psDir = new File(codebaseFile.getAbsolutePath() + "/" + problemSet);
		logger.info("Problem set dir: " + psDir.getAbsolutePath());
		Set<String> executables = new HashSet<String>();
		for (File usrDir: psDir.listFiles()) {
			if (usrDir.isDirectory() && !usrDir.getName().startsWith(".")) {
				String userDirName = usrDir.getName();
				
				for (File classFile: usrDir.listFiles()) {
					String className = classFile.getName();
					String fullName = problemSet + "." + userDirName + "." + className.substring(0, className.length() - 6);
					
					Class checkClass = Class.forName(fullName);
					try {
						Method mainMethod = checkClass.getMethod("main", mainParameters);
						executables.add(checkClass.getName());
					} catch (NoSuchMethodException noSuch) {
						//Means this class has no main
					}catch (Exception ex) {
						logger.error("Error: ", ex);
					}
				}
			}
		}
		
		//int coreNum = Runtime.getRuntime().availableProcessors();
		//logger.info("Processor number: " + coreNum);
		System.out.println("Configuration: " + IOCloneConfig.getInstance());
		StringBuilder normalBuilder = new StringBuilder();
		normalBuilder.append(HEADER + "\n");
		
		StringBuilder sb = new StringBuilder();
		sb.append(HEADER + "\n");
		executables.forEach(c->{
			String command = PRE_COMMAND 
					+ " -cp hito-lib/CloneDetector-0.0.1-SNAPSHOT.jar:" 
					+ transformed + " " 
					+ IO_DRIVER + " " 
					+ c;
			sb.append("echo \'Executing " + command + "\'\n");
			sb.append(command + "\n\n");
			
			String normalCommand = "java -cp CloneDetector-0.0.1-SNAPSHOT.jar:" + codebase + " " + c.getClass().getName();
			normalBuilder.append("echo \'Executing " + command + "\'\n");
			normalBuilder.append(normalCommand + "\n\n");
		});
		
		try {
			String normalName = NORMAL_PRE + problemSet + ".sh";
			File normalFile = new File(normalName);
			BufferedWriter nw = new BufferedWriter(new FileWriter(normalFile));
			nw.write(normalBuilder.toString());
			nw.close();
			System.out.println("Generate normal script: " + normalFile.getAbsolutePath());
			
			String scriptsName = SCRIPTS_PRE + problemSet + ".sh";
			File scriptsFile = new File(scriptsName);
			BufferedWriter bw = new BufferedWriter(new FileWriter(scriptsFile));
			bw.write(sb.toString());
			bw.close();
			System.out.println("Generate transformed script: " + scriptsFile.getAbsolutePath());

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

