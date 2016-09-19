package edu.columbia.cs.psl.ioclones.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.cs.psl.ioclones.DependencyAnalyzer;
import edu.columbia.cs.psl.ioclones.config.IOCloneConfig;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class FlowCloneTransformer implements ClassFileTransformer {
		
	private static final Logger logger = LogManager.getLogger(FlowCloneTransformer.class);
	
	@Override
	public byte[] transform(ClassLoader loader, 
			String className, 
			Class<?> classBeingRedefined, 
			ProtectionDomain protectionDomain, 
			byte[] classfileBuffer) {
		try {			
			if (protectionDomain != null) {
				String protection = protectionDomain.getCodeSource().getLocation().getPath();
				//System.out.println("Protection domain: " + protection);
				if (!ClassInfoUtils.checkProtectionDomain(protection)) {
					return classfileBuffer;
				}
				
				if (protection.matches(".*CloneDetector.*.jar")) {
					//System.out.println("Capture file: " + protection);
					return classfileBuffer;
				}
			}
			
			if (className == null) {
				//When will the class name be null?lambda probably
				//logger.warn("Capture null class name");
				return classfileBuffer;
			}
			
			String name = ClassInfoUtils.cleanType(className);
			if (!ClassInfoUtils.shouldInstrument(name)) {
				//System.out.println("Black-out class: " + name);
				return classfileBuffer;
			}
			
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
				@Override
				protected String getCommonSuperClass(String type1, String type2) {
					try {
						return super.getCommonSuperClass(type1, type2);
					} catch (Exception ex) {
						logger.error("Common super exception: ", ex);
						return "java/lang/Unknown";
					}
				}
			};
			
			cr.accept(new ClassVisitor(Opcodes.ASM5, cw1) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					// TODO Auto-generated method stub
					return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
				}
			}, ClassReader.EXPAND_FRAMES);
			
			ClassReader analysisReader = new ClassReader(cw1.toByteArray());
			ClassWriter analysisWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			FlowCloneInstrumenter fci = new FlowCloneInstrumenter(new CheckClassAdapter(analysisWriter, false));
			//FlowCloneInstrumenter fci = new FlowCloneInstrumenter(analysisWriter);
			ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, fci) {
				String className;

				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					super.visit(version, access, name, signature, superName, interfaces);
					this.className = name;
				}

				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					// TODO Auto-generated method stub
					MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
					boolean isInterface = ClassInfoUtils.checkAccess(access, Opcodes.ACC_INTERFACE);
					boolean isSynthetic = ClassInfoUtils.checkAccess(access, Opcodes.ACC_SYNTHETIC);
					if (isInterface || isSynthetic) {
						return mv;
					} else if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
						return mv;
					} else if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
						return mv;
					} else if (name.equals("hashCode") && desc.equals("()I")) {
						return mv;
					} else {
						mv = new DependencyAnalyzer(this.className, 
								access, 
								name, 
								desc, 
								signature, 
								exceptions, 
								mv, 
								true, 
								false, 
								true, 
								false);
						return mv;
					}
				}
			};
			analysisReader.accept(cv, ClassReader.EXPAND_FRAMES);
			
			if (IOCloneConfig.getInstance().isDebug()) {
				File debugDir = new File("debug");
				if (!debugDir.exists()) {
					debugDir.mkdir();
				}
				
				File f = new File(debugDir.getAbsolutePath() + "/" + name + ".class");
				FileOutputStream fos = new FileOutputStream(f);
				//ByteArrayOutputStream bos = new ByteArrayOutputStream(cw.toByteArray().length);
				//bos.write(cw.toByteArray());
				//bos.writeTo(fos);
				fos.write(analysisWriter.toByteArray());
				fos.close();
			}
			
			return analysisWriter.toByteArray();
		} catch (Exception ex) {
			logger.error("Fail to transform class: ", ex);
		}
		return classfileBuffer;
	}

}
