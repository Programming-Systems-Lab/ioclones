package edu.columbia.cs.psl.clones.analysis;


import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

public class DependentValue extends BasicValue {
	public static final BasicValue NULL_VALUE = new BasicValue(Type.getType("Lnull;"));

	public boolean flowsToOutput;
	public HashSet<DependentValue> deps;
	public AbstractInsnNode src;

	static int idCounter;
	
	int id;
	
	public void addDep(DependentValue d) {
		if (d != null && deps == null)
			deps = new HashSet<DependentValue>();
			deps.add(d);
	}

	public DependentValue(Type type) {
		super(type);
		this.id = ++idCounter;
	}

	@Override
	public String toString() {
		if (this == NULL_VALUE)
			return "N";
		else
			return (flowsToOutput ? "T" : "F") + formatDesc() +"#"+id +(deps == null ? "()" : "("+deps+")");
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

	public Collection<DependentValue> tag() {
		LinkedList<DependentValue> ret = new LinkedList<DependentValue>();
		if (!flowsToOutput) {
			flowsToOutput = true;
			ret.add(this);
			if (deps != null)
				for (DependentValue v : deps) {
					if (!v.flowsToOutput)
						ret.addAll(v.tag());
				}
		}
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (flowsToOutput ? 1231 : 1237);
		result = prime * result + ((src == null) ? 0 : src.hashCode());
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
