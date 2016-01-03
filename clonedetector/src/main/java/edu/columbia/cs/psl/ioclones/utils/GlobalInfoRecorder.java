package edu.columbia.cs.psl.ioclones.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;

public class GlobalInfoRecorder {
	
	private static final Logger logger = LogManager.getLogger(GlobalInfoRecorder.class);
	
	private static final AtomicInteger methodIndexer = new AtomicInteger();
	
	private static final AtomicInteger counter = new AtomicInteger();
	
	private static final ThreadLocal<Integer> threadIndexer = new ThreadLocal<Integer>() {
		public Integer initialValue() {
			return counter.getAndIncrement();
		}
	};
	
	private static final Map<String, HashSet<IORecord>> ioRecords = new HashMap<String, HashSet<IORecord>>();
	
	private static Object recordLock = new Object();
	
	private static int recordCounter = 0;
	
	public static int getThreadIndex() {
		return threadIndexer.get();
	}
	
	public static int getMethodIndex() {
		return methodIndexer.getAndIncrement();
	}
	
	public static int getRecordCounter() {
		return recordCounter;
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
	
	public static int reportIOs(String baseDir, String zipName) {
		synchronized(recordLock) {
			try {
				String zipFilePath = baseDir + "/" + zipName + ".zip";
				FileOutputStream zipFile = new FileOutputStream(zipFilePath);
				ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(zipFile));
				
				int exactIOs = 0;
				for (String mKey: ioRecords.keySet()) {
					HashSet<IORecord> ios = ioRecords.get(mKey);
					
					ShutdownLogger.appendMessage("Methods: " + mKey);
					ShutdownLogger.appendMessage("# of records: " + ios.size());
					
					exactIOs += ios.size();
					
					String[] parsed = mKey.split("-");
					String className = parsed[0];
					String pkgName = ClassInfoUtils.parsePkgName(className);
					
					String methodName = parsed[1];
					String desc = parsed[2];
					
					/*String classDirString = baseDir + "/" + className;
					File classDir = new File(classDirString);
					if (!classDir.exists()) {
						classDir.mkdir();
					}*/
					
					ios.forEach(io->{
						//logger.info(io);
						IOUtils.cleanNonSerializables(io.getInputs());
						IOUtils.cleanNonSerializables(io.getOutputs());
						
						//String filePath = classDir.getAbsolutePath() + "/" + methodName + "-" + io.getId() + ".xml";
						StringBuilder sb = new StringBuilder();
						sb.append(pkgName + "/");
						sb.append(className + ClassInfoUtils.DELIM);
						sb.append(methodName + ClassInfoUtils.DELIM);
						sb.append(io.getId() + ".xml");
						
						try {
							ZipEntry entry = new ZipEntry(sb.toString());
							zipStream.putNextEntry(entry);
							
							//File file = new File(filePath);
							String xmlString = IOUtils.fromObj2XML(io);
							byte[] data = xmlString.getBytes();
							zipStream.write(data, 0, data.length);
							zipStream.closeEntry();
							
							//IOUtils.writeFile(xmlString, file);
						} catch (Exception ex) {
							ShutdownLogger.appendException(ex);
							ex.printStackTrace();
						}
					});
				};
				zipStream.close();
				
				return exactIOs;
			} catch (Exception ex) {
				ShutdownLogger.appendException(ex);
			}
			return 0;
		}
	}
}
