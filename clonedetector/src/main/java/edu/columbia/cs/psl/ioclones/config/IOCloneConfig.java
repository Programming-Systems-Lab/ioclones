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
	
	private boolean dynamic;
	
	//private boolean objDep;
	
	private int callLimit;
	
	//private String xmlAlg;
	
	private int floatScale = 2;
	
	private int depth;
	
	private boolean control;
	
	private boolean writer;
	
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
	
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}
	
	public boolean isDynamic() {
		return this.dynamic;
	}
	
	/*public void setObjDep(boolean objDep) {
		this.objDep = objDep;
	}
	
	public boolean isObjDep() {
		return this.objDep;
	}*/
	
	public void setCallLimit(int callLimit) {
		this.callLimit = callLimit;
	}
	
	public int getCallLimit() {
		return this.callLimit;
	}
	
	/*public void setXmlAlg(String xmlAlg) {
		this.xmlAlg = xmlAlg;
	}
	
	public String getXmlAlg() {
		return this.xmlAlg;
	}*/
	
	public void setFloatScale(int floatScale) {
		this.floatScale = floatScale;
	}
	
	public int getFloatScale() {
		return this.floatScale;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public int getDepth() {
		return this.depth;
	}
	
	public void setControl(boolean control) {
		this.control = control;
	}
	
	public boolean isControl() {
		return this.control;
	}
	
	public void setWriter(boolean writer) {
		this.writer = writer;
	}
	
	public boolean isWriter() {
		return this.writer;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Debug: " + this.debug + "\n");
		sb.append("Dynamic: " + this.dynamic + "\n");
		//sb.append("Record obj dep: " + this.objDep + "\n");
		sb.append("Call limit: " + this.callLimit + "\n");
		//sb.append("XML alg: " + this.xmlAlg + "\n");
		sb.append("Float scale: " + this.floatScale + "\n");
		sb.append("Depth: " + this.depth + "\n");
		sb.append("Control: " + this.control + "\n");
		sb.append("Writer: " + this.writer + "\n");
		return sb.toString();
	}
	
}
