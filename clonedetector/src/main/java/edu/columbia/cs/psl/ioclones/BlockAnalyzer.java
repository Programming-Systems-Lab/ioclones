package edu.columbia.cs.psl.ioclones;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import edu.columbia.cs.psl.ioclones.pojo.BlockInfo;
import edu.columbia.cs.psl.ioclones.pojo.LabelInfo;

public class BlockAnalyzer {
	
	private static final Logger logger = LogManager.getLogger(BlockAnalyzer.class);
	
	private static final Set<Integer> returns = new HashSet<Integer>();
	
	private String className;
	
	private String methodName;
	
	private String desc;
	
	private InsnList instructions;
	
	private Map<String, LabelInfo> labels = new HashMap<String, LabelInfo>();
	
	private Map<String, BlockInfo> blocks = new HashMap<String, BlockInfo>();
	
	private Map<AbstractInsnNode, BlockInfo> instLookup = new HashMap<AbstractInsnNode, BlockInfo>(); 
	
	private boolean newLabel = false;
	
	private LabelInfo lastLabel;
	
	private LabelInfo curLabel;
	
	static {
		returns.add(Opcodes.IRETURN);
		returns.add(Opcodes.LRETURN);
		returns.add(Opcodes.FRETURN);
		returns.add(Opcodes.DRETURN);
		returns.add(Opcodes.ARETURN);
		returns.add(Opcodes.RETURN);
	}
	
	public BlockAnalyzer(String className, 
			String methodName, 
			String desc, 
			InsnList instructions) {
		this.className = className;
		this.methodName = methodName;
		this.desc = desc;
		this.instructions = instructions;
	}
	
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
			logger.error("Error method: " + this.className + " " + this.methodName + " " + this.desc);
			logger.error("Block info: " + this.blocks);
		}
		
		logger.info("Head size: " + blockHeads.size());
	}
	
	public void insertGuide(Collection<AbstractInsnNode> targets) {
		for (AbstractInsnNode ct: targets) {
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
							this.instructions.insertBefore(valNode, new LdcInsnNode(DependencyAnalyzer.INPUT_MSG));
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
							this.instructions.insertBefore(val1, new LdcInsnNode(DependencyAnalyzer.INPUT_MSG));
							this.instructions.insertBefore(val2, new LdcInsnNode(DependencyAnalyzer.INPUT_MSG));
							break ;
						default:
							logger.info("Not handle control op: " + jumpNode.getOpcode());
					}
				} else if (last instanceof TableSwitchInsnNode 
						|| last instanceof LookupSwitchInsnNode) {
					//TableSwitchInsnNode tableNode = (TableSwitchInsnNode) last;
					int curPos = this.instructions.indexOf(last);
					AbstractInsnNode val = this.instructions.get(curPos - 1);
					this.instructions.insertBefore(val, new LdcInsnNode(DependencyAnalyzer.INPUT_MSG));
				}
			}
		}
	}
	
	public Map<String, BlockInfo> getBlocks() {
		return this.blocks;
	}
	
	public BlockInfo lookupInst(AbstractInsnNode inst) {
		return this.instLookup.get(inst);
	}

}
