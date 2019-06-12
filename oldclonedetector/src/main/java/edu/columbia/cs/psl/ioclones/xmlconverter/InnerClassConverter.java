package edu.columbia.cs.psl.ioclones.xmlconverter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class InnerClassConverter implements Converter {
	
	private static final Logger logger = LogManager.getLogger(InnerClassConverter.class);
	
	//private final ReflectionProvider reflectionProvider;
	
	private final XStream xstream;
	
	private Object convertLock = new Object();
	
	private Map<Class, Converter> defaultConverters = new HashMap<Class, Converter>();
	
	public InnerClassConverter(XStream xstream) {
		this.xstream = xstream;
	}
	
	public void registerDefaultConverter(Class clazz, Converter defaultConverter) {
		this.defaultConverters.put(clazz, defaultConverter);
	}

	@Override
	public boolean canConvert(Class type) {
		// TODO Auto-generated method stub
		if (type.getEnclosingClass() != null) {
			System.out.println("Inner class: " + type.getName());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		// TODO Auto-generated method stub
		
		//Need to climb the class tree?
		if (source != null) {
			Class clazz = source.getClass();
			
			synchronized(convertLock) {
				if (!defaultConverters.containsKey(clazz)) {
					//Need to climb the class tree?
					for (Field f: clazz.getDeclaredFields()) {
						if (f.isSynthetic()) {
							//f.setAccessible(true);
							//f.set(source, null);
							System.out.println("Omit field: " + f.getName());
							this.xstream.omitField(clazz, f.getName());
						}
					}
				}
				defaultConverters.put(clazz, null);
			}
		}
		
		if (source == null) {
            // todo: this is duplicated in TreeMarshaller.start()
            String name = this.xstream.getMapper().serializedClass(null);
            ExtendedHierarchicalStreamWriterHelper.startNode(writer, name, Mapper.Null.class);
            writer.endNode();
        } else {
            String name = this.xstream.getMapper().serializedClass(source.getClass());
            ExtendedHierarchicalStreamWriterHelper.startNode(writer, name, source.getClass());
            context.convertAnother(source);
            writer.endNode();
        }
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		// TODO Auto-generated method stub
		Class type = HierarchicalStreams.readClassType(reader, this.xstream.getMapper());
		System.out.println("Class type: " + type.getClass().getName());
		System.exit(-1);
        return context.convertAnother(null, type);
	}

}
