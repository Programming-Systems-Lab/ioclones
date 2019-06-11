package edu.columbia.cs.psl.ioclones;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;

import edu.columbia.cs.psl.ioclones.analysis.CalleeCounter;

public class CalleeAnalyzer extends MethodVisitor {
	
	public CalleeAnalyzer(final String className, 
			int access, 
			final String name, 
			final String desc, 
			String signature, 
			String[] exceptions, 
			final MethodVisitor cmv, 
			boolean debug) {
		super(Opcodes.ASM5, 
				new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
			
			@Override
			public void visitEnd() {
				System.out.println("Analyze: " + className + " " + name);
				CalleeCounter counter = new CalleeCounter();
				
				Analyzer a = new Analyzer(counter);
				try {
					a.analyze(className, this);
					System.out.println("Instruction/callee size: " + this.instructions.size() + " " + counter.counter);
					System.out.println("Touched method insns: " + counter.visitedCallees.size());
					System.out.println("Merge count: " + counter.mergeCounter);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				super.visitEnd();
				this.accept(cmv);
			}
		});
	}
}
