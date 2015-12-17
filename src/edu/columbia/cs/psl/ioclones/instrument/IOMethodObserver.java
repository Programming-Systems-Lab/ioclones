package edu.columbia.cs.psl.ioclones.instrument;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
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

}
