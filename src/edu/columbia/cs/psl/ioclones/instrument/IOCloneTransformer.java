package edu.columbia.cs.psl.ioclones.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;

public class IOCloneTransformer implements ClassFileTransformer {
	
	private static final Logger logger = LogManager.getLogger(IOCloneTransformer.class);
		
	private static boolean DEBUG = false;
	
	@Override
	public byte[] transform(ClassLoader loader, 
			String className, 
			Class<?> classBeingRedefined, 
			ProtectionDomain protectionDomain, 
			byte[] classfileBuffer) {
		try {
			if (protectionDomain != null) {
				String protection = protectionDomain.getCodeSource().getLocation().getPath();
				if (!ClassInfoUtils.checkProtectionDomain(protection)) {
					return classfileBuffer;
				}
			}
			
			if (className == null) {
				//When will the class name be null?lambda probably
				//logger.warn("Capture null class name");
				return classfileBuffer;
			}
			
			String name = ClassInfoUtils.cleanType(className);
			if (!ClassInfoUtils.shouldInstrument(name)) {
				//System.out.println("Black-out class: " + name);
				return classfileBuffer;
			}
			
			//System.out.println("Protection domain: " + protectionDomain.getCodeSource().getLocation().getFile());
			
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
			//IOCloneInstrumenter ioc = new IOCloneInstrumenter(new CheckClassAdapter(cw, false));
			IOCloneInstrumenter ioc = new IOCloneInstrumenter(cw);
			cr.accept(ioc, ClassReader.EXPAND_FRAMES);
			
			if (DEBUG) {
				File debugDir = new File("debug");
				if (!debugDir.exists()) {
					debugDir.mkdir();
				}
				
				File f = new File(debugDir.getAbsolutePath() + "/" + name + ".class");
				FileOutputStream fos = new FileOutputStream(f);
				//ByteArrayOutputStream bos = new ByteArrayOutputStream(cw.toByteArray().length);
				//bos.write(cw.toByteArray());
				//bos.writeTo(fos);
				fos.write(cw.toByteArray());
				fos.close();
			}
			
			return cw.toByteArray();
		} catch (Exception ex) {
			logger.error("Fail to transforme class: ", ex);
		}
		return classfileBuffer;
	}

}
