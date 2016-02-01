package edu.columbia.cs.psl.ioclones.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;

import edu.columbia.cs.psl.ioclones.pojo.CalleeRecord;
import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class ExploreValueInterpreter extends DependentValueInterpreter {
	
	private static final Logger logger = LogManager.getLogger(ExploreValueInterpreter.class);
	
	public ExploreValueInterpreter(Type[] args, Type retType) {
		super(args, retType, null, null, true);
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
				String className = ClassInfoUtils.cleanType(methodInst.owner);
				ClassInfo calleeInfo = GlobalInfoRecorder.queryClassInfo(className);
				if (calleeInfo != null) {
					String methodNameArgs = ClassInfoUtils.methodNameArgs(methodInst.name, methodInst.desc);
					Map<Integer, TreeSet<Integer>> calleeWritten = null;
					if (opcode == INVOKESTATIC || opcode == INVOKESPECIAL) {
						calleeWritten = ClassInfoUtils.queryMethod(className, methodNameArgs, true, MethodInfo.PUBLIC, false);
					} else {
						calleeWritten = ClassInfoUtils.queryMethod(className, methodNameArgs, false, MethodInfo.PUBLIC, false);
					}
					
					calleeWritten.forEach((w, deps)->{
						DependentValue written = dvs.get(w);
						
						if (written.isReference()) {
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
				
				return ret;
			default:
				return super.naryOperation(insn, values);
		}
	}

}
