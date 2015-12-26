package edu.columbia.cs.psl.ioclones;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.analysis.DependentValue;
import edu.columbia.cs.psl.ioclones.analysis.DependentValueInterpreter;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class DependencyAnalyzer extends MethodVisitor {
	public DependencyAnalyzer(final String className, 
			int access, 
			final String name, 
			final String desc, 
			String signature, 
			String[] exceptions, 
			final MethodVisitor cmv) {
		
		super(Opcodes.ASM5, 
				new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
			
			
			@Override
			public void visitEnd() {
				DependentValueInterpreter dvi = new DependentValueInterpreter();
				Analyzer a = new Analyzer(dvi);
				try {
					Frame[] fr = a.analyze(className, this);
															
					//1st round, collect vals relevant to outputs
					LinkedList<DependentValue> inputs = new LinkedList<DependentValue>();
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
									Collection<DependentValue> toOutput = retVal.tag();
									inputs.addAll(toOutput);
									System.out.println("Output instruction: " + insn );
									System.out.println("Dependent val: " + toOutput);
									break;									
							}
						}
						i++;
						insn = insn.getNext();
					}
					
					Map<Integer, DependentValue> params = dvi.getParams();
					System.out.println("Input param: " + params);
					params.forEach((id, val)->{
						if (val.getDeps() != null && val.getDeps().size() > 0) {
							//This means that the input is an object that has been written
							System.out.println("Dirty input val: " + val);
							/*System.out.println("Src inst: ");
							val.getSrcs().forEach(src->System.out.println(src));*/
							val.getDeps().forEach(d->{
								System.out.println("Written to input (output): " + d + " " + d.getSrcs());
								Collection<DependentValue> toOutput = d.tag();
								inputs.addAll(toOutput);
								System.out.println("Dependent val: ");
								toOutput.forEach(to->System.out.println(to));
							});
						}
					});

					//print debug info
					insn = this.instructions.getFirst();
					i = 0;
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
						
					System.out.println("Relevant input number: " + inputs.size());
					for(DependentValue v : inputs) {
						System.out.println("Input: " + v);
						if (v.getSrcs() != null) {
							v.getSrcs().forEach(src->{
								System.out.println("Input instruction: " + src);
								this.instructions.insert(src, new LdcInsnNode("Input val: " + v));
							});
							
						}
						/*if(v.src != null) {
							System.out.println("Input instruction: " + v.src);
							this.instructions.insert(v.src, new LdcInsnNode("Input val: " + v.toString()));
						}*/
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