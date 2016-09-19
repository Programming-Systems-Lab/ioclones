package edu.columbia.cs.psl.ioclones.analysis.dynamic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintPropagater.HitoLabel;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;

public class HitoTaintChecker {
		
	public static boolean checkTaint(Taint t, Object val, String execId) {
		if (t == null) {
			System.err.println("Output with no taint on method: " + execId + " val: " + val);
			return false;
		}
		
		LinkedList deps = t.getDependencies();
		if (deps == null) {
			System.err.println("Output with no deps on method: " + execId + " val: " + val);
			return false;
		}
		
		return true;
	}
	
	public static void interpretIO(Object val, Taint t, long execIdx) {
		if (val == null) {
			System.err.println("Null output: " + execIdx);
			return ;
		}
		
		if (t == null) {
			System.err.println("Output with no taint on method: " + execIdx + " val: " + val);
			return ;
		}
		
		LinkedList deps = t.getDependencies();
		//Some taints are copied, so also need to analyze labels...
		ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
		
		System.out.println("Output of " + execIdx + " val: " + val);
		if (deps != null && deps.getFirst() != null) {
			System.out.println("Analyzing deps");
			Node<Object> curNode = deps.getFirst();
			while (curNode != null) {
				ArrayList<HitoLabel> dep = (ArrayList<HitoLabel>)curNode.entry;
				for (HitoLabel d: dep) {
					if (d.execIdx >= execIdx) {
						System.out.println("Input: " + d.val);
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
						System.out.println("Input: " + lbl.val);
					}
				} else if (lbl.execIdx > execIdx) {
					System.out.println("Input: " + lbl.val);
				}
			}
		} else {
			System.out.println("No labels");
		}
		
	}
	
	public static void analyzeTaint(int val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		interpretIO(val, t, execIdx);
	}
	
	public static void analyzeTaint(short val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		interpretIO(val, t, execIdx);
	}
	
	public static void analyzeTaint(byte val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		interpretIO(val, t, execIdx);
	}
	
	public static void analyzeTaint(boolean val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		interpretIO(val, t, execIdx);
	}
	
	public static void analyzeTaint(char val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		interpretIO(val, t, execIdx);
	}
	
	public static void analyzeTaint(long val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		interpretIO(val, t, execIdx);
	}
	
	public static void analyzeTaint(float val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		interpretIO(val, t, execIdx);
	}
	
	public static void analyzeTaint(double val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		interpretIO(val, t, execIdx);
	}
	
	public static void analyzeTaint(String val, long execIdx) {
		if (val == null) {
			return ;
		}
				
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
					inputs.add(hl.val);
				}
			}
		}
		
		System.out.println("Output of " + execIdx + " val: " + val);
		for (Object o: inputs) {
			System.out.println("Input: " + o);
		}
	}
	
	public static void analyzeTaint(Object obj, long execIdx, int depth) {
		if (obj == null) {
			return ;
		}
		
		if (depth == 0) {
			return ;
		}
		
		if (obj.getClass().isArray()) {
			arrayChecker(obj, execIdx, depth);
		} else if (Collection.class.isAssignableFrom(obj.getClass())) {
			Collection collection = (Collection)obj;
			collectionChecker(collection, execIdx, depth);
		} else if (Map.class.isAssignableFrom(obj.getClass())) {
			Map map = (Map)obj;
			collectionChecker(map.values(), execIdx, depth);
		} else {
			objChecker(obj, execIdx, depth);
		}
	}
	
	public static void objChecker(Object obj, long execIdx, int depth) {
		if (obj == null) {
			return ;
		}
		
		Class clazz = obj.getClass();
		HashSet<Field> fields = FieldController.collectInstanceFields(clazz);
		try {
			for (Field f: fields) {
				if (f.getType() == int.class) {
					int val = f.getInt(obj);
					analyzeTaint(val, execIdx);
				} else if (f.getType() == short.class) {
					short val = f.getShort(obj);
					analyzeTaint(val, execIdx);
				} else if (f.getType() == boolean.class) {
					boolean val = f.getBoolean(obj);
					analyzeTaint(val, execIdx);
				} else if (f.getType() == byte.class) {
					byte val = f.getByte(obj);
					analyzeTaint(val, execIdx);
				} else if (f.getType() == char.class) {
					char val = f.getChar(obj);
					analyzeTaint(val, execIdx);
				} else if (f.getType() == float.class) {
					float val = f.getFloat(obj);
					analyzeTaint(val, execIdx);
				} else if (f.getType() == long.class) {
					long val = f.getLong(obj);
					analyzeTaint(val, execIdx);
				} else if (f.getType() == double.class) {
					double val = f.getDouble(obj);
					analyzeTaint(val, execIdx);
				} else if (String.class.isAssignableFrom(f.getType())) {
					String val = (String) f.get(obj);
					analyzeTaint(val, execIdx);
				} else {
					Object val = f.get(obj);
					analyzeTaint(val, execIdx, depth - 1);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void arrayChecker(Object obj, long execIdx, int depth) {
		if (obj == null) {
			return ;
		}
				
		int length = Array.getLength(obj);
		Class type = obj.getClass().getComponentType();
		if (type == int.class) {
			for (int i = 0; i < length; i++) {
				int val = Array.getInt(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else if (type == short.class) {
			for (int i = 0; i < length; i++) {
				short val = Array.getShort(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else if (type == char.class) {
			for (int i = 0; i < length; i++) {
				char val = Array.getChar(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else if (type == boolean.class) {
			for (int i = 0; i < length; i++) {
				boolean val = Array.getBoolean(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else if (type == byte.class) {
			for (int i = 0; i < length; i++) {
				byte val = Array.getByte(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else if (type == float.class) {
			for (int i = 0; i < length; i++) {
				float val = Array.getFloat(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else if (type == long.class) {
			for (int i = 0; i < length; i++) {
				long val = Array.getLong(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else if (type == double.class) {
			for (int i = 0; i < length; i++) {
				double val = Array.getDouble(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else if (String.class.isAssignableFrom(type)) {
			for (int i = 0; i < length; i++) {
				String val = (String)Array.get(obj, i);
				analyzeTaint(val, execIdx);
			}
		} else {
			for (int i = 0; i < length; i++) {
				Object val = Array.get(obj, i);
				analyzeTaint(val, execIdx, depth - 1);
			}
		}
	}
	
	public static void collectionChecker(Collection collection, long execIdx, int depth) {
		if (collection == null) {
			return ;
		}
		
		for (Object o: collection) {
			analyzeTaint(o, execIdx, depth - 1);
		}
	}
}
