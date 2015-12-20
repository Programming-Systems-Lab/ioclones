package edu.columbia.cs.psl.ioclones.instrument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.utils.BytecodeUtils;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class IOMethodObserver extends MethodVisitor {
	
	private static final Logger logger = LogManager.getLogger(IOMethodObserver.class);
	
	private static final String VALUE_OF = "valueOf";
	
	private static final String INIT = "<init>";
	
	private String ownerClass;
	
	private String superClass;
	
	private String name;
	
	private String desc;
	
	private String methodKey;
	
	private String returnType;
	
	private String signature;
	
	private String[] exceptions;
	
	private LocalVariablesSorter lvs;
	
	private int recordId = -1;
	
	private boolean begin = true;
	
	public IOMethodObserver(MethodVisitor mv, 
			String ownerClass, 
			String superClass,
			String name, 
			String desc, 
			String signature, 
			String[] exceptions) {
		super(Opcodes.ASM5, mv);
		this.ownerClass = ownerClass;
		this.superClass = superClass;
		this.name = name;
		this.desc = desc;
		String[] parsedKeys = 
				ClassInfoUtils.genMethodKey(this.ownerClass, this.name, this.desc);
		this.methodKey = parsedKeys[0];
		this.returnType = parsedKeys[1];
		this.signature = signature;
		this.exceptions = exceptions;
		
		if (name.equals("<init>")) {
			this.begin = false;
		}
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
		
		/*if (!this.begin) {
			return ;
		}*/
		
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
		if (!this.begin) {
			this.mv.visitInsn(opcode);
			return ;
		}
		
		if (BytecodeUtils.xload(opcode) || BytecodeUtils.arrLoad(opcode)) {
			//For xloads, record and insert the val into inputs
			this.mv.visitInsn(opcode);
			
			if (opcode == Opcodes.ACONST_NULL) {
				this.mv.visitInsn(Opcodes.DUP);
			} else if ((opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) 
					|| opcode == Opcodes.BIPUSH 
					|| opcode == Opcodes.SIPUSH) {
				this.handlePrimitive(Integer.class);
			} else if (opcode == Opcodes.LCONST_0 
					|| opcode == Opcodes.LCONST_1) {
				this.handlePrimitive(Long.class);
			} else if (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2) {
				this.handlePrimitive(Float.class);
			} else if (opcode == Opcodes.DCONST_0 
					|| opcode == Opcodes.DCONST_1) {
				this.handlePrimitive(Double.class);
			} else if (BytecodeUtils.arrLoad(opcode)) {
				this.handleArr(opcode);
			} else {
				logger.error("Invalid xload opcode: " + opcode);
				return ;
			}
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		} else if (BytecodeUtils.arrStore(opcode)) {
			//For arr stores, pull the val from inputs and store it in outputs
			this.mv.visitInsn(opcode);
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"pullOutput", 
					"()V", 
					false);
		} else if (BytecodeUtils.ops(opcode)) {
			this.mv.visitInsn(opcode);
			
			if (BytecodeUtils.iops(opcode)) {
				this.handlePrimitive(Integer.class);
			} else if (BytecodeUtils.lops(opcode)) {
				this.handlePrimitive(Long.class);
			} else if (BytecodeUtils.fops(opcode)) {
				this.handlePrimitive(Float.class);
			} else if (BytecodeUtils.dops(opcode)) {
				this.handlePrimitive(Double.class);
			}
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		} else if (BytecodeUtils.converts(opcode)) {
			this.mv.visitInsn(opcode);
			
			if (BytecodeUtils.toi(opcode)) {
				this.handlePrimitive(Integer.class);
			} else if (BytecodeUtils.tol(opcode)) {
				this.handlePrimitive(Long.class);
			} else if (BytecodeUtils.tof(opcode)) {
				this.handlePrimitive(Float.class);
			} else if (BytecodeUtils.tod(opcode)) {
				this.handlePrimitive(Double.class);
			} else if (opcode == Opcodes.I2B) {
				this.handlePrimitive(Byte.class);
			} else if (opcode == Opcodes.I2C) {
				this.handlePrimitive(Character.class);
			} else if (opcode == Opcodes.I2S) {
				this.handlePrimitive(Short.class);
			} else {
				logger.error("Invalid conversion bytecode: " + opcode);
				return ;
			}
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		} else if (opcode == Opcodes.POP || opcode == Opcodes.POP2) {
			this.mv.visitInsn(opcode);
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.ICONST_1);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"removeObjs", 
					"(I)V", 
					false);
		} else if (opcode == Opcodes.ARRAYLENGTH) {
			this.mv.visitInsn(opcode);
			
			this.handlePrimitive(Integer.class);
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.mv.visitInsn(Opcodes.ICONST_1);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerAndReplace", 
					"(Ljava/lang/Object;ZI)V", 
					false);
		} else if (BytecodeUtils.xreturn(opcode)) {
			//For ret, pull the val from inputs and store it in outputs
			if (opcode != Opcodes.RETURN) {
				this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						Type.getInternalName(IORecord.class), 
						"pullOutput", 
						"()V", 
						false);
			}
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(GlobalInfoRecorder.class), 
					"registerIO", 
					"(Ledu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
					false);
			
			this.mv.visitInsn(opcode);
		}
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		this.mv.visitLdcInsn(cst);
		
		if (!this.begin) {
			return ;
		}
		
		if (cst instanceof Handle) {
			//Don't record method handler
			return ;
		} else if (cst instanceof Float) {
			this.handlePrimitive(Float.class);
		} else if (cst instanceof Long) {
			this.handlePrimitive(Long.class);
		} else if (cst instanceof Double) {
			this.handlePrimitive(Double.class);
		} else if (cst instanceof String || cst instanceof Type) {
			this.mv.visitInsn(Opcodes.DUP);
		} else {
			logger.error("Unexpected obj type: ", cst);
			return ;
		}
		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
		this.mv.visitInsn(Opcodes.SWAP);
		this.mv.visitInsn(Opcodes.ICONST_0);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				Type.getInternalName(IORecord.class), 
				"registerInput", 
				"(Ljava/lang/Object;Z)V", 
				false);
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		this.mv.visitVarInsn(opcode, var);
		
		if (!this.begin) {
			return ;
		}
		
		if (BytecodeUtils.xload(opcode)) {			
			boolean ser = this.handleLoadStore(opcode);
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			if (ser) {
				this.mv.visitInsn(Opcodes.ICONST_1);
			} else {
				this.mv.visitInsn(Opcodes.ICONST_0);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		} else if (BytecodeUtils.xstore(opcode)) {
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"pullOutput", 
					"()V", 
					false);
		}
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		this.mv.visitFieldInsn(opcode, owner, name, desc);
		
		if (!this.begin) {
			return ;
		}
		
		if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
			Type fieldType = Type.getType(desc);
			boolean ser = this.handleType(fieldType);
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			if (ser) {
				this.mv.visitInsn(Opcodes.ICONST_1);
			} else {
				this.mv.visitInsn(Opcodes.ICONST_0);
			}
			
			if (opcode == Opcodes.GETSTATIC) {	
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						Type.getInternalName(IORecord.class), 
						"registerInput", 
						"(Ljava/lang/Object;Z)V", 
						false);
			} else {
				this.mv.visitInsn(Opcodes.ICONST_1);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						Type.getInternalName(IORecord.class), 
						"registerAndReplace", 
						"(Ljava/lang/Object;ZI)V", 
						false);
			}
		} else if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"pullOutput", 
					"()V", 
					false);
		}
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
		
		if (!this.begin) {
			owner = ClassInfoUtils.cleanType(owner);
			if (owner.equals(this.ownerClass) && name.equals(INIT)) {
				this.begin = true;
			} else if (owner.equals(this.superClass) && name.equals(INIT)) {
				this.begin = true;
			}
			
			/*if (this.begin) {
				logger.info("Capture init: " + owner + " " + name + " " + desc);
			}*/
			
			return ;
		}
		
		Type methodType = Type.getMethodType(desc);
		//Type[] args = methodType.getArgumentTypes();
		Type ret = methodType.getReturnType();
		if (ret.getSort() == Type.VOID) {
			return ;
		} else {
			boolean ser = this.handleType(ret);
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			
			if (ser) {
				this.mv.visitInsn(Opcodes.ICONST_1);
			} else {
				this.mv.visitInsn(Opcodes.ICONST_0);
			}
			
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		}
	}
	
	private void convertToInst(int num) {
		if (num == 1) {
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
	
	private boolean handleArr(int opcode) {
		boolean ser = false;
		if (opcode == Opcodes.IALOAD 
				|| opcode == Opcodes.IASTORE) {
			this.handlePrimitive(Integer.class);
		} else if (opcode == Opcodes.LALOAD 
				|| opcode == Opcodes.LASTORE) {
			this.handlePrimitive(Long.class);
		} else if (opcode == Opcodes.FALOAD 
				|| opcode == Opcodes.FASTORE) {
			this.handlePrimitive(Float.class);
		} else if (opcode == Opcodes.DALOAD 
				|| opcode == Opcodes.DASTORE) {
			this.handlePrimitive(Double.class);
		} else if (opcode == Opcodes.BALOAD 
				|| opcode == Opcodes.BASTORE) {
			this.handlePrimitive(Byte.class);
		} else if (opcode == Opcodes.CALOAD 
				|| opcode == Opcodes.CASTORE) {
			this.handlePrimitive(Character.class);
		} else if (opcode == Opcodes.SALOAD 
				|| opcode == Opcodes.SASTORE) {
			this.handlePrimitive(Short.class);
		} else if (opcode == Opcodes.AALOAD 
				|| opcode == Opcodes.AASTORE) {
			this.mv.visitInsn(Opcodes.DUP);
			ser = true;
		}
		return ser;
	}
	
	private boolean handleLoadStore(int opcode) {
		boolean ser = false;
		
		if (opcode == Opcodes.ILOAD 
				|| opcode == Opcodes.ISTORE) {
			this.handlePrimitive(Integer.class);
		} else if (opcode == Opcodes.FLOAD 
				|| opcode == Opcodes.FSTORE) {
			this.handlePrimitive(Float.class);
		} else if (opcode == Opcodes.LLOAD 
				|| opcode == Opcodes.LSTORE) {
			this.handlePrimitive(Long.class);
		} else if (opcode == Opcodes.DLOAD 
				|| opcode == Opcodes.DSTORE) {
			this.handlePrimitive(Double.class);
		} else if (opcode == Opcodes.ALOAD 
				|| opcode == Opcodes.ASTORE) {
			this.mv.visitInsn(Opcodes.DUP);
			ser = true;
		}
		
		return ser;
	}
	
	private boolean handleType(Type type) {
		boolean ser = false;
		int sort = type.getSort();
		if (sort == Type.INT) {
			this.handlePrimitive(Integer.class);
		} else if (sort == Type.SHORT) {
			this.handlePrimitive(Short.class);
		} else if (sort == Type.BYTE) {
			this.handlePrimitive(Byte.class);
		} else if (sort == Type.CHAR) {
			this.handlePrimitive(Character.class);
		} else if (sort == Type.BOOLEAN) {
			//Convert boolean to integer
			this.handlePrimitive(Integer.class);
		} else if (sort == Type.FLOAT) {
			this.handlePrimitive(Float.class);
		} else if (sort == Type.LONG) {
			this.handlePrimitive(Long.class);
		} else if (sort == Type.DOUBLE) {
			this.handlePrimitive(Double.class);
		} else if (type.equals(Type.getType(String.class))) {
			this.mv.visitInsn(Opcodes.DUP);
		} else if (sort == Type.ARRAY || sort == Type.OBJECT) {
			this.mv.visitInsn(Opcodes.DUP);
			ser = true;
		}
		return ser;
	}
	
	private void handlePrimitive(Class<?> primClass) {
		if (primClass.equals(Integer.class)) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(I)Ljava/lang/Integer;", 
					false);
		} else if (primClass.equals(Float.class)) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(F)Ljava/lang/Float;", 
					false);
		} else if (primClass.equals(Byte.class)) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(B)Ljava/lang/Byte;", 
					false);
		} else if (primClass.equals(Character.class)) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(C)Ljava/lang/Character;", 
					false);
		} else if (primClass.equals(Short.class)) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(S)Ljava/lang/Short;", 
					false);
		} else if (primClass.equals(Long.class)) {
			this.mv.visitInsn(Opcodes.DUP2);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(J)Ljava/lang/Long;", 
					false);
		} else if (primClass.equals(Double.class)) {
			this.mv.visitInsn(Opcodes.DUP2);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(D)Ljava/lang/Double;", 
					false);
		}
	}

}
