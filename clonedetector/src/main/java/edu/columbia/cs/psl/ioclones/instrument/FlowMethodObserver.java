package edu.columbia.cs.psl.ioclones.instrument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.ioclones.DependencyAnalyzer;
import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.utils.BytecodeUtils;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class FlowMethodObserver extends MethodVisitor implements Opcodes {
	
	private static Logger logger = LogManager.getLogger(FlowMethodObserver.class);
	
	private static final String VALUE_OF = "valueOf";
	
	private static final String INIT = "<init>";
	
	private String className;
	
	private String superName;
	
	private String name;
		
	private String desc;
	
	private String signature;
	
	private String[] exceptions;
	
	private String methodKey;
	
	private String returnType;
	
	private boolean begin = true;
	
	private LocalVariablesSorter lvs;
	
	private int recordId = -1;
	
	private boolean recordOutput = false;
	
	private boolean recordInput = false;
	
	public FlowMethodObserver(final MethodVisitor mv, 
			final String className,
			final String superName, 
			final String name,
			final String desc, 
			String signature, 
			String[] exceptions) {
		super(Opcodes.ASM5, mv);
		//super(Opcodes.ASM5, access, name, desc, signature, exceptions);
		this.className = className;
		this.superName = superName;
		this.name = name;
		this.desc = desc;
		this.signature = signature;
		this.exceptions = exceptions;
		String[] parsedKeys = 
				ClassInfoUtils.genMethodKey(this.className, this.name, this.desc);
		this.methodKey = parsedKeys[0];
		this.returnType = parsedKeys[1];
		
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
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
		
		if (!this.begin) {
			owner = ClassInfoUtils.cleanType(owner);
			if (owner.equals(this.className) && name.equals(INIT)) {
				this.begin = true;
			} else if (owner.equals(this.superName) && name.equals(INIT)) {
				this.begin = true;
			}
			
			return ;
		}
	}
	
	@Override
	public void visitInsn(int opcode) {
		if (!this.begin) {
			this.mv.visitInsn(opcode);
			return ;
		}
		
		if (this.recordInput) {
			this.mv.visitInsn(opcode);
			
			this.recordInput = false;
			boolean ser = false;
			switch(opcode) {
				case IALOAD:
					this.handlePrimitive(Integer.class);
					break ;
				case LALOAD:
					this.handlePrimitive(Long.class);
					break ;
				case FALOAD:
					this.handlePrimitive(Float.class);
					break ;
				case DALOAD:
					this.handlePrimitive(Double.class);
					break ;
				case AALOAD:
					ser = true;
					this.mv.visitInsn(DUP);
					break ;
				case BALOAD:
					this.handlePrimitive(Byte.class);
					break ;
				case CALOAD:
					this.handlePrimitive(Character.class);
					break ;
				case SALOAD:
					this.handlePrimitive(Short.class);
					break ;
				default:
					logger.error("Invalid input type: " + opcode);
					System.exit(-1);
			}
			
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitInsn(SWAP);
			if (!ser) {
				this.mv.visitInsn(ICONST_0);
			} else {
				this.mv.visitInsn(ICONST_1);
			}
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		} else if (this.recordOutput) {
			this.recordOutput = false;
			boolean ser = false;
			switch(opcode) {
				case IASTORE:
				case IRETURN:
					this.handlePrimitive(Integer.class);
					break ;
				case LASTORE:
				case LRETURN:
					this.handlePrimitive(Long.class);
					break ;
				case FASTORE:
				case FRETURN:
					this.handlePrimitive(Float.class);
					break ;
				case DASTORE:
				case DRETURN:
					this.handlePrimitive(Double.class);
					break ;
				case AASTORE:
				case ARETURN:
					ser = true;
					this.mv.visitInsn(DUP);
					break ;
				case BASTORE:
					this.handlePrimitive(Byte.class);
					break ;
				case CASTORE:
					this.handlePrimitive(Character.class);
					break ;
				case SASTORE:
					this.handlePrimitive(Short.class);
					break ;
				default:
					logger.error("Invalid output type: " + opcode);
					System.exit(-1);
			}
			
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitInsn(SWAP);
			if (!ser) {
				this.mv.visitInsn(ICONST_0);
			} else {
				this.mv.visitInsn(ICONST_1);
			}
			
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerOutput", 
					"(Ljava/lang/Object;Z)V", 
					false);
			
			if (BytecodeUtils.xreturn(opcode)) {
				this.mv.visitVarInsn(ALOAD, this.recordId);
				this.mv.visitMethodInsn(INVOKESTATIC, 
						Type.getInternalName(GlobalInfoRecorder.class), 
						"registerIO", 
						"(Ledu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
						false);
			}
			
			this.mv.visitInsn(opcode);
		} else {
			this.mv.visitInsn(opcode);
		}
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		this.mv.visitIntInsn(opcode, operand);
		if (this.recordInput) {
			this.recordInput = false;
			
			//Do we need to record empty array?
			if (opcode == NEWARRAY) {
				this.recordInput = false;
			}
		}
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		this.mv.visitVarInsn(opcode, var);
		if (this.recordInput) {
			this.recordInput = false;
			
			boolean ser = false;
			switch(opcode) {
				case ILOAD:
					this.handlePrimitive(Integer.class);
					break ;
				case LLOAD:
					this.handlePrimitive(Long.class);
					break ;
				case FLOAD:
					this.handlePrimitive(Float.class);
					break ;
				case DLOAD:
					this.handlePrimitive(Double.class);
					break ;
				case ALOAD:
					ser = true;
					this.mv.visitInsn(DUP);
					break ;
				default:
					logger.error("Invalid inptu type: " + opcode + " " + var);
					System.exit(-1);
			}
			
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitInsn(SWAP);
			if (!ser) {
				this.mv.visitInsn(ICONST_0);
			} else {
				this.mv.visitInsn(ICONST_1);
			}
			
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerOutput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		} else if (this.recordOutput) {
			//Weird if we touch here
			this.recordOutput = false;
			logger.error("Invalid output type: " + opcode + " " + var);
		}
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		this.mv.visitTypeInsn(opcode, type);
		if (this.recordInput) {
			if (opcode == NEW) {
				//Wait until invokespecial
				this.mv.visitInsn(DUP);
			} else if (opcode == ANEWARRAY) {
				//Do we need to record empty array...
				this.recordInput = false;
				
				this.mv.visitInsn(DUP);
				this.mv.visitVarInsn(ALOAD, this.recordId);
				this.mv.visitInsn(SWAP);
				this.mv.visitInsn(ICONST_1);
				
				this.mv.visitMethodInsn(INVOKEVIRTUAL, 
						Type.getInternalName(IORecord.class), 
						"registerOutput", 
						"(Ljava/lang/Object;Z)V", 
						false);
			}
		}
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		if (cst instanceof String) {
			String literal = (String) cst;
			if (literal.equals(DependencyAnalyzer.INPUT_MSG)) {
				this.recordInput = true;
			} else if (literal.equals(DependencyAnalyzer.OUTPUT_MSG)) {
				this.recordOutput = true;
			} else {
				this.mv.visitLdcInsn(cst);
			}
		} else {
			this.mv.visitLdcInsn(cst);
		}
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
