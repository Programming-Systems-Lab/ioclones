package edu.columbia.cs.psl.ioclones.analysis;

import java.io.Console;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.sim.AbstractSim;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class InvokeCollector {
	
	public static final Options options = new Options();
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	static {
		options.addOption("cb", true, "codebase");
		options.addOption("io", true, "io repo");
	}
	
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		String codebase = cmd.getOptionValue("cb");
		File codebaseFile = new File(codebase);
		//File codebaseFile = new File("/Users/mikefhsu/Desktop/code_repos/io_play/bin");
		if (!codebaseFile.exists()) {
			System.err.println("Invalid codebase: " + codebaseFile.getAbsolutePath());
			System.exit(-1);
		}
		
		String[] iorepos = cmd.getOptionValues("io");
		List<File> iorepoFiles = new ArrayList<File>();
		for (String iorepo: iorepos) {
			File iorepoFile = new File(iorepo);
			if (!iorepoFile.exists()) {
				System.out.println("Invalid io repo: " + iorepoFile.getAbsolutePath());
				continue ;
			}
			
			iorepoFiles.add(iorepoFile);
		}
		
		System.out.println("Codebase: " + codebaseFile.getAbsolutePath());
		StringBuilder sb = new StringBuilder();
		for (File iorepoFile: iorepoFiles) {
			sb.append(iorepoFile.getAbsolutePath() + " ");
		}
		
		System.out.println("IO Repos: " + sb.toString());
		
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
			ex.printStackTrace();
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
				ex.printStackTrace();
			}
		});
		
		int totalRecords = 0;
		for (List<IORecord> records: allRecords.values()) {
			totalRecords += records.size();
		}
		
		System.out.println("Reporting repo information");
		allRecords.forEach((path, records)->{
			System.out.println("Repo: " + path);
			System.out.println("Records: " + records.size());
		});
		
		System.out.println("Total IO records: " + totalRecords);
	}

}
