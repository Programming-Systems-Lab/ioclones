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
import java.util.HashSet;
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

	public static double simThresh = 0.0;

	public static double inputSimThresh = 0.5;

    enum ComparisonType
    {
        EXHAUSTIVE, INDIVIDUAL, COMPARISON;
    }

	static {
		options.addOption("cb", true, "Codebase");
		options.addOption("io", true, "IO Repo");
		options.addOption("alg", true, "XML algorithm");
		options.addOption("mode", true, "Exhaustive/Individual/Comparison mode");
		options.addOption("eName", true, "Export name");
		options.addOption("db", true, "DB URL");
		options.addOption("user", true, "DB Username");
		options.addOption("pw", true, "DB Password");
		options.addOption("lenFilter",false, "Length filter");
		options.addOption("target", true, "Target method name");

		options.getOption("cb").setRequired(true);
		options.getOption("io").setRequired(true);
		options.getOption("io").setArgs(Option.UNLIMITED_VALUES);
		options.getOption("alg").setRequired(true);
	}

    public static void runSimAnalysisDriver(String codebase,
                                            String[] iorepos,
                                            String alg,
                                            String exportName,
                                            String mode,
                                            String target,
                                            boolean lenFilter,
                                            String db,
                                            String userName,
                                            String pw) throws Exception {
        long beginMem = Runtime.getRuntime().freeMemory();
		File codebaseFile = new File(codebase);

		if (!codebaseFile.exists()) {
			logger.error("Invalid codebase: " + codebaseFile.getAbsolutePath());
			System.exit(-1);
		}

        List<File> iorepoFiles = new ArrayList<File>();
		for (String iorepo: iorepos) {
			File iorepoFile = new File(iorepo);
			if (!iorepoFile.exists()) {
				logger.warn("Invalid io repo: " + iorepoFile.getAbsolutePath());
				continue ;
			}

			iorepoFiles.add(iorepoFile);
		}

        AbstractSim.XML_ALG = alg;

        ComparisonType cMode = ComparisonType.EXHAUSTIVE;
        if (mode.equals("exhaustive")) {
            cMode = ComparisonType.EXHAUSTIVE;
        } else if (mode.equals("individual")) {
            if(target == null) {
                logger.error("-target {method_name} option not found; required for mode individual... exiting");
                System.exit(-1);
            }

            cMode = ComparisonType.INDIVIDUAL;
        } else if (mode.equals("comparison")) {
            cMode = ComparisonType.COMPARISON;
        }
        else {
            logger.error("Mode " + mode + " not recognized... exiting");
            System.exit(-1);
        }
        
		logger.info("Codebase: " + codebaseFile.getAbsolutePath());
		StringBuilder sb = new StringBuilder();
		for (File iorepoFile: iorepoFiles) {
			sb.append(iorepoFile.getAbsolutePath() + " ");
		}

        AbstractSim.lenFilter = lenFilter;

		logger.info("IO Repos: " + sb.toString());
		logger.info("XML alg: " + AbstractSim.XML_ALG);
		logger.info("Comparison mode: " + cMode);
		logger.info("Length filter: " + lenFilter);
		logger.info("Export name: " + exportName);
        logger.info("DB: " + db);
        logger.info("Username: " + userName);
        
        if (pw == null) {
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

        // finished parsing args, running analysis now
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

		List<IOWorker> workers = new ArrayList<IOWorker>();

		if (cMode == ComparisonType.EXHAUSTIVE) {
			logger.info("Exhaustive mode");

			List<IORecord> putAll = new ArrayList<IORecord>();
			allRecords.values().forEach(records->{
				putAll.addAll(records);
			});

			for (int i = 0; i < putAll.size(); i++) {
				IORecord control = putAll.get(i);

				for (int j = i + 1; j < putAll.size(); j++) {
					IORecord test = putAll.get(j);

					if (control.getMethodKey().equals(test.getMethodKey())) {
						continue;
					}

					IOWorker worker = new IOWorker();
					worker.control = control;
					worker.test = test;
					worker.invokeId = compCounter++;

					workers.add(worker);
				}
			}
		}
        else if (cMode == ComparisonType.INDIVIDUAL) {
            logger.info("Individual mode");

			List<IORecord> putAll = new ArrayList<IORecord>();
			allRecords.values().forEach(records->{
				putAll.addAll(records);
			});
            Set<Integer> exploredMethodIndices = new HashSet<Integer>();

			for (int i = 0; i < putAll.size(); i++) {
				IORecord control = putAll.get(i);
                if(!target.equals(control.getMethodKey().split("-")[1])){
                    continue;
                }
                exploredMethodIndices.add(i);
                logger.debug("Target found: " + control.getMethodKey());

				for (int j = 0; j < putAll.size(); j++) {
					IORecord test = putAll.get(j);
					if (control.getMethodKey().equals(test.getMethodKey()) || exploredMethodIndices.contains(j)) {
                        // don't compare to self or already compared methods with same method name
						continue;
					}

					IOWorker worker = new IOWorker();
					worker.control = control;
					worker.test = test;
					worker.invokeId = compCounter++;

					workers.add(worker);
				}
			}
        }
		else {
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

        // compare I/Os and compute similarities
		Map<String, ArrayList<IOSim>> relevantSims = new HashMap<String, ArrayList<IOSim>>();
		int cloneCounter = 0;
		for (Future<IOSim> simF: simFutures) {
			try {
				IOSim simObj = simF.get();
				if (simObj != null) {
					String key = simObj.methodKeys;
					boolean show = false;
					if (relevantSims.containsKey(key)) {
						ArrayList<IOSim> curSims = relevantSims.get(key);

						// check the number of elements already in the arraylist
						if (curSims.size() == 1) { // exactly one element in arraylist
							if (simObj.inSim > curSims.get(0).inSim) {
								if (curSims.get(0).inSim >= inputSimThresh) { // add simObj
									curSims.add(simObj);
								} else { // set simObj to new element of arraylist
									curSims.remove(0);
									curSims.add(simObj);
								}
// 								show = true;
							}
							else if (simObj.inSim >= inputSimThresh) {
								curSims.add(simObj);
// 								show = true;
							}
						} else if (simObj.inSim >= inputSimThresh) {
							curSims.add(simObj);
// 							show = true;
						}
					} else {
						ArrayList<IOSim> newSim = new ArrayList<IOSim>();
						newSim.add(simObj);
						relevantSims.put(key, newSim);
						cloneCounter++;
// 						show = true;
					}

					if (show) {
						logger.info("Comp. key: " + simObj.methodKeys);
						logger.info("Method keys: " + simObj.methodIds);
						logger.info("Best sim.: " + simObj.sim);
						logger.info("Input sim.:" + simObj.inSim);
						logger.info("Output sim: " + simObj.outSim);
					}
				}
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		}

		Map<String, IOSim> toExport = new HashMap<String, IOSim>();
		// work on getting weighted average here
		for (Map.Entry<String, ArrayList<IOSim>> entry : relevantSims.entrySet()) {
			String key = entry.getKey();
			ArrayList<IOSim> sims = entry.getValue();

			double[] weights = new double[sims.size()];
			for (int i = 0; i < sims.size(); i++) {
				weights[i] = Math.exp(sims.get(i).inSim);
			}

			double softmax_denom = 0;
			for (double weight: weights) {
				softmax_denom += weight;
			}

			for (int i = 0; i < sims.size(); i++) {
				weights[i] /= softmax_denom;
// 				System.out.println("weight: " + weights[i]);
			}

			float newSimScore = 0;
			for (int i = 0; i < sims.size(); i++) {
				newSimScore += weights[i] * sims.get(i).sim;
// 				System.out.println("sims: " + sims.get(i).sim);
// 				System.out.println("newSimScore " + newSimScore);
			}

			IOSim newIOSim = new IOSim(sims.get(0), newSimScore);
			toExport.put(key, newIOSim);
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
    }

	public static void main(String args[]) throws Exception {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		String db = null;
		String userName = null;
		String pw = null;
        String target = null;

		if (!cmd.hasOption("cb") || !cmd.hasOption("io") || !cmd.hasOption("alg")) {
			logger.error("No codebase or io repo to process...");
			System.exit(-1);
		}

		String codebase = cmd.getOptionValue("cb");
		String[] iorepos = cmd.getOptionValues("io");

		String alg = cmd.getOptionValue("alg");
		if (alg == null) {
			alg = AbstractSim.DHASH;
		}

		String exportName = cmd.getOptionValue("eName");
		if (exportName == null) {
			exportName = "default";
		}

		String mode = cmd.getOptionValue("mode");
        if (mode == null) {
            mode = "exhaustive";
        }

		boolean lenFilter = false;
		if (cmd.hasOption("lenFilter")) {
			lenFilter = true;
		}

        if (cmd.hasOption("target")) {
            target = cmd.getOptionValue("target");
        }

		if (cmd.hasOption("db")) {
			//db, username and pw should be together
			db = urlHeader + cmd.getOptionValue("db") + urlTail;
			userName = cmd.getOptionValue("user");

			if (cmd.hasOption("pw")) {
				pw = cmd.getOptionValue("pw");
			}
		}

        // execute here
        runSimAnalysisDriver(codebase, iorepos, alg, exportName, mode, target, lenFilter, db, userName, pw);
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
			this.methods.put(test.getMethodKey(), test.getId());
			this.methods.put(control.getMethodKey(), control.getId());

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

		public IOSim(IOSim copy, double newSim) { // copy constructor, w/ new sim
			for (String key: copy.methods.keySet()) {
				this.methods.put(key, copy.methods.get(key));
			}
			this.sim = newSim;
			this.inSim = copy.inSim;
			this.outSim = copy.outSim;
			this.methodKeys = copy.methodKeys;
			this.methodIds = copy.methodIds;
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

			/*int maxIn, minIn;
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
			}*/

			IOSim simObj = new IOSim(this.test, this.control);
			//NoOrderAnalyzer analyzer = new NoOrderAnalyzer();
			FastAnalyzer analyzer = new FastAnalyzer();

			double inSim = analyzer.similarity(controlIn, testIn);
			double outSim = analyzer.similarity(controlOut, testOut);
			if (inSim == 0.0 && outSim == 0.0) {
				return null;
			}

			//double sim = AbstractSim.linear.correlation(inSim, outSim);
			double sim = AbstractSim.expo.correlation(inSim, outSim);

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
