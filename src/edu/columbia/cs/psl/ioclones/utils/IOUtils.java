package edu.columbia.cs.psl.ioclones.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.thoughtworks.xstream.XStream;

public class IOUtils {
	
	private static Logger logger = LogManager.getLogger(IOUtils.class);
	
	public static <T> void writeJson(T obj, TypeToken typeToken, String fileName) {
		GsonBuilder gb = new GsonBuilder();
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
		File blackFile = new File("./info/blacklist.txt");
		if (!blackFile.exists()) {
			return ret;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(blackFile));
			String buf = "";
			while ((buf = br.readLine()) != null) {
				ret.add(buf);
			}
			br.close();
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		return ret;
	}
	
	public static Object newObject(Object obj) {
		XStream xstream = new XStream();
		String objString = xstream.toXML(obj);
		//System.out.println("objString: " + objString);
		Object newObj = xstream.fromXML(objString);
		return newObj;
	}
	
	public static void cleanNonSerializables(Collection c) {
		//Need a better way to decide which objs we don't want
		Iterator it = c.iterator();
		while (it.hasNext()) {
			Object o = it.next();
			if (OutputStream.class.isAssignableFrom(o.getClass()) 
					|| InputStream.class.isAssignableFrom(o.getClass())) {
				it.remove();
				logger.info("Remove stream obj: " + o);
			}
		}
	}
}
