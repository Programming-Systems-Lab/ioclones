package edu.columbia.cs.psl.ioclones.driver;

import java.io.Console;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.sim.AbstractSim;
import edu.columbia.cs.psl.ioclones.sim.AbstractSim.DHSComparator;
import edu.columbia.cs.psl.ioclones.sim.FastAnalyzer;
import edu.columbia.cs.psl.ioclones.sim.NoOrderAnalyzer;
import edu.columbia.cs.psl.ioclones.sim.SimAnalyzer;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class SimAnalysisDriver {
	
	private static final Logger logger = LogManager.getLogger(SimAnalysisDriver.class);
	
	public static final String urlHeader = "jdbc:mysql://";
	
	public static final String urlTail = "?useServerPrepStmts=false&rewriteBatchedStatements=true&autoReconnect=true";
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	private static Options options = new Options();
	
	private static int compCounter = 0;
	
	public static double simThresh = 0.7;
	
	static {
		options.addOption("cb", true, "Codebase");
		options.addOption("io", true, "IO Repo");
		options.addOption("alg", true, "XML algorithm");
		options.addOption("mode", true, "Exhaustive/Comparison mode");
		options.addOption("eName", true, "Export name");
		options.addOption("db", true, "DB URL");
		options.addOption("user", true, "DB Username");
		options.addOption("pw", true, "DB Password");
		options.addOption("lenFilter",false, "Length filter");
		
		options.getOption("cb").setRequired(true);
		options.getOption("io").setRequired(true);
		options.getOption("io").setArgs(Option.UNLIMITED_VALUES);
		options.getOption("alg").setRequired(true);
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
		
		if (!cmd.hasOption("cb") || !cmd.hasOption("io") || !cmd.hasOption("alg")) {
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
		
		String[] iorepos = cmd.getOptionValues("io");
		List<File> iorepoFiles = new ArrayList<File>();
		for (String iorepo: iorepos) {
			File iorepoFile = new File(iorepo);
			if (!iorepoFile.exists()) {
				logger.warn("Invalid io repo: " + iorepoFile.getAbsolutePath());
				continue ;
			}
			
			iorepoFiles.add(iorepoFile);
		}
		
		String alg = cmd.getOptionValue("alg");
		if (alg == null) {
			alg = AbstractSim.DHASH;
		}
		AbstractSim.XML_ALG = alg;
		
		String exportName = cmd.getOptionValue("eName");
		if (exportName == null) {
			exportName = "default";
		}
		
		String mode = cmd.getOptionValue("mode");
		boolean exhaustive = false;
		if (mode == null) {
			exhaustive = true;
		} else {
			if (mode.equals("exhaustive")) {
				exhaustive = true;
			} else {
				exhaustive = false;
			}
		}
		
		logger.info("Codebase: " + codebaseFile.getAbsolutePath());
		StringBuilder sb = new StringBuilder();
		for (File iorepoFile: iorepoFiles) {
			sb.append(iorepoFile.getAbsolutePath() + " ");
		}
		
		boolean lenFilter = false;
		if (cmd.hasOption("lenFilter")) {
			lenFilter = true;
		}
		AbstractSim.lenFilter = lenFilter;
		
		logger.info("IO Repos: " + sb.toString());
		logger.info("XML alg: " + AbstractSim.XML_ALG);
		logger.info("Exhaustive mode: " + exhaustive);
		logger.info("Length filter: " + lenFilter);
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
			
			if (cmd.hasOption("pw")) {
				pw = cmd.getOptionValue("pw");
			} else {
				while (true) {
					Console console = System.console();
					char[] pwArray = console.readPassword("Password: ");
					pw = new String(pwArray);
					
					logger.info("Try DB connection...");
					Connection attempt = IOUtils.getConnection(db, userName, pw);
					if (attempt == null) {
						logger.warn("Connection fails...");
						String retry = console.readLine("Fail to connect to database, try again?");
						boolean retryVal = Boolean.valueOf(retry);
						if (!retryVal) {
							break ;
						}
					} else {
						logger.info("Connection succeeds!");
						break ;
					}
				}
			}
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
		
		Map<String, List<File>> allZips = new HashMap<String, List<File>>();
		for (File iorepoFile: iorepoFiles) {
			//Don't care direct records now...
			List<File> zips = new ArrayList<File>();
			IOUtils.collectIOZips(iorepoFile, zips);
			
			allZips.put(iorepoFile.getAbsolutePath(), zips);
		}
		
		Map<String, List<IORecord>> allRecords = new HashMap<String, List<IORecord>>();
		
		ExecutorService es = 
				Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		Map<String, List<Future<List<IORecord>>>> results = new HashMap<String, List<Future<List<IORecord>>>>();
		
		allZips.forEach((path, zips)->{
			List<Future<List<IORecord>>> records = new ArrayList<Future<List<IORecord>>>();
			zips.forEach(zip->{
				Future<List<IORecord>> future = es.submit(new Callable<List<IORecord>>(){

					@Override
					public List<IORecord> call() throws Exception {
						// TODO Auto-generated method stub
						List<IORecord> ret = new ArrayList<IORecord>();
						IOUtils.unzipIORecords(zip, ret);
						/*ret.forEach(record->{
							Set<Object> cleanInputs = NoOrderAnalyzer.cleanCollection(record.getInputs());
							Set<Object> cleanOutputs = NoOrderAnalyzer.cleanCollection(record.getOutputs());
							
							record.cleanInputs = cleanInputs;
							record.cleanOutputs = cleanOutputs;
						});*/
						return ret;
					}
				});
				records.add(future);
			});
			
			results.put(path, records);
		});		
		es.shutdown();
		while (!es.isTerminated());
				
		/*if (directRecords.size() > 0) {
			allRecords.addAll(directRecords);
		}*/
		
		results.forEach((path, futureList)->{
			try {
				List<IORecord> pathRecords = new ArrayList<IORecord>();
				for (Future<List<IORecord>> f: futureList) {
					List<IORecord> r = f.get();
					pathRecords.addAll(r);
				}
				
				allRecords.put(path, pathRecords);
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		});
		
		int totalRecords = 0;
		for (List<IORecord> records: allRecords.values()) {
			totalRecords += records.size();
		}
		
		logger.info("Reporting repo information");
		allRecords.forEach((path, records)->{
			logger.info("Repo: " + path);
			logger.info("Records: " + records.size());
		});
		
		logger.info("Total IO records: " + totalRecords);
		
		long afterLoading = Runtime.getRuntime().freeMemory();
		long diffMem = afterLoading - beginMem;
		logger.info("Loading memory: " + ((double)diffMem)/Math.pow(10, 6));
		
		Instant start = Instant.now();
		ExecutorService simEs = 
				Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		/*ExecutorService simEs = 
				Executors.newFixedThreadPool(1);*/
		List<IOWorker> workers = new ArrayList<IOWorker>();
		
		if (exhaustive) {
			logger.info("Exhaustive mode");
			
			List<IORecord> putAll = new ArrayList<IORecord>();
			allRecords.values().forEach(records->{
				putAll.addAll(records);
			});
			
			for (int i = 0; i < putAll.size(); i++) {
				IORecord control = putAll.get(i);
				String controlClass = control.getMethodKey().split("-")[0];
				int controlDot = controlClass.lastIndexOf(".");
				String controlPkg = controlClass.substring(0, controlDot);
				
				for (int j = i + 1; j < putAll.size(); j++) {
					IORecord test = putAll.get(j);
					if (control.getMethodKey().equals(test.getMethodKey())) {
						continue ;
					}
					
					String testClass = test.getMethodKey().split("-")[0];
					int testDot = testClass.lastIndexOf(".");
					String testPkg = testClass.substring(0, testDot);
					if (controlPkg.toLowerCase().equals(testPkg.toLowerCase())) {
						continue ;
					}
					
					IOWorker worker = new IOWorker();
					worker.control = control;
					worker.test = test;
					worker.invokeId = compCounter++;
					
					workers.add(worker);
				}
			}
		} else {
			logger.info("Comparison mode");
			
			List<String> paths = new ArrayList<String>(allRecords.keySet());
			logger.info("Repos: " + paths);
			for (int i = 0; i < paths.size(); i++) {
				String path1 = paths.get(i);
				List<IORecord> records1 = allRecords.get(path1);
				
				for (int j = i + 1; j < paths.size(); j++) {
					String path2 = paths.get(j);
					List<IORecord> records2 = allRecords.get(path2);
					
					for (IORecord control: records1) {
						for (IORecord test: records2) {
							IOWorker worker = new IOWorker();
							worker.control = control;
							worker.test = test;
							worker.invokeId = compCounter++;
							workers.add(worker);
						}
					}
				}
			}
		}
		logger.info("Qualified comparisons: " + workers.size());
		
		List<Future<IOSim>> simFutures = new ArrayList<Future<IOSim>>();
		for (IOWorker worker: workers) {
			Future<IOSim> simFuture = simEs.submit(worker);
			
			if (simFuture == null) {
				logger.warn("Null future: " + worker.control.getMethodKey() + " " + worker.control.getId() + " " + worker.test.getMethodKey() + " " + worker.test.getId());
			} else {
				simFutures.add(simFuture);
			}
		}
		
		simEs.shutdown();
		while (!simEs.isTerminated());
		
		Map<String, IOSim> toExport = new HashMap<String, IOSim>();
		int cloneCounter = 0;
		for (Future<IOSim> simF: simFutures) {
			try {
				IOSim simObj = simF.get();
				if (simObj != null) {
					String key = simObj.methodKeys;
					boolean show = false;
					if (toExport.containsKey(key)) {
						IOSim curSim = toExport.get(key);
						if ((simObj.sim - curSim.sim) > SimAnalyzer.TOLERANCE) {
							toExport.put(key, simObj);
							show = true;
						}
					} else {
						toExport.put(key, simObj);
						cloneCounter++;
						show = true;
					}
					
					if (show) {
						logger.info("Comp. key: " + simObj.methodKeys);
						logger.info("Mehtod keys: " + simObj.methodIds);
						logger.info("Best sim.: " + simObj.sim);
						logger.info("Input sim.:" + simObj.inSim);
						logger.info("Output sim: " + simObj.outSim);
					}
				}
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		}
		
		Instant end = Instant.now();
		Duration duration = Duration.between(start, end);
		long elapsed = duration.getSeconds();
		logger.info("Total exeuction time: " + elapsed);
		logger.info("Captured clones: " + cloneCounter);
				
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
		
		public TreeMap<String, Integer> methods = new TreeMap<String, Integer>();
		
		public double sim = 0.0;
		
		public double inSim = 0.0;
		
		public double outSim = 0.0;
		
		public String methodKeys;
		
		public String methodIds;
		
		public IOSim(IORecord control, IORecord test) {
			//Order does not matter
			this.methods.put(control.getMethodKey(), control.getId());
			this.methods.put(test.getMethodKey(), test.getId());
			
			StringBuilder keys = new StringBuilder();
			StringBuilder idx = new StringBuilder();
			for (String key: this.methods.keySet()) {
				Integer val = this.methods.get(key);
				
				keys.append(key + "@");
				idx.append(val + "@");
			}
			
			this.methodKeys = keys.toString().substring(0, keys.length() - 1);
			this.methodIds = idx.toString().substring(0, idx.length() - 1);
		}
	}
	
	public static class IOWorker implements Callable<IOSim> {
		
		private int invokeId;
		
		private IORecord control;
		
		private IORecord test;
		
		public void setInvokeId(int invokeId) {
			this.invokeId = invokeId;
		}

		@Override
		public IOSim call() throws Exception {
			boolean show = (this.invokeId % 5000 == 0);
			if (show) {
				logger.info("Invoking #" + this.invokeId);
			}
			
			//Compute the best sim here, try to filter out unnecessary computation
			List<Object> controlIn = control.sortedInputs;
			List<Object> controlOut = control.sortedOutputs;
			
			List<Object> testIn = test.sortedInputs;
			List<Object> testOut = test.sortedOutputs;
			
			int maxIn, minIn;
			maxIn = minIn = 0;
			if (controlIn == null || testIn == null) {
				maxIn = 0;
				minIn = 0;
			} else {
				maxIn = Math.max(controlIn.size(), testIn.size());
				minIn = Math.min(controlIn.size(), testIn.size());
			}
			
			double bestIn = -1.0;
			if (maxIn == 0) {
				bestIn = 0;
			} else {
				bestIn = ((double)minIn)/maxIn;
			}
			
			int maxOut, minOut;
			maxOut = minOut = 0;
			if (controlOut == null || testOut == null) {
				maxOut = 0;
				minOut = 0;
			} else {
				maxOut = Math.max(controlOut.size(), testOut.size());
				minOut = Math.min(controlOut.size(), testOut.size());
			}
			
			double bestOut = -1;
			if (maxOut == 0) {
				bestOut = 0;
			} else {
				bestOut = ((double)minOut)/maxOut;
			}
			
			double bestSim = AbstractSim.linear.correlation(bestIn, bestOut);
			if (Double.compare(bestSim, simThresh) < 0) {
				return null;
			}
			
			IOSim simObj = new IOSim(this.control, this.test);
			//NoOrderAnalyzer analyzer = new NoOrderAnalyzer();
			FastAnalyzer analyzer = new FastAnalyzer();
			
			double inSim = analyzer.similarity(controlIn, testIn);
			double outSim = analyzer.similarity(controlOut, testOut);
			//double sim = AbstractSim.expo.correlation(inSim, outSim);
			double sim = AbstractSim.linear.correlation(inSim, outSim);
			
			//long afterMem = Runtime.getRuntime().freeMemory();
			//double memDiff = ((double)(afterMem - beforeMem))/Math.pow(10, 6);
			
			/*if (memDiff > 1000) {
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
			}*/
			
			if (show) {
				logger.info("End #" + this.invokeId);
				//logger.info("Used mem: " + memDiff);
			}
			
			int result = Double.compare(sim, simThresh);
			if (result >= 0) {
				simObj.sim = sim;
				simObj.inSim = inSim;
				simObj.outSim = outSim;
				
				return simObj;
			} else {
				return null;
			}
		}
		
	}

}
