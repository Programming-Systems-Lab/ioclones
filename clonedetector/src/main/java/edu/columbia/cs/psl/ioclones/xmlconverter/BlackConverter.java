package edu.columbia.cs.psl.ioclones.xmlconverter;

import java.io.Reader;
import java.io.Writer;
import java.util.StringTokenizer;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class BlackConverter implements Converter {

	@Override
	public boolean canConvert(Class arg0) {
		// TODO Auto-generated method stub
		if (arg0 == null) {
			return false;
		}
		
		/*if (Reader.class.isAssignableFrom(arg0) || Writer.class.isAssignableFrom(arg0) || StringTokenizer.class.isAssignableFrom(arg0)) {
			System.out.println("Capture one");
		}*/
		return Reader.class.isAssignableFrom(arg0) || Writer.class.isAssignableFrom(arg0) || StringTokenizer.class.isAssignableFrom(arg0);
	}

	@Override
	public void marshal(Object arg0, HierarchicalStreamWriter arg1, MarshallingContext arg2) {
		// TODO Auto-generated method stub
		//System.out.println("Marshal nothing");
		//System.out.println("Context: " + arg2.keys());
		//arg1.setValue("Non-convertable: " + arg0.getClass());
		//arg1.close();
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
		// TODO Auto-generated method stub
		//System.out.println("Unmarshal to null");
		return null;
	}

}
