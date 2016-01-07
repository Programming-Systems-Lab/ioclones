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
import java.nio.channels.FileLock;
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
	
	private static void flushBuf(int threadId) {
		if (buf.length() > BUF_SIZE) {
			try {
				File shutdownLog = new File(logPrefix + threadId + ".log");
				if (!shutdownLog.exists()) {
					shutdownLog.createNewFile();
				}
				FileLock fLock = null;
				FileInputStream shutdownStream = new FileInputStream(shutdownLog);
				while ((fLock = shutdownStream.getChannel().tryLock()) != null);
				
				Files.write(shutdownLog.toPath(), buf.toString().getBytes(), StandardOpenOption.APPEND);
				shutdownStream.close();
				
				buf = new StringBuilder();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static void appendMessage(String msg) {
		int threadId = GlobalInfoRecorder.getThreadIndex();
		System.out.println(msgPrefix + threadId + ": " + msg);
		
		buf.append(msg + "\n");
		flushBuf(threadId);
	}
	
	public static void finalFlush() {
		int threadId = GlobalInfoRecorder.getThreadIndex();
		flushBuf(threadId);
	}
	
}
