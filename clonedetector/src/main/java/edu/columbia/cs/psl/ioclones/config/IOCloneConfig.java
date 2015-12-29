package edu.columbia.cs.psl.ioclones.config;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class IOCloneConfig {
	
	private static final Logger logger = LogManager.getLogger(IOCloneConfig.class);
	
	private static IOCloneConfig instance;
	
	private boolean debug;
	
	private IOCloneConfig() {
		
	}
	
	public static IOCloneConfig getInstance() {
		if (instance == null) {
			TypeToken<IOCloneConfig> token = new TypeToken<IOCloneConfig>(){};
			File configFile = new File("config/io.config");
			
			if (!configFile.exists()) {
				logger.error("Config file does not exists: " + configFile.getAbsolutePath());
				instance = new IOCloneConfig();
				return instance;
			} else {
				instance = IOUtils.readJson(configFile, token);
			}
		}
		
		return instance;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public boolean isDebug() {
		return this.debug;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Debug: " + this.debug + "\n");
		return sb.toString();
	}
	
}
