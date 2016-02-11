package edu.columbia.cs.psl.ioclones.utils;

import java.util.Set;

import com.cedarsoftware.util.DeepEquals;
import com.cedarsoftware.util.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

public class DeepHash {
	
	public static int deepHash(Object obj) {
		Set<Object> visited = new HashSet<>();
        LinkedList<Object> stack = new LinkedList<>();
        stack.addFirst(obj);
        int hash = 0;

        while (!stack.isEmpty())
        {
            obj = stack.removeFirst();
            if (obj == null || visited.contains(obj))
            {
                continue;
            }

            visited.add(obj);

            if (obj.getClass().isArray())
            {
                int len = Array.getLength(obj);
                for (int i = 0; i < len; i++)
                {
                    stack.addFirst(Array.get(obj, i));
                }
                continue;
            }

            if (obj instanceof Collection)
            {
                stack.addAll(0, (Collection)obj);
                continue;
            }

            if (obj instanceof Map)
            {
                stack.addAll(0, ((Map)obj).keySet());
                stack.addAll(0, ((Map)obj).values());
                continue;
            }
            
            if (obj instanceof Double) {
            	double d = (double) obj;
            	BigDecimal bd = new BigDecimal(d).setScale(2, BigDecimal.ROUND_HALF_UP);
            	Double after = new Double(bd.doubleValue());
            	hash += after.hashCode();
            	continue ;
            }
            
            if (obj instanceof Float) {
            	float f = (float) obj;
            	BigDecimal bd = new BigDecimal(f).setScale(2, BigDecimal.ROUND_HALF_UP);
            	Float after = new Float(bd.floatValue());
            	hash += after.hashCode();
            	hash += after.hashCode();
            }
            
            /*if (obj instanceof Double || obj instanceof Float)
            {
            	// just take the integral value for hashcode
            	// equality tests things more comprehensively
            	stack.add(Math.round(((Number) obj).doubleValue()));
            	continue;
            }*/
            
            if (DeepEquals.hasCustomHashCode(obj.getClass()))
            {   // A real hashCode() method exists, call it.
                hash += obj.hashCode();
                continue;
            }
           
            Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(obj.getClass());
            for (Field field : fields)
            {
                try
                {
                    stack.addFirst(field.get(obj));
                }
                catch (Exception ignored) { }
            }
        }
        return hash;
	}

}
