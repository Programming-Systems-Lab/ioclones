package edu.columbia.cs.psl.ioclones;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.analysis.DependentValue;
import edu.columbia.cs.psl.ioclones.analysis.DependentValueInterpreter;
import edu.columbia.cs.psl.ioclones.pojo.ParamInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class DependencyAnalyzer extends MethodVisitor {
	
	private static final Logger logger = LogManager.getLogger(DependencyAnalyzer.class);
	
	public static final String OUTPUT_MSG = "__$$COLUMBIA_IO_OUTPUT";
	
	public static final String TAINTED_IN = "__$$COLUMBIA_IO_TAINT@";
	
	public static final String TAINTED_STATIC = "__$$COLUMBIA_IO_TAINTSTATIC";
	
	public static final String INPUT_CHECK_MSG = "__$$COLUMBIA_IO_CHECK";
	
	public static final String INPUT_MSG = "__$$COLUMBIA_IO_INPUT";
	
	public static final String INPUT_COPY_0_MSG = "__$$COLUMBIA_IO_INPUT0";
	
	public static final String INPUT_COPY_1_MSG = "__$$COLUMBIA_IO_INPUT1";
	
	public static final String INPUT_COPY_2_MSG = "__$$COLUMBIA_IO_INPUT2";
	
	public DependencyAnalyzer(final String className, 
			int access, 
			final String name, 
			final String desc, 
			String signature, 
			String[] exceptions, 
			final MethodVisitor cmv, 
			boolean trackStatic, 
			boolean debug) {
		
		super(Opcodes.ASM5, 
				new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
												
			@Override
			public void visitEnd() {
				logger.info("Analyzing " + className + " " + name + " " + desc);
				
				//BlockAnalyzer blockAnalyzer = new BlockAnalyzer(className, name, desc, this.instructions);
				//blockAnalyzer.constructBlocks();
				
				boolean isStatic = ClassInfoUtils.checkAccess(access, Opcodes.ACC_STATIC);
				Type[] args = null;
				if (isStatic) {
					args = ClassInfoUtils.genMethodArgs(this.desc, null);
				} else {
					args = ClassInfoUtils.genMethodArgs(this.desc, className);
				}
				Type returnType = Type.getReturnType(this.desc);
				List<ParamInfo> paramInfos = ClassInfoUtils.computeMethodArgs(args);
				
				String methodNameArgs = ClassInfoUtils.methodNameArgs(name, desc);
				DependentValueInterpreter dvi = new DependentValueInterpreter(args, 
						returnType, 
						className, 
						methodNameArgs, 
						true, 
						true, 
						trackStatic);
				
				Analyzer a = new Analyzer(dvi);
				try {
					Frame[] fr = a.analyze(className, this);
					//System.out.println("Merge count: " + dvi.mergeCounter);
					if (!dvi.giveup) {
						Map<Integer, WrittenParam> writtenParams = new HashMap<Integer, WrittenParam>();
						//Determine which input has been written
						for (int j = 0; j < dvi.getParamList().size(); j++) {
							DependentValue paramVal = dvi.getParamList().get(j);
							if (paramVal.written) {
								LinkedList<DependentValue> writtenDeps = paramVal.tag();
								
								if (writtenDeps.size() > 0) {
									writtenDeps.removeFirst();
								}
								
								WrittenParam wp = new WrittenParam();
								wp.paramIdx = paramInfos.get(j).runtimeIdx;
								wp.deps = writtenDeps;
															
								writtenParams.put(paramVal.id, wp);
							}
						}
						
						//Only for written static...
						Map<DependentValue, LinkedList<DependentValue>> writtenStatics = 
								new HashMap<DependentValue, LinkedList<DependentValue>>();
						
						Map<DependentValue, LinkedList<DependentValue>> ios = 
								new HashMap<DependentValue, LinkedList<DependentValue>>();
						AbstractInsnNode insn = this.instructions.getFirst();
						int i = 0;
						
						while(insn != null) {
							Frame fn = fr[i];
							if(fn != null) {					
								//does this insn create output?
								int opcode = insn.getOpcode();
								if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.ARETURN) {
									//What are we returning?
									DependentValue retVal = (DependentValue)fn.getStack(fn.getStackSize() - 1);
									if (!writtenParams.containsKey(retVal.id)) {
										LinkedList<DependentValue> toOutput = retVal.tag();
										retVal.addOutSink(insn);
										
										//The first will be the ret itself
										if (toOutput.size() > 0) {
											toOutput.removeFirst();
											ios.put(retVal, toOutput);
										} else {
											//This means that this output has been analyzed before (merge)
											logger.warn("Visited val: " + retVal);
											logger.warn("Corresponding inst: " + insn);
										}
									}
								} else if (trackStatic && opcode == Opcodes.PUTSTATIC) {
									DependentValue retVal = (DependentValue)fn.getStack(fn.getStackSize() - 1);
									if (!writtenParams.containsKey(retVal.id)) {
										LinkedList<DependentValue> toOutput = retVal.tag();
										retVal.addOutSink(insn);
										
										//The first will be the ret itself
										if (toOutput.size() > 0) {
											toOutput.removeFirst();
											
											if (retVal.isReference() 
													&& !ClassInfoUtils.isImmutable(retVal.getType())) {
												writtenStatics.put(retVal, toOutput);
											} else {
												ios.put(retVal, toOutput);
											}
										} else {
											//This means that this output has been analyzed before (merge)
											logger.warn("Visited val for static: " + retVal);
											logger.warn("Corresponding inst: " + insn);
										}
									}
								}
							}
							i++;
							insn = insn.getNext();
						}
						
						if (debug) {
							this.debug(fr);
						}
						
						//Set<Integer> touched = new HashSet<Integer>();
						Set<AbstractInsnNode> visitedInInsns = new HashSet<AbstractInsnNode>();
						if (ios.size() > 0 || writtenParams.size() > 0) {
							//Need to analyze which control instruction should be recorded, jumps will affect outputs
							//logger.info("Cand. single controls: " + dvi.getSingleControls().size());
							dvi.getSingleControls().forEach((sc, val)->{
								this.instructions.insertBefore(sc, new LdcInsnNode(INPUT_MSG));
							});
							
							//logger.info("Cand. double controls: " + dvi.getDoubleControls().size());
							dvi.getDoubleControls().forEach((dc, vals)->{
								this.instructions.insertBefore(dc, new LdcInsnNode(INPUT_COPY_2_MSG));
							});
						}
						
						for (DependentValue o: ios.keySet()) {
							LinkedList<DependentValue> inputs = ios.get(o);
							
							if (o.getOutSinks() != null) {
								o.getOutSinks().forEach(sink->{
									this.instructions.insertBefore(sink, new LdcInsnNode(OUTPUT_MSG));
								});
								//touched.add(o.id);
								
								if (inputs != null) {
									for (DependentValue input: inputs) {
										if (input.getInSrcs() == null 
												|| input.getInSrcs().size() == 0) {
											continue ;
										}
										
										input.getInSrcs().forEach(src->{
											if (!visitedInInsns.contains(src)) {
												this.instructions.insertBefore(src, new LdcInsnNode(INPUT_MSG));
												visitedInInsns.add(src);
											}
										});
										//touched.add(input.id);
									}
								}
							} else {
								logger.warn("Empty inst for output: " + o);
							}
						}
											
						StringBuilder writtenBuilder = new StringBuilder();
						for (WrittenParam wp: writtenParams.values()) {
							writtenBuilder.append(wp.paramIdx + "-");
							
							for (DependentValue dv: wp.deps) {
								if (dv.getInSrcs() == null 
										|| dv.getInSrcs().size() == 0) {
									continue ;
								}
								
								dv.getInSrcs().forEach(src->{
									if (!visitedInInsns.contains(src)) {
										this.instructions.insertBefore(src, new LdcInsnNode(INPUT_MSG));
										visitedInInsns.add(src);
									}
								});
							}
						}
						
						if (writtenBuilder.length() > 0) {
							String writtenMsg = writtenBuilder.substring(0, writtenBuilder.length() - 1);
							writtenMsg = TAINTED_IN + writtenMsg;
							this.instructions.insert(new LdcInsnNode(writtenMsg));
						}
						
						if (trackStatic) {
							for (DependentValue wStatic: writtenStatics.keySet()) {
								LinkedList<DependentValue> sDeps = writtenStatics.get(wStatic);
								
								wStatic.getOutSinks().forEach(sSink->{
									this.instructions.insertBefore(sSink, new LdcInsnNode(TAINTED_STATIC));
								});
								
								if (sDeps != null) {
									for (DependentValue sDep: sDeps) {
										if (sDep.getInSrcs() == null || sDep.getInSrcs().size() == 0) {
											continue ;
										}
										
										sDep.getInSrcs().forEach(sIn->{
											if (!visitedInInsns.contains(sIn)) {
												this.instructions.insertBefore(sIn, new LdcInsnNode(INPUT_MSG));
												visitedInInsns.add(sIn);
											}
										});
									}
								}
							}
						}
						
						for (int j = 0; j < dvi.getParamList().size(); j++) {
							DependentValue inputParam = dvi.getParamList().get(j);
							if (inputParam.isReference() 
									&& !ClassInfoUtils.isImmutable(inputParam.getType())) {
								if (!isStatic && j == 0) {
									continue ;
								}
								
								if (inputParam.getInSrcs() != null 
										&& inputParam.getInSrcs().size() > 0) {
									inputParam.getInSrcs().forEach(check->{
										this.instructions.insert(check, new LdcInsnNode(INPUT_CHECK_MSG));
									});
								}
							}
						}
					} else {
						logger.warn("Incomplete: " +  className + " " + name + " " + desc);
						logger.warn("Inst/callee size: " + this.instructions.size() + " " + dvi.calleeNum);
					}
				} catch (Exception ex) {
					//ex.printStackTrace();
					logger.error("Error: ", ex);
				}
				
				super.visitEnd();
				this.accept(cmv);
			}
			
			public void debug(Frame[] fr) {
				//print debug info
				AbstractInsnNode insn = this.instructions.getFirst();
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

						this.instructions.insertBefore(insn, new LdcInsnNode(stack));
						this.instructions.insertBefore(insn, new LdcInsnNode(locals));
					}
					i++;
					insn = insn.getNext();
				}
			}
			
			public boolean recordControl(DependentValue val, Set<Integer> touched) {
				//if val has no src, leave it to control
				if (val.getInSrcs() == null || val.getInSrcs().size() == 0) {
					return true;
				}
				
				//If val is relevant to output, it will be recorded...
				if (!touched.contains(val.id)) {
					return true;
				}
				
				return false;
			} 
		});
	}
	
	public static class WrittenParam {
		int paramIdx;
				
		List<DependentValue> deps;
	}
}