package edu.columbia.cs.psl.clones.analysis;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;


public class DependentValueInterpreter extends BasicInterpreter {
	@Override
	public BasicValue newValue(Type type) {
		if (type == null) {
			return BasicValue.UNINITIALIZED_VALUE;
		} else if (type.getSort() == Type.VOID)
			return null;
		return new DependentValue(type);
		//		return super.newValue(type);
	}

	@Override
	public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case Opcodes.GETFIELD:
			DependentValue ret = (DependentValue) super.unaryOperation(insn, value);
//			ret.addDep((DependentValue) value);
			ret.src = insn;
			return ret;
		default:
			return super.unaryOperation(insn, value);
		}
	}

	@Override
	public BasicValue merge(BasicValue v, BasicValue w) {
		if (v == BasicValue.UNINITIALIZED_VALUE && w == BasicValue.UNINITIALIZED_VALUE)
		{
			return v;
		}
		if (!(v instanceof DependentValue || w instanceof DependentValue))
			return super.merge(v, w);
//				System.out.println("Merge " + v + w);

		if (v.equals(w))
		{
//			System.out.println("EQ");
			return v;
		}

		if (v instanceof DependentValue && w instanceof DependentValue) {
//			System.out.println("Both sinkable");
			DependentValue sv = (DependentValue) v;
			DependentValue sw = (DependentValue) w;
			if ((v.getType() == null || v.getType().getDescriptor().equals("Lnull;")) && (w.getType() == null || w.getType().getDescriptor().equals("Lnull;"))) {
				if ((sw.src != null && sv.deps != null && sw != null && sv.deps.contains(sw)) || (sw.src == null && sw.deps != null && sv.deps != null && sv.deps.containsAll(sw.deps)))
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
//			System.out.println("V null");
			return w;
		} else if (w.getType() == null || w.getType().getDescriptor().equals("Lnull;")) {
//			System.out.println("W null");
			return v;
		}
		//		if(v instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(v.getType()) && !((SinkableArrayValue)v).flowsToInstMethodCall)
		//		{
		//			relevant.addAll(((SinkableArrayValue)v).tag());
		//		}
		//		if(w instanceof SinkableArrayValue && TaintUtils.isPrimitiveArrayType(w.getType()) && !((SinkableArrayValue)w).flowsToInstMethodCall)
		//		{
		//			relevant.addAll(((SinkableArrayValue)w).tag());
		//		}
		if(v.getType().getDescriptor().equals("Ljava/lang/Object;"))
			return v;
		BasicValue r = new DependentValue(Type.getType(Object.class));
//		System.out.println("Super merge");
//		BasicValue r = super.merge(v, w);
//		System.out.println("Ret " + r);
		return r;
	}
	@Override
	public BasicValue binaryOperation(final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case IALOAD:
		case BALOAD:
		case CALOAD:
		case SALOAD:
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
			DependentValue ret = new DependentValue(Type.INT_TYPE);
			ret.addDep((DependentValue) value1);
			ret.addDep((DependentValue) value2);
			return ret;
		case FALOAD:
		case FADD:
		case FSUB:
		case FMUL:
		case FDIV:
		case FREM:
			return BasicValue.FLOAT_VALUE;
		case LALOAD:
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
			return BasicValue.LONG_VALUE;
		case DALOAD:
		case DADD:
		case DSUB:
		case DMUL:
		case DDIV:
		case DREM:
			return BasicValue.DOUBLE_VALUE;
		case AALOAD:
			return BasicValue.REFERENCE_VALUE;
		case LCMP:
		case FCMPL:
		case FCMPG:
		case DCMPL:
		case DCMPG:
			return BasicValue.INT_VALUE;
		case IF_ICMPEQ:
		case IF_ICMPNE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
		case PUTFIELD:
			return null;
		default:
			throw new Error("Internal error.");
		}
	}

	@Override
	public BasicValue newOperation(final AbstractInsnNode insn) throws AnalyzerException {
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
			DependentValue ret = new DependentValue(Type.INT_TYPE);
//			ret.src = insn;
			return ret;
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
				return BasicValue.INT_VALUE;
			} else if (cst instanceof Float) {
				return BasicValue.FLOAT_VALUE;
			} else if (cst instanceof Long) {
				return BasicValue.LONG_VALUE;
			} else if (cst instanceof Double) {
				return BasicValue.DOUBLE_VALUE;
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
			return newValue(Type.getType(((FieldInsnNode) insn).desc));
		case NEW:
			return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
		default:
			throw new Error("Internal error.");
		}
	}
}
