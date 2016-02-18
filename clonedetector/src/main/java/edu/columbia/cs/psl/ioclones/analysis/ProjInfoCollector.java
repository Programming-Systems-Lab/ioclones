package edu.columbia.cs.psl.ioclones.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.columbia.cs.psl.ioclones.utils.ClassDataTraverser;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class ProjInfoCollector {
	
	public final static Logger logger = LogManager.getLogger(ProjInfoCollector.class);
	
	public static Options options = new Options(); 
	
	static {
		options.addOption("cb", true, "codebase");
		options.getOption("cb").setRequired(true);
	}
	
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		String codebase = null;
		
		codebase = cmd.getOptionValue("cb");
		if (codebase == null) {
			System.err.println("Please specify input directory");
			System.exit(-1);
		}
		logger.info("Codebase: " + codebase);
		
		List<byte[]> container = new ArrayList<byte[]>();
		ClassDataTraverser.collectDir(codebase, container);
		logger.info("Total classes: " + container.size());
		
		final Map<String, Set<Integer>> methodRecords = new TreeMap<String, Set<Integer>>();
		
		for (int i = 0; i < container.size(); i++) {
			byte[] classdata = container.get(i);
			
			try {
				ClassReader analysisReader = new ClassReader(classdata);
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5) {
										
					String className;
					
					@Override
					public void visit(int version, 
							int access, 
							String name, 
							String signature, 
							String superName, 
							String[] interfaces) {
						super.visit(version, access, name, signature, superName, interfaces);
						this.className = ClassInfoUtils.cleanType(name);
					}
					
					@Override
					public MethodVisitor visitMethod(int access, 
							String name, 
							String desc, 
							String signature, 
							String[] exceptions) {
						MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
						boolean isSynthetic = ClassInfoUtils.checkAccess(access, Opcodes.ACC_SYNTHETIC);
						boolean isNative = ClassInfoUtils.checkAccess(access, Opcodes.ACC_NATIVE);
						boolean isInterface = ClassInfoUtils.checkAccess(access, Opcodes.ACC_INTERFACE);
						boolean isAbstract = ClassInfoUtils.checkAccess(access, Opcodes.ACC_ABSTRACT);
						if (isInterface || isSynthetic || isNative || isAbstract) {
							return mv;
						} else if (name.equals("toString") && desc.equals("()Ljava/lang/String;")) {
							return mv;
						} else if (name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z")) {
							return mv;
						} else if (name.equals("hashCode") && desc.equals("()I")) {
							return mv;
						} else if (name.equals("<init>") || name.equals("<clinit>")) {
							return mv;
						} else {
							mv = new LineNumberer(this.className, name, desc, mv, methodRecords);
							return mv;
						}
					}
				};
				analysisReader.accept(cv, ClassReader.EXPAND_FRAMES);
			} catch (Exception ex) {
				logger.info("Error: ", ex);
			}
		}
				
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("results/lineinfo.csv", true));
			String header = "method,lines\n";
			
			logger.info("Reporting method lines");
			logger.info("Method #: " + methodRecords.size());
			int totalLines = 0;
			StringBuilder sb = new StringBuilder();
			sb.append(header);
			for (String m: methodRecords.keySet()) {
				Set<Integer> lines = methodRecords.get(m);
				logger.info("Method: " + m);
				logger.info("Lines: " + lines.size());
				totalLines += lines.size();
				
				String info = m + "," + lines.size() + "\n";
				sb.append(info);
			}
			logger.info("Total lines: " + totalLines);
			bw.write(sb.toString());
			bw.close();	
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static class LineNumberer extends MethodVisitor {
		
		public Map<String, Set<Integer>> ptr; 
		
		public HashSet<Integer> lines = new HashSet<Integer>();
		
		public String methodKey;
		
		public LineNumberer(String className, 
				String methodName, 
				String desc, 
				MethodVisitor mv, 
				Map<String, Set<Integer>> methodRecords) {
			super(Opcodes.ASM5, mv);
			this.methodKey = ClassInfoUtils.genMethodKey(className, methodName, desc)[0];
			ptr = methodRecords;
		}
		
		@Override
		public void visitLineNumber(int line, Label start) {
			super.visitLineNumber(line, start);
			this.lines.add(line);
		}
		
		@Override
		public void visitEnd() {
			//System.out.println("Method key: " + this.methodKey);
			//System.out.println("Lines: " + lines);
			
			if (this.lines.size() > 0) {
				ptr.put(this.methodKey, this.lines);
			} else {
				logger.warn("0 line method: " + this.methodKey);
			}
			
			super.visitEnd();
		}
	}
	
	public static class ClassContainer {
		
		public String className;
		
		public Map<String, Integer> methodLines = new HashMap<String, Integer>();
	}

}
