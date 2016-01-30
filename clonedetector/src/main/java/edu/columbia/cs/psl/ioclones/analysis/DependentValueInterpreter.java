package edu.columbia.cs.psl.ioclones.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import edu.columbia.cs.psl.ioclones.pojo.CalleeRecord;
import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;


public class DependentValueInterpreter extends BasicInterpreter {
		
	private static Logger logger = LogManager.getLogger(DependentValueInterpreter.class);
	
	public transient boolean hasCallees = false;
	
	public String className;
	
	private boolean explore = false;
		
	protected Type[] allTypes;
	
	protected int initValCount = 0;
	
	protected boolean initParams = false;
	
	protected boolean objDep = false;
	
	protected boolean idxDep = false;
	
	protected Map<Integer, DependentValue> params = new HashMap<Integer, DependentValue>();
	
	protected List<DependentValue> paramList = new ArrayList<DependentValue>();
	
	protected Map<Integer, DependentValue> convertMap = new HashMap<Integer, DependentValue>();
	
	protected Map<AbstractInsnNode, DependentValue[]> doubleControls = new HashMap<AbstractInsnNode, DependentValue[]>();
	
	protected Map<AbstractInsnNode, DependentValue> singelControls = new HashMap<AbstractInsnNode, DependentValue>();
	
	public DependentValueInterpreter(Type[] args, Type retType, boolean explore) {
		if (retType.getSort() == Type.VOID) {
			this.initParams = true;
		}
		
		this.allTypes = args;
		this.explore = explore;
	}
	
	public void propagateDepToOwners(DependentValue owner, DependentValue dep) {
		LinkedList<DependentValue> queue = new LinkedList<DependentValue>();
		Set<DependentValue> visited = new HashSet<DependentValue>();
		queue.add(owner);
		while (queue.size() > 0) {
			DependentValue dv = queue.removeFirst();
			dv.addDep(dep);
			
			dv.written = true;
			visited.add(dv);
			
			if (dv.getOwners() != null) {
				dv.getOwners().forEach(o->{
					if (!visited.contains(o) && !queue.contains(o)) {
						queue.add(o);
					}
				});
			}
		}
	}
	
	public boolean checkValueOrigin(DependentValue val) {		
		Set<DependentValue> visited = new HashSet<DependentValue>();
		LinkedList<DependentValue> queue = new LinkedList<DependentValue>();
		queue.add(val);
		
		while (queue.size() > 0) {
			DependentValue dv = queue.removeFirst();
			if (this.params.containsKey(dv.id)) {
				return true ;
			}
			visited.add(dv);
			
			if (dv.getOwners() != null) {
				dv.getOwners().forEach(o->{
					if (!visited.contains(o) && !queue.contains(o)) {
						queue.add(o);
					}
				});
			}
		}
		
		return false;
	}
	
	public Map<Integer, DependentValue> getParams() {
		return this.params;
	}
	
	public List<DependentValue> getParamList() {
		return this.paramList;
	}
	
	public Map<AbstractInsnNode, DependentValue[]> getDoubleControls() {
		return this.doubleControls;
	}
	
	public Map<AbstractInsnNode, DependentValue> getSingleControls() {
		return this.singelControls;
	}
	
	public int queryInputParamIndex(int symbolicId) {
		for (int i = 0; i < this.paramList.size(); i++) {
			DependentValue dv = this.paramList.get(i);
			if (dv.id == symbolicId) {
				return i;
			}
		}
		
		return -1;
	}
	
	public void inferInputParamIndices(DependentValue dv, 
			TreeSet<Integer> record, 
			Set<Integer> visited) {
		if (dv == null) {
			return ;
		}
		
		if (visited.contains(dv.id)) {
			return ;
		}
		
		visited.add(dv.id);
		
		if (this.params.containsKey(dv.id)) {
			record.add(this.queryInputParamIndex(dv.id));
		}
		
		if (dv.getDeps() == null) {
			return ;
		}
		
		for (DependentValue dep: dv.getDeps()) {
			inferInputParamIndices(dep, record, visited);
		}
	}
		
	@Override
	public BasicValue newValue(Type type) {
		if (type == null) {
			return BasicValue.UNINITIALIZED_VALUE;
		} else if (type.getSort() == Type.VOID) {
			return null;
		}
		DependentValue dv = new DependentValue(type);
		
		if (!this.initParams) {
			//Ignore the return val, if it's not void
			this.initParams = true;
			return dv;
		} else if (this.initValCount < this.allTypes.length) {
			Type curType = this.allTypes[this.initValCount++];
			if (curType.equals(dv.getType())) {
				this.params.put(dv.id, dv);
				this.paramList.add(dv);
			} else {
				logger.error("Incompatible type: " + curType + " " + dv.getType());
			}
		}
		
		return dv;
		//		return super.newValue(type);
	}
	
	@Override
	public BasicValue newOperation(final AbstractInsnNode insn) throws AnalyzerException {
		//System.out.println("New op: " + insn);
		
		switch (insn.getOpcode()) {
			case ACONST_NULL:
				return newValue(Type.getObjectType("null"));
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
				//DependentValue ret = new DependentValue(Type.INT_TYPE);
				//return ret;
				return newValue(Type.INT_TYPE);
			case LCONST_0:
			case LCONST_1:
				return newValue(Type.LONG_TYPE);
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
				return newValue(Type.FLOAT_TYPE);
			case DCONST_0:
			case DCONST_1:
				return newValue(Type.DOUBLE_TYPE);
			case BIPUSH:
			case SIPUSH:
				return newValue(Type.INT_TYPE);
			case LDC:
				Object cst = ((LdcInsnNode) insn).cst;
				if (cst instanceof Integer) {
					//return BasicValue.INT_VALUE;
					return newValue(Type.INT_TYPE);
				} else if (cst instanceof Float) {
					//return BasicValue.FLOAT_VALUE;
					return newValue(Type.FLOAT_TYPE);
				} else if (cst instanceof Long) {
					//return BasicValue.LONG_VALUE;
					return newValue(Type.LONG_TYPE);
				} else if (cst instanceof Double) {
					//return BasicValue.DOUBLE_VALUE;
					return newValue(Type.DOUBLE_TYPE);
				} else if (cst instanceof String) {
					return newValue(Type.getObjectType("java/lang/String"));
				} else if (cst instanceof Type) {
					int sort = ((Type) cst).getSort();
					if (sort == Type.OBJECT || sort == Type.ARRAY) {
						return newValue(Type.getObjectType("java/lang/Class"));
					} else if (sort == Type.METHOD) {
						return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
					} else {
						throw new IllegalArgumentException("Illegal LDC constant " + cst);
					}
				} else if (cst instanceof Handle) {
					return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
				} else {
					throw new IllegalArgumentException("Illegal LDC constant " + cst);
				}
			case JSR:
				return BasicValue.RETURNADDRESS_VALUE;
			case GETSTATIC:
				DependentValue ret = new DependentValue(Type.getType(((FieldInsnNode) insn).desc));
				//ret.src = insn;
				ret.addInSrc(insn);
				//return newValue(Type.getType(((FieldInsnNode) insn).desc));
				return ret;
			case NEW:
				DependentValue newRet = (DependentValue) newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
				//newRet.addSrc(insn);
				return newRet;
			default:
				logger.error("Invalid new operation: " + insn);
				throw new Error("Internal error.");
		}
	}
	
	@Override
	public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		//System.out.println("Copy op: " + insn + " " + value);
		
		DependentValue dv = (DependentValue) value;
		switch(insn.getOpcode()) {
			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.FLOAD:
			case Opcodes.DLOAD:
			case Opcodes.ALOAD:
				//For capturing xloads that might be input
				if (this.params.containsKey(dv.id)) {
					dv.addInSrc(insn);
				}
			default:
				//return super.copyOperation(insn, value);
				return super.copyOperation(insn, dv);
		}
	}

	@Override
	public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		//System.out.println("Unary op: " + insn + " " + value);
		
		DependentValue oriVal = null;
		DependentValue ret = null;
		switch (insn.getOpcode()) {
			case IINC:
				//The value here should be dependent value
				oriVal = (DependentValue) value;				
				ret = (DependentValue) newValue(value.getType());
				
				if (this.params.containsKey(oriVal.id)) {
					oriVal.addInSrc(insn);
				}
				
				ret.addDep(oriVal);
				return value;
			case INEG:
	        case L2I:
	        case F2I:
	        case D2I:
	        case I2B:
	        case I2C:
	        case I2S:
	        	oriVal = (DependentValue) value;
	        	if (this.convertMap.containsKey(oriVal.id)) {
	        		return this.convertMap.get(oriVal.id);
	        	}
	        	
	        	ret = (DependentValue) newValue(Type.INT_TYPE);
	        	ret.addDep(oriVal);
	        	this.convertMap.put(oriVal.id, ret);
	        	return ret;
	        case ARRAYLENGTH:
	            //return BasicValue.INT_VALUE;
	        	oriVal = (DependentValue) value;
	        	if (this.convertMap.containsKey(oriVal.id)) {
	        		return this.convertMap.get(oriVal.id);
	        	}
	        	
	            ret = (DependentValue) newValue(Type.INT_TYPE);
	            ret.addOwner(oriVal);
	            if (checkValueOrigin(oriVal)) {
	            	ret.addInSrc(insn);
	            }
	            
	            this.convertMap.put(oriVal.id, ret);
	            return ret;
	        case FNEG:
	        case I2F:
	        case L2F:
	        case D2F:
	        	//return BasicValue.FLOAT_VALUE;
	        	oriVal = (DependentValue) value;
	        	if (this.convertMap.containsKey(oriVal.id)) {
	        		return this.convertMap.get(oriVal.id);
	        	}
	        	
	        	ret = (DependentValue) newValue(Type.FLOAT_TYPE);
	            ret.addDep(oriVal);
	            this.convertMap.put(oriVal.id, ret);
	            return ret;
	        case LNEG:
	        case I2L:
	        case F2L:
	        case D2L:
	            //return BasicValue.LONG_VALUE;
	        	oriVal = (DependentValue) value;
	        	if (this.convertMap.containsKey(oriVal.id)) {
	        		return this.convertMap.get(oriVal.id);
	        	}
	        	
	        	ret = (DependentValue) newValue(Type.LONG_TYPE);
	        	ret.addDep(oriVal);
	        	this.convertMap.put(oriVal.id, ret);
	        	return ret;
	        case DNEG:
	        case I2D:
	        case L2D:
	        case F2D:
	            //return BasicValue.DOUBLE_VALUE;
	            oriVal = (DependentValue) value;	            
	            if (this.convertMap.containsKey(oriVal.id)) {
	            	return this.convertMap.get(oriVal.id);
	            }
	            
	            ret = (DependentValue) newValue(Type.DOUBLE_TYPE);
	            ret.addDep(oriVal);
	            this.convertMap.put(oriVal.id, ret);
	            return ret;
			case GETFIELD:
				ret = (DependentValue) super.unaryOperation(insn, value);
				//ret.src = insn;
				
				DependentValue owner = (DependentValue)value;
				if (checkValueOrigin(owner)) {
					ret.addInSrc(insn);
				}
				
				ret.addOwner(owner);				
				//System.out.println("Getfield: " + insn + " " + ret);
				return ret;
			case NEWARRAY:
			case ANEWARRAY:
				DependentValue size = (DependentValue) value;
				ret = (DependentValue) super.unaryOperation(insn, size);
				ret.addDep(size);
				//ret.addSrc(insn);
				return ret;
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IFNULL:
			case IFNONNULL:
			case TABLESWITCH:
			case LOOKUPSWITCH:				
				DependentValue cont = (DependentValue) value;
				this.getSingleControls().put(insn, cont);
				return null;
			case CHECKCAST:
				DependentValue checked = (DependentValue) value;
				return checked;
			case INSTANCEOF:
				DependentValue instanceVal = (DependentValue) value;
				return instanceVal;
			default:
				return super.unaryOperation(insn, value);
		}
	}
	
	@Override
	public BasicValue binaryOperation(final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2) throws AnalyzerException {
		//System.out.println("Binary op: " + insn + " " + value1 + " " + value2);
		
		DependentValue ret = null;
		DependentValue arrRef = null;
		DependentValue idx = null;
		switch (insn.getOpcode()) {
			case IALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				arrRef = (DependentValue) value1;
				idx = (DependentValue) value2;
				ret = new DependentValue(Type.INT_TYPE);
				
				if (this.idxDep)
					arrRef.addDep(idx);
				
				ret.addOwner(arrRef);
			
				if (checkValueOrigin(arrRef)) {
					ret.addInSrc(insn);
				}
				
				return ret;
			case IADD:
			case ISUB:
			case IMUL:
			case IDIV:
			case IREM:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IOR:
			case IXOR:
				ret = new DependentValue(Type.INT_TYPE);
				DependentValue dep1 = (DependentValue) value1;
				DependentValue dep2 = (DependentValue) value2;
				ret.addDep(dep1);
				ret.addDep(dep2);
				return ret;
			case FALOAD:
				arrRef = (DependentValue) value1;
				idx = (DependentValue) value2;
				ret = new DependentValue(Type.FLOAT_TYPE);
				
				if (this.idxDep)
					arrRef.addDep(idx);
				
				ret.addOwner(arrRef);
				
				if (checkValueOrigin(arrRef)) {
					ret.addInSrc(insn);
				}
				
				return ret;
			case FADD:
			case FSUB:
			case FMUL:
			case FDIV:
			case FREM:
				ret = new DependentValue(Type.FLOAT_TYPE);
				ret.addDep((DependentValue) value1);
				ret.addDep((DependentValue) value2);
				return ret;
			case LALOAD:
				arrRef = (DependentValue) value1;
				idx = (DependentValue) value2;
				ret = new DependentValue(Type.LONG_TYPE);
				
				if (this.idxDep)
					arrRef.addDep(idx);
				
				ret.addOwner(arrRef);
				
				if (checkValueOrigin(arrRef)) {
					ret.addInSrc(insn);
				}
				
				return ret;
			case LADD:
			case LSUB:
			case LMUL:
			case LDIV:
			case LREM:
			case LSHL:
			case LSHR:
			case LUSHR:
			case LAND:
			case LOR:
			case LXOR:
				ret = new DependentValue(Type.LONG_TYPE);
				ret.addDep((DependentValue) value1);
				ret.addDep((DependentValue) value2);
				return ret;
			case DALOAD:
				arrRef = (DependentValue) value1;
				idx = (DependentValue) value2;
				ret = new DependentValue(Type.DOUBLE_TYPE);
				
				if (this.idxDep)
					arrRef.addDep(idx);
				
				ret.addOwner(arrRef);
				
				if (checkValueOrigin(arrRef)) {
					ret.addInSrc(insn);
				}
				
				return ret;
			case DADD:
			case DSUB:
			case DMUL:
			case DDIV:
			case DREM:
				ret = new DependentValue(Type.DOUBLE_TYPE);
				ret.addDep((DependentValue) value1);
				ret.addDep((DependentValue) value2);
				//return BasicValue.DOUBLE_VALUE;
				return ret;
			case AALOAD:
				arrRef = (DependentValue) value1;
				idx = (DependentValue) value2;
				ret = new DependentValue(BasicValue.REFERENCE_VALUE.getType());
				
				if (this.idxDep)
					arrRef.addDep(idx);
				
				ret.addOwner(arrRef);
								
				if (checkValueOrigin(arrRef)) {
					ret.addInSrc(insn);
				}
				
				//return BasicValue.REFERENCE_VALUE;
				return ret;
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
				ret = new DependentValue(Type.INT_TYPE);
				ret.addDep((DependentValue) value1);
				ret.addDep((DependentValue) value2);
				//return BasicValue.INT_VALUE;
				return ret;
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
				DependentValue cont1 = (DependentValue) value1;
				DependentValue cont2 = (DependentValue) value2;
				
				DependentValue[] record = {cont1, cont2};
				this.doubleControls.put(insn, record);
				
				return null;
			case PUTFIELD:
				if (value2 == BasicValue.UNINITIALIZED_VALUE) {
					return null;
				}
				
				DependentValue objRef = (DependentValue) value1;
				DependentValue written = (DependentValue) value2;
				propagateDepToOwners(objRef, written);
				/*if (polluteInput) {
					written.addOutSink(insn);
				}*/
				written.addOwner(objRef);
				
				return null;
			default:
				throw new Error("Internal error.");
		}
	}
	
	@Override
	public BasicValue ternaryOperation(AbstractInsnNode insn, 
			BasicValue val1, 
			BasicValue val2, 
			BasicValue val3) throws AnalyzerException {
		//System.out.println("Ternary op: " + insn + " " + val1 + " " + val2 + " " + val3);
		
		DependentValue objRef = (DependentValue)val1;
		//Should record idx for array, too detailed?
		DependentValue idx = (DependentValue)val2;
		DependentValue val = (DependentValue)val3;
				
		//Should we care about the idx?
		if (this.idxDep)
			this.propagateDepToOwners(objRef, idx);
		
		this.propagateDepToOwners(objRef, val);
		
		val.addOwner(objRef);
		
		return super.ternaryOperation(insn, val1, val2, val3);
	}
	
	@Override
	public BasicValue naryOperation(AbstractInsnNode insn,
            List values) throws AnalyzerException {
		//System.out.println("Nary op: " + insn + " " + values);
		
		List<DependentValue> dvs = (List<DependentValue>) values;
		int opcode = insn.getOpcode();
		switch(opcode) {
			case INVOKESTATIC:
			case INVOKESPECIAL:
			case INVOKEVIRTUAL:
			case INVOKEINTERFACE:
				MethodInsnNode methodInst = (MethodInsnNode) insn;
				Type retType = Type.getReturnType(methodInst.desc);
				
				DependentValue ret = (DependentValue) newValue(retType);
				
				if (methodInst.owner.equals("java/lang/Object") && methodInst.name.equals("<init>")) {
					return ret;
				} else if (methodInst.name.equals("toString") && methodInst.desc.equals("()Ljava/lang/String;")) {
					if (this.objDep) {
						ret.addDep(dvs.get(0));
					}
					return ret;
				} else if (methodInst.name.equals("equals") && methodInst.desc.equals("(Ljava/lang/Object;)Z")) {
					if (this.objDep) {
						ret.addDep(dvs.get(0));
						ret.addDep(dvs.get(1));
					} else {
						ret.addDep(dvs.get(1));
					}
					return ret;
				} else if (methodInst.name.equals("hashCode") && methodInst.desc.equals("()I")) {
					if (this.objDep) {
						ret.addDep(dvs.get(0));
					}
					
					return ret;
				}
				
				Type ownerType = Type.getType(methodInst.owner);
				if (ownerType.getSort() == Type.ARRAY) {
					if (dvs.size() == 0) {
						return ret;
					} else {
						if (this.objDep && ret != null) {
							for (DependentValue dv: dvs) {
								ret.addDep(dv);
							}
						} else if (ret != null) {
							for (int i = 1; i < dvs.size(); i++) {
								DependentValue dv = dvs.get(i);
								ret.addDep(dv);;
							}
						}
						return ret;
					}
				}
				
				//Check if this callee is possible to write inputs
				boolean shouldCheck = false;
				for (DependentValue dv: dvs) {
					if (dv.isReference()) {
						shouldCheck = true;
					}
					break ;
				}
				
				if (shouldCheck) {
					this.hasCallees = true;
					
					String className = ClassInfoUtils.cleanType(methodInst.owner);
					ClassInfo calleeInfo = GlobalInfoRecorder.queryClassInfo(className);
					if (calleeInfo != null) {
						String methodNameArgs = ClassInfoUtils.methodNameArgs(methodInst.name, methodInst.desc);
						Map<Integer, TreeSet<Integer>> calleeWritten = null;
						if (opcode == INVOKESTATIC || opcode == INVOKESPECIAL) {
							calleeWritten = ClassInfoUtils.queryMethod(className, methodNameArgs, true, MethodInfo.PUBLIC);
						} else {
							calleeWritten = ClassInfoUtils.queryMethod(className, methodNameArgs, false, MethodInfo.PUBLIC);
						}
						
						if (calleeWritten != null) {
							calleeWritten.forEach((w, deps)->{
								DependentValue written = dvs.get(w);
								
								if (!written.isReference()) {
									logger.error("Suspicious written: " + className + " " + methodNameArgs + " " + written);
								}
								
								if (this.params.containsKey(written.id)) {
									for (Integer d: deps) {
										DependentValue dep = dvs.get(d);
										written.addDep(dep);
									}
								}
							});
						}
					} else if (this.explore && calleeInfo == null) {
						logger.error("Missed class: " + className + " " + methodInst.name + " " + methodInst.desc);
						logger.error("Current class: " + this.className);
					}
				}
				
				if (ret == null || dvs == null || dvs.size() == 0) {
					return ret; 
				}
				
				if (this.objDep || insn.getOpcode() == INVOKESTATIC) {
					for (DependentValue dv: dvs) {
						ret.addDep(dv);
					}
				} else {
					for (int i = 1; i < dvs.size(); i++) {
						ret.addDep(dvs.get(i));
					}
				}
								
				return ret;
			case INVOKEDYNAMIC:
				InvokeDynamicInsnNode dynamicInsn = (InvokeDynamicInsnNode) insn;
				Type dynRetType = Type.getReturnType(dynamicInsn.desc);
				DependentValue dynRet = (DependentValue) newValue(dynRetType);
				for (DependentValue dv: dvs) {
					dynRet.addDep(dv);
				}
				return dynRet;
			case MULTIANEWARRAY:
				DependentValue mulArr = (DependentValue) newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
				dvs.forEach(dv->{
					mulArr.addDep(dv);
				});
				//mulArr.addSrc(insn);
				return mulArr;
			default:
				return super.naryOperation(insn, values);
		}
	}
	
	@Override
	public void returnOperation(AbstractInsnNode insn, 
			BasicValue value, 
			BasicValue expected) throws AnalyzerException {
		//System.out.println("Return op: " + insn + " " + value + " " + expected);
		super.returnOperation(insn, value, expected);
		//Bind instruction at analyzer
	}
	
	@Override
	public BasicValue merge(BasicValue v, BasicValue w) {
		//System.out.println("V: " + v);
		//System.out.println("W: " + w);
		if (v == BasicValue.UNINITIALIZED_VALUE 
				&& w == BasicValue.UNINITIALIZED_VALUE) {
			return v;
		}
		
		if (!(v instanceof DependentValue 
				|| w instanceof DependentValue)) {
			return super.merge(v, w);
		}
		
		//DependentValue tmpV = (DependentValue) v;
		//DependentValue tmpW = (DependentValue) w;

		if (v.equals(w)) {
			return v;
		}

		if (v instanceof DependentValue && w instanceof DependentValue) {
			DependentValue sv = (DependentValue) v;
			DependentValue sw = (DependentValue) w;
			
			if ((v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) 
					&& (w.getType() == null || w.getType().getDescriptor().equals("Lnull;"))) {
				if ((sw.getInSrcs() != null && sv.getDeps() != null && sw != null && sv.getDeps().contains(sw)) 
						|| (sw.getInSrcs() == null && sw.getDeps() != null && sv.getDeps() != null && sv.getDeps().containsAll(sw.getDeps())))
					return v;
				else {
					sv.addDep(sw);
					return v;
				}
			}
			
			if (v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) {
				sw.addDep(sv);
				return w;
			} else if (w.getType() == null || w.getType().getDescriptor().equals("Lnull;")) {
				sv.addDep(sw);
				return v;
			} else {
				if (v.getType().equals(w.getType())) {
					sv.addDep(sw);					
					return v;
				}
			}
		}
		
		if (v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) {
			return w;
		} else if (w.getType() == null || w.getType().getDescriptor().equals("Lnull;")) {
			return v;
		}
		
		//System.out.println("Touch merging objects");
		if(v.getType().getDescriptor().equals("Ljava/lang/Object;"))
			return v;
		
		BasicValue r = new DependentValue(Type.getType(Object.class));
		//System.out.println();
		return r;
	}
}
