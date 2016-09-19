package edu.columbia.cs.psl.ioclones.analysis.dynamic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;

public class FieldController {
	
	private static HashMap<Class, HashSet<Field>> cache = new HashMap<Class, HashSet<Field>>();
	
	public static synchronized HashSet<Field> collectInstanceFields(Class<?> clazz) {
		if (cache.containsKey(clazz)) {
			return cache.get(clazz);
		}
		
		HashSet<Field> ret = new HashSet<Field>();
		try {
			Field[] fs = clazz.getDeclaredFields();
			for (Field f: fs) {
				int modifiers = f.getModifiers();
				//No need to handle static fields here
				if (Modifier.isStatic(modifiers)) {
					continue ;
				}
				
				if (f.isSynthetic()) {
					continue ;
				}
				
				f.setAccessible(true);
				ret.add(f);
			}
			
			//Interface contains final static field, no need to handle here
			Class<?> superClazz = clazz.getSuperclass();
			if (superClazz != null) {
				HashSet<Field> superFields = collectInstanceFields(superClazz);
				
				if (superFields != null && superFields.size() > 0) {
					ret.addAll(superFields);
				}
			}
			
			cache.put(clazz, ret);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return ret;
	}

}
