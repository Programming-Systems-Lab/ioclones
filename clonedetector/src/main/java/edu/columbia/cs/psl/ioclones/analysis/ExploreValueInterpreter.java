package edu.columbia.cs.psl.ioclones.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;

import edu.columbia.cs.psl.ioclones.pojo.CalleeRecord;
import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class ExploreValueInterpreter extends DependentValueInterpreter {
	
	public ExploreValueInterpreter(Type[] args, Type retType, MethodInfo mi) {
		super(args, retType, mi);
		System.out.println("Explore: " + mi.getMethodKey());
	}
	
	@Override
	public BasicValue naryOperation(AbstractInsnNode insn,
            List values) throws AnalyzerException {
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
								
				if (dvs == null || dvs.size() == 0) {
					return ret; 
				}
								
				if (methodInst.owner.equals("java/lang/Object") && methodInst.name.equals("<init>")) {
					return ret;
				} else if (methodInst.name.equals("toString") && methodInst.desc.equals("()Ljava/lang/String;")) {
					return ret;
				} else if (methodInst.name.equals("equals") && methodInst.desc.equals("(Ljava/lang/Object;)Z")) {
					return ret;
				} else if (methodInst.name.equals("hashCode") && methodInst.desc.equals("()I")) {
					return ret;
				}
				
				/*
				 * For each parameter of the method we are calling,
				 * are any of the parameters outputs?
				 * If so, then mark the value that flows into that parameter slot
				 * as an output!
				 */
				
				Map<Integer, Integer> potentialOutputs = null;
				for (int i = 0; i < dvs.size(); i++) {
					DependentValue dv = dvs.get(i);
					if (dv.isReference() && this.params.containsKey(dv.id)) {
						Type dvType = dv.getType();
						if (!ClassInfoUtils.isImmutable(dvType)) {
							//Which param?
							int callerParam = this.queryInputParamIndex(dv.id);
							if (potentialOutputs == null) {
								potentialOutputs = new HashMap<Integer, Integer>();
							}
							potentialOutputs.put(i, callerParam);
						}
					}
				}
				
				if (potentialOutputs != null) {
					String cleanOwner = ClassInfoUtils.cleanType(methodInst.owner);
					String calleeKey = ClassInfoUtils.genMethodKey(cleanOwner, methodInst.name, methodInst.desc)[0];
					CalleeRecord cr = new CalleeRecord(calleeKey);
					cr.setPotentialOutputs(potentialOutputs);
					
					if (opcode != INVOKESPECIAL && opcode != INVOKESTATIC) {
						cr.fixed = true;
					}
					mi.addCallee(cr);
				}
				
				return ret;
			default:
				return super.naryOperation(insn, values);
		}
	}

}
