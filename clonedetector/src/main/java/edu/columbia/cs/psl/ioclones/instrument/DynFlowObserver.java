package edu.columbia.cs.psl.ioclones.instrument;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintChecker;
import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintPropagater;
import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.utils.BytecodeUtils;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class DynFlowObserver extends MethodVisitor implements Opcodes {
	
	public static final int DEPTH = 1;
	
	private boolean isConstructor;
	
	private String internalName;
	
	private String internalSuperName;
	
	private String className;
	
	private int access;
	
	private String name;
	
	private String desc;
	
	private Type[] args;
	
	private String signature;
	
	private String[] exceptions;
	
	private boolean isStatic;
	
	//Both methodKey and returnType are not internal name
	private String methodKey;
	
	private String returnType;
	
	private LocalVariablesSorter lvs;
	
	private int recordId = -1;
	
	public DynFlowObserver(String internalName, 
			String internalSuperName, 
			String className, 
			int access, 
			final String name, 
			final String desc, 
			String signature, 
			String[] exceptions, 
			final MethodVisitor mv) {
		super(Opcodes.ASM5, mv);
		
		this.internalName = internalName;
		this.internalSuperName = internalSuperName;
		this.className = className;
		this.access = access;
		this.name = name;
		if (this.name.equals("<init>")) {
			this.isConstructor = true;
		}
		this.desc = desc;
		this.signature = signature;
		this.exceptions = exceptions;
		this.isStatic = ClassInfoUtils.checkAccess(this.access, Opcodes.ACC_STATIC);
		String[] parsedKeys = 
				ClassInfoUtils.genMethodKey(this.className, this.name, this.desc);
		this.methodKey = parsedKeys[0];
		this.returnType = parsedKeys[1];
	}
	
	public void setLocalVariableSorter(LocalVariablesSorter lvs) {
		this.lvs = lvs;
	}
	
	public LocalVariablesSorter getLocalVariablesSorter() {
		return this.lvs;
	}
	
	public void processArg(Type arg, int argIdx) {
		String ioRecordType = Type.getInternalName(IORecord.class);
		String propagater = Type.getInternalName(HitoTaintPropagater.class);
		if (arg.getSort() == Type.INT) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(IJ)I", false);
			this.mv.visitVarInsn(ISTORE, argIdx);
		} else if (arg.getSort() == Type.SHORT) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(SJ)S", false);
			this.mv.visitVarInsn(ISTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.BYTE) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(BJ)B", false);
			this.mv.visitVarInsn(ISTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.BOOLEAN) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(ZJ)Z", false);
			this.mv.visitVarInsn(ISTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.CHAR) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(CJ)C", false);
			this.mv.visitVarInsn(ISTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.FLOAT) {
			this.mv.visitVarInsn(FLOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(FJ)F", false);
			this.mv.visitVarInsn(FSTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.DOUBLE) {
			this.mv.visitVarInsn(DLOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(DJ)D", false);
			this.mv.visitVarInsn(DSTORE, argIdx);
			argIdx += 2;
		} else if (arg.getSort() == Type.LONG) {
			this.mv.visitVarInsn(LLOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(JJ)J", false);
			this.mv.visitVarInsn(LSTORE, argIdx);
			argIdx += 2;
		} else if (arg.equals(Type.getType(String.class))) {
			this.mv.visitVarInsn(ALOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(Ljava/lang/String;J)V", false);
			argIdx++;
		} else if (arg.getSort() == Type.OBJECT || arg.getSort() == Type.ARRAY) {
			this.mv.visitVarInsn(ALOAD, argIdx);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
			this.convertToInst(DEPTH);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(Ljava/lang/Object;JI)V", false);
			
			//keep a reference to input obj
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.convertToInst(argIdx);
			this.mv.visitVarInsn(ALOAD, argIdx);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "preload", "(ILjava/lang/Object;)V", false);
			argIdx++;
		} else {
			System.err.println("Un-recognized type: " + arg);
			System.exit(-1);
		}
	}
	
	@Override
	public void visitCode() {
		this.mv.visitCode();
		
		Type ioRecordType = Type.getType(IORecord.class);
		this.recordId = this.lvs.newLocal(ioRecordType);
		this.mv.visitTypeInsn(Opcodes.NEW, ioRecordType.getInternalName());
		this.mv.visitInsn(Opcodes.DUP);
		this.mv.visitLdcInsn(this.methodKey);
		if (this.isStatic) {
			this.mv.visitInsn(Opcodes.ICONST_1);
		} else {
			this.mv.visitInsn(Opcodes.ICONST_0);
		}
		this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
				ioRecordType.getInternalName(), 
				"<init>", 
				"(Ljava/lang/String;Z)V", 
				false);
		this.mv.visitVarInsn(Opcodes.ASTORE, this.recordId);
		
		if (isStatic) {
			this.args = ClassInfoUtils.genMethodArgs(this.desc, null);
		} else {
			this.args = ClassInfoUtils.genMethodArgs(this.desc, this.internalName);
		}
		
		if (this.isConstructor) {
			int argIdx = 1;
			for (int i = 1; i < this.args.length; i++) {
				Type arg = this.args[i];
				this.processArg(arg, argIdx);
				if (arg.getSort() == Type.LONG || arg.getSort() == Type.DOUBLE) {
					argIdx += 2;
				} else {
					argIdx++;
				}
			}
		} else {
			int argIdx = 0;
			for (int i = 0; i < this.args.length; i++) {
				Type arg = this.args[i];
				this.processArg(arg, argIdx);
				if (arg.getSort() == Type.LONG || arg.getSort() == Type.DOUBLE) {
					argIdx += 2;
				} else {
					argIdx++;
				}
			}
		}
	}
	
	@Override
	public void visitInsn(int opcode) {
		String taintChecker = Type.getInternalName(HitoTaintChecker.class);
		if (BytecodeUtils.xreturn(opcode)) {
			if (opcode != RETURN) {
				Type retType = Type.getReturnType(this.desc);
				int sort = retType.getSort();
				
				if (sort == Type.LONG || sort == Type.DOUBLE) {
					this.mv.visitInsn(DUP2);
				} else {
					this.mv.visitInsn(DUP);
				}
				this.mv.visitVarInsn(ALOAD, this.recordId);
				
				if (sort == Type.INT) {
					this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(ILedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
				} else if (sort == Type.SHORT) {
					this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(SLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
				} else if (sort == Type.BOOLEAN) {
					this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(ZLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
				} else if (sort == Type.BYTE) {
					this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(BLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
				} else if (sort == Type.CHAR) {
					this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(CLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
				} else if (sort == Type.FLOAT) {
					this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(FLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
				} else if (sort == Type.LONG) {
					this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(JLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
				} else if (sort == Type.DOUBLE) {
					this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(DLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
				} else if (sort == Type.OBJECT || sort == Type.ARRAY) {
					if (retType.equals(Type.getType(String.class))) {
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(Ljava/lang/String;Ledu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else {
						this.convertToInst(DEPTH);
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(Ljava/lang/Object;ILedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					}
				} else {
					System.err.println("Un-recognized return type: " + retType);
					System.exit(-1);
				}
			}
			//Analyze if input has been written
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.convertToInst(DEPTH);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "summarizeWrittenInputs", "(Ledu/columbia/cs/psl/ioclones/pojo/IORecord;I)V", false);
			
			//Register io record
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKESTATIC, 
					Type.getInternalName(GlobalInfoRecorder.class), 
					"registerIO", 
					"(Ledu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
					false);
		}
		this.mv.visitInsn(opcode);
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
		
		if (this.isConstructor) {
			//This means that object.init or super init has not been touched
			//Taint "this"
			if (opcode == INVOKESPECIAL && name.equals("<init>")) {
				if (owner.equals(this.internalName) || owner.equals(this.internalSuperName)) {
					Type thisType = this.args[0];
					processArg(thisType, 0);
				}
			}
			this.isConstructor = false;
		}
	}
	
	private void convertToInst(int num) {
		if (num == 0) {
			this.mv.visitInsn(Opcodes.ICONST_0);
		} else if (num == 1) {
			this.mv.visitInsn(Opcodes.ICONST_1);
		} else if (num == 2) {
			this.mv.visitInsn(Opcodes.ICONST_2);
		} else if (num == 3) {
			this.mv.visitInsn(Opcodes.ICONST_3);
		} else if (num == 4) {
			this.mv.visitInsn(Opcodes.ICONST_4);
		} else if (num == 5) {
			this.mv.visitInsn(Opcodes.ICONST_5);
		} else if (num > 6 && num <= 127) {
			this.mv.visitIntInsn(Opcodes.BIPUSH, num);
		} else if (num > 127 && num <= 32767) {
			this.mv.visitIntInsn(Opcodes.SIPUSH, num);
		} else {
			this.mv.visitLdcInsn(num);
		}
	}
	
	public static void main(String[] ars) {
		System.out.println(Type.getType(String.class));
	}
}
