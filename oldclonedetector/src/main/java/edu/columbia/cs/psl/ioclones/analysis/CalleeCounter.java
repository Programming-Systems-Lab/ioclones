package edu.columbia.cs.psl.ioclones.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

public class CalleeCounter extends BasicInterpreter {
	
	public int counter = 0;
	
	public int mergeCounter = 0;
	
	public Set<AbstractInsnNode> visitedCallees = new HashSet<AbstractInsnNode>();
	
	@Override
	public BasicValue naryOperation(AbstractInsnNode insn,
            List values) throws AnalyzerException {
		if ((insn instanceof MethodInsnNode)) {
			this.counter++;
			if (this.visitedCallees.contains(insn)) {
				MethodInsnNode tmp = (MethodInsnNode) insn;
				System.out.println("Revisited callees: " + tmp.owner + " " + tmp.name + " " + tmp.desc);
			} else {
				this.visitedCallees.add(insn);
			}
		}
		return super.naryOperation(insn, values);
	}
	
	@Override
	public BasicValue merge(BasicValue v, BasicValue w) {
		//System.out.println("Merge: " + v + " " + w);
		this.mergeCounter++;
		return super.merge(v, w);
	}

}
