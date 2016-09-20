package edu.columbia.cs.psl.ioclones.analysis.dynamic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintPropagater.HitoLabel;
import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;

public class HitoTaintChecker {
	
	public static boolean shouldCheck(Object val, long execIdx) {
		if (!shouldSerialize(val)) {
			return false;
		} else {
			Taint t = MultiTainter.getTaint(val);
			
			if (t == null) {
				//object array will not be checked though...
				return false;
			}
			
			ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
			if (labels == null) {
				return false;
			}
			
			boolean shouldCheck = false;
			for (HitoLabel l: labels) {
				if (l.execIdx == execIdx) {
					shouldCheck = true;
					break ;
				}
			}
			return shouldCheck;
		}
	}
		
	public static boolean shouldSerialize(Object val) {
		Class clazz = val.getClass();
		if (String.class.isAssignableFrom(clazz) 
				|| Boolean.class.isAssignableFrom(clazz) 
				|| Character.class.isAssignableFrom(clazz) 
				|| Number.class.isAssignableFrom(clazz)) {
			return false;
		} else {
			return true;
		}
	}
	
	public static void summarizeWrittenInputs(IORecord record, int depth) {
		System.out.println("Summarizing written inputs");
		for (Object o: record.preload.values()) {
			if (HitoTaintChecker.shouldCheck(o, record.getId())) {
				HitoTaintChecker.analyzeTaint(o, depth, record);
			}
		}
	}
	
	public static void interpretDeps(Object val, Taint t, IORecord record) {
		if (val == null) {
			return ;
		}
		
		if (t == null) {
			System.err.println("Value with no taint on method: " + record.getId() + " val: " + val);
			return ;
		}
		
		long execIdx = record.getId();
		System.out.println("Analyzing method " + execIdx + " val: " + val);		
		LinkedList deps = t.getDependencies();
		//Some taints are copied, so also need to analyze labels...
		ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
		
		if (deps != null && deps.getFirst() != null) {
			System.out.println("Analyzing deps");
			Node<Object> curNode = deps.getFirst();
			while (curNode != null) {
				ArrayList<HitoLabel> dep = (ArrayList<HitoLabel>)curNode.entry;
				for (HitoLabel d: dep) {
					if (d.execIdx >= execIdx) {
						System.out.println("Dep: " + d.val);
						record.registerInput(d.val, shouldSerialize(d.val));
					}
				}
				curNode = curNode.next;
			}
		} else {
			System.out.println("No deps");
		}
		
		if (labels != null && labels.size() > 0) {
			System.out.println("Analyzing labels");
			for (HitoLabel lbl: labels) {
				if (lbl.execIdx == execIdx) {
					//For handling the copy of taint
					if (!lbl.val.equals(val)) {
						System.out.println("Dep: " + lbl.val);
						record.registerInput(lbl.val, shouldSerialize(lbl.val));
					}
				} else if (lbl.execIdx > execIdx) {
					System.out.println("Dep: " + lbl.val);				
					record.registerInput(lbl.val, shouldSerialize(lbl.val));
				}
			}
		} else {
			System.out.println("No labels");
		}
	}
		
	public static void analyzeTaint(int val, IORecord record) {
		Taint t = MultiTainter.getTaint(val);
		record.registerOutput(val, false);
		interpretDeps(val, t, record);
	}
	
	public static void analyzeTaint(short val, IORecord record) {
		Taint t = MultiTainter.getTaint(val);
		record.registerOutput(val, false);
		interpretDeps(val, t, record);
	}
	
	public static void analyzeTaint(byte val, IORecord record) {
		Taint t = MultiTainter.getTaint(val);
		record.registerOutput(val, false);
		interpretDeps(val, t, record);
	}
	
	public static void analyzeTaint(boolean val, IORecord record) {
		Taint t = MultiTainter.getTaint(val);
		record.registerOutput(val, false);
		interpretDeps(val, t, record);
	}
	
	public static void analyzeTaint(char val, IORecord record) {
		Taint t = MultiTainter.getTaint(val);
		interpretDeps(val, t, record);
	}
	
	public static void analyzeTaint(long val, IORecord record) {
		Taint t = MultiTainter.getTaint(val);
		record.registerOutput(val, false);
		interpretDeps(val, t, record);
	}
	
	public static void analyzeTaint(float val, IORecord record) {
		Taint t = MultiTainter.getTaint(val);
		record.registerOutput(val, false);
		interpretDeps(val, t, record);
	}
	
	public static void analyzeTaint(double val, IORecord record) {
		Taint t = MultiTainter.getTaint(val);
		record.registerOutput(val, false);
		interpretDeps(val, t, record);
	}
	
	public static void analyzeTaint(String val, IORecord record) {
		if (val == null) {
			return ;
		}
		
		long execIdx = record.getId();
		record.registerOutput(val, false);
		System.out.println("Value of " + execIdx + " val: " + val);
		//string's deps are in labels, not in deps
		HashSet<Object> inputs = new HashSet<Object>();
		for (int i = 0; i < val.length(); i++) {
			Taint t = MultiTainter.getTaint(val.charAt(i));
			
			if (t == null) {
				continue ;
			}
			
			if (t.lbl == null) {
				continue ;
			}
			
			ArrayList<HitoLabel> deps = (ArrayList<HitoLabel>)t.getLabel();
			for (HitoLabel hl : deps) {
				if (hl.execIdx >= execIdx) {
					System.out.println("Dep: " + hl.val);
					record.registerInput(hl.val, shouldSerialize(hl.val));
				}
			}
		}
	}
	
	public static void analyzeTaint(Object obj, int depth, IORecord record) {
		if (obj == null) {
			return ;
		}
		
		if (depth == 0) {
			return ;
		}
		
		if (obj.getClass().isArray()) {
			arrayChecker(obj, depth, record);
		} else if (Collection.class.isAssignableFrom(obj.getClass())) {
			Collection collection = (Collection)obj;
			collectionChecker(collection, depth, record);
		} else if (Map.class.isAssignableFrom(obj.getClass())) {
			Map map = (Map)obj;
			collectionChecker(map.values(), depth, record);
		} else {
			objChecker(obj, depth, record);
		}
	}
	
	public static void objChecker(Object obj, int depth, IORecord record) {
		if (obj == null) {
			return ;
		}
		
		Class clazz = obj.getClass();
		HashSet<Field> fields = FieldController.collectInstanceFields(clazz);
		try {
			for (Field f: fields) {
				if (f.getType() == int.class) {
					int val = f.getInt(obj);
					analyzeTaint(val, record);
				} else if (f.getType() == short.class) {
					short val = f.getShort(obj);
					analyzeTaint(val, record);
				} else if (f.getType() == boolean.class) {
					boolean val = f.getBoolean(obj);
					analyzeTaint(val, record);
				} else if (f.getType() == byte.class) {
					byte val = f.getByte(obj);
					analyzeTaint(val, record);
				} else if (f.getType() == char.class) {
					char val = f.getChar(obj);
					analyzeTaint(val, record);
				} else if (f.getType() == float.class) {
					float val = f.getFloat(obj);
					analyzeTaint(val, record);
				} else if (f.getType() == long.class) {
					long val = f.getLong(obj);
					analyzeTaint(val, record);
				} else if (f.getType() == double.class) {
					double val = f.getDouble(obj);
					analyzeTaint(val, record);
				} else if (String.class.isAssignableFrom(f.getType())) {
					String val = (String) f.get(obj);
					analyzeTaint(val, record);
				} else {
					Object val = f.get(obj);
					analyzeTaint(val, depth - 1, record);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void arrayChecker(Object obj, int depth, IORecord record) {
		if (obj == null) {
			return ;
		}
				
		int length = Array.getLength(obj);
		Class type = obj.getClass().getComponentType();
		if (type == int.class) {
			for (int i = 0; i < length; i++) {
				int val = Array.getInt(obj, i);
				analyzeTaint(val, record);
			}
		} else if (type == short.class) {
			for (int i = 0; i < length; i++) {
				short val = Array.getShort(obj, i);
				analyzeTaint(val, record);
			}
		} else if (type == char.class) {
			for (int i = 0; i < length; i++) {
				char val = Array.getChar(obj, i);
				analyzeTaint(val, record);
			}
		} else if (type == boolean.class) {
			for (int i = 0; i < length; i++) {
				boolean val = Array.getBoolean(obj, i);
				analyzeTaint(val, record);
			}
		} else if (type == byte.class) {
			for (int i = 0; i < length; i++) {
				byte val = Array.getByte(obj, i);
				analyzeTaint(val, record);
			}
		} else if (type == float.class) {
			for (int i = 0; i < length; i++) {
				float val = Array.getFloat(obj, i);
				analyzeTaint(val, record);
			}
		} else if (type == long.class) {
			for (int i = 0; i < length; i++) {
				long val = Array.getLong(obj, i);
				analyzeTaint(val, record);
			}
		} else if (type == double.class) {
			for (int i = 0; i < length; i++) {
				double val = Array.getDouble(obj, i);
				analyzeTaint(val, record);
			}
		} else if (String.class.isAssignableFrom(type)) {
			for (int i = 0; i < length; i++) {
				String val = (String)Array.get(obj, i);
				analyzeTaint(val, record);
			}
		} else {
			for (int i = 0; i < length; i++) {
				Object val = Array.get(obj, i);
				analyzeTaint(val, depth - 1, record);
			}
		}
	}
	
	public static void collectionChecker(Collection collection, int depth, IORecord record) {
		if (collection == null) {
			return ;
		}
		
		for (Object o: collection) {
			analyzeTaint(o, depth - 1, record);
		}
	}
}
