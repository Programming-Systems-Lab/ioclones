package edu.columbia.cs.psl.ioclones.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;

public class LabelInfo implements InfoInterface{
	
	public static final int NOCOND = 0;
	
	public static final int COND = 1;
	
	public static final int UNCOND = 2;
	
	//private Label label;
	private String _id;
	
	//private double fraction = 0.0;
	
	//private double[] dists = new double[BytecodeCategory.getCatCount()];
	
	private Map<Integer, Double> dists = new HashMap<Integer, Double>();
	
	private Set<String> parents = new HashSet<String>();
	
	private Set<String> children = new HashSet<String>();
	
	private Set<String> loopChildren = new HashSet<String>();
	
	private List<AbstractInsnNode> insts = new ArrayList<AbstractInsnNode>();
	
	private int condCode = NOCOND;
	
	private boolean head = false;
	
	public transient boolean entry = false;
	
	public LabelInfo(Label label) {
		this._id = label.toString();
	}
	
	@Override
	public void set_id(String label) {
		this._id = label;
	}
	
	@Override
	public String get_id() {
		return this._id;
	}
		
	public void addParent(LabelInfo p) {
		this.parents.add(p.get_id());
	}
	
	public void setParents(Set<String> parents) {
		this.parents = parents;
	}
	
	public Set<String> getParents() {
		return this.parents;
	}
	
	public void addChild(LabelInfo c) {
		this.children.add(c.get_id());
	}
	
	public void setChildren(Set<String> children) {
		this.children = children;
	}
	
	public Set<String> getChildren() {
		return this.children;
	}
	
	public void addLoopChild(String loopC) {
		this.loopChildren.add(loopC);
	}
	
	public void setLoopChildren(Set<String> loopChildren) {
		this.loopChildren = loopChildren;
	}
	
	public Set<String> getLoopChildren() {
		return this.loopChildren;
	}
	
	public void moveChildToLoop(String toMove) {
		this.children.remove(toMove);
		this.loopChildren.add(toMove);
	}
	
	public void setCondCode(int condCode) {
		this.condCode = condCode;
	}
	
	public int getCondCode() {
		return this.condCode;
	}
		
	public void setHead(boolean head) {
		this.head = head;
	}
	
	public boolean isHead() {
		return this.head;
	}
	
	public void addInst(AbstractInsnNode inst) {
		this.insts.add(inst);
	}
	
	public List<AbstractInsnNode> getInsts() {
		return this.insts;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LabelInfo)) {
			return false;
		}
		
		LabelInfo tmp = (LabelInfo) o;
		if (tmp._id.equals(this._id)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return this._id.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Label: " + this._id + "\n");
		//sb.append("Frac: " + this.fraction + "\n");
		sb.append("Head: " + this.head + "\n");
		sb.append("Cond: " + this.condCode + "\n");
		sb.append("Show insts: " + "\n");
		if (this.insts.size() > 0) {
			this.insts.forEach(inst->sb.append(inst + "\n"));
		}
		return sb.toString();
	}

}
