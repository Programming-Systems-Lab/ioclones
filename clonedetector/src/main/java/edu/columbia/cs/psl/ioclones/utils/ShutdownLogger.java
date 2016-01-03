package edu.columbia.cs.psl.ioclones.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class ShutdownLogger {
	
	private static final String msgPrefix = "ShutdownLogger-";
	
	private static int BUF_SIZE = 5000;
	
	//private static int FILE_SIZE = 500 * (int)Math.pow(10, 6); 
	
	private static StringBuilder buf = new StringBuilder();
	
	private static final String logPrefix = "logs/shutdown_";
	
	public static void appendException(Exception toRecord) {
		//int threadId = GlobalInfoRecorder.getThreadIndex();
		//System.out.println(msgPrefix + threadId + ":");
		//toRecord.printStackTrace();
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		toRecord.printStackTrace(pw);
		
		appendMessage(sw.toString());
	}
	
	public static void appendMessage(String msg) {
		int threadId = GlobalInfoRecorder.getThreadIndex();
		System.out.println(msgPrefix + threadId + ": " + msg);
		buf.append(msg + "\n");
		
		if (buf.length() > BUF_SIZE) {
			try {
				File shutdownLog = new File(logPrefix + threadId + ".log");
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
				int threadId = GlobalInfoRecorder.getThreadIndex();
				File shutdownLog = new File(logPrefix + threadId + ".log");
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
