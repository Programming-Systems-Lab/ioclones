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
	
	private static final Set<Integer> OPS = new HashSet<Integer>();
	
	private static final Set<Integer> IOPS = new HashSet<Integer>();
	
	private static final Set<Integer> FOPS = new HashSet<Integer>();
	
	private static final Set<Integer> LOPS = new HashSet<Integer>();
	
	private static final Set<Integer> DOPS = new HashSet<Integer>();
	
	private static final Set<Integer> CONVERTS = new HashSet<Integer>();
	
	private static final Set<Integer> toI = new HashSet<Integer>();
	
	private static final Set<Integer> toL = new HashSet<Integer>();
	
	private static final Set<Integer> toF = new HashSet<Integer>();
	
	private static final Set<Integer> toD = new HashSet<Integer>();
	
	static {
		for (int i = Opcodes.ACONST_NULL; i <= 45; i++) {
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
		
		for (int i = Opcodes.IADD; i <= Opcodes.LXOR; i++) {
			OPS.add(i);
		}
		
		for (int i = Opcodes.I2L ; i<= Opcodes.I2S; i++) {
			CONVERTS.add(i);
		}
		
		IOPS.add(Opcodes.IADD);
		IOPS.add(Opcodes.ISUB);
		IOPS.add(Opcodes.IMUL);
		IOPS.add(Opcodes.IDIV);
		IOPS.add(Opcodes.IREM);
		IOPS.add(Opcodes.INEG);
		IOPS.add(Opcodes.ISHL);
		IOPS.add(Opcodes.ISHR);
		IOPS.add(Opcodes.IUSHR);
		IOPS.add(Opcodes.IAND);
		IOPS.add(Opcodes.IOR);
		IOPS.add(Opcodes.IXOR);
		
		LOPS.add(Opcodes.LADD);
		LOPS.add(Opcodes.LSUB);
		LOPS.add(Opcodes.LMUL);
		LOPS.add(Opcodes.LDIV);
		LOPS.add(Opcodes.LREM);
		LOPS.add(Opcodes.LNEG);
		LOPS.add(Opcodes.LSHL);
		LOPS.add(Opcodes.LSHR);
		LOPS.add(Opcodes.LUSHR);
		LOPS.add(Opcodes.LAND);
		LOPS.add(Opcodes.LOR);
		LOPS.add(Opcodes.LXOR);
		
		FOPS.add(Opcodes.FADD);
		FOPS.add(Opcodes.FSUB);
		FOPS.add(Opcodes.FMUL);
		FOPS.add(Opcodes.FDIV);
		FOPS.add(Opcodes.FREM);
		FOPS.add(Opcodes.FNEG);
		
		DOPS.add(Opcodes.DADD);
		DOPS.add(Opcodes.DSUB);
		DOPS.add(Opcodes.DMUL);
		DOPS.add(Opcodes.DDIV);
		DOPS.add(Opcodes.DREM);
		DOPS.add(Opcodes.DNEG);
		
		toI.add(Opcodes.L2I);
		toI.add(Opcodes.F2I);
		toI.add(Opcodes.D2I);
		
		toL.add(Opcodes.I2L);
		toL.add(Opcodes.F2L);
		toL.add(Opcodes.D2L);
		
		toF.add(Opcodes.I2F);
		toF.add(Opcodes.D2F);
		toF.add(Opcodes.L2F);
		
		toD.add(Opcodes.I2D);
		toD.add(Opcodes.L2D);
		toD.add(Opcodes.F2D);
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
	
	public static boolean ops(int opcode) {
		return OPS.contains(opcode);
	}
	
	public static boolean iops(int opcode) {
		return IOPS.contains(opcode);
	}
	
	public static boolean lops(int opcode) {
		return LOPS.contains(opcode);
	}
	
	public static boolean fops(int opcode) {
		return FOPS.contains(opcode);
	}
	
	public static boolean dops(int opcode) {
		return DOPS.contains(opcode);
	}
	
	public static boolean converts(int opcode) {
		return CONVERTS.contains(opcode);
	}
	
	public static boolean toi(int opcode) {
		return toI.contains(opcode);
	}
	
	public static boolean tol(int opcode) {
		return toL.contains(opcode);
	}
	
	public static boolean tof(int opcode) {
		return toF.contains(opcode);
	}
	
	public static boolean tod(int opcode) {
		return toD.contains(opcode);
	}

}
