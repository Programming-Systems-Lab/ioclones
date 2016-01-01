package edu.columbia.cs.psl.ioclones.driver;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.TreeSet;

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
		Set<String> executableClasses = new TreeSet<String>();
		for (File usrDir: psDir.listFiles()) {
			if (usrDir.isDirectory() && !usrDir.getName().startsWith(".")) {
				String userDirName = usrDir.getName();
				
				for (File classFile: usrDir.listFiles()) {
					String className = classFile.getName();
					String fullName = args[1] + "." + userDirName + "." + className.substring(0, className.length() - 6);
					
					Class checkClass = Class.forName(fullName);
					try {
						Method mainMethod = checkClass.getMethod("main", mainParameters);
						
						if (mainMethod == null) {
							logger.warn("Cannot detect main method: " + checkClass.getName());
						} else {
							executableClasses.add(checkClass.getName());
						}
					} catch (Exception ex) {
						logger.error("Error: ", ex);
					}
				}
			}
		}
		
		executableClasses.forEach(c->System.out.println(c));
	}

}
