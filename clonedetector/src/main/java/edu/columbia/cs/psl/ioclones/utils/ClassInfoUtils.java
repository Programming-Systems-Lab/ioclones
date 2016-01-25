package edu.columbia.cs.psl.ioclones.utils;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.internal.MethodSorter;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.ioclones.analysis.DependentValue;
import edu.columbia.cs.psl.ioclones.pojo.CalleeRecord;
import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;

public class ClassInfoUtils {
	
	private static final Logger logger = LogManager.getLogger(ClassInfoUtils.class);
	
	public static final String RE_SLASH = ".";
	
	public static final String DELIM = "-";
	
	private static final Set<String> BLACK_PREFIX = IOUtils.blackPrefix();
	
	private static final File libDir = new File(System.getProperty("user.home") + "/.m2");
	
	private static final Set<Type> immutables = new HashSet<Type>();
	
	private static final Map<String, Set<Integer>> paramCache = new HashMap<String, Set<Integer>>();
	
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
		
		//methodKey = className + methodName + args
		String methodKey = owner + DELIM + name + DELIM + argString;
		String returnString = returnType.toString();
		if (returnType.getSort() == Type.OBJECT) {
			returnString = returnString.substring(1, returnString.length());
		}
		String typeString = cleanType(returnString);
		String[] ret = {methodKey, typeString};
		return ret;
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
	
	public static boolean checkOwnerInParams(Set<Integer> params, DependentValue dv) {
		DependentValue ptr = dv;
		while (ptr != null) {
			if (params.contains(dv.id)) {
				return true;
			}
			
			ptr = dv.owner;
		}
		return false;
	}
	
	public static void collectClassesInJar(File jarFile, List<InputStream> container) {
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
	
	public static boolean isImmutable(Type t) {
		return immutables.contains(t);
	}
	
	public static void stabelizeMethod(MethodInfo method) {
		method.stabelized = true;
		
		for (CalleeRecord ci: method.getFixedCallees()) {
			String calleeKey = ci.getMethodKey();
			String className = calleeKey.split(ClassInfoUtils.RE_SLASH)[0];
			
			Map<Integer, DependentValue> calleeCallerBridge = ci.getCalleeCallerBridge(); 
			
			//level here does not matter
			Set<Integer> writtenParams = ClassInfoUtils.queryMethod(className, 
					calleeKey, 
					true, MethodInfo.PUBLIC);
			
			calleeCallerBridge.forEach((ce, cr)->{
				if (writtenParams.contains(ce)) {
					
				}
			});
		}
	}
	
	public static Set<Integer> queryMethod(String className, 
			String methodKey, 
			boolean isFixed, 
			int level) {
		if (paramCache.containsKey(methodKey)) {
			return paramCache.get(methodKey);
		}
		
		Set<Integer> writtenParams = new TreeSet<Integer>();
		ClassInfo ci = GlobalInfoRecorder.queryClassInfo(className);
		if (ci == null) {
			logger.error("Missed class: " + className);
			System.exit(-1);
		}
		
		if (isFixed) {
			MethodInfo mi = ci.getMethods().get(methodKey);
			if (mi != null) {
				if (!mi.stabelized) {
					stabelizeMethod(mi);
				}
				
				if (mi.getWriteParams().size() > 0) {
					writtenParams.addAll(mi.getWriteParams());
				}
			} else {
				writtenParams.addAll(queryMethod(ci.getParent(), methodKey, isFixed, level));
			}
			paramCache.put(methodKey, writtenParams);
			
			return writtenParams;
		}
		
		MethodInfo mi = ci.getMethods().get(methodKey);
		if (mi != null) {
			if (!mi.stabelized) {
				stabelizeMethod(mi);
			}
			
			writtenParams.addAll(mi.getWriteParams());
			level = mi.getLevel();
		}
		
		//Climb up
		if (ci.getParent() != null) {
			writtenParams.addAll(searchUp(ci.getParent(), methodKey, level));
		}
		
		//Go down
		for (String child: ci.getChildren()) {
			writtenParams.addAll(searchDown(child, methodKey, level));
		}
		paramCache.put(methodKey, writtenParams);
		
		return writtenParams;
	}
	
	public static Set<Integer> searchUp(String className, 
			String methodKey, 
			int level) {	
		ClassInfo ci = GlobalInfoRecorder.queryClassInfo(className);
		MethodInfo mi = ci.getMethods().get(methodKey);
				
		Set<Integer> ret = new TreeSet<Integer>();
		if (mi != null && mi.getLevel() <= level && !mi.isFinal()) {
			if (!mi.stabelized) {
				stabelizeMethod(mi);
			}
			
			ret.addAll(mi.getWriteParams());
		}
		
		if (ci.getParent() != null) {
			if (mi == null)
				ret.addAll(searchUp(ci.getParent(), methodKey, level));
			else
				ret.addAll(searchUp(ci.getParent(), methodKey, mi.getLevel()));
		}
		
		return ret;
	}
	
	public static Set<Integer> searchDown(String className, 
			String methodKey, 
			int level) {
		ClassInfo ci = GlobalInfoRecorder.queryClassInfo(className);
		MethodInfo mi = ci.getMethods().get(methodKey);
		Set<Integer> ret = new TreeSet<Integer>();
		if (mi != null && mi.getLevel() >= level) {
			if (!mi.stabelized) {
				stabelizeMethod(mi);
			}
			
			ret.addAll(mi.getWriteParams());
		}
		
		if (mi != null) {
			if (!mi.isFinal()) {
				ci.getChildren().forEach(child->{
					ret.addAll(searchDown(child, methodKey, mi.getLevel()));
				});
			}
			
		} else {
			ci.getChildren().forEach(child->{
				ret.addAll(searchDown(child, methodKey, level));
			});
		}
		
		return ret;
	}
}
