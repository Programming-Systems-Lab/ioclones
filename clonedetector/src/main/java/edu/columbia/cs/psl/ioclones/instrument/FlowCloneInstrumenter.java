package edu.columbia.cs.psl.ioclones.instrument;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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
			boolean isStatic = ClassInfoUtils.checkAccess(access, Opcodes.ACC_STATIC);
			Type[] args = Type.getArgumentTypes(desc);
			List<Integer> runtimeParamList = new ArrayList<Integer>();
			int initId = 0;
			if (!isStatic) {
				initId = 1;
			}
			
			if (args.length > 0) {
				runtimeParamList.add(initId);
				Type lastType = args[0];
				for (int i = 1; i < args.length; i++) {
					int lastSort = lastType.getSort();
					int lastId = runtimeParamList.get(runtimeParamList.size() - 1);
					if (lastSort == Type.DOUBLE || lastSort == Type.LONG) {
						runtimeParamList.add(lastId + 2);
					} else {
						runtimeParamList.add(lastId + 1);
					}
					lastType = args[i];
				}
			}
			
			FlowMethodObserver fmo = new FlowMethodObserver(mv, 
					this.className, 
					this.superName, 
					name, 
					desc, 
					runtimeParamList, 
					signature, 
					exceptions);
			LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, fmo);
			fmo.setLocalVariablesSorter(lvs);
			return fmo.getLocalVariablesSorter();
		}
	}
}
