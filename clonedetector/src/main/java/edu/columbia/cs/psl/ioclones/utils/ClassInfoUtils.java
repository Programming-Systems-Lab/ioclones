package edu.columbia.cs.psl.ioclones.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.ioclones.analysis.DependentValue;
import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;
import edu.columbia.cs.psl.ioclones.pojo.ParamInfo;

public class ClassInfoUtils {
	
	private static final Logger logger = LogManager.getLogger(ClassInfoUtils.class);
	
	public static final String RE_SLASH = ".";
	
	public static final String DELIM = "-";
	
	private static final Set<String> BLACK_PREFIX = IOUtils.blackPrefix();
	
	private static final File libDir = new File(System.getProperty("user.home") + "/.m2");
	
	private static final Set<Type> immutables = new HashSet<Type>();
	
	private static final HashMap<String, Boolean> writableCache = new HashMap<String, Boolean>();
		
	static {
		Type stringType = Type.getType(String.class);
		immutables.add(stringType);
		Type intType = Type.getType(Integer.class);
		immutables.add(intType);
		Type doubleType = Type.getType(Double.class);
		immutables.add(doubleType);
		Type floatType = Type.getType(Float.class);
		immutables.add(floatType);
		Type longType = Type.getType(Long.class);
		immutables.add(longType);
		Type shortType = Type.getType(Short.class);
		immutables.add(shortType);
		Type byteType = Type.getType(Byte.class);
		immutables.add(byteType);
		Type booleanType = Type.getType(Boolean.class);
		immutables.add(booleanType);
		Type charType = Type.getType(Character.class);
		immutables.add(charType);
	}
	
	public static String genClassFieldKey(String className, String fieldName) {
		return className + DELIM + fieldName;
	}
	
	public static String cleanType(String typeString) {
		//return typeString.replace("/", ClassUtils.RE_SLASH).replace(";", "");
		return typeString.replace("/", ClassInfoUtils.RE_SLASH);
	}
	
	public static String[] genMethodKey(String owner, String name, String desc) {
		Type returnType = Type.getReturnType(desc);
		String nameWithArgs = methodNameArgs(name, desc);
		
		//methodKey = className + methodName + args
		String methodKey = owner + DELIM + nameWithArgs;
		String returnString = returnType.toString();
		if (returnType.getSort() == Type.OBJECT) {
			returnString = returnString.substring(1, returnString.length());
		}
		String typeString = cleanType(returnString);
		String[] ret = {methodKey, typeString};
		return ret;
	}
	
	public static String methodNameArgs(String name, String desc) {
		Type[] args = Type.getArgumentTypes(desc);
		Type returnType = Type.getReturnType(desc);
		StringBuilder argBuilder = new StringBuilder();
		for (Type arg: args) {
			String argString = null;
			if (arg.getSort() == Type.ARRAY) {
				int dim = arg.getDimensions();
				Type arrType = arg.getElementType();
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < dim; i++) {
					sb.append("[");
				}
				sb.append(parseType(arrType));
				argString = sb.toString();
			} else {
				argString = parseType(arg);
			}
			argBuilder.append(cleanType(argString) + "+");
		}
		
		String argString = null;
		if (argBuilder.length() == 0) {
			argString = "()";
		} else {
			argString = "(" + argBuilder.substring(0, argBuilder.length() - 1) + ")";
		}
		
		return name + DELIM + argString;
	}
	
	public static String parsePkgName(String className) {
		int lastDot = className.lastIndexOf(".");
		String pkgName = className.substring(0, lastDot);
		return pkgName;
	}
	
	public static String parseType(Type t) {
		if (t.getSort() == Type.OBJECT) {
			return t.getInternalName();
		} else {
			return t.toString();
		}
	}
	
	public static boolean checkAccess(int access, int mask) {
		return ((access & mask) != 0);
	}
	
	public static boolean shouldInstrument(String className) {
		for (String b: BLACK_PREFIX) {
			if (className.startsWith(b)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean checkProtectionDomain(String domainPath) {
		File domainFile = new File(domainPath);
		try {
			String checkDir = domainFile.getParentFile().getCanonicalPath();
			String libDirPath = libDir.getCanonicalPath();
			//System.out.println("Check dir: " + checkDir);
			//System.out.println("Lib path: " + libDirPath);
			if (libDirPath.equals(checkDir)) {
				return false;
			} else {
				return true;
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		return false;
	}
	
	public static void collectClassesInJar(File jarFile, 
			List<InputStream> container) {
		try {
			JarFile jarInstance = new JarFile(jarFile);
			Enumeration<JarEntry> entries = jarInstance.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				//logger.info("Entry: " + entryName);
				
				if (entry.isDirectory()) {
					continue ;
				}
				
				if (!entryName.startsWith("java")) {
					continue ;
				}
				
				if (entryName.endsWith(".class")) {
					InputStream entryStream = jarInstance.getInputStream(entry);
					container.add(entryStream);
					
					//logger.info("Retrieve class: " + entry.getName());
					/*String className = entryName.replace("/", ".");
					className = className.substring(0, className.lastIndexOf("."));
					Class clazz = loader.loadClass(className);
					System.out.println("Class name: " + clazz.getProtectionDomain().toString());*/
				}
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
	
	public static void genJVMLookupTable(File jarFile, Map<String, InputStream> lookup) {
		try {
			JarFile jarInstance = new JarFile(jarFile);
			Enumeration<JarEntry> entries = jarInstance.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				
				if (entry.isDirectory()) {
					continue ;
				}
				
				if (entryName.endsWith(".class")) {
					//String className = entryName.replace("/", ".");
					String className = entryName.substring(0, entryName.lastIndexOf("."));
					className = className.replace("/", ".");
					InputStream entryStream = jarInstance.getInputStream(entry);
					lookup.put(className, entryStream);
					
					//logger.info("Retrieve class: " + entry.getName());
					/*
					Class clazz = loader.loadClass(className);
					System.out.println("Class name: " + clazz.getProtectionDomain().toString());*/
				}
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
	
	public static void genRepoClasses(File file, List<InputStream> container) {
		if (file.getName().startsWith(".")) {
			return ;
		}
		
		if (file.isDirectory()) {
			for (File f: file.listFiles()) {
				genRepoClasses(f, container);
			}
		} else {
			if (file.getName().endsWith(".class")) {
				try {
					InputStream is = new FileInputStream(file);
					container.add(is);
				} catch (Exception ex) {
					logger.error("Error: ", ex);
				}
			}
		}
	}
	
	public static boolean isImmutable(Type t) {
		return immutables.contains(t);
	}
	
	/*public static void stabelizeMethod(MethodInfo method) {
		method.stabelized = true;
		if (method.writtenInputs == null) {
			method.writtenInputs = new TreeSet<Integer>();
		}
		
		for (CalleeRecord ci: method.getCallees()) {
			String calleeKey = ci.getMethodKey();
			System.out.println("Callee key: " + calleeKey);
			String className = calleeKey.split(ClassInfoUtils.DELIM)[0];
			
			//From callee to caller map
			Map<Integer, Integer> potentialOutputs = ci.getPotentialOutputs();
			
			//Conservatively query the class hierarchy, level here does not matter
			TreeSet<Integer> writtenParams = null;
			if (ci.fixed) {
				writtenParams = queryMethod(className, calleeKey, true, MethodInfo.PUBLIC);
			} else {
				writtenParams = queryMethod(className, calleeKey, false, MethodInfo.PUBLIC);
			}
			System.out.println("writtenParams: " + writtenParams);
			
			for (Integer calleeId: potentialOutputs.keySet()) {
				if (writtenParams.contains(calleeId)) {
					int callerId = potentialOutputs.get(calleeId);
					method.writtenInputs.add(callerId);
				}
			}
		}
		
		//SymbolicValueAnalyzer.analyzeValue(method);
	}*/
	
	public static Map<Integer, TreeSet<Integer>> queryMethod(String className, 
			String methodNameArgs, 
			boolean isFixed, 
			int level, boolean detail) {
		
		Map<Integer, TreeSet<Integer>> writtenParams = null;
		ClassInfo curInfo = GlobalInfoRecorder.queryClassInfo(className);
		
		if (isFixed) {
			MethodInfo curMethod = curInfo.getMethodInfo(methodNameArgs);
			if (curMethod != null) {
				return curMethod.getWrittenParams();
			} else {
				if (curInfo.getParent() == null) {
					return null;
				} else {
					return queryMethod(curInfo.getParent(), methodNameArgs, isFixed, level, detail);
				}
			}
		}
		
		if (writtenParams == null) {
			writtenParams = new HashMap<Integer, TreeSet<Integer>>();
		}
		
		MethodInfo curMethod = curInfo.getMethodInfo(methodNameArgs);
		if (curMethod != null) {
			if (curMethod.getWrittenParams() != null)
				unionMap(curMethod.getWrittenParams(), writtenParams);
		} else if (curInfo.getParent() != null) {
			//Climb up
			Map<Integer, TreeSet<Integer>> superQuery = searchUp(curInfo.getParent(), methodNameArgs, level);
			if (detail) {
				System.out.println("Parent: " + curInfo.getParent() + " " + methodNameArgs);
				System.out.println("Super query: " + superQuery);
			}
			unionMap(superQuery, writtenParams);
		}
		
		//Go down
		for (String child: curInfo.getChildren()) {
			Map<Integer, TreeSet<Integer>> childQuery = searchDown(child, methodNameArgs, level);
			if (detail) {
				System.out.println("Child: " + child + " " + methodNameArgs);
				System.out.println("Child query: " + childQuery);
			}
			
			unionMap(childQuery, writtenParams);
		}
		
		return writtenParams;
	}
	
	public static boolean isWritable(String className) {		
		if (writableCache.containsKey(className)) {
			return writableCache.get(className);
		}
		
		LinkedList<String> queue = new LinkedList<String>();
		queue.add(className);
		//logger.info("Entry class name: " + className);
		while (queue.size() > 0) {
			String curName = queue.removeFirst();
			if (curName.equals("java.io.Writer") || curName.equals("java.io.OutputStream")) {
				writableCache.put(className, true);
				return true;
			}
			
			ClassInfo ci = GlobalInfoRecorder.queryClassInfo(curName);
			/*if (ci == null) {
				logger.info("No class info: " + curName);
				System.exit(-1);
			}*/
			
			if (ci.getParent() != null) {
				queue.add(ci.getParent());
			}
			
			if (ci.getInterfaces().size() > 0) {
				queue.addAll(ci.getInterfaces());
			}
		}
		
		writableCache.put(className, false);
		return false;
	}
	
	public static Map<Integer, TreeSet<Integer>> searchUp(String className, 
			String nameArgs, 
			int level) {	
		Map<Integer, TreeSet<Integer>> ret = new HashMap<Integer, TreeSet<Integer>>();
		ClassInfo ci = GlobalInfoRecorder.queryClassInfo(className);
		MethodInfo mi = ci.getMethodInfo(nameArgs);
		
		if (mi != null && mi.getLevel() <= level) {
			if (mi.getWrittenParams() != null) {
				unionMap(mi.getWrittenParams(), ret);
			}
		} else  if (ci.getParent() != null){
			Map<Integer, TreeSet<Integer>> superQuery = null;
			superQuery = searchUp(ci.getParent(), nameArgs, level);
			unionMap(superQuery, ret);
		}
		
		return ret;
	}
	
	public static Map<Integer, TreeSet<Integer>> searchDown(String className, 
			String nameArgs, 
			int level) {
		Map<Integer, TreeSet<Integer>> ret = new HashMap<Integer, TreeSet<Integer>>();
		ClassInfo ci = GlobalInfoRecorder.queryClassInfo(className);
		MethodInfo mi = ci.getMethodInfo(nameArgs);
		
		if (mi != null && mi.getLevel() >= level) {
			if (mi.getWrittenParams() != null) {
				unionMap(mi.getWrittenParams(), ret);
			}
		}
		
		if (mi != null) {
			if (!mi.isFinal()) {
				ci.getChildren().forEach(child->{
					Map<Integer, TreeSet<Integer>> childQuery = searchDown(child, nameArgs, mi.getLevel());
					unionMap(childQuery, ret);
				});
				
			}
		} else {
			ci.getChildren().forEach(child->{
				Map<Integer, TreeSet<Integer>> childQuery = searchDown(child, nameArgs, level);
				unionMap(childQuery, ret);
			});
		}
		
		return ret;
	}
	
	public static <T> void unionMap(Map<T, TreeSet<T>> toAdd, 
			Map<T, TreeSet<T>> original) {
		toAdd.forEach((o, is)->{
			if (original.containsKey(o)) {
				original.get(o).addAll(is);
			} else {
				TreeSet<T> copy = new TreeSet<T>(is);
				original.put(o, copy);
			}
		});
	}
	
	public static Type[] genMethodArgs(String methodDesc, String className) {
		Type[] args = null;
		
		//This means static
		if (className == null) {
			args = Type.getArgumentTypes(methodDesc);
		} else {
			Type[] methodArgs = Type.getArgumentTypes(methodDesc);
			args = new Type[methodArgs.length + 1];
			args[0] = Type.getObjectType(className);
			for (int i = 1; i < args.length; i++) {
				args[i] = methodArgs[i - 1];
			}
		}
		return args;
	}
	
	public static List<ParamInfo> computeMethodArgs(Type[] args) {
		List<ParamInfo> paramInfos = new ArrayList<ParamInfo>();
		int curId = 0;		
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				ParamInfo pi = new ParamInfo();
				Type curType = args[i];
				pi.paramType = curType;
				pi.runtimeIdx = curId;
				paramInfos.add(pi);
				
				int curSort = curType.getSort();
				if (curSort == Type.DOUBLE || curSort == Type.LONG) {
					curId += 2;
				} else {
					curId++;
				}
			}
		}
		
		return paramInfos;
	}
}
