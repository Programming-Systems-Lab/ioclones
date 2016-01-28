package edu.columbia.cs.psl.ioclones.analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;

public class SymbolicValueAnalyzer {
	
	private static final Logger logger = LogManager.getLogger(SymbolicValueAnalyzer.class);
	
	private static final boolean debug = false;
	
	public static void analyzeValue(MethodInfo mi) {
		
		//1st round, collect vals relevant to outputs
		Map<DependentValue, LinkedList<DependentValue>> ios = 
				new HashMap<DependentValue, LinkedList<DependentValue>>();
				
		mi.flowToRet = new TreeSet<Integer>();
		if (mi.writtenInputs == null) {
			mi.writtenInputs = new TreeSet<Integer>();
		}
		mi.flowToStatic = new TreeSet<Integer>();
		
		AbstractInsnNode insn = mi.insts.getFirst();
		int i = 0;			
		while(insn != null) {
			Frame fn = mi.frames[i];
			if(fn != null) {							
				//does this insn create output?
				switch(insn.getOpcode()) {
					//The outputs are values exit after the method ends
					//Include return value and values written to input objs
					case Opcodes.IRETURN:
					case Opcodes.LRETURN:
					case Opcodes.FRETURN:
					case Opcodes.DRETURN:
					case Opcodes.ARETURN:
					case Opcodes.PUTSTATIC:
						//What are we returning?
						DependentValue retVal = (DependentValue)fn.getStack(fn.getStackSize() - 1);
						LinkedList<DependentValue> toOutput = retVal.tag();
						retVal.addOutSink(insn);
						
						//The first will be the ret itself
						if (toOutput.size() == 0) {
							//This means that this output has been analyzed before (merge)
							//logger.warn("Visited val: " + retVal);
							//logger.warn("Corresponding inst: " + insn);
						} else {
							toOutput.removeFirst();
							ios.put(retVal, toOutput);
							
							for (DependentValue dv: toOutput) {
								int paramId = mi.dvi.queryInputParamIndex(dv.id);
								
								if (paramId != -1) {
									if (insn.getOpcode() == Opcodes.PUTSTATIC) {
										mi.flowToStatic.add(paramId);
									} else {
										mi.flowToRet.add(paramId);
									}
								} 
							}
						}
						
						//System.out.println("Output val with inst: " + retVal + " " + insn);
						//System.out.println("Dependent val: " + toOutput);
						break;									
				}
			}
			i++;
			insn = insn.getNext();
		}
		
		if (debug) {
			debug(mi.frames, mi.insts);
		}
		
		Map<Integer, DependentValue> params = mi.dvi.getParams();
		System.out.println("Input param: " + params);
		params.forEach((id, val)->{						
			if (val.getDeps() != null && val.getDeps().size() > 0) {
				//This means that the input is an object that has been written
				System.out.println("Dirty input val: " + val);
				int inputIdx = mi.dvi.queryInputParamIndex(id);
				mi.writtenInputs.add(inputIdx);
			}
		});
	}
		
	public static void debug(Frame[] fr, InsnList instructions) {
		//print debug info
		AbstractInsnNode insn = instructions.getFirst();
		int i = 0;
		while(insn != null) {
			Frame fn = fr[i];
			if(fn != null) {
				String stack = "Stack: ";
				for(int j = 0; j < fn.getStackSize(); j++) {
					stack += fn.getStack(j)+ " ";
				}
					
				String locals = "Locals ";
				for(int j = 0; j < fn.getLocals(); j++) {
					locals += fn.getLocal(j) + " ";
				}

				instructions.insertBefore(insn, new LdcInsnNode(stack));
				instructions.insertBefore(insn, new LdcInsnNode(locals));
			}
			i++;
			insn = insn.getNext();
		}
	}

}
