package edu.columbia.cs.psl.ioclones.analysis;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
import edu.columbia.cs.psl.ioclones.pojo.MethodInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;

public class CalleeAnalyzer {
	
	private static final Logger logger = LogManager.getLogger(CalleeAnalyzer.class);
	
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
				cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
					@Override
					public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
						// TODO Auto-generated method stub
						return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
					}
				}, ClassReader.EXPAND_FRAMES);
				
				ClassReader analysisReader = new ClassReader(cw.toByteArray());
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
						this.cv.visit(version, access, name, signature, superName, interfaces);
						
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
				analysisReader.accept(cv, ClassReader.EXPAND_FRAMES);
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
		Map<String, ClassInfo> allClasses = GlobalInfoRecorder.getClassInfo();
		List<MethodInfo> notStable = new ArrayList<MethodInfo>();
		allClasses.forEach((name, clazz)->{
			System.out.println("Class name: " + clazz.getClassName());
			
			Map<String, MethodInfo> methods = clazz.getMethods();
			for (MethodInfo method: methods.values()) {				
				TreeSet<Integer> writtenParam = method.getWriteParams();
				if (writtenParam != null && writtenParam.size() > 0) {
					System.out.println("Direct writer: " + method.getMethodKey());
					System.out.println("Written params: " + method.getWriteParams());
				}
				
				int callSize = method.getFixedCallees().size() + method.getFloatingCallees().size();
				if (callSize > 0) {
					//System.out.println("Not fixed: " + method.methodKey + " " + callSize + " " + method.getRefSize());
					notStable.add(method);
				} else {
					method.stabelized = true;
				}
			}
		});
		
		//Start to summarize the methods with their callees...
		System.out.println("Not fixed methods");
		notStable.forEach(n->{
			System.out.println(n.getMethodKey());
			System.out.println("Fixed callee");
			n.getFixedCallees().forEach(f->{
				System.out.println(f.getMethodKey() + " " + f.getCalleeCallerBridge());
			});
			
			System.out.println("Not fixed callees");
			n.getFloatingCallees().forEach(f->{
				System.out.println(f.getMethodKey() + " " + f.getCalleeCallerBridge());
			});
		});
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
					
					DependentValueInterpreter dvi = new DependentValueInterpreter(args, returnType, info);
					Analyzer a = new Analyzer(dvi);
					try {
						Frame[] fr = a.analyze(className, this);
						for (int i = 0; i < dvi.getParamList().size(); i++) {
							DependentValue val = dvi.getParamList().get(i);
							if (val.written) {
								info.addWriteParams(i);
							}
						}
						
						info.insts = this.instructions;
						info.frames = fr;
						info.dvi = dvi;
					} catch (Exception ex) {
						logger.info("Error: ", ex);
					}
				}
			});
		}
	}
}
