package edu.columbia.cs.psl.ioclones.instrument;

import java.io.File;
import java.util.HashSet;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.google.gson.reflect.TypeToken;

import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintChecker;
import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintPropagater;
import edu.columbia.cs.psl.ioclones.config.IOCloneConfig;
import edu.columbia.cs.psl.ioclones.driver.IOHelper;
import edu.columbia.cs.psl.ioclones.instrument.FlowMethodObserver.WriterFlow;
import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.utils.BytecodeUtils;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class DynFlowObserver extends MethodVisitor implements Opcodes {
	
	public static final int DEPTH = IOCloneConfig.getInstance().getDepth();
	
	public static int MAIN_COUNTER = 0;
	
	private static HashSet<String> WRITERS;
	
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
	
	static {
		File writerFile = new File("./config/writers.json");
		if (!writerFile.exists()) {
			WRITERS = new HashSet<String>();
			System.out.println("Find no writer file: " + writerFile.getAbsolutePath());
		} else {
			TypeToken<HashSet<String>> token = new TypeToken<HashSet<String>>(){};
			WRITERS = IOUtils.readJson(writerFile, token);
			
			if (WRITERS == null) {
				WRITERS = new HashSet<String>();
				System.out.println("Emptyp writer file");
			} else {
				System.out.println("Total writers: " + WRITERS.size());
			}
		}
	}
	
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
		//String propagater = Type.getInternalName(HitoTaintPropagater.class);
		if (arg.getSort() == Type.INT) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.propagateTaint(arg, false);
			this.mv.visitVarInsn(ISTORE, argIdx);
		} else if (arg.getSort() == Type.SHORT) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.propagateTaint(arg, false);
			this.mv.visitVarInsn(ISTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.BYTE) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.propagateTaint(arg, false);
			this.mv.visitVarInsn(ISTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.BOOLEAN) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.propagateTaint(arg, false);
			this.mv.visitVarInsn(ISTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.CHAR) {
			this.mv.visitVarInsn(ILOAD, argIdx);
			this.propagateTaint(arg, false);
			this.mv.visitVarInsn(ISTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.FLOAT) {
			this.mv.visitVarInsn(FLOAD, argIdx);
			this.propagateTaint(arg, false);
			this.mv.visitVarInsn(FSTORE, argIdx);
			argIdx++;
		} else if (arg.getSort() == Type.DOUBLE) {
			this.mv.visitVarInsn(DLOAD, argIdx);
			this.propagateTaint(arg, false);
			this.mv.visitVarInsn(DSTORE, argIdx);
			argIdx += 2;
		} else if (arg.getSort() == Type.LONG) {
			this.mv.visitVarInsn(LLOAD, argIdx);
			this.propagateTaint(arg, false);
			this.mv.visitVarInsn(LSTORE, argIdx);
			argIdx += 2;
		} else if (arg.equals(Type.getType(String.class))) {
			this.mv.visitVarInsn(ALOAD, argIdx);
			this.propagateTaint(arg, false);
			argIdx++;
		} else if (arg.getSort() == Type.OBJECT || arg.getSort() == Type.ARRAY) {
			this.mv.visitVarInsn(ALOAD, argIdx);
			this.propagateTaint(arg, false);
			
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
		
		boolean isPublic = ClassInfoUtils.checkAccess(this.access, Opcodes.ACC_PUBLIC);
		if (isPublic 
				&& this.isStatic 
				&& this.name.equals("main") 
				&& desc.equals("([Ljava/lang/String;)V")) {
			//System.out.println("Main method: " + this.className);
			MAIN_COUNTER++;
			this.mv.visitLdcInsn(this.className);
			this.mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(IOHelper.class), "init", "(Ljava/lang/String;)V", false);
		}
		
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
				analyzeTaint(retType);
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
		if (!name.equals("<init>")) {
			String taintChecker = Type.getInternalName(HitoTaintChecker.class);
			
			String replace = owner.replace("/", ".");
			if (WRITERS.contains(replace)) {
				Type[] args = Type.getArgumentTypes(desc);
				WriterFlow[] infos = new WriterFlow[args.length];
				
				for (int i = args.length - 1; i >= 0; i--) {
					Type curType = args[i];
					int curLocal = this.lvs.newLocal(curType);
					int curSort = curType.getSort();
					if (curSort == Type.OBJECT || curSort == Type.ARRAY) {
						this.mv.visitVarInsn(ASTORE, curLocal);
					} else if (curSort == Type.FLOAT) {
						this.mv.visitVarInsn(FSTORE, curLocal);
					} else if (curSort == Type.LONG) {
						this.mv.visitVarInsn(LSTORE, curLocal);
					} else if (curSort == Type.DOUBLE) {
						this.mv.visitVarInsn(DSTORE, curLocal);
					} else {
						this.mv.visitVarInsn(ISTORE, curLocal);
					}
					WriterFlow wf = new WriterFlow();
					wf.newVar = curLocal;
					wf.dataType = curType;
					
					infos[i] = wf;
				}
				
				for (int i = 0; i < infos.length; i++) {
					WriterFlow wf = infos[i];
					Type dataType = wf.dataType;
					int sort = dataType.getSort();
					
					if (sort == Type.OBJECT || sort == Type.ARRAY) {
						this.mv.visitVarInsn(ALOAD, wf.newVar);
					} else if (sort == Type.LONG) {
						this.mv.visitVarInsn(LLOAD, wf.newVar);
					} else if (sort == Type.DOUBLE) {
						this.mv.visitVarInsn(DLOAD, wf.newVar);
					} else if (sort == Type.FLOAT) {
						this.mv.visitVarInsn(FLOAD, wf.newVar);
					} else {
						this.mv.visitVarInsn(ILOAD, wf.newVar);
					}
					
					if (sort == Type.LONG || sort == Type.DOUBLE) {
						this.mv.visitInsn(DUP2);
					} else {
						this.mv.visitInsn(DUP);
					}
					this.mv.visitVarInsn(ALOAD, this.recordId);
					
					if (sort == Type.OBJECT || sort == Type.ARRAY) { //pass
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(Ljava/lang/Object;Ledu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else if (sort == Type.INT) { //pass
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(ILedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else if (sort == Type.SHORT) { //pass
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(JLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else if (sort == Type.CHAR) { //pass
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(CLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else if (sort == Type.BOOLEAN) { //pass
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(ZLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else if (sort == Type.BYTE) { //pass
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(BLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else if (sort == Type.FLOAT) { //fail
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(FLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else if (sort == Type.LONG) { //fail
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(JLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else if (sort == Type.DOUBLE) { //fail
						this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, 
								"recordWriter", "(DLedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", false);
					} else {
						System.err.println("Un-recognized data type: " + dataType);
						System.exit(-1);
					}
				}
			}
		}
		
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
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {		
		switch(opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
				this.mv.visitInsn(DUP);
				this.mv.visitVarInsn(ALOAD, this.recordId);
				this.mv.visitMethodInsn(INVOKESTATIC, 
						Type.getInternalName(HitoTaintChecker.class), 
						"recordControl", 
						"(ILedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
						false);
				break ;
			case IFNULL:
			case IFNONNULL:
				this.mv.visitInsn(DUP);
				this.mv.visitVarInsn(ALOAD, this.recordId);
				this.mv.visitMethodInsn(INVOKESTATIC, 
					Type.getInternalName(HitoTaintChecker.class), 
					"recordControl", 
					"(Ljava/lang/Object;Ledu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
					false);
				break ;
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
				this.mv.visitInsn(Opcodes.DUP2);
				for (int i = 0; i < 2; i++) {
					this.mv.visitVarInsn(ALOAD, this.recordId);
					this.mv.visitMethodInsn(INVOKESTATIC, 
							Type.getInternalName(HitoTaintChecker.class), 
							"recordControl", 
							"(ILedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
							false);
				}
				break ;
			case IF_ACMPEQ:
			case IF_ACMPNE:
				this.mv.visitInsn(Opcodes.DUP2);
				for (int i = 0; i < 2; i++) {
					this.mv.visitVarInsn(ALOAD, this.recordId);
					this.mv.visitMethodInsn(INVOKESTATIC, 
						Type.getInternalName(HitoTaintChecker.class), 
						"recordControl", 
						"(Ljava/lang/Object;Ledu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
						false);
				}
				break ;
			case GOTO:
			case JSR:
				break ;
			default:
				System.err.println("Unhandled jump: " + opcode);
		}
		this.mv.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		this.mv.visitInsn(DUP);
		this.mv.visitVarInsn(ALOAD, this.recordId);
		this.mv.visitMethodInsn(INVOKESTATIC, 
				Type.getInternalName(HitoTaintChecker.class), 
				"recordControl", 
				"(ILedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
				false);
		
		this.mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		this.mv.visitInsn(DUP);
		this.mv.visitVarInsn(ALOAD, this.recordId);
		this.mv.visitMethodInsn(INVOKESTATIC, 
				Type.getInternalName(HitoTaintChecker.class), 
				"recordControl", 
				"(ILedu/columbia/cs/psl/ioclones/pojo/IORecord;)V", 
				false);
		
		this.mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (opcode == GETSTATIC) {
			this.mv.visitFieldInsn(opcode, owner, name, desc);
			
			Type type = Type.getType(desc);
			
			if (type.getSort() == Type.OBJECT) {
				String internal = type.getInternalName();
				String replace = internal.replace("/", ".");
				if (WRITERS.contains(replace)) {
					return ;
				}
			}
			
			int sort = type.getSort();			
			this.propagateTaint(type, true);
			if (sort != Type.OBJECT && sort != Type.ARRAY) {
				//put back tainted primitives
				this.mv.visitFieldInsn(PUTSTATIC, owner, name, desc);
			}
			
			this.mv.visitFieldInsn(opcode, owner, name, desc);
		} else if (opcode == PUTSTATIC) {
			Type type = Type.getType(desc);	
			
			if (type.getSort() == Type.OBJECT) {
				String internal = type.getInternalName();
				String replace = internal.replace("/", ".");
				if (WRITERS.contains(replace)) {
					this.mv.visitFieldInsn(opcode, owner, name, desc);
					return ;
				}
			}
			
			this.analyzeTaint(type);
			this.mv.visitFieldInsn(opcode, owner, name, desc);
		} else {
			this.mv.visitFieldInsn(opcode, owner, name, desc);
		}
	}
	
	public void analyzeTaint(Type type) {
		String taintChecker = Type.getInternalName(HitoTaintChecker.class);
		int sort = type.getSort();
		
		if (sort == Type.LONG || sort == Type.DOUBLE) {
			this.mv.visitInsn(DUP2);
		} else {
			this.mv.visitInsn(DUP);
		}
		this.mv.visitVarInsn(ALOAD, this.recordId);
		
		if (sort == Type.INT) {
			this.mv.visitInsn(ICONST_1);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(ILedu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
		} else if (sort == Type.SHORT) {
			this.mv.visitInsn(ICONST_1);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(SLedu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
		} else if (sort == Type.BOOLEAN) {
			this.mv.visitInsn(ICONST_1);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(ZLedu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
		} else if (sort == Type.BYTE) {
			this.mv.visitInsn(ICONST_1);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(BLedu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
		} else if (sort == Type.CHAR) {
			this.mv.visitInsn(ICONST_1);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(CLedu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
		} else if (sort == Type.FLOAT) {
			this.mv.visitInsn(ICONST_1);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(FLedu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
		} else if (sort == Type.LONG) {
			this.mv.visitInsn(ICONST_1);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(JLedu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
		} else if (sort == Type.DOUBLE) {
			this.mv.visitInsn(ICONST_1);
			this.mv.visitInsn(ICONST_0);
			this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(DLedu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
		} else if (sort == Type.OBJECT || sort == Type.ARRAY) {
			if (type.equals(Type.getType(String.class))) {
				this.mv.visitInsn(ICONST_1);
				this.mv.visitInsn(ICONST_0);
				this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(Ljava/lang/String;Ledu/columbia/cs/psl/ioclones/pojo/IORecord;ZZ)V", false);
			} else {
				this.convertToInst(DEPTH);
				this.mv.visitInsn(ICONST_1);
				this.mv.visitInsn(ICONST_0);
				this.mv.visitMethodInsn(INVOKESTATIC, taintChecker, "analyzeTaint", "(Ljava/lang/Object;Ledu/columbia/cs/psl/ioclones/pojo/IORecord;IZZ)V", false);
			}
		} else {
			System.err.println("Un-recognized return type: " + type);
			System.exit(-1);
		}
	}
	
	public void propagateTaint(Type type, boolean isClass) {
		String ioRecordType = Type.getInternalName(IORecord.class);
		String propagater = Type.getInternalName(HitoTaintPropagater.class);
		
		if (isClass) {
			this.mv.visitLdcInsn(Long.MAX_VALUE);
		} else {
			this.mv.visitVarInsn(ALOAD, this.recordId);
			this.mv.visitMethodInsn(INVOKEVIRTUAL, ioRecordType, "getId", "()J", false);
		}
		
		if (type.getSort() == Type.INT) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(IJ)I", false);
		} else if (type.getSort() == Type.BOOLEAN) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(ZJ)Z", false);
		} else if (type.getSort() == Type.BYTE) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(BJ)B", false);
		} else if (type.getSort() == Type.CHAR) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(CJ)C", false);
		} else if (type.getSort() == Type.SHORT) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(SJ)S", false);
		} else if (type.getSort() == Type.FLOAT) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(FJ)F", false);
		} else if (type.getSort() == Type.LONG) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(JJ)J", false);
		} else if (type.getSort() == Type.DOUBLE) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(DJ)D", false);
		} else if (type.equals(Type.getType(String.class))) {
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(Ljava/lang/String;J)V", false);
		} else if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
			this.convertToInst(DEPTH);
			this.mv.visitMethodInsn(INVOKESTATIC, propagater, "propagateTaint", "(Ljava/lang/Object;JI)V", false);
		} else {
			System.err.println("Un-recognized type: " + type);
			System.exit(-1);
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
}
