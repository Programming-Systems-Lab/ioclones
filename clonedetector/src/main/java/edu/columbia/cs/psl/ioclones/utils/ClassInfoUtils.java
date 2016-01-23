package edu.columbia.cs.psl.ioclones.utils;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.internal.MethodSorter;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.ioclones.analysis.DependentValue;
import edu.columbia.cs.psl.ioclones.analysis.JVMAnalyzer.ClassInfo;
import edu.columbia.cs.psl.ioclones.analysis.JVMAnalyzer.MethodInfo;

public class ClassInfoUtils {
	
	private static final Logger logger = LogManager.getLogger(ClassInfoUtils.class);
	
	public static final String RE_SLASH = ".";
	
	public static final String DELIM = "-";
	
	private static final Set<String> BLACK_PREFIX = IOUtils.blackPrefix();
	
	private static final File libDir = new File(System.getProperty("user.home") + "/.m2");
	
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
}
