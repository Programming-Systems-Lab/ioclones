package edu.columbia.cs.psl.ioclones.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class FlowCloneInstrumenter extends ClassVisitor {

	private String className;
	
	private String superName;
	
	private String[] interfaces;
	
	public FlowCloneInstrumenter(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}
	
	@Override
	public void visit(int version, 
			int access, 
			String className, 
			String signature, 
			String superName, 
			String[] interfaces) {
		this.cv.visit(version, access, className, signature, superName, interfaces);
		
		this.className = ClassInfoUtils.cleanType(className);
		this.superName = ClassInfoUtils.cleanType(superName);
		this.interfaces = new String[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			this.interfaces[i] = ClassInfoUtils.cleanType(interfaces[i]);
		}
	}
	
	@Override
	public MethodVisitor visitMethod(int access, 
			String name, 
			String desc, 
			String signature, 
			String[] exceptions) {
		MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
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
			FlowMethodObserver fmo = new FlowMethodObserver(mv, 
					this.className, 
					this.superName, 
					name, 
					desc, 
					signature, 
					exceptions);
			LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, fmo);
			fmo.setLocalVariablesSorter(lvs);
			return fmo.getLocalVariablesSorter();
		}
	}
}
