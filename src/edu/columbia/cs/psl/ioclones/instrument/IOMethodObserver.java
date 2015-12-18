package edu.columbia.cs.psl.ioclones.instrument;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.utils.BytecodeUtils;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class IOMethodObserver extends MethodVisitor {
	
	private String ownerClass;
	
	private String name;
	
	private String desc;
	
	private String methodKey;
	
	private String returnType;
	
	private String signature;
	
	private String[] exceptions;
	
	private LocalVariablesSorter lvs;
	
	private int recordId = -1;
	
	public IOMethodObserver(MethodVisitor mv, 
			String ownerClass, 
			String name, 
			String desc, 
			String signature, 
			String[] exceptions) {
		super(Opcodes.ASM5, mv);
		this.ownerClass = ownerClass;
		this.name = name;
		this.desc = desc;
		String[] parsedKeys = ClassInfoUtils.genMethodKey(this.ownerClass, this.name, this.desc);
		this.methodKey = parsedKeys[0];
		this.returnType = parsedKeys[1];
		this.signature = signature;
		this.exceptions = exceptions;
	}
	
	public void setLocalVariablesSorter(LocalVariablesSorter lvs) {
		this.lvs = lvs;
	}
	
	public LocalVariablesSorter getLocalVariablesSorter() {
		return this.lvs;
	}
	
	@Override
	public void visitCode() {
		this.mv.visitCode();
		
		Type ioRecordType = Type.getType(IORecord.class);
		this.recordId = this.lvs.newLocal(ioRecordType);
		this.mv.visitTypeInsn(Opcodes.NEW, ioRecordType.getInternalName());
		this.mv.visitInsn(Opcodes.DUP);
		this.mv.visitLdcInsn(this.methodKey);
		this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
				ioRecordType.getInternalName(), 
				"<init>", 
				"(Ljava/lang/String;)V", 
				false);
		this.mv.visitVarInsn(Opcodes.ASTORE, this.recordId);
	}
	
	@Override
	public void visitInsn(int opcode) {
		this.mv.visitInsn(opcode);
		
		if (!BytecodeUtils.xload(opcode)) {
			return ;
		}
		
		if ((opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) 
				|| opcode == Opcodes.BIPUSH 
				|| opcode == Opcodes.SIPUSH
				|| opcode == Opcodes.IALOAD) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf", "(I)Ljava/lang/Integer;", false);
		} else if (opcode == Opcodes.LCONST_0 
				|| opcode == Opcodes.LCONST_1 
				|| opcode == Opcodes.LALOAD) {
			this.mv.visitInsn(Opcodes.DUP2);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Long.class), "valueOf", "(J)Ljava/lang/Long;", false);
		} else if ((opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2) 
				|| opcode == Opcodes.FALOAD) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "valueOf", "(F)Ljava/lang/Float;", false);
		} else if (opcode == Opcodes.DCONST_0 
				|| opcode == Opcodes.DCONST_1 
				|| opcode == Opcodes.DALOAD) {
			this.mv.visitInsn(Opcodes.DUP2);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Double.class), "valueOf", "(D)Ljava/lang/Double;", false);
		} else if (opcode == Opcodes.AALOAD) {
			this.mv.visitInsn(Opcodes.DUP);
		} else if (opcode == Opcodes.BALOAD) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Byte.class), "valueOf", "(B)Ljava/lang/Byte;", false);
		} else if (opcode == Opcodes.CALOAD) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Character.class), "valueOf", "(C)Ljava/lang/Character;", false);
		} else if (opcode == Opcodes.SALOAD) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Short.class), "valueOf", "(S)Ljava/lang/Short;", false);
		}
		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
		this.mv.visitInsn(Opcodes.SWAP);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(IORecord.class), "registerInput", "(Ljava/lang/Object;)V", false);
	}

}
