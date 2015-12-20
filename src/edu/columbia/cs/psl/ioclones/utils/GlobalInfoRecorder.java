package edu.columbia.cs.psl.ioclones.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;

public class GlobalInfoRecorder {
	
	private static final Logger logger = LogManager.getLogger(GlobalInfoRecorder.class);
	
	private static final AtomicInteger methodIndexer = new AtomicInteger();
	
	private static final Map<String, List<IORecord>> ioRecords = new HashMap<String, List<IORecord>>();
	
	private static Object recordLock = new Object();
	
	public static int getMethodIndex() {
		return methodIndexer.getAndIncrement();
	}
	
	public static void registerIO(IORecord io) {
		synchronized(recordLock) {
			String methodKey = io.getMethodKey();
			if (ioRecords.containsKey(methodKey)) {
				ioRecords.get(methodKey).add(io);
			} else {
				List<IORecord> rList = new ArrayList<IORecord>();
				rList.add(io);
				ioRecords.put(methodKey, rList);
			}
		}
	}
	
	public static void reportIOs(String baseDir) {
		synchronized(recordLock) {
			ioRecords.forEach((mKey, ios)->{
				logger.info("Methods: " + mKey);
				logger.info("# of records: " + ios.size());
				
				String methodDirString = baseDir + "/" + mKey;
				File methodDir = new File(methodDirString);
				if (!methodDir.exists()) {
					methodDir.mkdir();
				}
				
				ios.forEach(io->{
					logger.info(io);
					
					IOUtils.cleanNonSerializables(io.getInputs());
					IOUtils.cleanNonSerializables(io.getOutputs());
					String filePath = methodDirString + "/" + io.getId() + ".json";
					TypeToken<IORecord> ioToken = new TypeToken<IORecord>(){};
					try {
						IOUtils.writeJson(io, ioToken, filePath);
					} catch (Exception ex) {
						logger.error("Fail to write: " + filePath);
					}				
					
				});
			});
		}
	}
}
