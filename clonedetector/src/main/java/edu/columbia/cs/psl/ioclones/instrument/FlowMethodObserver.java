package edu.columbia.cs.psl.ioclones.instrument;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ioclones.DependencyAnalyzer;
import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.pojo.ParamInfo;
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
	
	private List<ParamInfo> paramInfos;
	
	private String signature;
	
	private String[] exceptions;
	
	private String methodKey;
	
	private String returnType;
	
	private boolean isStatic;
	
	//private boolean begin = true;
	
	private LocalVariablesSorter lvs;
	
	private int recordId = -1;
	
	private boolean recordOutput = false;
	
	private InputSig recordInput = null;
	
	private boolean thisWritten = false;
	
	private Stack<String> newTypes = new Stack<String>();
	
	//private int copySignal = -1;
	
	public FlowMethodObserver(final MethodVisitor mv, 
			final String className,
			final String superName, 
			final String name,
			final String desc, 
			final List<ParamInfo> paramInfos, 
			String signature, 
			String[] exceptions, 
			boolean isStatic) {
		super(Opcodes.ASM5, mv);
		//super(Opcodes.ASM5, access, name, desc, signature, exceptions);
		this.className = className;
		this.superName = superName;
		this.name = name;
		this.desc = desc;
		this.paramInfos = paramInfos;
		this.signature = signature;
		this.exceptions = exceptions;
		this.isStatic = isStatic;
		String[] parsedKeys = 
				ClassInfoUtils.genMethodKey(this.className, this.name, this.desc);
		this.methodKey = parsedKeys[0];
		this.returnType = parsedKeys[1];
		
		/*if (name.equals("<init>")) {
			this.begin = false;
		}*/
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
		
		int start = 0;
		if (!isStatic) {
			start = 1;
		}
		
		for (; start < this.paramInfos.size(); start++) {
			ParamInfo pi = this.paramInfos.get(start);
			int paramId = pi.runtimeIdx;
			Type paramType = pi.paramType;
			int paramSort = paramType.getSort();
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.convertToInst(paramId);
			
			switch (paramSort) {
				case Type.INT:
					this.mv.visitVarInsn(Opcodes.ILOAD, paramId);
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
							Type.getInternalName(Integer.class), 
							VALUE_OF, 
							"(I)Ljava/lang/Integer;", 
							false);
					break ;
				case Type.SHORT:
					this.mv.visitVarInsn(Opcodes.ILOAD, paramId);
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
							Type.getInternalName(Short.class), 
							VALUE_OF, 
							"(S)Ljava/lang/Short;", 
							false);
					break ;
				case Type.BOOLEAN:
					this.mv.visitVarInsn(Opcodes.ILOAD, paramId);
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
							Type.getInternalName(Boolean.class), 
							VALUE_OF, 
							"(Z)Ljava/lang/Boolean;", 
							false);
					break ;
				case Type.BYTE:
					this.mv.visitVarInsn(Opcodes.ILOAD, paramId);
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
							Type.getInternalName(Byte.class), 
							VALUE_OF, 
							"(B)Ljava/lang/Byte;", 
							false);
					break ;
				case Type.CHAR:
					this.mv.visitVarInsn(Opcodes.ILOAD, paramId);
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
							Type.getInternalName(Character.class), 
							VALUE_OF, 
							"(C)Ljava/lang/Character;", 
							false);
					break ;
				case Type.LONG:
					this.mv.visitVarInsn(Opcodes.LLOAD, paramId);
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
							Type.getInternalName(Long.class), 
							VALUE_OF, 
							"(J)Ljava/lang/Long;", 
							false);
					break ;
				case Type.FLOAT:
					this.mv.visitVarInsn(Opcodes.FLOAD, paramId);
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
							Type.getInternalName(Float.class), 
							VALUE_OF, 
							"(F)Ljava/lang/Float;", 
							false);
					break ;
				case Type.DOUBLE:
					this.mv.visitVarInsn(Opcodes.DLOAD, paramId);
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
							Type.getInternalName(Double.class), 
							VALUE_OF, 
							"(D)Ljava/lang/Double;", 
							false);
					break ;
				case Type.OBJECT:
				case Type.ARRAY:
					this.mv.visitVarInsn(Opcodes.ALOAD, paramId);
					break ;
				default:
					logger.error("Invalid data tyep: " + paramType.getClassName());
			}
			
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					ioRecordType.getInternalName(), 
					"preload", 
					"(ILjava/lang/Object;)V", 
					false);
		}
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		this.mv.visitTypeInsn(opcode, type);
		if (opcode == Opcodes.NEW) {
			this.mv.visitInsn(Opcodes.DUP);
			this.newTypes.push(type);
		} else if (opcode == Opcodes.ANEWARRAY) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"blackObject", 
					"(Ljava/lang/Object;)V", 
					false);
		}
	}
	
	@Override
	public void visitInsn(int opcode) {
		/*if (!this.begin) {
			this.mv.visitInsn(opcode);
			return ;
		}*/
		if (this.recordInput != null) {
			this.mv.visitInsn(opcode);
			
			this.recordInput = null;
			//System.out.println("End record input: " + opcode);
			boolean ser = false;
			switch(opcode) {
				case IALOAD:
					this.handlePrimitive(Integer.class, true);
					break ;
				case LALOAD:
					this.handlePrimitive(Long.class, true);
					break ;
				case FALOAD:
					this.handlePrimitive(Float.class, true);
					break ;
				case DALOAD:
					this.handlePrimitive(Double.class, true);
					break ;
				case AALOAD:
					ser = true;
					this.mv.visitInsn(DUP);
					break ;
				case BALOAD:
					this.handlePrimitive(Byte.class, true);
					break ;
				case CALOAD:
					this.handlePrimitive(Character.class, true);
					break ;
				case SALOAD:
					this.handlePrimitive(Short.class, true);
					break ;
				case ARRAYLENGTH:
					this.handlePrimitive(Integer.class, true);
					break ;
				default:
					logger.error("Invalid input type: " + opcode);
					//System.exit(-1);
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
					"registerValueFromInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
			
			return ;
		} else if (this.recordOutput) {
			this.recordOutput = false;
			boolean ser = false;
			switch(opcode) {
				case IASTORE:
				case IRETURN:
					this.handlePrimitive(Integer.class, true);
					break ;
				case LASTORE:
				case LRETURN:
					this.handlePrimitive(Long.class, true);
					break ;
				case FASTORE:
				case FRETURN:
					this.handlePrimitive(Float.class, true);
					break ;
				case DASTORE:
				case DRETURN:
					this.handlePrimitive(Double.class, true);
					break ;
				case AASTORE:
				case ARETURN:
					ser = true;
					this.mv.visitInsn(DUP);
					break ;
				case BASTORE:
					//No matter byte ore boolean, goes to byte
					this.handlePrimitive(Byte.class, true);
					break ;
				case CASTORE:
					this.handlePrimitive(Character.class, true);
					break ;
				case SASTORE:
					this.handlePrimitive(Short.class, true);
					break ;
				default:
					logger.error("Invalid output type: " + opcode);
					//System.exit(-1);
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
		}
		
		if (BytecodeUtils.xreturn(opcode)) {
			if (this.thisWritten) {
				this.mv.visitVarInsn(ALOAD, this.recordId);
				this.mv.visitVarInsn(ALOAD, 0);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						Type.getInternalName(IORecord.class), 
						"queueInWritten", 
						"(Ljava/lang/Object;)V", 
						false);
			}
			
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"summarizeWrittens", 
					"()V", 
					false);
			
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
	public void visitIntInsn(int opcode, int operand) {
		//System.out.println(opcode);
		this.mv.visitIntInsn(opcode, operand);
		if (opcode == Opcodes.NEWARRAY) {
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.mv.visitInsn(Opcodes.SWAP);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"blackObject", 
					"(Ljava/lang/Object;)V", 
					false);
		}
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		//System.out.println(opcode);
		this.mv.visitVarInsn(opcode, var);
		if (this.recordInput != null) {
			this.recordInput = null;
			
			boolean ser = false;
			switch(opcode) {
				case ILOAD:
					this.handlePrimitive(Integer.class, true);
					break ;
				case LLOAD:
					this.handlePrimitive(Long.class, true);
					break ;
				case FLOAD:
					this.handlePrimitive(Float.class, true);
					break ;
				case DLOAD:
					this.handlePrimitive(Double.class, true);
					break ;
				case ALOAD:
					ser = true;
					this.mv.visitInsn(DUP);
					break ;
				default:
					logger.error("Invalid input type: " + opcode + " " + var);
					//System.exit(-1);
			}
			
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitInsn(SWAP);
			this.convertToInst(var);
			
			if (!ser) {
				this.mv.visitInsn(ICONST_0);
			} else {
				this.mv.visitInsn(ICONST_1);
			}
			//this.convertToInst(var);
			
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;IZ)V", 
					false);
			return ;
		}
		
		if (opcode == ISTORE 
				|| opcode == FSTORE 
				|| opcode == LSTORE 
				|| opcode == DSTORE) {
			ParamInfo checkParam = this.checkParam(var);
			if (checkParam != null) {
				this.mv.visitVarInsn(ALOAD, this.recordId);
				this.convertToInst(var);
				
				if (opcode == ISTORE) {
					this.mv.visitVarInsn(ILOAD, var);
					this.handlePrimitive(Integer.class, false);
				} else if (opcode == FSTORE) {
					this.mv.visitVarInsn(FLOAD, var);
					this.handlePrimitive(Float.class, false);
				} else if (opcode == LSTORE) {
					this.mv.visitVarInsn(LLOAD, var);
					this.handlePrimitive(Long.class, false);
				} else if (opcode == DSTORE) {
					this.mv.visitVarInsn(DLOAD, var);
					this.handlePrimitive(Double.class, false);
				}
				
				this.mv.visitMethodInsn(INVOKEVIRTUAL, 
						Type.getInternalName(IORecord.class), 
						"attemptStopParam", 
						"(ILjava/lang/Object;)V", 
						false);
			}
		}
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		//System.out.println(opcode);
		if (this.recordInput != null) {
			this.mv.visitFieldInsn(opcode, owner, name, desc);
			
			this.recordInput = null;
			Type type = Type.getType(desc);
			boolean ser = this.handleType(type);
			
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitInsn(SWAP);
			
			if (!ser) {
				this.mv.visitInsn(ICONST_0);
			} else {
				this.mv.visitInsn(ICONST_1);
			}
			
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerValueFromInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		} else if (this.recordOutput){
			this.recordOutput = false;
			
			Type type = Type.getType(desc);
			boolean ser = this.handleType(type);
			
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
			
			this.mv.visitFieldInsn(opcode, owner, name, desc);
		} else {
			this.mv.visitFieldInsn(opcode, owner, name, desc);
		}
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		//System.out.println(opcode);
		if (this.recordInput != null) {
			int copySignal = recordInput.msg;
			this.recordInput = null;
			
			switch(opcode) {
				case IFEQ:
				case IFNE:
				case IFLT:
				case IFGE:
				case IFGT:
				case IFLE:
					this.handlePrimitive(Integer.class, true);
					this.mv.visitVarInsn(ALOAD, this.recordId);
					this.mv.visitInsn(SWAP);
					this.mv.visitInsn(ICONST_0);
					this.mv.visitMethodInsn(INVOKEVIRTUAL, 
							Type.getInternalName(IORecord.class), 
							"registerInput", 
							"(Ljava/lang/Object;Z)V", 
							false);
					break ;
				case IFNULL:
				case IFNONNULL:
					this.mv.visitInsn(Opcodes.DUP);
					this.mv.visitVarInsn(ALOAD, this.recordId);
					this.mv.visitInsn(SWAP);
					this.mv.visitInsn(ICONST_1);
					this.mv.visitMethodInsn(INVOKEVIRTUAL, 
							Type.getInternalName(IORecord.class), 
							"registerInput", 
							"(Ljava/lang/Object;Z)V", 
							false);
					break ;
				case IF_ICMPEQ:
				case IF_ICMPNE:
				case IF_ICMPLT:
				case IF_ICMPGE:
				case IF_ICMPGT:
				case IF_ICMPLE:
					this.mv.visitInsn(Opcodes.DUP2);
					if (copySignal == 2) {
						for (int i = 0; i < 2; i++) {
							this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
									Type.getInternalName(Integer.class), 
									VALUE_OF, 
									"(I)Ljava/lang/Integer;", 
									false);
							this.mv.visitVarInsn(ALOAD, this.recordId);
							this.mv.visitInsn(SWAP);
							this.mv.visitInsn(ICONST_0);
							this.mv.visitMethodInsn(INVOKEVIRTUAL, 
									Type.getInternalName(IORecord.class), 
									"registerInput", 
									"(Ljava/lang/Object;Z)V", 
									false);
						}
					} else if (copySignal == 0) {
						//Record the one under the top
						this.mv.visitInsn(Opcodes.POP);
						this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
								Type.getInternalName(Integer.class), 
								VALUE_OF, 
								"(I)Ljava/lang/Integer;", 
								false);
						this.mv.visitVarInsn(ALOAD, this.recordId);
						this.mv.visitInsn(SWAP);
						this.mv.visitInsn(ICONST_0);
						this.mv.visitMethodInsn(INVOKEVIRTUAL, 
								Type.getInternalName(IORecord.class), 
								"registerInput", 
								"(Ljava/lang/Object;Z)V", 
								false);
					} else if (copySignal == 1) {
						//Record the top
						this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
								Type.getInternalName(Integer.class), 
								VALUE_OF, 
								"(I)Ljava/lang/Integer;", 
								false);
						this.mv.visitVarInsn(ALOAD, this.recordId);
						this.mv.visitInsn(SWAP);
						this.mv.visitInsn(ICONST_0);
						this.mv.visitMethodInsn(INVOKEVIRTUAL, 
								Type.getInternalName(IORecord.class), 
								"registerInput", 
								"(Ljava/lang/Object;Z)V", 
								false);
						this.mv.visitInsn(Opcodes.POP);
					} else {
						logger.info("Invalid copy signal: " + copySignal);
					}
					break ;
			}
		}
		
		this.mv.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		//System.out.println(LOOKUPSWITCH);
		if (this.recordInput != null) {
			this.recordInput = null;
			
			this.handlePrimitive(Integer.class, true);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitInsn(SWAP);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		}
		this.mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		//System.out.println(TABLESWITCH);
		if (this.recordInput != null) {
			this.recordInput = null;
			
			this.handlePrimitive(Integer.class, true);
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitInsn(SWAP);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;Z)V", 
					false);
		}
		this.mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
		
	@Override
	public void visitLdcInsn(Object cst) {
		//System.out.println(LDC);
		if (cst instanceof String) {
			String literal = (String) cst;
			if (literal.equals(DependencyAnalyzer.INPUT_MSG)) {
				InputSig is = new InputSig();
				this.recordInput = is;
			} else if (literal.equals(DependencyAnalyzer.INPUT_CHECK_MSG)) {				
				this.mv.visitInsn(Opcodes.DUP);
				this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
				this.mv.visitInsn(Opcodes.SWAP);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						Type.getInternalName(IORecord.class), 
						"probeOwner", 
						"(Ljava/lang/Object;)V", 
						false);
			} else if (literal.equals(DependencyAnalyzer.INPUT_COPY_0_MSG)) {
				InputSig is = new InputSig();
				is.msg = 0;
				this.recordInput = is;
				//this.copySignal = 0;
			} else if (literal.equals(DependencyAnalyzer.INPUT_COPY_1_MSG)) {
				InputSig is = new InputSig();
				is.msg = 1;
				this.recordInput = is;
				//this.copySignal = 1;
			} else if (literal.equals(DependencyAnalyzer.INPUT_COPY_2_MSG)) {
				InputSig is = new InputSig();
				is.msg = 2;
				this.recordInput = is;
				//this.copySignal = 2;
			} else if (literal.equals(DependencyAnalyzer.OUTPUT_MSG)) {
				this.recordOutput = true;
			} else if (literal.equals(DependencyAnalyzer.TAINTED_STATIC)) {
				this.mv.visitInsn(Opcodes.DUP);
				this.mv.visitVarInsn(ALOAD, this.recordId);
				this.mv.visitInsn(Opcodes.SWAP);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						Type.getInternalName(IORecord.class), 
						"queueInWritten", 
						"(Ljava/lang/Object;)V", 
						false);
			} else if (literal.startsWith(DependencyAnalyzer.TAINTED_IN)) {
				String allId = literal.split("@")[1];
				String[] idStrings = allId.split("-");
				
				for (int i = 0; i < idStrings.length; i++) {
					String idString = idStrings[i];
					int id = Integer.valueOf(idString);
					if (!this.isStatic && id == 0) {
						this.thisWritten = true;
						continue ;
					}
					
					this.mv.visitVarInsn(ALOAD, this.recordId);
					this.mv.visitVarInsn(ALOAD, id);
					this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
							Type.getInternalName(IORecord.class), 
							"queueInWritten", 
							"(Ljava/lang/Object;)V", 
							false);
				}
			} else {
				this.mv.visitLdcInsn(cst);
			}
		} else {
			this.mv.visitLdcInsn(cst);
		}
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		if (this.recordInput != null) {
			this.recordInput = null;
			
			//Record input and then stop the recording of this var
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitVarInsn(ILOAD, var);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(Integer.class), 
					VALUE_OF, 
					"(I)Ljava/lang/Integer;", 
					false);
			this.convertToInst(var);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"registerInput", 
					"(Ljava/lang/Object;IZ)V", 
					false);
		}
		
		if (this.checkParam(var) != null) {
			this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
			this.convertToInst(var);
			this.mv.visitInsn(Opcodes.ACONST_NULL);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, 
					Type.getInternalName(IORecord.class), 
					"attemptStopParam", 
					"(ILjava/lang/Object;)V", 
					false);
			
		}
		this.mv.visitIincInsn(var, increment);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		this.mv.visitMultiANewArrayInsn(desc, dims);
		
		this.mv.visitInsn(Opcodes.DUP);
		this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
		this.mv.visitInsn(Opcodes.SWAP);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				Type.getInternalName(IORecord.class), 
				"blackObject", 
				"(Ljava/lang/Object;)V", 
				false);
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
		if (this.newTypes.size() > 0) {
			if (name.equals("<init>")) {
				String curType = this.newTypes.pop();
				if (!curType.equals(owner)) {
					logger.error("Suspicious new session: " + curType + " " + owner);
				} else {
					this.mv.visitVarInsn(Opcodes.ALOAD, this.recordId);
					this.mv.visitInsn(Opcodes.SWAP);
					this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
							Type.getInternalName(IORecord.class), 
							"blackObject", 
							"(Ljava/lang/Object;)V", 
							false);
				}
			}
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
		
	private boolean handleType(Type type) {
		boolean ser = false;
		int sort = type.getSort();
		if (sort == Type.INT) {
			this.handlePrimitive(Integer.class, true);
		} else if (sort == Type.SHORT) {
			this.handlePrimitive(Short.class, true);
		} else if (sort == Type.BYTE) {
			this.handlePrimitive(Byte.class, true);
		} else if (sort == Type.CHAR) {
			this.handlePrimitive(Character.class, true);
		} else if (sort == Type.BOOLEAN) {
			//Convert boolean to byte, because of BALOAD, BASTORE
			this.handlePrimitive(Byte.class, true);
			//this.handlePrimitive(Boolean.class);
		} else if (sort == Type.FLOAT) {
			this.handlePrimitive(Float.class, true);
		} else if (sort == Type.LONG) {
			this.handlePrimitive(Long.class, true);
		} else if (sort == Type.DOUBLE) {
			this.handlePrimitive(Double.class, true);
		} else if (type.equals(Type.getType(String.class))) {
			this.mv.visitInsn(Opcodes.DUP);
		} else if (sort == Type.ARRAY || sort == Type.OBJECT) {
			this.mv.visitInsn(Opcodes.DUP);
			ser = true;
		}
		return ser;
	}
	
	private void handlePrimitive(Class<?> primClass, boolean dup) {
		if (primClass.equals(Integer.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(I)Ljava/lang/Integer;", 
					false);
		} else if (primClass.equals(Float.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(F)Ljava/lang/Float;", 
					false);
		} else if (primClass.equals(Boolean.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(Z)Ljava/lang/Boolean;", 
					false);
		} else if (primClass.equals(Byte.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(B)Ljava/lang/Byte;", 
					false);
		} else if (primClass.equals(Character.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(C)Ljava/lang/Character;", 
					false);
		} else if (primClass.equals(Short.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(S)Ljava/lang/Short;", 
					false);
		} else if (primClass.equals(Boolean.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(Z)Ljava/lang/Boolean;", 
					false);
		} else if (primClass.equals(Long.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP2);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(J)Ljava/lang/Long;", 
					false);
		} else if (primClass.equals(Double.class)) {
			if (dup) {
				this.mv.visitInsn(Opcodes.DUP2);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(primClass), 
					VALUE_OF, 
					"(D)Ljava/lang/Double;", 
					false);
		}
	}
	
	public ParamInfo checkParam(int varId) {
		for (ParamInfo pi: this.paramInfos) {
			if (pi.runtimeIdx == varId) {
				return pi;
			}
		}
		
		return null;
	}
	
	public static class InputSig {
		int msg = -1;
	}
}
