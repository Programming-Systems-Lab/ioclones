package edu.columbia.cs.psl.ioclones.driver;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.sim.NoOrderAnalyzer;
import edu.columbia.cs.psl.ioclones.sim.SimAnalyzer;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;
import edu.columbia.cs.psl.ioclones.utils.XMLDiffer;

public class SimAnalysisDriver {
	
	private static final Logger logger = LogManager.getLogger(SimAnalysisDriver.class);
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	public static void main(String args[]) throws Exception {
		File codebaseFile = new File("/Users/mikefhsu/Desktop/code_repos/io_play/bin");
		if (!codebaseFile.exists()) {
			logger.error("Invalid codebase: " + codebaseFile.getAbsolutePath());
			System.exit(-1);
		}
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Class sysClass = URLClassLoader.class;
		Method method = sysClass.getDeclaredMethod("addURL", parameters);
		method.setAccessible(true);
		method.invoke(sysloader, new Object[]{codebaseFile.toURI().toURL()});
		
		File f1 = new File("/Users/mikefhsu/Desktop/code_repos/R5P1Y11.aditsu/R5P1Y11.aditsu.Cakes-get-389.xml");
		File f2 = new File("/Users/mikefhsu/Desktop/code_repos/R5P1Y11.aditsu/R5P1Y11.aditsu.Cakes-get-390.xml");
		//xmlDiff(f1, f2);
		/*Object obj1 = IOUtils.fromXML2Obj(f1);
		Object obj2 = IOUtils.fromXML2Obj(f2);
		
		String xml1 = IOUtils.fromObj2XML(obj1);
		String xml2 = IOUtils.fromObj2XML(obj2);
		
		XMLDiffer.xmlDiff(xml1, xml2);*/
		
		IORecord io1 = (IORecord)IOUtils.fromXML2Obj(f1);
		IORecord io2 = (IORecord)IOUtils.fromXML2Obj(f2);
		System.out.println("IO1: " + io1.getInputs().size() + " " + io1.getOutputs().size());
		System.out.println("IO2: " + io2.getInputs().size() + " " + io2.getOutputs().size());
		
		SimAnalyzer analyzer = new NoOrderAnalyzer();
		double inSim = analyzer.similarity(io1.getInputs(), io2.getInputs());
		double outSim = analyzer.similarity(io1.getOutputs(), io2.getOutputs());
		System.out.println("In sim: " + inSim);
		System.out.println("Out sim: " + outSim);
	}

}
