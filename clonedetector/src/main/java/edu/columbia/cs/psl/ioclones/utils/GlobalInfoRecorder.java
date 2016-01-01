package edu.columbia.cs.psl.ioclones.utils;

import java.util.HashMap;
import java.util.HashSet;
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
	
	private static final Map<String, HashSet<IORecord>> ioRecords = new HashMap<String, HashSet<IORecord>>();
	
	private static Object recordLock = new Object();
	
	private static int recordCounter = 0;
	
	public static int getMethodIndex() {
		return methodIndexer.getAndIncrement();
	}
	
	public static void registerIO(IORecord io) {
		synchronized(recordLock) {
			recordCounter++;
			if (io.getInputs().size() == 0 
					&& io.getOutputs().size() == 0) {
				logger.info("Empty io record: " + io.getMethodKey());
				return ;
			}
			
			String methodKey = io.getMethodKey();
			if (ioRecords.containsKey(methodKey)) {
				ioRecords.get(methodKey).add(io);
			} else {
				HashSet<IORecord> rSet = new HashSet<IORecord>();
				rSet.add(io);
				ioRecords.put(methodKey, rSet);
			}
		}
	}
	
	public static void reportIOs(String baseDir) {
		synchronized(recordLock) {
			ioRecords.forEach((mKey, ios)->{
				System.out.println("Methods: " + mKey);
				System.out.println("# of records: " + ios.size());
				
				String[] parsed = mKey.split("-");
				String className = parsed[0];
				String methodName = parsed[1];
				String desc = parsed[2];
				
				String classDirString = baseDir + "/" + className;
				File classDir = new File(classDirString);
				if (!classDir.exists()) {
					classDir.mkdir();
				}
				
				ios.forEach(io->{
					//logger.info(io);
					
					IOUtils.cleanNonSerializables(io.getInputs());
					IOUtils.cleanNonSerializables(io.getOutputs());
					/*String filePath = methodDirString + "/" + io.getId() + ".json";
					TypeToken<IORecord> ioToken = new TypeToken<IORecord>(){};
					try {
						IOUtils.writeJson(io, ioToken, filePath);
					} catch (Exception ex) {
						logger.error("Fail to write: " + filePath);
					}*/
					
					String filePath = classDir.getAbsolutePath() + "/" + methodName + "-" + io.getId() + ".xml";
					File file = new File(filePath);
					String xmlString = IOUtils.fromObj2XML(io);
					IOUtils.writeFile(xmlString, file);
				});
			});
			System.out.println("Total IO records: " + recordCounter);
		}
	}
}
