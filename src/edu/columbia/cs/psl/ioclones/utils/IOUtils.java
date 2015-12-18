package edu.columbia.cs.psl.ioclones.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thoughtworks.xstream.XStream;

public class IOUtils {
	
	private static Logger logger = LogManager.getLogger(IOUtils.class);
	
	public static Set<String> blackPrefix() {
		Set<String> ret = new HashSet<String>();
		File blackFile = new File("./info/blacklist.txt");
		if (!blackFile.exists()) {
			return ret;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(blackFile));
			String buf = "";
			while ((buf = br.readLine()) != null) {
				ret.add(buf);
			}
			br.close();
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		return ret;
	}
	
	public static Object newObject(Object obj) {
		XStream xstream = new XStream();
		String objString = xstream.toXML(obj);
		//System.out.println("objString: " + objString);
		Object newObj = xstream.fromXML(objString);
		return newObj;
	}
}
