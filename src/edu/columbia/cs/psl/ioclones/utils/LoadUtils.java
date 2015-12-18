package edu.columbia.cs.psl.ioclones.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadUtils {
	
	private static Logger logger = LogManager.getLogger(LoadUtils.class);
	
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

}
