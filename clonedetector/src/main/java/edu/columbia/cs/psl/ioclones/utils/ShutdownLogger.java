package edu.columbia.cs.psl.ioclones.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShutdownLogger {
	
	private static final String msgPrefix = "ShutdownLogger-";
	
	private static Logger logger = LogManager.getLogger(ShutdownLogger.class);
	
	private static int BUF_SIZE = 500;
	
	private static int FILE_SIZE = 500 * (int)Math.pow(10, 6); 
	
	private static StringBuilder buf = new StringBuilder();
	
	private static final String logPath = "logs/shutdown_" + GlobalInfoRecorder.getThreadIndex() + ".log";
	
	static {
		File shutdownLog = new File(logPath);
		try {
			if (!shutdownLog.exists()) {
				shutdownLog.createNewFile();
			} else {
				if (shutdownLog.length() > FILE_SIZE) {
					Date now = new Date();
										
					Files.move(shutdownLog.toPath(), 
							shutdownLog.toPath().resolveSibling("shutdown_" + now.toString() + ".log"), 
							StandardCopyOption.REPLACE_EXISTING);
					
					shutdownLog = new File(logPath);
					shutdownLog.createNewFile();
					
					/*ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
					ZipEntry entry = new ZipEntry("shutdown.log");
					out.putNextEntry(entry);
					
					InputStream is = new FileInputStream(shutdownLog);
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					StringBuilder sb = new StringBuilder();
					String tmp = "";
					while ((tmp = reader.readLine()) != null) {
						sb.append(tmp + "\n");
					}
					byte[] data = sb.toString().getBytes();
					out.write(data, 0, data.length);
					
					out.closeEntry();
					out.close();*/
				}
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
	}
	
	public static void appendException(Exception toRecord) {
		System.out.println(msgPrefix + GlobalInfoRecorder.getThreadIndex() + ":");
		toRecord.printStackTrace();
		
		File logFile = new File(logPath);
		try {
			FileWriter fw = new FileWriter(logFile, true);
			PrintWriter pw = new PrintWriter(fw, true);
			toRecord.printStackTrace(pw);
		} catch (Exception ex) {
			//No way to log this, since we are in the shut-down process...
			ex.printStackTrace();
		}
	}
	
	public static void appendMessage(String msg) {
		System.out.println(msgPrefix + GlobalInfoRecorder.getThreadIndex() + ": " + msg);
		buf.append(msg);
		
		if (buf.length() > BUF_SIZE) {
			try {
				File shutdownLog = new File(logPath);
				if (!shutdownLog.exists()) {
					shutdownLog.createNewFile();
				}
				
				Files.write(shutdownLog.toPath(), buf.toString().getBytes(), StandardOpenOption.APPEND);
				buf = new StringBuilder();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static void finalFlush() {
		if (buf.length() > 0) {
			try {
				File shutdownLog = new File(logPath);
				if (!shutdownLog.exists()) {
					shutdownLog.createNewFile();
				}
				
				Files.write(shutdownLog.toPath(), buf.toString().getBytes(), StandardOpenOption.APPEND);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
}
