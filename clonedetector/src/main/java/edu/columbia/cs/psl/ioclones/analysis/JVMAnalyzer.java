package edu.columbia.cs.psl.ioclones.analysis;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;

import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class JVMAnalyzer {
	
	private static final Logger logger = LogManager.getLogger(JVMAnalyzer.class);
	
	public static void main(String[] args) {
		String jreLibPath = System.getProperty("sun.boot.class.path");
		String[] jreLibs = jreLibPath.split(":");
		
		List<InputStream> container = new ArrayList<InputStream>();
		
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
		
		try {
			for (InputStream is: container) {
				ClassReader cr = new ClassReader(is);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
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
						this.cv.visit(version, access, name, signature, superName, interfaces);
						
						this.className = ClassInfoUtils.cleanType(name);
						logger.info("Name: " + this.className);
						
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
						boolean isInterface = ClassInfoUtils.checkAccess(access, Opcodes.ACC_INTERFACE);
						boolean isAbstract = ClassInfoUtils.checkAccess(access, Opcodes.ACC_ABSTRACT);
						
						MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
						if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
							return mv;
						} else if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
							return mv;
						} else if (name.equals("hashCode") && desc.equals("()I")) {
							return mv;
						} else if (isSynthetic || isInterface || isAbstract) {
							String[] parsed = ClassInfoUtils.genMethodKey(this.className, name, desc);
							MethodInfo mi = new MethodInfo(parsed[0]);
							mi.setLevel(MethodInfo.NO_CHECK);
							this.classInfo.addMethod(mi);
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
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		Map<String, ClassInfo> allClasses = GlobalInfoRecorder.getClassInfo();
		allClasses.forEach((name, clazz)->{
			System.out.println("Class name: " + clazz.getClassName());
			
			Map<String, MethodInfo> methods = clazz.getMethods();
			for (MethodInfo method: methods.values()) {
				if (method.summarized) {
					continue ;
				}
				
				TreeSet<Integer> writtenParam = method.getWriteParams();
				if (writtenParam != null && writtenParam.size() > 0) {
					System.out.println("Direct writer: " + method.getMethodKey());
					System.out.println("Written params: " + method.getWriteParams());
				}
			}
		});
		
		//Start to summarize the methods with their callees...
	}
	
	public static class WriterExplorer extends MethodVisitor {
		
		public int access;
		
		public String className;
		
		public String methodName;
		
		public String desc;
		
		//public Map<Integer, Boolean> paramMap = new HashMap<Integer, Boolean>();
		
		public WriterExplorer(MethodVisitor mv, 
				int access, 
				String className, 
				String methodName, 
				String desc, 
				String signature, 
				String[] exceptions,
				ClassInfo ownerClass) {
			super(Opcodes.ASM5, new MethodNode(Opcodes.ASM5, 
					access, 
					methodName, 
					desc, 
					signature, 
					exceptions) {
				
				@Override
				public void visitMethodInsn(int opcode, 
						String owner, 
						String name, 
						String desc, 
						boolean itf) {
					super.visitMethodInsn(opcode, owner, methodName, desc, itf);
					if (owner.equals("java.lang.Object") && name.equals("<init>")) {
						return ;
					}
					
					if (opcode == Opcodes.INVOKESTATIC) {
						
					}
				}
				
				@Override
				public void visitEnd() {
					String[] parsed = ClassInfoUtils.genMethodKey(className, methodName, this.desc);
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
					
					logger.info("Method key: " + methodKey);
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
					
					DependentValueInterpreter dvi = new DependentValueInterpreter(args, returnType);
					Analyzer a = new Analyzer(dvi);
					try {
						Frame[] fr = a.analyze(className, this);
						for (int i = 0; i < dvi.getParamList().size(); i++) {
							DependentValue val = dvi.getParamList().get(i);
							if (val.written) {
								info.addWriteParams(i);
							}
						}
						
						AbstractInsnNode insn = this.instructions.getFirst();
						while (insn != null) {
							if (insn instanceof MethodInsnNode) {
								MethodInsnNode methodInsn = (MethodInsnNode) insn;
								List<String> callee = new ArrayList<String>();
								callee.add(methodInsn.owner);
								callee.add(methodInsn.name);
								callee.add(methodInsn.desc);
								
								if (methodInsn.getOpcode() == Opcodes.INVOKESTATIC) {
									info.addStaticCallee(callee);
								} else {
									info.addCallee(callee);
								}
							}
							
							insn = insn.getNext();
						}
					} catch (Exception ex) {
						logger.info("Error: ", ex);
					}
				}
			});
		}
	}
	
	public static class ClassInfo {
		
		private String className;
		
		private String parent;
		
		private List<String> interfaces = new ArrayList<String>();
		
		private List<String> children = new ArrayList<String>();
		
		private Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
		
		public ClassInfo(String className) {
			this.className = className;
		}
		
		public String getClassName() {
			return this.className;
		}
		
		public void setParent(String parent) {
			this.parent = parent;
		}
		
		public String getParent() {
			return this.parent;
		}
		
		public void addInterface(String inter) {
			this.interfaces.add(inter);
		}
		
		public List<String> getInterfaces() {
			return this.interfaces;
		}
		
		public void addChild(String child) {
			this.children.add(child);
		}
		
		public List<String> getChildren() {
			return this.children;
		}
		
		public void addMethod(MethodInfo method) {
			this.methods.put(method.getMethodKey(), method);
		}
		
		public Map<String, MethodInfo> getMethods() {
			return methods;
		}
	}
	
	public static class MethodInfo {
		
		public static final int NO_CHECK = 0;
		
		public static final int PUBLIC = 1;
		
		public static final int PROTECTED = 2;
		
		public static final int DEFAULT = 3;
		
		public static final int PRIVATE = 4;
		
		public transient boolean summarized = false;
		
		private String methodKey;
		
		private TreeSet<Integer> writeParams = new TreeSet<Integer>();
		
		private Set<List<String>> staticCallees = new HashSet<List<String>>();
		
		private Set<List<String>> callees = new HashSet<List<String>>();
		
		private int level = -1;
		
		private boolean isFinal = false;
		
		public MethodInfo(String methodKey) {
			this.methodKey = methodKey;
		}
		
		public String getMethodKey() {
			return this.methodKey;
		}
		
		public void addWriteParams(int paramId) {			
			this.writeParams.add(paramId);
		}
		
		public TreeSet<Integer> getWriteParams() {
			return this.writeParams;
		}
		
		public void addStaticCallee(List<String> staticCallee) {
			this.staticCallees.add(staticCallee);
		}
		
		public Set<List<String>> getStaticCallees() {
			return this.staticCallees;
		}
		
		public void addCallee(List<String> callee) {
			this.callees.add(callee);
		}
		
		public Set<List<String>> getCallees() {
			return this.callees;
		}
		
		public void setLevel(int level) {
			this.level = level;
		}
		
		public int getLevel() {
			return this.level;
		}
		
		public void setFinal(boolean isFinal) {
			this.isFinal = isFinal;
		}
		
		public boolean isFinal() {
			return isFinal;
		}
	}
}
