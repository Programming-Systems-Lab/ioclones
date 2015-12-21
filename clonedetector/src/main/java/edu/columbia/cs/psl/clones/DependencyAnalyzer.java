package edu.columbia.cs.psl.clones;

import java.util.HashSet;
import java.util.LinkedList;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.clones.analysis.DependentValue;
import edu.columbia.cs.psl.clones.analysis.DependentValueInterpreter;

public class DependencyAnalyzer extends MethodVisitor {
	public DependencyAnalyzer(final String className, int access, final String name, final String desc, String signature, String[] exceptions, final MethodVisitor cmv) {
		super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
			@Override
			public void visitEnd() {
				Analyzer a = new Analyzer(new DependentValueInterpreter());
				try {
					Frame[] fr = a.analyze(className, this);
					
					LinkedList<DependentValue> inputs = new LinkedList<DependentValue>();
					AbstractInsnNode insn = this.instructions.getFirst();
					int i = 0;
					while(insn != null)
					{
						Frame fn = fr[i];
						if(fn != null)
						{
							//does this insn create output?
							switch(insn.getOpcode())
							{
							case Opcodes.IRETURN:
								//What are we returning?
								DependentValue retVal = (DependentValue) fn.getStack(fn.getStackSize()-1);
								inputs.addAll(retVal.tag());
								break;
							}
						}
						i++;
						insn = insn.getNext();
					}

					//print debug info
					 insn = this.instructions.getFirst();
						 i = 0;
						while(insn != null)
						{
							Frame fn = fr[i];
							if(fn != null)
							{
								String stack = "Stack: ";
								for(int j = 0; j<fn.getStackSize();j ++)
									stack += fn.getStack(j)+ " ";
								String locals = "Locals ";
								for(int j = 0; j < fn.getLocals(); j++)
								{
									locals += fn.getLocal(j) + " ";
								}

								this.instructions.insertBefore(insn, new LdcInsnNode(stack));
								this.instructions.insertBefore(insn, new LdcInsnNode(locals));
							}
							i++;
							insn = insn.getNext();
						}
						
						for(DependentValue v : inputs)
						{
							if(v.src != null)
							{
								this.instructions.insert(v.src, new LdcInsnNode("Log the input before this"));
							}
						}
				} catch (AnalyzerException e) {
					e.printStackTrace();
				}
				super.visitEnd();
				this.accept(cmv);
			}
		});
	}

}
