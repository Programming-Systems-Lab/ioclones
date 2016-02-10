package edu.columbia.cs.psl.ioclones;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;


public class Main {
	
	public static final Logger logger = LogManager.getLogger(Main.class);
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		//logger.info("Loading class info");
		//IOUtils.unzipClassInfo();
		IOUtils.loadMethodIODeps("cb");
		
		File clazz = new File(args[0]);

		final ClassReader cr1 = new ClassReader(new FileInputStream(clazz));
//		PrintWriter pw = new PrintWriter(new FileWriter("z.txt"));
		PrintWriter pw = new PrintWriter(System.out);
		/*ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
			@Override
			protected String getCommonSuperClass(String type1, String type2) {
				try {
					return super.getCommonSuperClass(type1, type2);
				} catch (Exception ex) {
					//					System.err.println("err btwn " + type1 + " " +type2);
					return "java/lang/Unknown";
				}
			}
		};*/
		
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		
		cr1.accept(new ClassVisitor(Opcodes.ASM5, cw1) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
				return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
			}
		}, ClassReader.EXPAND_FRAMES);
		
		final ClassReader cr = new ClassReader(cw1.toByteArray());
		TraceClassVisitor tcv = new TraceClassVisitor(null,new Textifier(),pw);
		//ClassWriter tcv = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
		ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, tcv) {
			String className;

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, name, signature, superName, interfaces);
				this.className = name;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
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
					mv = new DependencyAnalyzer(className, access, name, desc, signature, exceptions, mv, true);
					//mv = new CalleeAnalyzer(className, access, name, desc, signature, exceptions, mv, true);
					return mv;
				}
			}
		};
		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		pw.flush();
	}
}
