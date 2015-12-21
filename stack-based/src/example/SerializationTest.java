package example;

import java.util.List;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class SerializationTest {
	
	public static void main(String[] args) {
		//Object test = Object.class;
		//Map test = new HashMap();
		//test.put(null, 5);
		//Object test = null;
		//Object[] test = {"1"};
		MyObject mo = new MyObject(5);
		List test = new ArrayList();
		test.add(mo);
		try {
			//ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
			//oos.writeObject(mo);
			
			//FileOutputStream fout = new FileOutputStream("ser/tmp.ser");
			//ObjectOutputStream oos = new ObjectOutputStream(fout);
			//oos.writeObject(mo);;
			//oos.close();
			
			String xmlString = IOUtils.fromObj2XML(test);
			System.out.println(xmlString);
			//Object revert = IOUtils.fromXML2Obj(xmlString);
			//System.out.println(revert.getClass());
			
			File toWrite = new File("ser/test.xml");
			BufferedWriter bw = new BufferedWriter(new FileWriter(toWrite));
			bw.write(xmlString);
			bw.close();
			
			File toRead = new File("ser/test.xml");
			Object obj = IOUtils.fromXML2Obj(toRead);
			if (obj != null)
				System.out.println(obj.getClass());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}

}
