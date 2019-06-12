package edu.columbia.cs.psl.ioclones.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AbstractInsnNode;

public class BlockInfo implements InfoInterface {
	
	private static final Logger logger = LogManager.getLogger(BlockInfo.class);
	
	public static final String prefix = "B:";
	
	private String headLabelString;
	
	private List<LabelInfo> labels = new ArrayList<LabelInfo>();
	
	private List<AbstractInsnNode> blockInsts = new ArrayList<AbstractInsnNode>();
	
	private Set<String> blockParents = new HashSet<String>();
	
	private Set<String> blockChildren;
	
	private Set<String> blockLoopChildren = new HashSet<String>();
	
	public boolean entry = false;
	
	@Override
	public String get_id() {
		// TODO Auto-generated method stub
		return this.headLabelString;
	}

	@Override
	public void set_id(String headLabelString) {
		// TODO Auto-generated method stub
		this.headLabelString = prefix + headLabelString;
	}
	
	public void addLabel(LabelInfo label) {
		this.labels.add(label);
	}
	
	public List<LabelInfo> getLabels() {
		return this.labels;
	}
			
	public LabelInfo getHeadLabel() {
		if (this.labels.isEmpty()) {
			return null;
		}
		return this.labels.get(0);
	}
	
	public LabelInfo getTailLabel() {
		if (this.labels.isEmpty()) {
			return null;
		}
		return this.labels.get(this.labels.size() - 1);
	}
	
	public Set<String> getBlockParents() {
		return this.blockParents;
	}
	
	public Set<String> getBlockChildren() {
		return this.blockChildren;
	}
	
	public Set<String> getBlockLoopChildren() {
		return this.blockLoopChildren;
	}
	
	public List<AbstractInsnNode> getBlockInsts() {
		return this.blockInsts;
	}
				
	public void migrateLoopChildBlock(String childBlock) {
		this.blockChildren.remove(childBlock);
		this.blockLoopChildren.add(childBlock);
	}
	
	public void summarizeLabels(Map<String, BlockInfo> blockCache) {
		//Summarize callees
		this.labels.forEach(l->{			
			//Instructions
			this.blockInsts.addAll(l.getInsts());
		});
		
		if (this.labels.size() == 0) {
			return ;
		}
		
		//Add prefix to parents and children
		Set<String> parents;
		Set<String> children;
		if (this.labels.size() == 1) {
			LabelInfo theLabel = this.labels.get(0);
			parents = theLabel.getParents();
			children = theLabel.getChildren();
		} else {
			LabelInfo headLabel = this.labels.get(0);
			LabelInfo tailLabel = this.labels.get(this.labels.size() - 1);
			parents = headLabel.getParents();
			children = tailLabel.getChildren();
		}
		//this.blockParents = parents.stream().map(p->{return prefix + p;}).collect(Collectors.toCollection(HashSet::new));
		for (String p: parents) {
			//Find corresponding block
			BlockInfo parentBlock = blockCache.get(p);
			
			if (parentBlock == null) {
				logger.error("Null parent: " + p);
			}
			
			this.blockParents.add(parentBlock.get_id());
		}
		this.blockChildren = children.stream().map(c->{return prefix + c;}).collect(Collectors.toCollection(HashSet::new));
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BlockInfo)) {
			return false;
		}
		
		BlockInfo tmp = (BlockInfo)o;
		return tmp.get_id().equals(this.headLabelString);
	}
	
	@Override
	public int hashCode() {
		return this.headLabelString.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Block: " + this.headLabelString + "\n");
		sb.append("Labels: " + "\n");
		this.labels.forEach(l->{
			sb.append(l.get_id() + "\n");
		});
		sb.append("Parents: " + this.blockParents + "\n");
		sb.append("Children: " + this.blockChildren + "\n");
		sb.append("Loop children: " + this.blockLoopChildren + "\n");
		
		return sb.toString();
	}

}