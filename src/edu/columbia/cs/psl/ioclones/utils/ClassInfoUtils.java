package edu.columbia.cs.psl.ioclones.utils;

import org.objectweb.asm.Type;

public class ClassInfoUtils {
	
	public static final String RE_SLASH = ".";
	
	public static final String DELIM = "-";
	
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

}
