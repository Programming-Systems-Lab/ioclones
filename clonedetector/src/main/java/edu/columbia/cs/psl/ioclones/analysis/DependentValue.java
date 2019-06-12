package edu.columbia.cs.psl.ioclones.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * DependentValue extends BasicValue with the ability to store dependencies and InSrc/OutSink instructions. See below
 * for more details.
 */
public class DependentValue extends BasicValue {
	
	public static final BasicValue NULL_VALUE = new BasicValue(Type.getType("Lnull;"));
	
	private static int idCounter;
	
	private boolean flowsToOutput;
	
	//public transient DependentValue owner;
	private transient HashSet<DependentValue> owners = new HashSet<DependentValue>();
	
	public transient boolean written = false;
	
	public transient boolean flown = false;
	
	private HashSet<DependentValue> deps = new HashSet<DependentValue>();
	
	//public AbstractInsnNode src;
	
	private Set<AbstractInsnNode> inSrcs;
	
	private Set<AbstractInsnNode> outSinks;
	
	public int id;
		
	public DependentValue(Type type) {
		super(type);
		this.id = ++idCounter;
	}
	
	public Set<DependentValue> getOwners() {
		return this.owners;
	}

	//used more or less on in the DVI
	public void addDep(DependentValue d) {
		if (d != null && this.deps == null)
			this.deps = new HashSet<DependentValue>();

		this.deps.add(d);
        
        // 2-way relationship; make sure for each owner there is a dependent
        if (d.owners == null) {
			d.owners = new HashSet<DependentValue>();
		}
		d.owners.add(this);
	}

	/**
	 * Dependencies are values that have modified/interacted with this DV in some way. E.g., if you IADD x and y to get
	 * z, z is considered to be dependent on x and y. Dependencies are added to DVs in the DVI. Used for tracing where
	 * an input/output came from.
	 *
	 * @return a set of dependencies
	 */
	public HashSet<DependentValue> getDeps() {
		return this.deps;
	}
	
	public void addInSrc(AbstractInsnNode src) {
		if (this.inSrcs == null)
			this.inSrcs = new HashSet<AbstractInsnNode>();

		this.inSrcs.add(src);
	}

	/**
	 * In sources are instructions that return a variable that is classified as an input. Tend to be loading
	 * instructions (ILOAD, LLOAD, etc.).
	 *
	 * @return collection of in sources
	 */
	public Collection<AbstractInsnNode> getInSrcs() {
		return this.inSrcs;
	}
	
	public void addOutSink(AbstractInsnNode sink) {
		if (this.outSinks == null)
			this.outSinks = new HashSet<AbstractInsnNode>();
		
		this.outSinks.add(sink);
	}

	/**
	 * Out sinks are instructions that return a variable that is classified as an output. E.g. the return statement
	 * takes a dependent value as an input. This input DV is the return value of another instruction. This other
	 * instruction is then considered to be an out sink. The classification of what variable counts as an output occurs
	 * in DependencyAnalyzer.
	 *
	 * @return a collection of out sinks
	 */
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

	/**
	 * Performs a breath first traversal of the DV's dependency tree. The dependency tree contains all of the values
	 * that this DV depends on. These dependencies are all added in the DVI.
	 *
	 * @return a linked list of all dependencies of this DV.
	 */
	public LinkedList<DependentValue> tag() {
		LinkedList<DependentValue> ret = new LinkedList<DependentValue>();

		//System.out.println("Current vale: " + this);
		//System.out.println("Src instruction: " + this.srcs);
		//System.out.println("Deps: " + this.deps);

		LinkedList<DependentValue> queue = new LinkedList<DependentValue>();
		HashSet<DependentValue> visited = new HashSet<DependentValue>();
		queue.add(this);
		while (queue.size() > 0) {
			DependentValue toAdd = queue.removeFirst();
			ret.add(toAdd);
			visited.add(toAdd);
			if (toAdd.deps != null) {
				for (DependentValue child: toAdd.deps) {
					if (!queue.contains(child) 
							&& !visited.contains(child)) {
						queue.add(child);
					}
				}
			}
		}

// 		if (!this.flowsToOutput) {
// 			this.flowsToOutput = true;
// 			ret.add(this);
// 			if (this.deps != null) {
// 				for (DependentValue v : this.deps) {
// 					if (!v.flowsToOutput) {
// 						ret.addAll(v.tag());
// 					}
// 				}
// 			}
// 		}
		return ret;
	}

	@Override
	public int hashCode() {
		/*final int prime = 31;
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
		return result;*/
		return this.id;
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
