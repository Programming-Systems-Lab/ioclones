package edu.columbia.cs.psl.ioclones.analysis;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.driver.IODriver;
import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class PreAnalyzer {
	
	private static final Logger logger = LogManager.getLogger(PreAnalyzer.class);
	
	private static final String rtJarPath = "/Library/Java/JavaVirtualMachines/jdk1.8.0_65.jdk/Contents/Home/jre/lib/rt.jar";
	
	public static void main(String[] args) {
		List<InputStream> container = new ArrayList<InputStream>();
		String profileName = null;
		if (args.length == 0) {
			logger.info("JVM profiling mode");
			profileName = "jvm_profile";
			
			/*String jreLibPath = System.getProperty("sun.boot.class.path");
			String[] jreLibs = jreLibPath.split(":");
			
			for (String s: jreLibs) {
				System.out.println(s);
				
				File jarFile = new File(s);
				if (!jarFile.exists()) {
					logger.warn("Invalid jar path: " + s);
					continue ;
				}
				
				ClassInfoUtils.collectClassesInJar(jarFile, container);
			}*/
			File rtFile = new File(rtJarPath);
			ClassInfoUtils.collectClassesInJar(rtFile, container);
			logger.info("Total collected jvm class file: " + container.size());
		} else {
			String codebasePath = args[0];
			int lastIdx = codebasePath.lastIndexOf("/");
			if (lastIdx == -1) {
				profileName = codebasePath;
			} else {
				profileName = codebasePath.substring(lastIdx + 1, codebasePath.length());
			}
			
			File codebase = new File(args[0]);
			
			if (!codebase.exists()) {
				logger.error("Invalid codebase: " + codebase.getAbsolutePath());
				System.exit(-1);
			}
			
			logger.info("Profiling: " + codebase.getAbsolutePath());
			logger.info("Profile name: " + profileName);
			ClassInfoUtils.genRepoClasses(codebase, container);
			logger.info("Total collected classes in codebase: " + container.size());
		}
		
		System.out.println("Classes to analyze: " + container.size());
		//copy inputstream
		Map<String, byte[]> classDatas = new HashMap<String, byte[]>();
		for (InputStream is: container) {
			try {
				ClassReader cr = new ClassReader(is);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
					@Override
					public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
						// TODO Auto-generated method stub
						return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
					}
				}, ClassReader.EXPAND_FRAMES);
				
				String className = ClassInfoUtils.cleanType(cr.getClassName());
				classDatas.put(className, cw.toByteArray());
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		}
		
		logger.info("Initialization phase");
		for (byte[] classData: classDatas.values()) {
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
									this.classInfo);
							return we;
						}
					}
				};
				analysisReader.accept(cv, ClassReader.EXPAND_FRAMES);
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		}
		
		int iteration = 0;
		do {
			logger.info("Search phase: " + iteration++);
			GlobalInfoRecorder.resetChangeCounter();
			for (byte[] classData: classDatas.values()) {
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
						System.out.println("Current class: " + className);
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
									this.classInfo);
							return we;
						}
					}
				};
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
			}
		} while(GlobalInfoRecorder.isChanged());
		
		logger.info("Report writtein params of methods");
		GlobalInfoRecorder.reportClassInfo();
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
				ClassInfo ownerClass) {
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
					
					DependentValueInterpreter dvi = new DependentValueInterpreter(args, returnType);
					//ExploreValueInterpreter fvi = new ExploreValueInterpreter(args, returnType);
					Analyzer a = new Analyzer(dvi);
					try {
						//Analyze callee here
						Frame[] fr = a.analyze(className, this);
						Map<Integer, TreeSet<Integer>> iterWritten = new HashMap<Integer, TreeSet<Integer>>();
						for (int j = 0; j < dvi.getParamList().size(); j++) {
							DependentValue val = dvi.getParamList().get(j);
							if (val.getDeps() != null && val.getDeps().size() > 0) {
								LinkedList<DependentValue> deps = val.tag();
								deps.removeFirst();
								System.out.println("Deps: ");
								deps.forEach(dep->{
									System.out.println(dvi.queryInputParamIndex(dep.id));
								});
								
								for (DependentValue dep: deps) {
									if (dvi.params.containsKey(dep.id)) {
										int depParam = dvi.queryInputParamIndex(dep.id);
										
										if (iterWritten.containsKey(j)) {
											iterWritten.get(j).add(depParam);
										} else {
											TreeSet<Integer> depParams = new TreeSet<Integer>();
											depParams.add(depParam);
											iterWritten.put(j, depParams);
										}
									}
								}
							}
						}
						
						if (info.getWrittenParams() == null) {
							//Initialization phase
							info.setWrittenParams(iterWritten);
							return ;
						}
						
						if (!info.getWrittenParams().equals(iterWritten)) {
							GlobalInfoRecorder.increChangeCounter();
							info.setWrittenParams(iterWritten);
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
