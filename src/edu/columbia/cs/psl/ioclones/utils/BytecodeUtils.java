package edu.columbia.cs.psl.ioclones.utils;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public class BytecodeUtils {
	
	private static final Set<Integer> XLOADS = new HashSet<Integer>();
	
	private static final Set<Integer> ARRLOADS = new HashSet<Integer>();
	
	private static final Set<Integer> XSTORES = new HashSet<Integer>();
	
	private static final Set<Integer> ARRSTORES = new HashSet<Integer>();
	
	private static final Set<Integer> XRETURNS = new HashSet<Integer>();
	
	static {
		for (int i = Opcodes.ICONST_M1; i <= 45; i++) {
			XLOADS.add(i);
		}
		
		for (int i = Opcodes.IALOAD; i <= Opcodes.SALOAD; i++) {
			ARRLOADS.add(i);
		}
		
		for (int i = Opcodes.ISTORE; i <= 78; i++) {
			XSTORES.add(i);
		}
		
		for (int i = Opcodes.IASTORE; i <= Opcodes.SASTORE; i++) {
			ARRSTORES.add(i);
		}
		
		for (int i = Opcodes.IRETURN; i <= Opcodes.RETURN; i++) {
			XRETURNS.add(i);
		}
	}
	
	public static boolean xload(int opcode) {
		return XLOADS.contains(opcode);
	}
	
	public static boolean arrLoad(int opcode) {
		return ARRLOADS.contains(opcode);
	}
	
	public static boolean xstore(int opcode) {
		return XSTORES.contains(opcode);
	}
	
	public static boolean arrStore(int opcode) {
		return ARRSTORES.contains(opcode);
	}
	
	public static boolean xreturn(int opcode) {
		return XRETURNS.contains(opcode);
	}

}
