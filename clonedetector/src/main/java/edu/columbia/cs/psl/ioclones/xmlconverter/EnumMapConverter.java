package edu.columbia.cs.psl.ioclones.xmlconverter;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class EnumMapConverter implements Converter {
	
	private final static Logger logger = LogManager.getLogger(EnumMapConverter.class);
	
	private static Map<Class, Map<String, Enum>> enumRecorder = new HashMap<Class, Map<String, Enum>>();
	
	public boolean canConvert(Class type) {
		if (type == null) {
			return false;
		}
		
        return type.isEnum() || Enum.class.isAssignableFrom(type);
    }

    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        //System.out.println("Source object: " + source.getClass());
        Enum enumObj = (Enum) source;
        //System.out.println("Name: " + enumObj.name());
        //System.out.println("Ordinal: " + enumObj.ordinal());
        
        if (!enumRecorder.containsKey(source.getClass())) {
        	Map<String, Enum> enumMap = new HashMap<String, Enum>();
        	enumMap.put(enumObj.name(), enumObj);
        	enumRecorder.put(source.getClass(), enumMap);
        } else {
        	Map<String, Enum> enumMap = enumRecorder.get(source.getClass());
        	if (!enumMap.containsKey(enumObj.name())) {
        		enumMap.put(enumObj.name(), enumObj);
        	}
        }
        
    	writer.setValue(((Enum) source).name());
    }

    @SuppressWarnings("unchecked")
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Class type = context.getRequiredType();
        if (type.getSuperclass() != Enum.class) {
            type = type.getSuperclass(); // polymorphic enums
        }
        String name = reader.getValue();
        //System.out.println("Check type and name: " + type.getName() + " " + name);
        //System.out.println("Current node name: " + reader.getNodeName());
        //System.out.println("Current node val: " + reader.getValue());
        try {
        	if (!enumRecorder.containsKey(type)) {
        		logger.error("Fail to retrieve enum type from recorder: " + type.getName());
        		return Enum.valueOf(type, name);
        	} else {
        		return enumRecorder.get(type).get(name);
        	}
            //return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            // failed to find it, do a case insensitive match
            for (Enum c : (Enum[])type.getEnumConstants())
                if (c.name().equalsIgnoreCase(name))
                    return c;
            
            // all else failed
            throw e;
        }
    }

}
