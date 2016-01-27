package edu.columbia.cs.psl.ioclones.analysis;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			
			String jreLibPath = System.getProperty("sun.boot.class.path");
			String[] jreLibs = jreLibPath.split(":");
			
			for (String s: jreLibs) {
				System.out.println(s);
				
				File jarFile = new File(s);
				if (!jarFile.exists()) {
					logger.warn("Invalid jar path: " + s);
					continue ;
				}
				
				ClassInfoUtils.collectClassesInJar(jarFile, container);
			}
			logger.info("Total collected jvm class file: " + container.size());
		} else {
			File codebase = new File(args[0]);
			profileName = "normal_profile";
			
			if (!codebase.exists()) {
				logger.error("Invalid codebase: " + codebase.getAbsolutePath());
				System.exit(-1);
			}
			
			logger.info("Profiling: " + codebase.getAbsolutePath());;
			ClassInfoUtils.genRepoClasses(codebase, container);
			logger.info("Total collected classes in codebase: " + container.size());
			
			//Map<String, InputStream> classLookup = new HashMap<String, InputStream>();
			Map<String, byte[]> classLookup = new HashMap<String, byte[]>();
			for (InputStream is: container) {
				try {
					ClassReader cr = new ClassReader(is);
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					cr.accept(cw, ClassReader.EXPAND_FRAMES);
					
					byte[] classData = cw.toByteArray();
					String className = ClassInfoUtils.cleanType(cr.getClassName());
					classLookup.put(className, classData);
				} catch (Exception ex) {
					logger.error("Error: ", ex);
				}
			}
			System.out.println("Classes from codebase: " + classLookup.keySet());
					
			LinkedList<String> requiredJVM = new LinkedList<String>();
			for (byte[] classData: classLookup.values()) {
				try {					
					ClassReader computeReader = new ClassReader(classData);
					ClassWriter computeWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					
					ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, computeWriter) {
						@Override
						public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
							super.visit(version, access, name, signature, superName, interfaces);
						}
						
						@Override
						public MethodVisitor visitMethod(int access, 
								String name, 
								String desc, 
								String signature, 
								String[] exceptions) {
							MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
							ObjInitCollector objCollector = new ObjInitCollector(mv, 
									requiredJVM, 
									classLookup.keySet());
							
							return objCollector;
						} 
					};
					computeReader.accept(cv, ClassReader.EXPAND_FRAMES);
				} catch (Exception ex) {
					logger.error("Error: ", ex);
				}
			}
			System.out.println("Required classes from JVM: " + requiredJVM);
									
			//Grab the sub class trees from JVM, focus on rt.jar
			Map<String, InputStream> jvmClasses = new HashMap<String, InputStream>();
			if (requiredJVM.size() > 0) {
				File rtJar = new File(rtJarPath);
				ClassInfoUtils.genJVMLookupTable(rtJar, jvmClasses);
			}
			
			while (requiredJVM.size() > 0) {
				try {
					String jvmClassName = requiredJVM.removeFirst();
					InputStream classStream = jvmClasses.get(jvmClassName);
					
					ClassReader cr = new ClassReader(classStream);
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					cr.accept(cw, ClassReader.EXPAND_FRAMES);
					
					byte[] classData = cw.toByteArray();
					classLookup.put(jvmClassName, classData);
					
					ClassReader computeReader = new ClassReader(classData);
					ClassWriter computeWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					
					ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, computeWriter) {						
						@Override
						public MethodVisitor visitMethod(int access, 
								String name, 
								String desc, 
								String signature, 
								String[] exceptions) {
							MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
							ObjInitCollector objCollector = new ObjInitCollector(mv, 
									requiredJVM, 
									classLookup.keySet());
							
							return objCollector;
						} 
					};
					computeReader.accept(cv, ClassReader.EXPAND_FRAMES);
				} catch (Exception ex) {
					logger.error("Error: ", ex);
				}
			}
		}
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
					String[] parsed = ClassInfoUtils.genMethodKey(className, methodName, methodDesc);
					String methodKey = parsed[0];
					
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
					MethodInfo info = new MethodInfo(methodKey);
					info.setLevel(level);
					info.setFinal(isFinal);
					ownerClass.addMethod(info);
					
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
					
					//DependentValueInterpreter dvi = new DependentValueInterpreter(args, returnType, info);
					ExploreValueInterpreter fvi = new ExploreValueInterpreter(args, returnType, info);
					Analyzer a = new Analyzer(fvi);
					try {
						//Analyze callee here
						Frame[] fr = a.analyze(className, this);
						info.insts = this.instructions;
						info.frames = fr;
						info.dvi = fvi;
					} catch (Exception ex) {
						logger.info("Error: ", ex);
					}
					this.mv.visitEnd();
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
