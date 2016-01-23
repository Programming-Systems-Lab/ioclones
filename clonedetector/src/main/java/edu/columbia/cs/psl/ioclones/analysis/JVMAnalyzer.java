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
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;

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
						this.classInfo = new ClassInfo(this.className);
						if (superName != null) {
							this.superName = ClassInfoUtils.cleanType(superName);
							this.classInfo.addParent(this.superName);
						}
						
						for (String inter: interfaces) {
							inter = ClassInfoUtils.cleanType(inter);
							this.classInfo.addParent(inter);
						}
						
						GlobalInfoRecorder.registerClassInfo(this.classInfo);
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
						
						if (isSynthetic) {
							return mv;
						} else if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
							return mv;
						} else if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
							return mv;
						} else if (name.equals("hashCode") && desc.equals("()I")) {
							return mv;
						} else if (isInterface || isAbstract) {
							String[] parsed = ClassInfoUtils.genMethodKey(this.className, name, desc);
							MethodInfo mi = new MethodInfo(parsed[0]);
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
			clazz.getMethods().forEach((mName, m)->{
				TreeSet<Integer> writtenParam = m.getWriteParams();
				if (writtenParam != null && writtenParam.size() > 0) {
					System.out.println("Direct writer: " + m.getMethodName());
					System.out.println("Written params: " + m.getWriteParams());
				}
			});
		}); 
	}
	
	public static class WriterExplorer extends MethodVisitor {
		
		public int access;
		
		public String className;
		
		public String methodName;
		
		public String desc;
		
		public Map<Integer, Boolean> paramMap = new HashMap<Integer, Boolean>();
		
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
				public void visitEnd() {
					String[] parsed = ClassInfoUtils.genMethodKey(className, methodName, this.desc);
					String methodKey = parsed[0];
					logger.info("Method key: " + methodKey);
					MethodInfo info = new MethodInfo(methodKey);
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
						a.analyze(className, this);
						for (int i = 0; i < dvi.getParamList().size(); i++) {
							DependentValue val = dvi.getParamList().get(i);
							if (val.written) {
								info.addWriteParams(i);
							}
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
		
		private List<String> parents = new ArrayList<String>();
		
		private List<String> children = new ArrayList<String>();
		
		private Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
		
		public ClassInfo(String className) {
			this.className = className;
		}
		
		public String getClassName() {
			return this.className;
		}
		
		public void addParent(String parent) {
			this.parents.add(parent);
		}
		
		public List<String> getParents() {
			return parents;
		}
		
		public void addChild(String child) {
			this.children.add(child);
		}
		
		public List<String> getChildren() {
			return this.children;
		}
		
		public void addMethod(MethodInfo method) {
			this.methods.put(method.getMethodName(), method);
		}
		
		public Map<String, MethodInfo> getMethods() {
			return methods;
		}
	}
	
	public static class MethodInfo {
		
		public transient boolean visited = false;
		
		private String methodName;
		
		private TreeSet<Integer> writeParams;
		
		private Set<String> callees;
		
		public MethodInfo(String methodName) {
			this.methodName = methodName;
		}
		
		public String getMethodName() {
			return this.methodName;
		}
		
		public void addWriteParams(int paramId) {
			if (this.writeParams == null) {
				this.writeParams = new TreeSet<Integer>();
			}
			
			this.writeParams.add(paramId);
		}
		
		public TreeSet<Integer> getWriteParams() {
			return this.writeParams;
		}
		
		public void addCallee(String callee) {
			if (this.callees == null) {
				this.callees = new HashSet<String>();
			}
			this.callees.add(callee);
		}
		
		public Set<String> getCallees() {
			return this.callees;
		}
	}
}
