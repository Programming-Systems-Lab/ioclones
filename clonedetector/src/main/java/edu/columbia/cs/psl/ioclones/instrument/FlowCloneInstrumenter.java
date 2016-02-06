package edu.columbia.cs.psl.ioclones.instrument;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
	
	//private Type thisType;
	
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
		
		//this.thisType = Type.getType(className);
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
			TreeMap<Integer, Type> runtimeParams = new TreeMap<Integer, Type>();
			int curId = 0;
			if (!isStatic) {
				//runtimeParams.put(0, this.thisType);
				curId = 1;
			}
			
			if (args.length > 0) {
				for (int i = 0; i < args.length; i++) {
					Type curType = args[0];
					runtimeParams.put(curId, curType);
					int curSort = curType.getSort();
					if (curSort == Type.DOUBLE || curSort == Type.LONG) {
						curId += 2;
					} else {
						curId++;
					}
				}
			}
			
			FlowMethodObserver fmo = new FlowMethodObserver(mv, 
					this.className, 
					this.superName, 
					name, 
					desc, 
					runtimeParams, 
					signature, 
					exceptions, 
					isStatic);
			LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, fmo);
			fmo.setLocalVariablesSorter(lvs);
			return fmo.getLocalVariablesSorter();
		}
	}
}
