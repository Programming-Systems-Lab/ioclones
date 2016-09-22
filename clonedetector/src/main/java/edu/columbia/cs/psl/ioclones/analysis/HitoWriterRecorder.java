package edu.columbia.cs.psl.ioclones.analysis;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.google.gson.reflect.TypeToken;

import edu.columbia.cs.psl.ioclones.config.IOCloneConfig;
import edu.columbia.cs.psl.ioclones.instrument.DynFlowObserver;
import edu.columbia.cs.psl.ioclones.utils.ClassDataTraverser;
import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class HitoWriterRecorder {
	
	private static final Logger logger = LogManager.getLogger(HitoAnalyzer.class);
	
	private static final Options options = new Options();
	
	public static HashSet<String> WRITERS = new HashSet<String>();
	
	private static int counter = 0;
	
	static {
		options.addOption("src", true, "source codebase");
		options.getOption("src").setRequired(true);
	}
	
	public static void main(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		String codebase = null;
		
		codebase = cmd.getOptionValue("src");
		if (codebase == null) {
			System.err.println("Please specify source codebase");
			System.exit(-1);
		}
		File sourceDir = new File(codebase);
		if (!sourceDir.exists()) {
			System.err.println("Invalid source codebase: " + sourceDir.getAbsolutePath());
			System.exit(-1);
		}
		System.out.println("Source codebase: " + codebase);
		
		try {
			//Add to classpath
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
		    method.setAccessible(true);
		    method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{sourceDir.toURI().toURL()});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		HashSet<String> allCalls = new HashSet<String>();
		if (sourceDir.isDirectory())
			processDirectory(sourceDir, true, allCalls);
		else if (sourceDir.getName().endsWith(".jar") || sourceDir.getName().endsWith(".zip") || sourceDir.getName().endsWith(".war"))
			processZip(sourceDir, allCalls);
		else if (sourceDir.getName().endsWith(".class"))
			processClass(sourceDir, allCalls);
		else {
			System.err.println("Unknown type for path " + sourceDir.getName());
			System.exit(-1);
		}
		
		System.out.println("Total calls: " + allCalls.size());
		try {
			for (String c: allCalls) {
				isWriter(c);
			}
			
			System.out.println("Total writer: " + WRITERS.size());
			for (String w: WRITERS) {
				System.out.println(w);
			}
			
			File toWrite = new File("./config/writers.json");
			TypeToken<HashSet<String>> token = new TypeToken<HashSet<String>>(){};
			IOUtils.writeJson(WRITERS, token, toWrite.getAbsolutePath());
			System.out.println("Writers recorded in: " + toWrite.getAbsolutePath());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void isWriter(String ownerName) {		
		try {
			LinkedList<String> queue = new LinkedList<String>();
			queue.add(ownerName);
			while (queue.size() > 0) {
				String curName = queue.removeFirst();
				if (curName.equals("java.io.Writer") || curName.equals("java.io.OutputStream")) {
					WRITERS.add(ownerName);
					return ;
				}
				
				Class curClass = Class.forName(curName);
				if (curClass.getSuperclass() != null) {
					queue.add(curClass.getSuperclass().getName());
				}
				
				if (curClass.getInterfaces() != null) {
					for (Class inter: curClass.getInterfaces()) {
						queue.add(inter.getName());
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
		
	public static void analyze(String path, InputStream is, HashSet<String> allCalls) {
		System.out.println("Processing: " + path);
		byte[] classData = ClassDataTraverser.cleanClass(is);
		try {
			ClassReader cr = new ClassReader(classData);
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {				
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
					} else if (isNative || isInterface || isAbstract) {
						return mv;
					} else {
						MethodVisitor opener = new MethodVisitor(Opcodes.ASM5, mv) {
							
							@Override
							public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
								if (!name.equals("<init>")) {
									String call = owner.replace("/", ".");
									allCalls.add(call);
								}
							}
						};
						return opener;
					}
				}
			};
			cr.accept(cv, ClassReader.EXPAND_FRAMES);
		} catch (Exception ex) {
			logger.error("Fail to instrument file: " + path);
			logger.error("Error msg: ", ex);
		}
	}
	
	private static void processClass(File f, HashSet<String> allCalls) {
		try {
			counter++;
			if (counter % 100 == 0) {
				System.out.println("Process #" + counter + " files");
			}
			
			String name = f.getName();
			InputStream is = new FileInputStream(f);
			analyze(f.getAbsolutePath(), is, allCalls);
			
			is.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void processDirectory(File f, boolean isFirstLevel, HashSet<String> allCalls) {
		if (f.getName().equals(".AppleDouble"))
			return;
		
		for (File fi : f.listFiles()) {
			if (fi.isDirectory())
				processDirectory(fi, false, allCalls);
			else if (fi.getName().endsWith(".class"))
				processClass(fi, allCalls);
			else if (fi.getName().endsWith(".jar") || fi.getName().endsWith(".zip") || fi.getName().endsWith(".war"))
				processZip(fi, allCalls);
		}
	}

	/**
	 * Handle Jar file, Zip file and War file
	 */
	public static void processZip(File f, HashSet<String> allCalls) {
		try {
			ZipFile zip = new ZipFile(f);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();

				if (e.getName().endsWith(".class")) {
					try {
						ZipEntry outEntry = new ZipEntry(e.getName());

						analyze(f.getAbsolutePath(), zip.getInputStream(e), allCalls);
					} catch (ZipException ex) {
						ex.printStackTrace();
						continue;
					}
				} else if (e.getName().endsWith(".jar")) {
					Random r = new Random();
					String markFileName = Long.toOctalString(System.currentTimeMillis())
						+ Integer.toOctalString(r.nextInt(10000))
						+ e.getName().replace("/", "");
					File tmp = new File("/tmp/" + markFileName);
					if (tmp.exists())
						tmp.delete();
					FileOutputStream fos = new FileOutputStream(tmp);
					byte buf[] = new byte[1024];
					int len;
					InputStream is = zip.getInputStream(e);
					while ((len = is.read(buf)) > 0) {
						fos.write(buf, 0, len);
					}
					is.close();
					fos.close();

					
					processZip(tmp, allCalls);
					tmp.delete();
					
					is.close();
				}
			}
			
			zip.close();
		} catch (Exception e) {
			System.err.println("Unable to process zip/jar: " + f.getAbsolutePath());
			e.printStackTrace();
		}
	}
}


