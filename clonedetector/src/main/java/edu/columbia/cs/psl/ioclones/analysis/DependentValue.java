package edu.columbia.cs.psl.ioclones.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

public class DependentValue extends BasicValue {
	
	public static final BasicValue NULL_VALUE = new BasicValue(Type.getType("Lnull;"));

	private boolean flowsToOutput;
	
	public transient DependentValue owner;
	
	private HashSet<DependentValue> deps;
	
	//public AbstractInsnNode src;
	
	private Set<AbstractInsnNode> inSrcs;
	
	private Set<AbstractInsnNode> outSinks;
		
	private static int idCounter;
	
	public int id;
		
	public DependentValue(Type type) {
		super(type);
		this.id = ++idCounter;
	}
	
	public void addDep(DependentValue d) {
		if (d != null && this.deps == null)
			this.deps = new HashSet<DependentValue>();
		
		this.deps.add(d);
	}
	
	public HashSet<DependentValue> getDeps() {
		return this.deps;
	}
	
	public void addInSrc(AbstractInsnNode src) {
		if (this.inSrcs == null)
			this.inSrcs = new HashSet<AbstractInsnNode>();
		
		this.inSrcs.add(src);
	}
		
	public Collection<AbstractInsnNode> getInSrcs() {
		return this.inSrcs;
	}
	
	public void addOutSink(AbstractInsnNode sink) {
		if (this.outSinks == null)
			this.outSinks = new HashSet<AbstractInsnNode>();
		
		this.outSinks.add(sink);
	}
	
	public Collection<AbstractInsnNode> getOutSinks() {
		return this.outSinks;
	}
	
	@Override
	public String toString() {
		if (this == NULL_VALUE) {
			return "N";
		} else {
			/*return (this.flowsToOutput ? "T" : "F") 
					+ formatDesc() + 
					"#" + this.id + (this.deps == null ? "()" : "("+this.deps+")");*/
			return (this.flowsToOutput ? "T" : "F") 
					+ formatDesc() + 
					"#" + this.id;
		}
	}

	private String formatDesc() {
		if (getType() == null)
			return "N";
		else if (this == UNINITIALIZED_VALUE) {
			return ".";
		} else if (this == RETURNADDRESS_VALUE) {
			return "A";
		} else if (this == REFERENCE_VALUE) {
			return "R";
		} else {
			return getType().getDescriptor();
		}
	}

	public LinkedList<DependentValue> tag() {
		LinkedList<DependentValue> ret = new LinkedList<DependentValue>();
		//System.out.println("Current vale: " + this);
		//System.out.println("Src instruction: " + this.srcs);
		//System.out.println("Deps: " + this.deps);
		if (!this.flowsToOutput) {
			this.flowsToOutput = true;
			ret.add(this);
			if (this.deps != null) {
				for (DependentValue v : this.deps) {
					if (!v.flowsToOutput) {
						ret.addAll(v.tag());
					}
				}
			}
		}
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (flowsToOutput ? 1231 : 1237);
		if (this.inSrcs == null && this.outSinks == null) {
			result = prime * result;
		} else {
			if (this.inSrcs != null) {
				for (AbstractInsnNode src: this.inSrcs) {
					result = prime * result + ((src == null) ? 0 : src.hashCode());
				}
			}
			
			if (this.outSinks != null) {
				for (AbstractInsnNode sink: this.outSinks) {
					result = prime * result + ((sink == null) ? 0: sink.hashCode());
				}
			}
			
		}
		
		//result = prime * result + ((src == null) ? 0 : src.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
		//		if (this == obj)
		//			return true;
		//		if (!super.equals(obj))
		//			return false;
		//		if (getClass() != obj.getClass())
		//			return false;
		//		SinkableArrayValue other = (SinkableArrayValue) obj;
		//		if (deps == null) {
		//			if (other.deps != null)
		//				return false;
		//		} else if (!deps.equals(other.deps))
		//			return false;
		//		if (flowsToInstMethodCall != other.flowsToInstMethodCall)
		//			return false;
		//		if (src == null) {
		//			if (other.src != null)
		//				return false;
		//		} else if (!src.equals(other.src))
		//			return false;
		//		return true;
	}

}
