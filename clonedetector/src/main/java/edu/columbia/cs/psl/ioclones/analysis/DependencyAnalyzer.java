package edu.columbia.cs.psl.ioclones;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.analysis.DependentValue;
import edu.columbia.cs.psl.ioclones.analysis.DependentValueInterpreter;
import edu.columbia.cs.psl.ioclones.pojo.ParamInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.analysis.ReverseCFGNode; 

public class DependencyAnalyzer extends MethodVisitor {
	
	private static final Logger logger = LogManager.getLogger(DependencyAnalyzer.class);
	
	public static final String OUTPUT_MSG = "__$$COLUMBIA_IO_OUTPUT";
	
	public static final String TAINTED_IN = "__$$COLUMBIA_IO_TAINT@";
	
	public static final String DEEP_STATIC = "__$$COLUMBIA_IO_DEEPSTATIC";
	
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
			boolean deepStatic, 
			boolean trackWriter, 
			boolean debug) {
		
		super(Opcodes.ASM5, 
				new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {

			/**
			 * Traces the DVs in the bytecode instructions modified by the DVI to find inputs and outputs.
			 * Input and output instructions are being marked wherever you see INPUT_MSG/OUTPUT_MSG and so on being
			 * inserted before an instruction.
			 */
			@Override
			public void visitEnd() {
				logger.info("Analyzing " + className + " " + name + " " + desc);
				

				//converts args of the method into ParamInfo objects
				boolean isStatic = ClassInfoUtils.checkAccess(access, Opcodes.ACC_STATIC);
                HashSet<String> putStatics = new HashSet<String>();
                HashSet<String> gotStatics = new HashSet<String>();
//                 HashSet<String> overwrittenStatics = new HashSet<String>();

				Type[] args = null;
				if (isStatic) {
					args = ClassInfoUtils.genMethodArgs(this.desc, null);
				} else {
					args = ClassInfoUtils.genMethodArgs(this.desc, className);
				}
				Type returnType = Type.getReturnType(this.desc);
				List<ParamInfo> paramInfos = ClassInfoUtils.computeMethodArgs(args);

				//creates new dvi to process method bytecode with.
				String methodNameArgs = ClassInfoUtils.methodNameArgs(name, desc);
				DependentValueInterpreter dvi = new DependentValueInterpreter(args, 
						returnType, 
						className, 
						methodNameArgs, 
						true, 
						true, 
						trackStatic, 
						trackWriter);

				Analyzer a = new Analyzer(dvi){
					protected Frame newFrame(int nLocals, int nStack) {
						return new ReverseCFGNode<DependentValue>(nLocals, nStack);
					}
					protected Frame newFrame(Frame src) {
						return new ReverseCFGNode<DependentValue>(src);
					}
					protected void newControlFlowEdge(int src, int dst) {
						ReverseCFGNode<DependentValue> s = (ReverseCFGNode<DependentValue>) getFrames()[src];
						ReverseCFGNode<DependentValue> d = (ReverseCFGNode<DependentValue>) getFrames()[dst]; 
						s.getChildren().add(d);
						d.getParents().add(s); 
					}
				};
				try {

					//the symbolic state of the execution stack frame at each bytecode instruction of the method.
					// The number of frames is the number of instructions (and labels) of the method.
					// A given frame is null if and only if the corresponding instruction cannot be reached (dead code).
					Frame[] fr = a.analyze(className, this);

					//create Exit node for CFG 
					ReverseCFGNode exit = new ReverseCFGNode(0, 0);
					exit.setEXIT(true); 

					if (!dvi.giveup) {

						Map<Integer, WrittenParam> writtenParams = new HashMap<Integer, WrittenParam>();
						//Determine which inputs have been written to.
						//dvi.getParamList returns the method args as DVs
						for (int j = 0; j < dvi.getParamList().size(); j++) {
							DependentValue paramVal = dvi.getParamList().get(j);

							//a param is only written to if there was a queryPropagateValue call in the DVI or
							//a interface/static n-ary op
							if (paramVal.written) {
								//tag() finds all of the dependencies of a value
								LinkedList<DependentValue> writtenDeps = paramVal.tag();

								//the first is the DV itself
								if (writtenDeps.size() > 0) {
									writtenDeps.removeFirst();
								}

								WrittenParam wp = new WrittenParam();
								wp.paramIdx = paramInfos.get(j).runtimeIdx;
								wp.deps = writtenDeps;

								writtenParams.put(paramVal.id, wp);
							}
						}

						//Not use this for the paper, since not sure if this helps...
						Map<DependentValue, LinkedList<DependentValue>> deepStatics = 
								new HashMap<DependentValue, LinkedList<DependentValue>>();
						if (deepStatic) {
							for (DependentValue dv: dvi.getClassMemberPool().values()) {
								if (dv.written) {
									LinkedList<DependentValue> writtenDeps = dv.tag();
									
									if (writtenDeps.size() > 0) {
										writtenDeps.removeFirst();
									}
									
									deepStatics.put(dv, writtenDeps);
								}
							}
						}

						//Only for value flows to OutputStream & Writer
						//Output vals cannot be captured here, do it in instrumenter
						Map<AbstractInsnNode, LinkedList<DependentValue>> flowToWriters = 
								new HashMap<AbstractInsnNode, LinkedList<DependentValue>>();

						//Only for written static...
						Map<DependentValue, LinkedList<DependentValue>> writtenStatics = 
								new HashMap<DependentValue, LinkedList<DependentValue>>();

						//map of a given value to all of its dependencies
						//deps are only added in the DVI, in most operations.
						//deps are the values that the given value are descended from.
						Map<DependentValue, LinkedList<DependentValue>> ios = 
								new HashMap<DependentValue, LinkedList<DependentValue>>();
						//same as above, but for if statements

						AbstractInsnNode insn = this.instructions.getFirst();
						int i = 0;
						
						ArrayList<Integer> unaryJumps = new ArrayList<Integer>();
						ArrayList<Integer> binaryJumps = new ArrayList<Integer>();

						while(insn != null) {
							Frame fn = fr[i];
							//if instruction isn't unreachable
							if(fn != null) {

								int opcode = insn.getOpcode();
								//if instruction is a return instruction
								if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.ARETURN) {

									//add as parent of exit node in CFG
									exit.getParents().add((ReverseCFGNode<DependentValue>)fn);

									//get the variable to be returned from the bottom of the stack.
									DependentValue retVal = (DependentValue) fn.getStack(fn.getStackSize() - 1);

									//if the return var is not an overwritten param
									if (!writtenParams.containsKey(retVal.id)) {
										// get all of its dependencies
										LinkedList<DependentValue> toOutput = retVal.tag();
										retVal.addOutSink(insn);

										//The first will be the ret itself
										//Currently, if an argument is only used in the return statement, it is not
										//counted as an input. If you wish to change this behavior, remove the
										// 'removeFirst()' line.
										if (toOutput.size() > 0) {
											ios.put(retVal, toOutput);
										} else {
											//This means that this output has been analyzed before (merge)
											logger.warn("Visited val: " + retVal);
											logger.warn("Corresponding inst: " + insn);
										}
									}

									//if the instruction is an unary if operation
								} else if(opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE) {
									unaryJumps.add(i);

								//like before, if the instruction is a binary if operation
								} else if(opcode >= Opcodes.IF_ICMPEQ && opcode <= Opcodes.IF_ACMPNE) {
									binaryJumps.add(i);

								} else if (trackStatic) { 
                                    if (opcode == Opcodes.PUTSTATIC) {// mark static as written
                                        if (insn.getType() == AbstractInsnNode.FIELD_INSN) {
                                            FieldInsnNode staticField = (FieldInsnNode) insn;
                                            putStatics.add(staticField.owner + "-" + staticField.name);
                                        }

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
                                    else if (opcode == Opcodes.GETSTATIC) {
                                        // don't mark as a insource if static has been overwritten before first read
                                        if (insn.getType() == AbstractInsnNode.FIELD_INSN) {
                                            FieldInsnNode staticField = (FieldInsnNode) insn;
                                            String staticName = staticField.owner + "-" + staticField.name;

                                            // experimental fix
                                            if (putStatics.contains(staticName) && !gotStatics.contains(staticName)) {
//                                                 overwrittenStatics.add(staticName); // don't need now, may want to use in better fix
                                                // assume that always have another instruction directly succeeding a GETSTATIC instruction
                                                DependentValue loadedStatic = (DependentValue) fr[i + 1].getStack(fr[i + 1].getStackSize() - 1);
                                                loadedStatic.getInSrcs().remove(insn);
                                            }

                                            gotStatics.add(staticName);
                                        }
                                    }
								} else if (trackWriter && 
										(opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE)) {
									MethodInsnNode methodInsn = (MethodInsnNode) insn;

									if (!methodInsn.name.equals("<init>")) {
										Type[] methodArgs = Type.getArgumentTypes(methodInsn.desc);
										if (methodArgs.length > 0) {
											String ownerName = methodInsn.owner.replace("/", ".");
											boolean writable = ClassInfoUtils.isWritable(ownerName);
											if (writable) {
												//logger.info("Capture writable: " + ownerName);
												int ptr = fn.getStackSize() - 1;
												int counter = 0;
												while (counter < methodArgs.length) {
													DependentValue dv = (DependentValue)fn.getStack(ptr - counter);
													counter++;

													LinkedList<DependentValue> deps = dv.tag();
													if (deps.size() > 0) {
														deps.removeFirst();
													}
													flowToWriters.put(insn, deps);
												}
											}
										}
									}
								}
							}
							i++;
							insn = insn.getNext();
						}

                        //remove all overwritten statics as insrcs
                        

                        //Here we begin our handling of control flow data dependencies
						//Get post dominators (called on exit node)
						Map<ReverseCFGNode<DependentValue>, Set<ReverseCFGNode<DependentValue>>> map = exit.getPostDominators();

						//List of all nodes control dependent on x 
						Set<ReverseCFGNode<DependentValue>> unaryControlDependencies = new HashSet<ReverseCFGNode<DependentValue>>();
						Set<ReverseCFGNode<DependentValue>> binaryControlDependencies = new HashSet<ReverseCFGNode<DependentValue>>();

						//For all nodes w/ unary jump instruction frames, iterate through all other frames, y, in CFG
						for(int k = 0; k< unaryJumps.size(); k++){
							ReverseCFGNode<DependentValue> jumpFrame = (ReverseCFGNode<DependentValue>) fr[ unaryJumps.get(k) ];

							for(int j = 0; j < fr.length; j++){
								ReverseCFGNode<DependentValue> y = (ReverseCFGNode<DependentValue>) fr[j];

                                if (y == null) {
                                    continue;
                                }

								Set<List<ReverseCFGNode<DependentValue>>> paths = jumpFrame.getAllPathsTo(y);

								boolean postDominatePath = true;

								//y is control dependent on the jump frame node iff...

								for(List path : paths){
									//...there exists a path from jump frame node to y where y post dominates
									//every vertex p in that path...

									for(Object vertex : path){
										Set<ReverseCFGNode<DependentValue>> vertices = map.get(vertex);
										if(!vertices.contains(y)){
											postDominatePath = false; 
											break;
										}
									}
                                    //...and y does not strictly post dominate the jump frame node
                                    if(postDominatePath){
                                        Set<ReverseCFGNode<DependentValue>> jumpFramePD = map.get(jumpFrame); 
                                        if(!jumpFramePD.contains(y) || y.equals(jumpFrame)){
                                            unaryControlDependencies.add(y);
                                        }
                                    }
                                }
							}
						}

                        // now handle all binary jumps
						for(int k = 0; k< binaryJumps.size(); k++){
							ReverseCFGNode<DependentValue> jumpFrame = (ReverseCFGNode<DependentValue>) fr[ binaryJumps.get(k)];
							for(int j = 0; j < fr.length; j++){
								ReverseCFGNode<DependentValue> y = (ReverseCFGNode<DependentValue>) fr[j];

                                if (y == null) {
                                    continue;
                                }

								Set<List<ReverseCFGNode<DependentValue>>> paths = jumpFrame.getAllPathsTo(y);

								boolean postDominatePath = true;

								//y is control dependent on the jump frame node iff...

								for(List path : paths){
									//...there exists a path from jump frame node to y where y post dominates
									//every vertex p in that path...

									for(Object vertex : path){
										Set<ReverseCFGNode<DependentValue>> vertices = map.get(vertex);
										if(!vertices.contains(y)){
											postDominatePath = false;
											break;
										}
									}

                                //...and y does not strictly post dominate the jump frame node
                                    if(postDominatePath){
                                        Set<ReverseCFGNode<DependentValue>> jumpFramePD = map.get(jumpFrame);
                                        if(!jumpFramePD.contains(y) || y.equals(jumpFrame)){
                                            binaryControlDependencies.add(y);
                                        }
                                    }
                                }
							}
						}

						if (debug) {
							this.debug(fr);
						}

						//Set<Integer> touched = new HashSet<Integer>();
						Set<AbstractInsnNode> visitedInInsns = new HashSet<AbstractInsnNode>();

						//for each output variable (variables used in return and otherwise) in ios
						for (DependentValue o: ios.keySet()) {
							//get dependencies of an var
							LinkedList<DependentValue> inputs = ios.get(o);

							if (o.getOutSinks() != null) {
								//mark each outsink of the var as an output
								//outsinks are the instructions that lead to the output var
								o.getOutSinks().forEach(sink->{
									this.instructions.insertBefore(sink, new LdcInsnNode(OUTPUT_MSG));
								});
								//touched.add(o.id);
							} else {
								logger.warn("Empty inst for output: " + o);
							}

							if (inputs != null) {
								//inputs only have insrcs if they come from instructions like loads and whatnot.
								for (DependentValue input: inputs) {
									if (input.getInSrcs() == null 
											|| input.getInSrcs().size() == 0) {
										continue ;
									}

									input.getInSrcs().forEach(src->{
										if (!visitedInInsns.contains(src)) {
											//mark as an input
											this.instructions.insertBefore(src, new LdcInsnNode(INPUT_MSG));
											visitedInInsns.add(src);
										}
									});
									//touched.add(input.id);
								}
							}
						}

						//just do unary for now
						for (ReverseCFGNode cv: unaryControlDependencies) {
							//get ancestors of a var
							Map<DependentValue, List<DependentValue>> unaryInputs = new HashMap<>();

							//get the if's argument from the bottom of the stack.
							DependentValue uif_operand = (DependentValue) cv.getStack(cv.getStackSize() - 1);

							if (!writtenParams.containsKey(uif_operand.id)) {
								// get all of its dependencies
								LinkedList<DependentValue> toOutput = uif_operand.tag();

								//We aren't removing the first value (the if's argument) from the LL here.
								//This is because this should count as the input being used in the function.
								if (toOutput.size() > 0) {
									unaryInputs.put(uif_operand, toOutput);
								} else {
									//This means that this output has been analyzed before (merge)
									logger.warn("Visited val: " + uif_operand);
									logger.warn("Corresponding inst: " + insn);
								}
							}

							for (DependentValue if_var: unaryInputs.keySet()) {
								//get ancestors of a var
								List<DependentValue> inputs = unaryInputs.get(if_var);
								//only looking for inputs here since nothing an if statement returns should count as an
								// output.

								if (inputs != null) {
									for (DependentValue input : inputs) {
										if (input.getInSrcs() == null
												|| input.getInSrcs().size() == 0) {
											continue;
										}

										//inputs only have insrcs if they come from instructions like loads and whatnot.
										input.getInSrcs().forEach(src -> {
											if (!visitedInInsns.contains(src)) {
												//mark as an input
												this.instructions.insertBefore(src, new LdcInsnNode(INPUT_MSG));
												visitedInInsns.add(src);
											}
										});
										//touched.add(input.id);
									}
								}
							}
						}

						for (ReverseCFGNode cv: binaryControlDependencies) {
							//get ancestors of a var
							Map<DependentValue, List<DependentValue>> binaryInputs = new HashMap<>();

							//get the if's argument from the bottom of the stack.
							DependentValue bif_operand_1 = (DependentValue) cv.getStack(cv.getStackSize() - 1);
							DependentValue bif_operand_2 = (DependentValue) cv.getStack(cv.getStackSize() - 2);


							if (!writtenParams.containsKey(bif_operand_1.id)) {
								// get all of its dependencies
								LinkedList<DependentValue> toOutput = bif_operand_1.tag();

								//We aren't removing the first value (the if's argument) from the LL here.
								//This is because this should count as the input being used in the function.
								if (toOutput.size() > 0) {
									binaryInputs.put(bif_operand_1, toOutput);
								} else {
									//This means that this output has been analyzed before (merge)
									logger.warn("Visited val: " + bif_operand_1);
									logger.warn("Corresponding inst: " + insn);
								}
							}

							if (!writtenParams.containsKey(bif_operand_2.id)) {
								// get all of its dependencies
								LinkedList<DependentValue> toOutput = bif_operand_2.tag();

								//We aren't removing the first value (the if's argument) from the LL here.
								//This is because this should count as the input being used in the function.
								if (toOutput.size() > 0) {
									binaryInputs.put(bif_operand_2, toOutput);
								} else {
									//This means that this output has been analyzed before (merge)
									logger.warn("Visited val: " + bif_operand_1);
									logger.warn("Corresponding inst: " + insn);
								}
							}

							for (DependentValue if_var: binaryInputs.keySet()) {
								//get ancestors of a var
								List<DependentValue> inputs = binaryInputs.get(if_var);
								//only looking for inputs here since nothing an if statement returns should count as an
								// output.

								if (inputs != null) {
									for (DependentValue input : inputs) {
										if (input.getInSrcs() == null
												|| input.getInSrcs().size() == 0) {
											continue;
										}

										//inputs only have insrcs if they come from instructions like loads and whatnot.
										input.getInSrcs().forEach(src -> {
											if (!visitedInInsns.contains(src)) {
												//mark as an input
												this.instructions.insertBefore(src, new LdcInsnNode(INPUT_MSG));
												visitedInInsns.add(src);
											}
										});
										//touched.add(input.id);
									}
								}
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

						//For tainted static input
						if (deepStatic) {
							for (DependentValue wSI: deepStatics.keySet()) {
								LinkedList<DependentValue> wsDeps = deepStatics.get(wSI);
								
								//This is actually the output
								if (wSI.getInSrcs() != null && wSI.getInSrcs().size() > 0) {
									//This is actually output
									wSI.getInSrcs().forEach(out->{
										this.instructions.insert(out, new LdcInsnNode(DEEP_STATIC));
									});
									
									//This is input
									for (DependentValue wsDep: wsDeps) {
										if (wsDep.getInSrcs() != null && wsDep.getInSrcs().size() > 0) {
											wsDep.getInSrcs().forEach(depInsn->{
												this.instructions.insertBefore(depInsn, new LdcInsnNode(INPUT_MSG));
											});
										}
									}
								}
							}
						}

						//For putstatic
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
						
						if (trackWriter) {
							for (AbstractInsnNode writeMethod: flowToWriters.keySet()) {
								this.instructions.insertBefore(writeMethod, new LdcInsnNode(OUTPUT_MSG));
								
								LinkedList<DependentValue> fw = flowToWriters.get(writeMethod);
								for (DependentValue fwd: fw) {
									if (fwd.getInSrcs() == null || fwd.getInSrcs().size() == 0) {
										continue ;
									}
									
									fwd.getInSrcs().forEach(fwdIn->{
										if (!visitedInInsns.contains(fwdIn)) {
											this.instructions.insertBefore(fwdIn, new LdcInsnNode(INPUT_MSG));
											visitedInInsns.add(fwdIn);
										}
									});
								}
							}
						}
						
						for (int j = 0; j < dvi.getParamList().size(); j++) {
							DependentValue inputParam = dvi.getParamList().get(j);
							if (inputParam.isReference() 
									&& !ClassInfoUtils.isImmutable(inputParam.getType())) {
// 								if (!isStatic && j == 0) { // why did we do this? is parameter 0 only useful if static?
// 									continue;
// 								}
								
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
