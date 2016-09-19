package edu.columbia.cs.psl.ioclones.analysis.dynamic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

public class HitoTaintPropagater {
		
	public static ArrayList<HitoLabel> newLabels(Object val, long execIdx) {
		ArrayList<HitoLabel> labels = new ArrayList<HitoLabel>();
		
		HitoLabel lbl = new HitoLabel();
		lbl.execIdx = execIdx;
		lbl.val = val;
		labels.add(lbl);
		
		return labels;
	}
	
	public static void handleTaint(Taint t, Object val, long execIdx) {
		Object labelObj = t.getLabel();
		ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)labelObj;
		HitoLabel lbl = new HitoLabel();
		lbl.execIdx = execIdx;
		lbl.val = val;
		labels.add(lbl);
	}
	
	public static int propagateTaint(int val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedInt(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static short propagateTaint(short val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedShort(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static byte propagateTaint(byte val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedByte(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static boolean propagateTaint(boolean val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedBoolean(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static char propagateTaint(char val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedChar(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static float propagateTaint(float val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedFloat(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static double propagateTaint(double val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedDouble(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static long propagateTaint(long val, long execIdx) {
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedLong(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static void propagateTaint(String val, long execIdx) {
		if (val == null) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			t = new Taint(labels);
			MultiTainter.taintedObject(val, t);
		} else {
			for (int i = 0; i < val.length(); i++) {
				propagateTaint(val.charAt(i), execIdx);
			}
		}
	}
	
	public static void propagateTaint(Object obj, long execIdx, int depth) {
		if (obj == null) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(obj);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(obj, execIdx);
			t = new Taint(labels);
			MultiTainter.taintedObject(obj, t);
		} else {
			ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
			if (labels == null) {
				labels = newLabels(obj, execIdx);
				t.lbl = labels;
			} else {
				HitoLabel label = new HitoLabel();
				label.val = obj;
				label.execIdx = execIdx;
				labels.add(label);
			}
		}
		
		if (depth == 0) {
			return ;
		}
		
		if (obj.getClass().isArray()) {
			arrayHelper(obj, execIdx, depth);
		} else if (Collection.class.isAssignableFrom(obj.getClass())) {
			Collection collection = (Collection)obj;
			collectionHelper(collection, execIdx, depth);
		} else if (Map.class.isAssignableFrom(obj.getClass())) {
			Map map = (Map)obj;
			Collection collection = map.values();
			collectionHelper(collection, execIdx, depth);
		} else {
			objHelper(obj, execIdx, depth);
		}
	}
	
	public static void arrayHelper(Object obj, long execIdx, int depth) {
		if (obj == null) {
			return ;
		}
				
		Class<?> type = obj.getClass().getComponentType();
		int length = Array.getLength(obj);
		if (length == 0) {
			return ;
		}
		
		if (type == int.class) {
			for (int i = 0; i < length; i++) {
				int val = Array.getInt(obj, i);
				val = propagateTaint(val, execIdx);
				Array.setInt(obj, i, val);
			}
		} else if (type == short.class) {
			for (int i = 0; i < length; i++) {
				short val = Array.getShort(obj, i);
				val = propagateTaint(val, execIdx);
				Array.setShort(obj, i, val);
			}
		} else if (type == byte.class) {
			for (int i = 0; i < length; i++) {
				byte val = Array.getByte(obj, i);
				val = propagateTaint(val, execIdx);
				Array.setByte(obj, i, val);
			}
		} else if (type == boolean.class) {
			for (int i = 0; i < length; i++) {
				boolean val = Array.getBoolean(obj, i);
				val = propagateTaint(val, execIdx);
				Array.setBoolean(obj, i, val);
			}
		} else if (type == char.class) {
			for (int i = 0; i < length; i++) {
				char val = Array.getChar(obj, i);
				val = propagateTaint(val, execIdx);
				Array.setChar(obj, i, val);
			}
		} else if (type == float.class) {
			for (int i = 0; i < length; i++) {
				float val = Array.getFloat(obj, i);
				val = propagateTaint(val, execIdx);
				Array.setFloat(obj, i, val);
			}
		} else if (type == double.class) {
			for (int i = 0; i < length; i++) {
				double val = Array.getDouble(obj, i);
				val = propagateTaint(val, execIdx);
				Array.setDouble(obj, i, val);
			}
		} else if (type == long.class) {
			for (int i = 0; i < length; i++) {
				long val = Array.getLong(obj, i);
				val = propagateTaint(val, execIdx);
				Array.setLong(obj, i, val);
			}
		} else if (String.class.isAssignableFrom(type)) {
			for (int i = 0; i < length; i++) {
				String val = (String)Array.get(obj, i);
				propagateTaint(val, execIdx);
			}
		} else {
			for (int i = 0; i < length; i++) {
				Object o = Array.get(obj, i);
				//objHelper(o, execIdx, depth - 1);
				propagateTaint(o, execIdx, depth - 1);
			}
		}
	}
	
	public static void collectionHelper(Collection collection, long execIdx, int depth) {
		if (collection == null) {
			return ;
		}
						
		for (Object o: collection) {
			//objHelper(o, execIdx, depth - 1);
			propagateTaint(o, execIdx, depth - 1);
		}
	}
		
	public static void objHelper(Object obj, long execIdx, int depth) {
		if (obj == null) {
			return ;
		}
		
		HashSet<Field> fields = FieldController.collectInstanceFields(obj.getClass());				
		try {
			for (Field f: fields) {
				if (f.getType().isPrimitive()) {
					Class<?> type = f.getType();
					if (type == int.class) {
						int val = f.getInt(obj);
						val = propagateTaint(val, execIdx);
						f.setInt(obj, val);
					} else if (type == short.class) {
						short val = f.getShort(obj);
						val = propagateTaint(val, execIdx);
						f.setShort(obj, val);
					} else if (type == byte.class) {
						byte val = f.getByte(obj);
						val = propagateTaint(val, execIdx);
						f.setByte(obj, val);
					} else if (type == boolean.class) {
						boolean val = f.getBoolean(obj);
						val = propagateTaint(val, execIdx);
						f.setBoolean(obj, val);
					} else if (type == char.class) {
						char val = f.getChar(obj);
						val = propagateTaint(val, execIdx);
						f.setChar(obj, val);
					} else if (type == float.class) {
						float val = f.getFloat(obj);
						val = propagateTaint(val, execIdx);
						f.setFloat(obj, val);
					} else if (type == double.class) {
						double val = f.getDouble(obj);
						val = propagateTaint(val, execIdx);
						f.setDouble(obj, val);
					} else if (type == long.class) {
						long val = f.getLong(obj);
						val = propagateTaint(val, execIdx);
						f.setLong(obj, val);
					} else {
						System.err.println("Un-identified primitive type: " + type);
						System.exit(-1);
					}
				} else if (String.class.isAssignableFrom(f.getType())) {
					String val = (String)f.get(obj);
					propagateTaint(val, execIdx);
				} else {
					//check if the object has been tainted
					Object val = f.get(obj);
					//objHelper(val, execIdx, depth - 1);
					propagateTaint(val, execIdx, depth - 1);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
		
	public static class HitoLabel {
		
		public long execIdx;
		
		public Object val;
		
		@Override
		public String toString() {
			return "Exec id: " + this.execIdx + " Val: " + this.val.toString();
		}
		
	}
}
