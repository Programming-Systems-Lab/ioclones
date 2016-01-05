package edu.columbia.cs.psl.ioclones.driver;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.sim.NoOrderAnalyzer;
import edu.columbia.cs.psl.ioclones.sim.SimAnalyzer;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class SimAnalysisDriver {
	
	private static final Logger logger = LogManager.getLogger(SimAnalysisDriver.class);
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	public static void main(String args[]) throws Exception {
		String codebase = args[0];
		File codebaseFile = new File(codebase);
		//File codebaseFile = new File("/Users/mikefhsu/Desktop/code_repos/io_play/bin");
		if (!codebaseFile.exists()) {
			logger.error("Invalid codebase: " + codebaseFile.getAbsolutePath());
			System.exit(-1);
		}
		
		String iorepo = args[1];
		File iorepoFile = new File(iorepo);
		if (!iorepoFile.exists()) {
			logger.error("Invalid io repo: " + iorepoFile.getAbsolutePath());
			System.exit(-1);
		}
		
		try {
			URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
			Class sysClass = URLClassLoader.class;
			Method method = sysClass.getDeclaredMethod("addURL", parameters);
			method.setAccessible(true);
			method.invoke(sysloader, new Object[]{codebaseFile.toURI().toURL()});
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
			Future<List<IORecord>> future = es.submit(new Callable<List<IORecord>>(){

				@Override
				public List<IORecord> call() throws Exception {
					// TODO Auto-generated method stub
					List<IORecord> ret = new ArrayList<IORecord>();
					IOUtils.unzipIORecords(zip, ret);
					return ret;
				}
			});
			results.add(future);
		});
		
		es.shutdown();
		while (es.isTerminated());
		
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
		
		ExecutorService simEs = 
				Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<IOSim>> simFutures = new ArrayList<Future<IOSim>>();
		for (int i = 0; i < allRecords.size(); i++) {
			for (int j = i + 1; j < allRecords.size(); j++) {
				IORecord control = allRecords.get(i);
				IORecord test = allRecords.get(j);
				
				if (control.getMethodKey().equals(test.getMethodKey())) {
					continue ;
				}
				
				Future<IOSim> simFuture = simEs.submit(new Callable<IOSim>() {

					@Override
					public IOSim call() throws Exception {
						// TODO Auto-generated method stub
						IOSim simObj = new IOSim(control.getMethodKey(), test.getMethodKey());
						SimAnalyzer analyzer = new NoOrderAnalyzer();
						double inSim = analyzer.similarity(control.getInputs(), test.getInputs());
						double outSim = analyzer.similarity(control.getOutputs(), test.getOutputs());
						double sim = inSim * outSim;
						
						if (sim > simObj.bestSim) {
							simObj.bestSim = sim;
						}
						
						return simObj;
					}
				});
				
				simFutures.add(simFuture);
			}
		}
		
		simEs.shutdown();
		while (simEs.isTerminated());
		
		simFutures.forEach(simF->{
			try {
				IOSim simObj = simF.get();
				logger.info("Comp. key: " + simObj.key);
				logger.info("Best sim.: " + simObj.bestSim);
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		});
		
		
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
		
		public Set<String> key = new HashSet<String>();
		
		public double bestSim = 0.0;
		
		public IOSim(String control, String test) {
			//Order does not matter
			this.key.add(control);
			this.key.add(test);
		}
	}

}
