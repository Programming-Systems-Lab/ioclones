package edu.columbia.cs.psl.ioclones;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.analysis.DependentValue;
import edu.columbia.cs.psl.ioclones.analysis.DependentValueInterpreter;
import edu.columbia.cs.psl.ioclones.pojo.LabelInfo;
import edu.columbia.cs.psl.ioclones.pojo.BlockInfo;

public class DependencyAnalyzer extends MethodVisitor {
	
	private static final Logger logger = LogManager.getLogger(DependencyAnalyzer.class);
	
	public static final String OUTPUT_MSG = "__$$COLUMBIA_IO_OUTPUT";
	
	public static final String INPUT_MSG = "__$$COLUMBIA_IO_INPUT";
	
	private static final Set<Integer> returns = new HashSet<Integer>();
	
	static {
		returns.add(Opcodes.IRETURN);
		returns.add(Opcodes.LRETURN);
		returns.add(Opcodes.FRETURN);
		returns.add(Opcodes.DRETURN);
		returns.add(Opcodes.ARETURN);
		returns.add(Opcodes.RETURN);
	}
	
	public DependencyAnalyzer(final String className, 
			int access, 
			final String name, 
			final String desc, 
			String signature, 
			String[] exceptions, 
			final MethodVisitor cmv, 
			boolean debug) {
		
		super(Opcodes.ASM5, 
				new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
			
			private Map<String, LabelInfo> labels = new HashMap<String, LabelInfo>();
			
			private Map<String, BlockInfo> blocks = new HashMap<String, BlockInfo>();
			
			private Map<AbstractInsnNode, BlockInfo> instLookup = new HashMap<AbstractInsnNode, BlockInfo>(); 
			
			private boolean newLabel = false;
			
			private LabelInfo lastLabel;
			
			private LabelInfo curLabel;
			
			public LabelInfo retrieveLabelInfo(Label l) {
				LabelInfo ret = null;
				
				if (this.labels.containsKey(l.toString())) {
					ret = this.labels.get(l.toString());
				} else {
					ret = new LabelInfo(l);
					
					if (this.labels.size() == 0) {
						ret.entry = true;
					}
					
					this.labels.put(l.toString(), ret);
				}
				
				this.newLabel = false;
				return ret;
			}
						
			public void handleLabel(Label label) {
				LabelInfo info = retrieveLabelInfo(label);
				
				this.lastLabel = this.curLabel;
				this.curLabel = info;
				this.labels.put(label.toString(), this.curLabel);
				
				if (this.lastLabel != null) {
					int lastCondCode = this.lastLabel.getCondCode();
					if (lastCondCode != LabelInfo.UNCOND) {
						this.lastLabel.addChild(this.curLabel);
						this.curLabel.addParent(this.lastLabel);
					}
					
					if (lastCondCode != LabelInfo.NOCOND) {
						this.curLabel.setHead(true);
					}
				} else {
					//The first label
					this.curLabel.setHead(true);
				}
			}
			
			public void constructBlocks() {
				AbstractInsnNode[] copyInsts = this.instructions.toArray();
				for (int i = 0; i < copyInsts.length; i++) {
					AbstractInsnNode insn = copyInsts[i];
					if (insn instanceof FrameNode) {
						continue ;
					}
					
					if (insn instanceof LineNumberNode) {
						continue ;
					}
					
					if (insn instanceof LabelNode) {
						LabelNode labelNode = (LabelNode) insn;
						Label label = labelNode.getLabel();
						this.handleLabel(label);
						
						continue ;
					}
					
					if (this.newLabel) {
						Label label = new Label();
						LabelNode newLabelNode = new LabelNode(label);
						this.instructions.insertBefore(insn, newLabelNode);
						
						this.handleLabel(newLabelNode.getLabel());
					}
					
					//All normal instructions
					if (returns.contains(insn.getOpcode())) {
						this.curLabel.setCondCode(LabelInfo.UNCOND);
					} else if (insn instanceof JumpInsnNode) {
						JumpInsnNode jump = (JumpInsnNode) insn;
						
						if (jump.getOpcode() == Opcodes.GOTO) {
							this.curLabel.setCondCode(LabelInfo.UNCOND);
						} else {
							this.curLabel.setCondCode(LabelInfo.COND);
						}
						
						LabelInfo dest = this.retrieveLabelInfo(jump.label.getLabel());
						dest.setHead(true);
						this.curLabel.addChild(dest);
						dest.addParent(this.curLabel);
						
						this.newLabel = true;
					} else if (insn instanceof TableSwitchInsnNode) {
						TableSwitchInsnNode tSwitch = (TableSwitchInsnNode) insn;
						
						if (tSwitch.dflt != null) {
							LabelInfo defaultInfo = this.retrieveLabelInfo(tSwitch.dflt.getLabel());
							defaultInfo.setHead(true);
							
							this.curLabel.addChild(defaultInfo);
							defaultInfo.addParent(this.curLabel);
						}
						
						List<LabelNode> labels = (List<LabelNode>) tSwitch.labels;
						
						for (LabelNode l: labels) {
							LabelInfo dest = this.retrieveLabelInfo(l.getLabel());
							dest.setHead(true);
							
							this.curLabel.addChild(dest);
							dest.addParent(this.curLabel);
						}
					} else if (insn instanceof LookupSwitchInsnNode) {
						LookupSwitchInsnNode lSwitch = (LookupSwitchInsnNode) insn;
						
						if (lSwitch.dflt != null) {
							LabelInfo defaultInfo = this.retrieveLabelInfo(lSwitch.dflt.getLabel());
							defaultInfo.setHead(true);
							
							this.curLabel.addChild(defaultInfo);
							defaultInfo.addParent(this.curLabel);
						}
						
						List<LabelNode> labels = (List<LabelNode>) lSwitch.labels;
						for (LabelNode l: labels) {
							LabelInfo dest = this.retrieveLabelInfo(l.getLabel());
							dest.setHead(true);
							
							this.curLabel.addChild(dest);
							dest.addParent(this.curLabel);
						}
					}
					
					this.curLabel.addInst(insn);
				}
				
				//Find head labels
				LinkedList<LabelInfo> heads = new LinkedList<LabelInfo>();
				this.labels.forEach((k, v)-> {
					if (v.getParents().size() == 0 || v.entry) {
						//v.setFraction(1.0);
						heads.add(v);
					}
				});
				
				//Merge labels to blocks
				Set<LabelInfo> labelExplored = new HashSet<LabelInfo>();
				Map<String, BlockInfo> blockCache = new HashMap<String, BlockInfo>();
				heads.forEach(label->{
					LinkedList<LabelInfo> queue = new LinkedList<LabelInfo>();
					queue.add(label);
					BlockInfo curBlock = null;
					while (queue.size() > 0) {
						LabelInfo tmp = queue.removeLast();
						//logger.info("Process label: " + tmp);
						if (labelExplored.contains(tmp)) {
							continue ;
						}
						labelExplored.add(tmp);
						
						tmp.getChildren().forEach(c->{
							LabelInfo cInfo = this.labels.get(c);
							if (!queue.contains(cInfo) || !labelExplored.contains(cInfo)) {
								queue.add(cInfo);
							}
						});
						
						if (tmp.isHead()) {
							BlockInfo newBlock = new BlockInfo();
							newBlock.set_id(tmp.get_id());
							newBlock.addLabel(tmp);
							this.blocks.put(newBlock.get_id(), newBlock);
							
							if (tmp.entry) {
								newBlock.entry = true;
							}
							
							curBlock = newBlock;
						} else {
							curBlock.addLabel(tmp);
						}
						blockCache.put(tmp.get_id(), curBlock);
					}
				});
				logger.info("First round of block analysis: " + this.blocks.size());
				
				LinkedList<BlockInfo> blockHeads = new LinkedList<BlockInfo>();
				try {
					this.blocks.forEach((key, block)->{
						block.summarizeLabels(blockCache);
						for (AbstractInsnNode bInst: block.getBlockInsts()) {
							this.instLookup.put(bInst, block);
						}
						
						if (block.getBlockParents().size() == 0) {
							blockHeads.add(block);
						}
					});
				} catch (Exception ex) {
					logger.error("Error: ", ex);
					logger.error("Error method: " + className + " " + name + " " + desc);
					logger.error("Block info: " + this.blocks);
				}
				
				logger.info("Head size: " + blockHeads.size());
				/*this.blocks.forEach((k, b)->{
					logger.info("Block: " + b.get_id());
					logger.info("Parents: " + b.getBlockParents());
					logger.info("Children: " + b.getBlockChildren());
				});*/
			}
									
			@Override
			public void visitEnd() {
				System.out.println("Analyzing " + className + " " + name + " " + desc);
				
				this.constructBlocks();
				
				DependentValueInterpreter dvi = new DependentValueInterpreter();
				Analyzer a = new Analyzer(dvi);
				try {					
					Frame[] fr = a.analyze(className, this);
															
					//1st round, collect vals relevant to outputs
					//LinkedList<DependentValue> inputs = new LinkedList<DependentValue>();
					Map<DependentValue, LinkedList<DependentValue>> ios = 
							new HashMap<DependentValue, LinkedList<DependentValue>>();
					AbstractInsnNode insn = this.instructions.getFirst();
					int i = 0;					
					while(insn != null) {
						Frame fn = fr[i];
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
									toOutput.removeFirst();
									ios.put(retVal, toOutput);
									//inputs.addAll(toOutput);
									System.out.println("Output val with inst: " + retVal + " " + insn);
									System.out.println("Dependent val: " + toOutput);
									/*List<List<AbstractInsnNode>> srcs = new ArrayList<List<AbstractInsnNode>>();
									toOutput.forEach(v->{
										srcs.add(v.getSrcs());
									});
									System.out.println("Dep srcs: " + srcs);*/
									break;									
							}
						}
						i++;
						insn = insn.getNext();
					}
					
					if (debug) {
						this.debug(fr);
					}
					
					Map<Integer, DependentValue> params = dvi.getParams();
					System.out.println("Input param: " + params);
					params.forEach((id, val)->{
						if (val.getDeps() != null && val.getDeps().size() > 0) {
							//This means that the input is an object that has been written
							System.out.println("Dirty input val: " + val);
							
							val.getDeps().forEach(d->{
								System.out.println("Written to input (output): " + d + " " + d.getInSrcs());
								LinkedList<DependentValue> toOutput = d.tag();
								System.out.println("Check to output: " + toOutput);
								
								if (toOutput.size() > 0) {
									//The first will be d itself
									toOutput.removeFirst();
									//inputs.addAll(toOutput);
									ios.put(d, toOutput);
									System.out.println("Dependent val: " + toOutput);
								} else {
									logger.info("Visited value: " + d);
								}
								
								
								/*if (d.getSrcs() != null && d.getSrcs().size() > 0) {
									d.getSrcs().forEach(src-> {
										this.instructions.insertBefore(src, new LdcInsnNode(OUTPUT_MSG));
									});
								}*/
							});
						}
					});
										
					System.out.println("Output number: " + ios.size());
					Set<AbstractInsnNode> controlTarget = new HashSet<AbstractInsnNode>();
					for (DependentValue o: ios.keySet()) {
						System.out.println("Output: " + o);
						LinkedList<DependentValue> inputs = ios.get(o);
						
						//If o's out sinks are null, something wrong
						if (o.getOutSinks() != null) {
							o.getOutSinks().forEach(sink->{
								controlTarget.add(sink);
								this.instructions.insertBefore(sink, new LdcInsnNode(OUTPUT_MSG));
							});
							
							if (inputs != null) {
								for (DependentValue input: inputs) {
									if (input.getInSrcs() == null 
											|| input.getInSrcs().size() == 0) {
										continue ;
									}
									
									input.getInSrcs().forEach(src->{
										controlTarget.add(src);
										this.instructions.insertBefore(src, new LdcInsnNode(INPUT_MSG));
									});
								}
							}
							
							//In case the input and output are the same value
							if (o.getInSrcs() != null) {
								//System.out.println("I is O: " + o);
								o.getInSrcs().forEach(src->{
									this.instructions.insertBefore(src, new LdcInsnNode(INPUT_MSG));
								});
							}
						} else {
							logger.error("Invalid summarziation of output insts: " + o);
						}
					}
					
					//Need to analyze which control instruction should be recorded
					for (AbstractInsnNode ct: controlTarget) {
						BlockInfo block = this.instLookup.get(ct);
						
						//Do a reverse DFS to see which control inst should be recorded
						LinkedList<String> queue = new LinkedList<String>();
						Set<String> explored = new HashSet<String>();
						explored.add(block.get_id());
						queue.addAll(block.getBlockParents());
						while(queue.isEmpty()) {
							BlockInfo cur = this.blocks.get(queue.removeFirst());
							explored.add(cur.get_id());
							cur.getBlockParents().forEach(p->{
								if (!explored.contains(p) && !queue.contains(p)) {
									queue.addLast(p);
								}
							});
							
							if (cur.getBlockInsts().size() == 0) {
								continue ;
							}
							
							List<AbstractInsnNode> insts = cur.getBlockInsts();
							AbstractInsnNode last = insts.get(insts.size() - 1);
							if (last.getOpcode() == Opcodes.GOTO) {
								continue ;
							}
							
							if (last instanceof JumpInsnNode) {
								JumpInsnNode jumpNode = (JumpInsnNode) last;
								int curPos = -1;
								switch(jumpNode.getOpcode()) {
									case Opcodes.IFEQ:
									case Opcodes.IFGE:
									case Opcodes.IFGT:
									case Opcodes.IFLE:
									case Opcodes.IFLT:
									case Opcodes.IFNE:
									case Opcodes.IFNONNULL:
									case Opcodes.IFNULL:
										curPos = this.instructions.indexOf(last);
										AbstractInsnNode valNode = this.instructions.get(curPos - 1);
										this.instructions.insertBefore(valNode, new LdcInsnNode(INPUT_MSG));
										break ;
									case Opcodes.IF_ICMPEQ:
									case Opcodes.IF_ICMPNE:
									case Opcodes.IF_ICMPLT:
									case Opcodes.IF_ICMPGE:
									case Opcodes.IF_ICMPGT:
									case Opcodes.IF_ICMPLE:
									case Opcodes.IF_ACMPEQ:
									case Opcodes.IF_ACMPNE:
										curPos = this.instructions.indexOf(last);
										AbstractInsnNode val1 = this.instructions.get(curPos - 2);
										AbstractInsnNode val2 = this.instructions.get(curPos - 1);
										this.instructions.insertBefore(val1, new LdcInsnNode(INPUT_MSG));
										this.instructions.insertBefore(val2, new LdcInsnNode(INPUT_MSG));
										break ;
									default:
										logger.info("Not handle control op: " + jumpNode.getOpcode());
								}
							} else if (last instanceof TableSwitchInsnNode) {
								
							} else if (last instanceof LookupSwitchInsnNode) {
								
							}
						}
					};
				} catch (AnalyzerException e) {
					e.printStackTrace();
				}
				super.visitEnd();
				this.accept(cmv);
				System.out.println();
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
		});
	}

}