package edu.columbia.cs.psl.ioclones.instrument;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.cs.psl.ioclones.utils.ClassInfoUtils;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

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
			String name = ClassInfoUtils.cleanType(className);
			
			if (!ClassInfoUtils.shouldInstrument(name)) {
				//System.out.println("Black-out class: " + name);
				return classfileBuffer;
			}
			
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
			IOCloneInstrumenter ioc = new IOCloneInstrumenter(new CheckClassAdapter(cw, false), name);
			cr.accept(ioc, ClassReader.EXPAND_FRAMES);
			
			if (DEBUG) {
				File debugDir = new File("./debug");
				if (!debugDir.exists()) {
					debugDir.mkdir();
				}
				
				FileOutputStream fos = 
						new FileOutputStream(debugDir.getAbsolutePath() + "/" + name + ".class");
				ByteArrayOutputStream bos = new ByteArrayOutputStream(cw.toByteArray().length);
				bos.write(cw.toByteArray());
				bos.writeTo(fos);
				fos.close();
			}
			
			return cw.toByteArray();
		} catch (Exception ex) {
			logger.error("Fail to transforme class: ", ex);
		}
		return classfileBuffer;
	}

}
