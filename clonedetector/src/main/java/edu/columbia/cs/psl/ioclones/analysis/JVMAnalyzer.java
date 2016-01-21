package edu.columbia.cs.psl.ioclones.analysis;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

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
		
		Map<String, ClassNode> records = new HashMap<String, ClassNode>();
		try {
			for (InputStream is: container) {
				ClassReader cr = new ClassReader(is);
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
					@Override
					public void visit(int version, 
							int access, 
							String name, 
							String signature, 
							String superName, 
							String[] interfaces) {
						this.cv.visit(version, access, name, signature, superName, interfaces);
						System.out.println("Name: " + name);
						ClassNode classNode = new ClassNode(name);
						System.out.println("Supername: " + superName);
						if (superName != null) {
							classNode.addParent(superName);
						}
						
						for (String inter: interfaces) {
							System.out.println("Interface: " + inter);
							classNode.addParent(inter);
						}
						System.out.println();
					}
				};
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
	
	public static class ClassNode {
		
		private String className;
		
		private List<String> parents = new ArrayList<String>();
		
		private List<String> children = new ArrayList<String>();
		
		public ClassNode(String className) {
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
	}
}
