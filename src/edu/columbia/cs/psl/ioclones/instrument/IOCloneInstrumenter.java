package edu.columbia.cs.psl.ioclones.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class IOCloneInstrumenter extends ClassVisitor {
	
	private String className;
	
	public IOCloneInstrumenter(ClassVisitor cv, String className) {
		super(Opcodes.ASM5, cv);
		this.className = className;
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
		} else {
			IOMethodObserver iom = new IOMethodObserver(mv, 
					this.className, 
					name, 
					desc, 
					signature, 
					exceptions);
			LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, iom);
			iom.setLocalVariablesSorter(lvs);
			return iom.getLocalVariablesSorter();
		}
	}

}
