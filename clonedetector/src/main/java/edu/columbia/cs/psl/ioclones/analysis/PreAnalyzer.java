package edu.columbia.cs.psl.ioclones.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassDataTraverser;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class PreAnalyzer {
	
	private static final Logger logger = LogManager.getLogger(PreAnalyzer.class);
	
	private static final String rtJarPath = "/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/rt.jar";
	
	public static void main(String[] args) {
		List<byte[]> container = new ArrayList<byte[]>();
		if (args.length == 0) {
			System.err.println("Please specify input directory");
			System.exit(-1);
		}
		ClassDataTraverser.collectDir(args[0], container);
		//container = ClassDataTraverser.filter(container, "java/util/HashMap");
		
		logger.info("Classes to analyze: " + container.size());
		logger.info("Initialization phase");
		int counter = 0;
		List<String> ori = new ArrayList<String>();
		for (byte[] classData: container) {
			counter++;
			if (counter % 1000 == 0) {
				logger.info("Analyzed: " + counter);
			}
			
			try {
				byte[] copy = Arrays.copyOf(classData, classData.length);
				
				ClassReader analysisReader = new ClassReader(copy);
				ClassWriter analysisWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, analysisWriter) {
					String className;
					
					String superName;
					
					ClassInfo classInfo;
					
					@Override
					public void visit(int version, 
							int access, 
							String name, 
							String signature, 
							String superName, 
							String[] interfaces) {
						super.visit(version, access, name, signature, superName, interfaces);
						
						this.className = ClassInfoUtils.cleanType(name);
						//logger.info("Name: " + this.className);
						ori.add(this.className);
						
						this.classInfo = GlobalInfoRecorder.queryClassInfo(this.className);
						if (this.classInfo == null) {
							this.classInfo = new ClassInfo(this.className);
							GlobalInfoRecorder.registerClassInfo(this.classInfo);
						}
						
						if (superName != null) {
							this.superName = ClassInfoUtils.cleanType(superName);
							ClassInfo superClass = GlobalInfoRecorder.queryClassInfo(this.superName);
							if (superClass == null) {
								superClass = new ClassInfo(this.superName);
								GlobalInfoRecorder.registerClassInfo(superClass);
							}
							
							this.classInfo.setParent(this.superName);
							superClass.addChild(this.className);
						}
						
						for (String inter: interfaces) {
							inter = ClassInfoUtils.cleanType(inter);
							ClassInfo interClass = GlobalInfoRecorder.queryClassInfo(inter);
							if (interClass == null) {
								interClass = new ClassInfo(inter);
								GlobalInfoRecorder.registerClassInfo(interClass);
							}
							
							this.classInfo.addInterface(inter);
							interClass.addChild(this.className);
						}
					}
					
					@Override
					public MethodVisitor visitMethod(int access, 
							String name, 
							String desc, 
							String signature, 
							String[] exceptions) {
						boolean isSynthetic = ClassInfoUtils.checkAccess(access, Opcodes.ACC_SYNTHETIC);
						boolean isNative = ClassInfoUtils.checkAccess(access, Opcodes.ACC_NATIVE);
						boolean isInterface = ClassInfoUtils.checkAccess(access, Opcodes.ACC_INTERFACE);
						boolean isAbstract = ClassInfoUtils.checkAccess(access, Opcodes.ACC_ABSTRACT);
						
						MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
						if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
							return mv;
						} else if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
							return mv;
						} else if (name.equals("hashCode") && desc.equals("()I")) {
							return mv;
						} else if (isSynthetic || isNative || isInterface || isAbstract) {
							return mv;
						} else {
							WriterExplorer we = new WriterExplorer(mv, 
									access, 
									this.className, 
									name, 
									desc, 
									signature, 
									exceptions, 
									this.classInfo, 
									false, 
									false);
							return we;
						}
					}
				};
				analysisReader.accept(cv, ClassReader.EXPAND_FRAMES);
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		}
		//Actually, two classes will be loaded twice from jre
		//netscape.javascript.JSException and netscape.javascript.JSObject
		/*logger.info("After analyzed: " + GlobalInfoRecorder.getClassInfo().size());
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("ori.csv"));
			for (String o: ori) {
				bw.write(o + "\n");
			}
			bw.flush();
			bw.close();
			
			bw = new BufferedWriter(new FileWriter("ana.csv"));
			for (String a: GlobalInfoRecorder.getClassInfo().keySet()) {
				bw.write(a + "\n");
			}
			bw.flush();
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}*/
		
		int iteration = 0;
		do {
			logger.info("Search phase: " + iteration++);
			final int curIter = iteration;
			
			GlobalInfoRecorder.resetChangeCounter();
			int searchCounter = 0;
			for (byte[] classData: container) {
				searchCounter++;
				if (searchCounter % 1000 == 0) {
					logger.info("Searched: " + searchCounter);
				}
				
				byte[] copy = Arrays.copyOf(classData, classData.length);
				ClassReader cr = new ClassReader(copy);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
					
					String className;
					
					ClassInfo classInfo;
					
					@Override
					public void visit(int version, 
							int access, 
							String name, 
							String signature, 
							String superName, 
							String[] interfaces) {
						super.visit(version, access, name, signature, superName, interfaces);
						this.className = ClassInfoUtils.cleanType(name);
						this.classInfo = GlobalInfoRecorder.queryClassInfo(this.className);
					}
					
					@Override
					public MethodVisitor visitMethod(int access, 
							String name, 
							String desc, 
							String signature, 
							String[] exceptions) {
						boolean isSynthetic = ClassInfoUtils.checkAccess(access, Opcodes.ACC_SYNTHETIC);
						boolean isNative = ClassInfoUtils.checkAccess(access, Opcodes.ACC_NATIVE);
						boolean isInterface = ClassInfoUtils.checkAccess(access, Opcodes.ACC_INTERFACE);
						boolean isAbstract = ClassInfoUtils.checkAccess(access, Opcodes.ACC_ABSTRACT);
						
						MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
						if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
							return mv;
						} else if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
							return mv;
						} else if (name.equals("hashCode") && desc.equals("()I")) {
							return mv;
						} else if (isSynthetic || isNative || isInterface || isAbstract) {
							return mv;
						} else {
							boolean reportChange = false;
							if (curIter > 12) {
								reportChange = true;
							}
							WriterExplorer we = new WriterExplorer(mv, 
									access, 
									this.className, 
									name, 
									desc, 
									signature, 
									exceptions, 
									this.classInfo, 
									true, 
									reportChange);
							return we;
						}
					}
				};
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
			}
		} while(GlobalInfoRecorder.isChanged());
		
		logger.info("Report written params of methods");
		GlobalInfoRecorder.reportClassInfo(true);
	}
	
	public static class WriterExplorer extends MethodVisitor {
		
		public int access;
		
		public String className;
		
		public String methodName;
		
		public String methodDesc;
		
		//public Map<Integer, Boolean> paramMap = new HashMap<Integer, Boolean>();
		
		public WriterExplorer(MethodVisitor mv, 
				int access, 
				String className, 
				String methodName, 
				String methodDesc, 
				String signature, 
				String[] exceptions,
				ClassInfo ownerClass, 
				boolean search, 
				boolean reportChange) {
			super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5, 
					access, 
					methodName, 
					methodDesc, 
					signature, 
					exceptions) {
				
				@Override
				public void visitEnd() {
					//String[] parsed = ClassInfoUtils.genMethodKey(className, methodName, methodDesc);
					
					boolean isFinal = ClassInfoUtils.checkAccess(access, Opcodes.ACC_FINAL);
					int level = -1;
					boolean isPublic = ClassInfoUtils.checkAccess(access, Opcodes.ACC_PUBLIC);
					if (!isPublic) {
						boolean isProtected = ClassInfoUtils.checkAccess(access, Opcodes.ACC_PROTECTED);
						if (!isProtected) {
							boolean isPrivate = ClassInfoUtils.checkAccess(access, Opcodes.ACC_PRIVATE);
							if (!isPrivate) {
								level = MethodInfo.DEFAULT;
							} else {
								level = MethodInfo.PRIVATE;
							}
						} else {
							level = MethodInfo.PROTECTED;
						}
					} else {
						level = MethodInfo.PUBLIC;
					}
					
					//logger.info("Method key: " + methodKey);
					String methodNameArgs = ClassInfoUtils.methodNameArgs(methodName, methodDesc);
					MethodInfo info = ownerClass.getMethodInfo(methodNameArgs);
					if (info == null) {
						info = new MethodInfo();
						info.setLevel(level);
						info.setFinal(isFinal);
						ownerClass.addMethodInfo(methodNameArgs, info);
					}
					
					if (info.leaf) {
						return ;
					}
					
					boolean isStatic = ClassInfoUtils.checkAccess(this.access, Opcodes.ACC_STATIC);
					Type[] args = null;
					if (isStatic) {
						args = Type.getArgumentTypes(this.desc);
					} else {
						Type[] methodArgs = Type.getArgumentTypes(this.desc);
						args = new Type[methodArgs.length + 1];
						args[0] = Type.getObjectType(className);
						for (int i = 1; i < args.length; i++) {
							args[i] = methodArgs[i - 1];
						}
					}
					Type returnType = Type.getReturnType(this.desc);
					
					DependentValueInterpreter dvi = new DependentValueInterpreter(args, 
							returnType, 
							className, 
							methodNameArgs, 
							search);
					//ExploreValueInterpreter fvi = new ExploreValueInterpreter(args, returnType);
					Analyzer a = new Analyzer(dvi);
					try {
						//Analyze callee here
						boolean show = false;
						/*if (ownerClass.getClassName().equals("javax.naming.ldap.LdapName") 
								&& methodNameArgs.equals("compareTo-(java.lang.Object)")) {
							System.out.println("Owner calss: " + ownerClass.getClassName() + " " + methodNameArgs);
							show = true;
						}*/
						
						Frame[] fr = a.analyze(className, this);
						Map<Integer, TreeSet<Integer>> iterWritten = new HashMap<Integer, TreeSet<Integer>>();
						for (int j = 0; j < dvi.getParamList().size(); j++) {
							DependentValue val = dvi.getParamList().get(j);
							if (val.written) {
								LinkedList<DependentValue> deps = val.tag();
								if (deps.size() == 0) {
									//System.out.println("Suspicious class: " + className);
									//System.out.println("Method name: " + methodNameArgs);
									//System.out.println("Param idx: " + dvi.queryInputParamIndex(val.id));
									//System.out.println("Param: " + val);
									//System.out.println("Deps: " + val.getDeps());
									//System.out.println("All params: " + dvi.getParamList());
									//System.exit(-1);
								} else {
									deps.removeFirst();
																		
									if (!iterWritten.containsKey(j)) {
										TreeSet<Integer> depParams = new TreeSet<Integer>();
										iterWritten.put(j, depParams);
									}
									
									for (DependentValue dep: deps) {
										int checkParamId = dvi.checkValueOrigin(dep, false);
										if (checkParamId != - 1 && checkParamId != j) {
											iterWritten.get(j).add(checkParamId);
										}
									}
									
									if (show) {
										System.out.println("Written val: " + val);
										System.out.println("Deps: " + deps);
										System.out.println("Iter writtens: " + iterWritten);
									}
								}
							}
						}
						
						if (info.getWrittenParams() == null) {
							//Initialization phase
							info.setWrittenParams(iterWritten);
							if (show) {
								System.out.println("----initial push: " + iterWritten);
							}
							
							if (!dvi.hasCallees) {
								info.leaf = true;
							}
							
							return ;
						}
						
						if (show) {
							System.out.println("----Special check (last): " + info.getWrittenParams());
							System.out.println("(now): " + iterWritten);
						}
						
						if (!info.getWrittenParams().equals(iterWritten)) {
							if (reportChange) {
								logger.info("Changed: " + ownerClass.getClassName() + " " + methodNameArgs);
								logger.info("Last: " + info.getWrittenParams());
								logger.info("Now: " + iterWritten);
							}
							ClassInfoUtils.unionMap(iterWritten, info.getWrittenParams());
							
							if (show) {
								System.out.println("After merging: " + info.getWrittenParams());
							}
							
							GlobalInfoRecorder.increChangeCounter();
							//info.setWrittenParams(iterWritten);
						}
					} catch (Exception ex) {
						logger.info("Error: ", ex);
					}
				}
			});
		}
	}
	
	public static class ObjInitCollector extends MethodVisitor {
		
		public LinkedList<String> recorder = null;
		
		public Set<String> constraints = null;
		
		public ObjInitCollector(MethodVisitor mv, 
				LinkedList<String> recorder, 
				Set<String> constraints) {
			super(Opcodes.ASM5, mv);
			this.recorder = recorder;
			this.constraints = constraints;
		}
		
		@Override
		public void visitTypeInsn(int opcode, String type) {
			this.mv.visitTypeInsn(opcode, type);
			
			if (opcode == Opcodes.NEW) {
				if (this.constraints != null) {
					if (!this.constraints.contains(type) && !this.recorder.contains(type)) {
						this.recorder.add(type);
					}
				} else  {
					if (!this.recorder.contains(type)) {
						this.recorder.add(type);
					}
				}
			}
		}
		
		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
			
			if (name.equals("<init>")) {
				return ;
			} else if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
				return ;
			} else if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
				return ;
			} else if (name.equals("hashCode") && desc.equals("()I")) {
				return ;
			}
			
			String cleanOwner = ClassInfoUtils.cleanType(owner);
			if (this.constraints != null) {
				if (!this.constraints.contains(cleanOwner) && !this.recorder.contains(cleanOwner)) {
					this.recorder.add(cleanOwner);
				}
			} else {
				if (!this.recorder.contains(cleanOwner))
					this.recorder.add(cleanOwner);
			}
		}
	}
}
