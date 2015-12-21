package edu.columbia.cs.psl.clones;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;


public class Main {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		File clazz = new File(args[0]);

		final ClassReader cr1 = new ClassReader(new FileInputStream(clazz));
//		PrintWriter pw = new PrintWriter(new FileWriter("z.txt"));
		PrintWriter pw = new PrintWriter(System.out);
		ClassWriter cw1 = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
			@Override
			protected String getCommonSuperClass(String type1, String type2) {
				try {
					return super.getCommonSuperClass(type1, type2);
				} catch (Exception ex) {
					//					System.err.println("err btwn " + type1 + " " +type2);
					return "java/lang/Unknown";
				}
			}
		};
		cr1.accept(new ClassVisitor(Opcodes.ASM4, cw1) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// TODO Auto-generated method stub
				return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
			}
		}, ClassReader.EXPAND_FRAMES);
		final ClassReader cr = new ClassReader(cw1.toByteArray());
		TraceClassVisitor tcv = new TraceClassVisitor(null,new Textifier(),pw);
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
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				
				mv =new DependencyAnalyzer(className, access, name, desc, signature, exceptions, mv);
				return mv;
			}
		};
		cr.accept(cv, ClassReader.EXPAND_FRAMES);
		pw.flush();
	}
}
