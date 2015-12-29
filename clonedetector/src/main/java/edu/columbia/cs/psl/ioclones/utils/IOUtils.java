package edu.columbia.cs.psl.ioclones.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.thoughtworks.xstream.XStream;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;

public class IOUtils {
	
	private static Logger logger = LogManager.getLogger(IOUtils.class);
	
	private static final Set<Class> blackObjects = new HashSet<Class>();
	
	private static Object boLock = new Object();
		
	public static <T> void writeJson(T obj, TypeToken typeToken, String fileName) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		Gson gson = gb.enableComplexMapKeySerialization().create();
		String toWrite = gson.toJson(obj, typeToken.getType());
		
		try {
			File f = new File(fileName);
			if (!f.exists()) {
				f.createNewFile();
			}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(toWrite);
			bw.close();
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
	
	public static <T> T readJson(File f, TypeToken typeToken) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		Gson gson = gb.create();
		
		try {
			 JsonReader jr = new JsonReader(new FileReader(f));
			 T ret = gson.fromJson(jr, typeToken.getType());
			 jr.close();
			 return ret;
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		return null;
	}
		
	public static Set<String> blackPrefix() {
		Set<String> ret = new HashSet<String>();
		
		try {
			ClassLoader clazzLoader = IOUtils.class.getClassLoader();
			InputStream blackStream = clazzLoader.getResourceAsStream("blacklist.txt");
			if (blackStream == null) {
				logger.warn("Find no black list file");
				return ret;
			}
			
			InputStreamReader isr = new InputStreamReader(blackStream);
			BufferedReader br = new BufferedReader(isr);
			String buf = "";
			while ((buf = br.readLine()) != null) {
				ret.add(buf);
			}
			isr.close();
			br.close();
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		return ret;
	}
	
	public static Object newObject(Object obj) {
		try {
			XStream xstream = new XStream();
			String objString = xstream.toXML(obj);
			if (obj.getClass().isArray()) {
				/*StringBuilder sb = new StringBuilder();
				for (int i = 0; i < Array.getLength(obj); i++) {
					sb.append(Array.get(obj, i) + " ");
				}
				System.out.println(sb.toString());*/
				System.out.println(Array.getLength(obj));
			}
			Object newObj = xstream.fromXML(objString);
			return newObj;
		} catch (Exception ex) {
			logger.error("Fail to create new obj: ", ex);
		}
		
		return null;
	}
	
	public static String fromObj2XML(Object obj) {
		try {
			XStream xstream = new XStream();
			String objString = xstream.toXML(obj);
			return objString;
		} catch (Exception ex) {
			logger.error("Fail to convert obj to xml: " + obj.getClass());
		}
		
		return null;
	}
	
	public static Object fromXML2Obj(File f) {
		try {
			XStream xstream = new XStream();
			Object obj = xstream.fromXML(f);
			return obj;
		} catch (Exception ex) {
			logger.error("Fail to convert file to obj: " + f.getAbsolutePath());
			logger.error("Trace", ex);
		}
		
		return null;
	}
	
	public static Object fromXML2Obj(String xmlString) {
		try {
			XStream xstream = new XStream();
			Object obj = xstream.fromXML(xmlString);
			return obj;
		} catch (Exception ex) {
			logger.error("Fail to convert xml string to obj: " + xmlString);
		}
		return null;
	}
	
	public static void writeFile(String contents, File f) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(contents);
			bw.close();
		} catch (Exception ex) {
			logger.error("Fail to write file: ", ex);
		}
	}
	
	public static void cleanNonSerializables(Collection c) {
		//Need a better way to filter objs, now follow xstream's documents
		Iterator it = c.iterator();
		while (it.hasNext()) {
			Object o = it.next();
			
			Class objClass = o.getClass();
			if (shouldRemove(o)) {
				it.remove();
				logger.info("Remove obj: " + o);
			}
			
			/*if (blackObjects.contains(o.getClass())) {
			it.remove();
		} else if (!attemptSerialization(o)) {
			blackObjects.add(o.getClass());
			logger.info("Remove non-serializable obj: " + o);
			
			it.remove();
		}*/
		}
	}
	
	private static boolean shouldRemove(Object o) {
		if (o == null) {
			return false;
		}
		
		Class objClass = o.getClass();
		if (OutputStream.class.isAssignableFrom(objClass) 
				|| InputStream.class.isAssignableFrom(objClass) 
				|| ClassLoader.class.isAssignableFrom(objClass) 
				|| Socket.class.isAssignableFrom(objClass)) {
			return true;
		}
		
		if (o.getClass().isArray()) {
			int length = Array.getLength(o);
			
			//Assume all objects in array are the same
			Object first = null;
			int counter = 0;
			while (counter < length) {
				first = Array.get(o, counter++);
				if (first != null) {
					break ;
				}
			}
			return shouldRemove(first);
		} else if (Collection.class.isAssignableFrom(objClass)) {
			Collection tmp = (Collection)o;
			
			Iterator tmpIt = tmp.iterator();
			Object first = null;
			while (tmpIt.hasNext()) {
				first = tmpIt.next();
				if (first != null) {
					break ;
				}
			}
			return shouldRemove(first);
		} else if (Map.class.isAssignableFrom(objClass)) {
			Map map = (Map)o;
			
			Iterator<Entry> tmpIt = map.entrySet().iterator();
			Object firstKey = null;
			Object firstVal = null;
			boolean[] get = {false, false};
			while (tmpIt.hasNext()) {
				Entry e = tmpIt.next();
				if (!get[0]) {
					if (e.getKey() != null) {
						get[0] = true;
						firstKey = e.getKey();
					}
				}
				
				if (!get[1]) {
					if (e.getValue() != null) {
						get[1] = true;
						firstVal = e.getValue();
					}
				}
				
				if (get[0] && get[1]) {
					break ;
				}
			}
			
			return shouldRemove(firstKey) || shouldRemove(firstVal);
		} else {
			return false;
		}
	}
	
	private static boolean attemptSerialization(Object obj) {
		synchronized(boLock) {
			try {
				System.out.println("Check class: " + obj.getClass());
				String fileName = "ser/" + obj.getClass() + ".xml";
				File toWrite = new File(fileName);
				BufferedWriter bw = new BufferedWriter(new FileWriter(toWrite));
				String xmlString = fromObj2XML(obj);
				bw.write(xmlString);
				bw.close();
				
				File toRead = new File(fileName);
				Object deser = fromXML2Obj(toRead);
				if (deser == null) {
					//toWrite.delete();
					System.out.println(obj.getClass() + " fails");
					return false;
				} else {
					//toWrite.delete();
					System.out.println(obj.getClass() + " passes");
					System.out.println(obj);
					return true;
				}
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
			
			return false;
		}
	}
	
	public static void collectIORecords(File dir, List<IORecord> recorder) {
		if (!dir.exists()) {
			logger.error("Non-exisiting directory: " + dir.getAbsolutePath());
			return ;
		}
		
		for (File f: dir.listFiles()) {
			if (f.getName().startsWith(".")) {
				continue ;
			}
			
			if (f.isDirectory()) {
				collectIORecords(f, recorder);
			} else {
				if (!f.getName().endsWith("xml")) {
					logger.error("Invalid profile: " + f.getAbsolutePath());
				} else {
					IORecord record = (IORecord)fromXML2Obj(f);
					if (record != null) {
						recorder.add(record);
					}
				}
			}
		}
	}
}
