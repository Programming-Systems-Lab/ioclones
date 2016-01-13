package edu.columbia.cs.psl.ioclones.driver;

import com.sun.management.HotSpotDiagnosticMXBean;

import java.io.Console;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MBeanServer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.sim.AbstractSim;
import edu.columbia.cs.psl.ioclones.sim.NoOrderAnalyzer;
import edu.columbia.cs.psl.ioclones.sim.SimAnalyzer;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class SimAnalysisDriver {
	
	private static final Logger logger = LogManager.getLogger(SimAnalysisDriver.class);
	
	private static final String urlHeader = "jdbc:mysql://";
	
	private static final String urlTail = "?useServerPrepStmts=false&rewriteBatchedStatements=true&autoReconnect=true";
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	private static Options options = new Options();
	
	private static int compCounter = 0;
	
	public static double simThresh = 0.7;
	
	static {
		options.addOption("cb", true, "Codebase");
		options.addOption("io", true, "IO Repo");
		options.addOption("eName", true, "Export name");
		options.addOption("db", true, "DB URL");
		options.addOption("user", true, "DB Username");
		/*Option codebase = Option.builder().argName("cb").desc("Codebase").build();
		Option io = Option.builder().argName("io").desc("IO Repo").build();
		Option eName = Option.builder().argName("eName").desc("Export name").build();
		Option db = Option.builder().argName("db").desc("DB URL").build();
		Option user = Option.builder().argName("user").desc("DB User").build();*/
	}
	
	public static void main(String args[]) throws Exception {
		long beginMem = Runtime.getRuntime().freeMemory();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		if (!cmd.hasOption("cb") || !cmd.hasOption("io")) {
			logger.error("No codebase or io repo to process...");
			System.exit(-1);
		}
		
		String codebase = cmd.getOptionValue("cb");
		File codebaseFile = new File(codebase);
		//File codebaseFile = new File("/Users/mikefhsu/Desktop/code_repos/io_play/bin");
		if (!codebaseFile.exists()) {
			logger.error("Invalid codebase: " + codebaseFile.getAbsolutePath());
			System.exit(-1);
		}
		
		String iorepo = cmd.getOptionValue("io");
		File iorepoFile = new File(iorepo);
		if (!iorepoFile.exists()) {
			logger.error("Invalid io repo: " + iorepoFile.getAbsolutePath());
			System.exit(-1);
		}
		
		String exportName = cmd.getOptionValue("eName");
		if (exportName == null) {
			exportName = "default";
		}
		
		logger.info("Codebase: " + codebaseFile.getAbsolutePath());
		logger.info("IO Repo: " + iorepoFile.getAbsolutePath());
		logger.info("Export name: " + exportName);
		
		String db = null;
		String userName = null;
		String pw = null;
		
		if (cmd.hasOption("db")) {
			//db, username and pw should be together
			db = urlHeader + cmd.getOptionValue("db") + urlTail;
			userName = cmd.getOptionValue("user");
			
			logger.info("DB: " + db);
			logger.info("Username: " + userName);
			
			Console console = System.console();
			char[] pwArray = console.readPassword("Password: ");
			pw = new String(pwArray);
		}
		
		String[] myPaths = System.getProperty("java.class.path").split(":");
		String[] javaPaths = System.getProperty("sun.boot.class.path").split(":");
		
		List<URL> toAdd = new ArrayList<URL>();
		
		for (String m: myPaths) {
			File mf = new File(m);
			toAdd.add(mf.toURI().toURL());
		}
		
		for (String j: javaPaths) {
			File jf = new File(j);
			toAdd.add(jf.toURI().toURL());
		}
		toAdd.add(codebaseFile.toURI().toURL());
		
		try {
			URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			Class sysClass = URLClassLoader.class;
			Method method = sysClass.getDeclaredMethod("addURL", parameters);
			method.setAccessible(true);
			//method.invoke(sysloader, new Object[]{codebaseFile.toURI().toURL()});
			for (URL url: toAdd) {
				method.invoke(sysloader, new Object[]{url});
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		List<IORecord> directRecords = new ArrayList<IORecord>();
		List<File> zips = new ArrayList<File>();
		IOUtils.collectIORecords(iorepoFile, directRecords, zips);
		List<IORecord> allRecords = new ArrayList<IORecord>();
		
		ExecutorService es = 
				Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<List<IORecord>>> results = new ArrayList<Future<List<IORecord>>>();
		
		zips.forEach(zip->{
			logger.info("Zip file: " + zip);
			Future<List<IORecord>> future = es.submit(new Callable<List<IORecord>>(){

				@Override
				public List<IORecord> call() throws Exception {
					// TODO Auto-generated method stub
					List<IORecord> ret = new ArrayList<IORecord>();
					IOUtils.unzipIORecords(zip, ret);
					ret.forEach(record->{
						Set<Object> cleanInputs = NoOrderAnalyzer.cleanCollection(record.getInputs());
						Set<Object> cleanOutputs = NoOrderAnalyzer.cleanCollection(record.getOutputs());
						
						record.cleanInputs = cleanInputs;
						record.cleanOutputs = cleanOutputs;
					});
					return ret;
				}
			});
			results.add(future);
		});
		
		es.shutdown();
		while (!es.isTerminated());
		
		if (directRecords.size() > 0) {
			allRecords.addAll(directRecords);
		}
		
		results.forEach(f->{
			try {
				List<IORecord> r = f.get();
				allRecords.addAll(r);
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		});
		
		logger.info("Total IO records: " + allRecords.size());
		long afterLoading = Runtime.getRuntime().freeMemory();
		long diffMem = afterLoading - beginMem;
		logger.info("Loading memory: " + ((double)diffMem)/Math.pow(10, 6));
		
		Instant start = Instant.now();
		ExecutorService simEs = 
				Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		/*ExecutorService simEs = 
				Executors.newFixedThreadPool(1);*/
		List<Future<IOSim>> simFutures = new ArrayList<Future<IOSim>>();
		for (int i = 0; i < allRecords.size(); i++) {
			IORecord control = allRecords.get(i);
			for (int j = i + 1; j < allRecords.size(); j++) {
				IORecord test = allRecords.get(j);
				
				if (control.getMethodKey().equals(test.getMethodKey())) {
					continue ;
				}
				
				IOWorker worker = new IOWorker();
				worker.control = control;
				worker.test = test;
				worker.invokeId = compCounter++;
				Future<IOSim> simFuture = simEs.submit(worker);
				
				simFutures.add(simFuture);
			}
		}
		
		logger.info("Qualified comparisons: " + simFutures.size());
		
		simEs.shutdown();
		while (!simEs.isTerminated());
		
		Map<String, IOSim> toExport = new HashMap<String, IOSim>();
		simFutures.forEach(simF->{
			try {
				IOSim simObj = simF.get();
				if (simObj != null) {
					String key = simObj.key.toString();
					if (toExport.containsKey(key)) {
						IOSim curSim = toExport.get(key);
						if ((simObj.sim - curSim.sim) > SimAnalyzer.TOLERANCE) {
							toExport.put(key, simObj);
						}
					} else {
						toExport.put(key, simObj);
					}
					logger.info("Comp. key: " + simObj.key);
					logger.info("Mehtod keys: " + Arrays.toString(simObj.methodIds));
					logger.info("Best sim.: " + simObj.sim);
					logger.info("Input sim.:" + simObj.inSim);
					logger.info("Output sim: " + simObj.outSim);
				}
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		});
		
		Instant end = Instant.now();
		Duration duration = Duration.between(start, end);
		long elapsed = duration.getSeconds();
		logger.info("Total exeuction time: " + elapsed);
				
		IOUtils.exportIOSimilarity(toExport.values(), 
				db, 
				userName, 
				pw, 
				exportName, 
				allRecords.size(), 
				compCounter, 
				elapsed);
		/*File f1 = new File("/Users/mikefhsu/Desktop/code_repos/R5P1Y11.aditsu/R5P1Y11.aditsu.Cakes-get-389.xml");
		File f2 = new File("/Users/mikefhsu/Desktop/code_repos/R5P1Y11.aditsu/R5P1Y11.aditsu.Cakes-get-390.xml");
		
		IORecord io1 = (IORecord)IOUtils.fromXML2Obj(f1);
		IORecord io2 = (IORecord)IOUtils.fromXML2Obj(f2);
		System.out.println("IO1: " + io1.getInputs().size() + " " + io1.getOutputs().size());
		System.out.println("IO2: " + io2.getInputs().size() + " " + io2.getOutputs().size());
		
		SimAnalyzer analyzer = new NoOrderAnalyzer();
		double inSim = analyzer.similarity(io1.getInputs(), io2.getInputs());
		double outSim = analyzer.similarity(io1.getOutputs(), io2.getOutputs());
		System.out.println("In sim: " + inSim);
		System.out.println("Out sim: " + outSim);*/
	}
	
	public static class IOSim {
		
		public List<String> key = new ArrayList<String>();
		
		public int[] methodIds = new int[2];
		
		public double sim = 0.0;
		
		public double inSim = 0.0;
		
		public double outSim = 0.0;
		
		public IOSim(String control, String test) {
			//Order does not matter
			this.key.add(control);
			this.key.add(test);
			Collections.sort(key);
		}
		
		public void setMethodId(IORecord control, IORecord test) {
			int controlIdx = this.key.indexOf(control.getMethodKey());
			int testIdx = this.key.indexOf(test.getMethodKey());
			this.methodIds[controlIdx] = control.getId();
			this.methodIds[testIdx] = test.getId();
		}
	}
	
	public static class IOWorker implements Callable<IOSim> {
		
		public int invokeId;
		
		public IORecord control;
		
		public IORecord test;

		@Override
		public IOSim call() throws Exception {
			boolean show = (this.invokeId % 5000 == 0);
			if (show) {
				logger.info("Invoking #" + this.invokeId);
			}
			
			long beforeMem = Runtime.getRuntime().freeMemory();
			
			IOSim simObj = new IOSim(this.control.getMethodKey(), this.test.getMethodKey());
			SimAnalyzer analyzer = new NoOrderAnalyzer();
			//System.out.println("Control input: " + control.getInputs());
			//System.out.println("Test input: " + test.getInputs());
			double inSim = analyzer.similarity(this.control.cleanInputs, this.test.cleanInputs);
			long afterIn = Runtime.getRuntime().freeMemory();
			double outSim = analyzer.similarity(this.control.cleanOutputs, this.test.cleanOutputs);
			long afterOut = Runtime.getRuntime().freeMemory();
			double sim = AbstractSim.expo.correlation(inSim, outSim);
			
			long afterMem = Runtime.getRuntime().freeMemory();
			double memDiff = ((double)(afterMem - beforeMem))/Math.pow(10, 6);
			
			if (memDiff > 1000) {
				logger.info("Large comp: " + this.control.getMethodKey() + " " + this.control.getId() + " " + this.test.getMethodKey() + " " + this.test.getId());
				logger.info("Mem diff: " + memDiff);
				logger.info("After in: " + ((double)(afterIn - beforeMem))/Math.pow(10, 6));
				//logger.info("Control in: " + this.control.cleanInputs);
				//logger.info("Test in: " + this.test.cleanInputs);
				logger.info("After out: " + ((double)(afterOut - afterIn))/Math.pow(10, 6));
				//logger.info("Control out: " + this.control.cleanInputs);
				//logger.info("Test out: " + this.test.cleanOutputs);
				this.control = null;
				this.test = null;
				System.gc();
				
				show = true;
				
				/*MBeanServer server = ManagementFactory.getPlatformMBeanServer();
				HotSpotDiagnosticMXBean diagBean = 
						ManagementFactory.newPlatformMXBeanProxy(server, 
								"com.sun.management:type=HotSpotDiagnostic", 
								HotSpotDiagnosticMXBean.class);
				File heapFile = new File("heap-test.hprof");
				logger.info("Dumping information: " + heapFile.getAbsolutePath());
				diagBean.dumpHeap(heapFile.getAbsolutePath(), true);
				System.exit(-1);*/
			}
			
			if (show) {
				logger.info("End #" + this.invokeId);
				logger.info("Used mem: " + memDiff);
			}
			
			if (sim - simThresh > SimAnalyzer.TOLERANCE) {
				simObj.sim = sim;
				simObj.inSim = inSim;
				simObj.outSim = outSim;
				simObj.setMethodId(control, test);
				
				return simObj;
			} else {
				return null;
			}
		}
		
	}

}
