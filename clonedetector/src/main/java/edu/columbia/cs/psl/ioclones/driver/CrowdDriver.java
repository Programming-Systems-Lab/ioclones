package edu.columbia.cs.psl.ioclones.driver;

import java.io.File;
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
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CrowdDriver {
	
	private static final Logger logger = LogManager.getLogger(CrowdDriver.class);
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	private static final Class[] mainParameters = new Class[]{String[].class};
	
	public static void main(String[] args) throws Exception {
		String codebase = args[0];
		String problemSet = args[1];
		File codebaseFile = new File(codebase);
		if (!codebaseFile.exists()) {
			logger.error("Invalid codebase: " + codebaseFile.getAbsolutePath());
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
					String fullName = args[1] + "." + userDirName + "." + className.substring(0, className.length() - 6);
					
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
		int coreNum = 1;
		ExecutorService executor = Executors.newFixedThreadPool(coreNum);
		List<Future<Void>> resultList = new ArrayList<Future<Void>>();
		executables.forEach(c->{
			ExecuteWorker newWorker = new ExecuteWorker(c, codebaseFile.getAbsolutePath());
			Future<Void> result = executor.submit(newWorker);
			resultList.add(result);
		});
		executor.shutdown();
		while (!executor.isTerminated());
		
		resultList.forEach(f->{
			try {
				f.get();
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		});
		logger.info("Complete crowd executions");
		
		/*executor.shutdown();
		try {
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdown();
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)){
					logger.error("Cannot shutdwon executor service normally");
				}
			}
		} catch (Exception ex) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}*/
	}
	
	public static class ExecuteWorker implements Callable<Void> {
		
		private static final Logger logger = LogManager.getLogger(ExecuteWorker.class);
		
		private String executeClass;
		
		private String executePath;
		
		public ExecuteWorker(String executeClass, String executePath) {
			this.executeClass = executeClass;
			this.executePath = executePath;
		}

		@Override
		public Void call() throws Exception {
			// TODO Auto-generated method stub
			ProcessBuilder pb = new ProcessBuilder();
			List<String> commands = new ArrayList<String>();
			commands.add("java");
			commands.add("-Xmx6g");
			commands.add("-javaagent:target/CloneDetector-0.0.1-SNAPSHOT.jar");
			commands.add("-noverify");
			commands.add("-cp");
			commands.add("target/CloneDetector-0.0.1-SNAPSHOT.jar:" + this.executePath);
			commands.add("edu.columbia.cs.psl.ioclones.driver.IODriver");
			commands.add(this.executeClass);
			
			logger.info("Executing: " + executeClass);
			logger.info("Commands: " + commands);
			
			Process process = pb.inheritIO().command(commands).start();
			//ProcessInfoHandler info = new ProcessInfoHandler(process.getInputStream());
			//info.start();
			
			//logger.info("Subprocess info: " + info.getOutputMsg());
			
			int exitVal = process.waitFor();
			if (exitVal != 0) {
				logger.error("Abnormal termination of process: " + this.executeClass);
			}
			
			return null;
		}
		
	}
	
	public static class ProcessInfoHandler extends Thread {
		private static final Logger logger = LogManager.getLogger(ProcessInfoHandler.class);
		
		private InputStream inputStream;
		
		private StringBuilder output = new StringBuilder();
		
		public ProcessInfoHandler(InputStream inputStream) {
			this.inputStream = inputStream;
		}
		
		public void run() {
			Scanner br = null;
			
			try {
				br = new Scanner(new InputStreamReader(this.inputStream));
				String line = null;
				while ((line = br.nextLine()) != null) {
					//Not real time msg
					//this.output.append(line);
					//logger.info(line);
					System.out.println(line);
				}
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			} finally {
				br.close();
			}
		}
		
		public String getOutputMsg() {
			return this.output.toString();
		}
	}

}
