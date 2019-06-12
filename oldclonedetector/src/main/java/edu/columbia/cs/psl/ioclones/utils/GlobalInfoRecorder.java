package edu.columbia.cs.psl.ioclones.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

import edu.columbia.cs.psl.ioclones.config.IOCloneConfig;
import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
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
	
	private static final Map<String, ClassInfo> classInfo = new HashMap<String, ClassInfo>();
	
	private static final Set<String> reachLimit = new HashSet<String>();
	
	private static int callLimit = IOCloneConfig.getInstance().getCallLimit();
	
	private static Object recordLock = new Object();
	
	private static Object classLock = new Object();
	
	private static int recordCounter = 0;
	
	private static int changeCounter = 0;
	
	public static int getThreadIndex() {
		return threadIndexer.get();
	}
	
	public static int getMethodIndex() {
		return methodIndexer.getAndIncrement();
	}
	
	public static int getRecordCounter() {
		return recordCounter;
	}
	
	public static void resetChangeCounter() {
		changeCounter = 0;
	}
	
	public static void increChangeCounter() {
		changeCounter++;
	}
	
	public static boolean isChanged() {
		logger.info("Changed method: " + changeCounter);
		return changeCounter != 0;
	}
		
	public static void registerIO(IORecord io) {
		synchronized(recordLock) {
			if (stopRecord(io.getMethodKey())) {
				return ;
			}
			
			recordCounter++;
			io.finalizeIOs();
			if (io.sortedInputs.size() == 0 
					&& io.sortedOutputs.size() == 0) {
				//logger.info("Empty io record: " + io.getMethodKey());
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
			
			if (ioRecords.get(methodKey).size() >= callLimit) {
				reachLimit.add(methodKey);
				logger.info("Reach limit: " + methodKey);
			}
		}
	}
	
	public static boolean stopRecord(String methodKey) {
		synchronized(recordLock) {
			return reachLimit.contains(methodKey);
		}
	}
	
	public static int reportIOs(String baseDir, String zipName) {
		synchronized(recordLock) {
			try {
				File checkBase = new File(baseDir);
				if (!checkBase.exists()) {
					checkBase.mkdirs();
				}
				
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
	
	public static void registerClassInfo(ClassInfo info) {
		synchronized(classLock) {
			classInfo.put(info.getClassName(), info);
		}
	}
	
	public static ClassInfo queryClassInfo(String className) {
		synchronized(classLock) {
			return classInfo.get(className);
		}
	}
	
	public static Map<String, ClassInfo> getClassInfo() {
		return classInfo;
	}
	
	public static void reportClassInfo(boolean db, boolean jvm) {
		synchronized(classLock) {
			logger.info("Total class info: " + classInfo.size());
			
			//Exporting profiling results to sqlite
			if (db) {
				if (jvm) {
					IOUtils.exportJVMIODeps(classInfo.values());
				} else {
					IOUtils.exportMethodIODeps(classInfo.values(), "cb");
				}
			} else {
				classInfo.values().forEach(info->{
					logger.info("Class name: " + info.getClassName());
					
					info.getMethodInfo().forEach((key, method)->{
						logger.info("Method: " + key + " " + method.getWrittenParams());
					});
				});
			}
		}
	}
	
	public static void reportClassProfiles(String baseDir, String profileName) {
		synchronized(classLock) {
			File checkBase = new File(baseDir);
			if (!checkBase.exists()) {
				checkBase.mkdirs();
			}
			
			try {
				String fileName = checkBase.getAbsolutePath() + "/" + profileName + ".zip";
				FileOutputStream zipFile = new FileOutputStream(fileName);
				ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(zipFile));
				
				classInfo.values().forEach(cInfo->{
					try {
						String zipEntryName = cInfo.getClassName() + ".xml";
						String xmlString = IOUtils.fromObj2XML(cInfo);
						
						ZipEntry entry = new ZipEntry(zipEntryName);
						zipStream.putNextEntry(entry);
						
						byte[] data = xmlString.getBytes();
						zipStream.write(data, 0, data.length);
						zipStream.closeEntry();
					} catch (Exception ex) {
						logger.error("Error: ", ex);
					}
				});;
				zipStream.close();
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
			
		}
	}
}
