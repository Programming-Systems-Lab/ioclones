package edu.columbia.cs.psl.ioclones.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import edu.columbia.cs.psl.ioclones.SiteAnalyzer;
import edu.columbia.cs.psl.ioclones.analysis.PreAnalyzer.WriterExplorer;
import edu.columbia.cs.psl.ioclones.pojo.ClassInfo;
import edu.columbia.cs.psl.ioclones.utils.ClassDataTraverser;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.GlobalInfoRecorder;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class SiteMain {
	
	private static final Logger logger = LogManager.getLogger(PreAnalyzer.class);
	
	private static final Options options = new Options();
	
	static {
		options.addOption("cb", true, "codebase");
		options.getOption("cb").setRequired(true);
	}
	
	public static void main(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		String codebase = null;
		
		codebase = cmd.getOptionValue("cb");
		if (codebase == null) {
			System.err.println("Please specify input directory");
			System.exit(-1);
		}
		logger.info("Codebase: " + codebase);
		
		IOUtils.loadMethodIODeps("cb");
		
		List<byte[]> container = new ArrayList<byte[]>();
		ClassDataTraverser.collectDir(codebase, container);
		//container = ClassDataTraverser.filter(container, "java/util/HashMap");
		
		logger.info("Classes to analyze: " + container.size());
		logger.info("Initialization phase");
		
		int counter = 0;
		Set<AbstractInsnNode> readInputParams = new HashSet<AbstractInsnNode>();
		Set<AbstractInsnNode> readNonInputParams = new HashSet<AbstractInsnNode>();
		for (byte[] classData: container) {
			counter++;
			if (counter % 1000 == 0) {
				logger.info("Analyzed: " + counter);
			}
			
			try {
				byte[] copy = Arrays.copyOf(classData, classData.length);
				
				ClassReader analysisReader = new ClassReader(copy);
				ClassWriter analysisWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, analysisWriter) {
					String internalClassName;
					
					String className;
					
					ClassInfo classInfo;
					
					@Override
					public void visit(int version, 
							int access, 
							String name, 
							String signature, 
							String superName, 
							String[] interfaces) {
						super.visit(version, access, name, signature, superName, interfaces);
						this.internalClassName = name;
						this.className = ClassInfoUtils.cleanType(name);
						this.classInfo = GlobalInfoRecorder.queryClassInfo(this.className);
					}
					
					@Override
					public MethodVisitor visitMethod(int access, 
							String name, 
							String desc, 
							String signature, 
							String[] exceptions) {
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
							SiteAnalyzer sa = new SiteAnalyzer(className, 
									access, 
									name, 
									desc, 
									signature, 
									exceptions, 
									mv, 
									readInputParams,
									readNonInputParams, 
									true,
									true, 
									true);
							return sa;
						}
					}
				};
				analysisReader.accept(cv, ClassReader.EXPAND_FRAMES);
			} catch (Exception ex) {
				logger.error("Error: ", ex);
			}
		}
		
		System.out.println("Direct read from input params: " + readInputParams.size());
		System.out.println("Read from non input params: " + readNonInputParams.size());
	}

}
