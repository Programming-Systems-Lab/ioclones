package edu.columbia.cs.psl.ioclones.analysis.dynamic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.phosphor.org.objectweb.asm.Type;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;

public class HitoTaintPropagater {
		
	public static ArrayList<HitoLabel> newLabels(Object val, long execIdx) {
		if (execIdx == -1) {
			return null;
		}
		
		ArrayList<HitoLabel> labels = new ArrayList<HitoLabel>();
		
		HitoLabel lbl = new HitoLabel();
		lbl.execIdx = execIdx;
		lbl.val = val;
		labels.add(lbl);
		
		return labels;
	}
	
	public static void handleTaint(Taint t, Object val, long execIdx) {
		if (execIdx == -1) {
			return ;
		}
		
		Object labelObj = t.getLabel();
		ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)labelObj;
		if (labels == null) {
			labels = new ArrayList<HitoLabel>();
			t.lbl = labels;
		}
		
		synchronized(labels) {
			HitoLabel lbl = new HitoLabel();
			lbl.execIdx = execIdx;
			lbl.val = val;
			
			labels.add(lbl);
		}
	}
	
	public static int propagateTaint(int val, long execIdx) {
		if (execIdx == -1) {
			return val;
		}
		
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedInt(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		/*if (execIdx == Long.MAX_VALUE) {
			System.out.println("Propagate class taint: " + val + " " + MultiTainter.getTaint(val));
		}*/
		
		return val;
	}
	
	public static short propagateTaint(short val, long execIdx) {
		if (execIdx == -1) {
			return val;
		}
		
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
		if (execIdx == -1) {
			return val;
		}
		
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
		if (execIdx == -1) {
			return val;
		}
		
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
		if (execIdx == -1) {
			return val;
		}
		
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
		if (execIdx == -1) {
			return val;
		}
		
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
		if (execIdx == -1) {
			return val;
		}
		
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
		if (execIdx == -1) {
			return val;
		}
		
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			val = MultiTainter.taintedLong(val, labels);
		} else {
			handleTaint(t, val, execIdx);
		}
		
		return val;
	}
	
	public static void propagateTaintPrimitiveObj(Object val, long execIdx) {
		if (execIdx == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			t = new Taint(labels);
			MultiTainter.taintedObject(val, t);
		} else {
			handleTaint(t, val, execIdx);
		}
	}
	
	public static void propagateTaint(String val, long execIdx) {
		if (execIdx == -1) {
			return ;
		}
		
		if (val == null) {
			return ;
		}
		
		/*System.out.println("Before tainting");
		for (int i = 0; i < val.length(); i++) {
			char c = val.charAt(i);
			System.out.println(c + " " + MultiTainter.getTaint(c));
		}*/
		
		ArrayList<HitoLabel> newLabels = newLabels(val, execIdx);
		Taint t = new Taint(newLabels);
		synchronized(t.lbl) {
			MultiTainter.taintedObject(val, t);
		}
		
		/*System.out.println("After tainting");
		for (int i = 0; i < val.length(); i++) {
			char c = val.charAt(i);
			System.out.println(c + " " + MultiTainter.getTaint(c));
		}*/
		
		/*Taint t = MultiTainter.getTaint(val);
		//System.out.println("Check string taint: " + val + " " + t);
		System.out.println("Propagating taint for: " + val);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			t = new Taint(labels);
			MultiTainter.taintedObject(val, t);
		}
		
		for (int i = 0; i < val.length(); i++) {
			char c = val.charAt(i);
			System.out.println("Original taint: " + c + " " + MultiTainter.getTaint(c));
		}
		
		for (int i = 0; i < val.length(); i++) {
			char c = val.charAt(i);
			Taint charT = MultiTainter.getTaint(c);
			if (charT == null) {
				ArrayList<HitoLabel> labels = newLabels(val, execIdx);
				c = MultiTainter.taintedChar(c, labels);
			} else {
				System.out.println("Add old");
				HitoLabel newLabel = new HitoLabel();
				newLabel.val = val;
				newLabel.execIdx = execIdx;
				
				ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)charT.lbl;
				System.out.println("Cur char: " + c + " " + newLabel + " " + labels);
				if (labels == null) {
					labels = new ArrayList<HitoLabel>();
					charT.lbl = labels;
				}
				labels.add(newLabel);
			}
			System.out.println("Cur char: " + c + " " + MultiTainter.getTaint(c));
			
			//propagateTaint(val.charAt(i), execIdx);
		}
		
		for (int i = 0; i < val.length(); i++) {
			char c = val.charAt(i);
			System.out.println("After taint: " + c + " " + MultiTainter.getTaint(c));
		}*/
	}
		
	public static void propagateTaint(Object obj, long execIdx) {
		if (execIdx == -1) {
			return ;
		}
		
		if (obj == null) {
			return ;
		}
		
		if ((obj instanceof Integer) 
				|| (obj instanceof Byte)
				|| (obj instanceof Boolean) 
				|| (obj instanceof Short)
				|| (obj instanceof Character)
				|| (obj instanceof Float) 
				|| (obj instanceof Double) 
				|| (obj instanceof Long)) {
			propagateTaintPrimitiveObj(obj, execIdx);
			return ;
		} else if (String.class.isAssignableFrom(obj.getClass())) {
			String val = (String)obj;
			propagateTaint(val, execIdx);
			return ;
		} else {
			//The tag on array itself is not useful at this point
			//Don't taint obj itself?
			if (!obj.getClass().isArray()) {
				objHelper(obj, execIdx);
			}
			
			if (obj.getClass().isArray()) {
				arrayHelper(obj, execIdx);
			} else if (Collection.class.isAssignableFrom(obj.getClass())) {
				Collection collection = (Collection)obj;
				collectionHelper(collection, execIdx);
			} else if (Map.class.isAssignableFrom(obj.getClass())) {
				Map map = (Map)obj;
				Collection collection = map.values();
				collectionHelper(collection, execIdx);
			} /*else {
				objHelper(obj, execIdx, depth);
			}*/
		}
	}
	
	public static void arrayHelper(Object obj, long execIdx) {
		if (execIdx == -1) {
			return ;
		}
		
		if (obj == null) {
			return ;
		}
				
		Class<?> type = obj.getClass().getComponentType();
		int length = Array.getLength(obj);
		if (length == 0) {
			return ;
		}
		
		if (type == int.class) {
			int[] arr = (int[])obj;
			for (int i = 0; i < arr.length; i++) {
				int val = propagateTaint(arr[i], execIdx);
				arr[i] = val;
			}
		} else if (type == short.class) {
			short[] arr = (short[])obj;
			for (int i = 0; i < arr.length; i++) {
				short val = propagateTaint(arr[i], execIdx);
				arr[i] = val;
			}
		} else if (type == byte.class) {
			byte[] arr = (byte[])obj;
			for (int i = 0; i < arr.length; i++) {
				byte val = propagateTaint(arr[i], execIdx);
				arr[i] = val;
			}
		} else if (type == boolean.class) {
			boolean[] arr = (boolean[])obj;
			for (int i = 0; i < arr.length; i++) {
				boolean val = propagateTaint(arr[i], execIdx);
				arr[i] = val;
			}
		} else if (type == char.class) {
			char[] arr = (char[])obj;
			for (int i = 0; i < arr.length; i++) {
				char val = propagateTaint(arr[i], execIdx);
				arr[i] = val;
			}
		} else if (type == float.class) {
			float[] arr = (float[])obj;
			for (int i = 0; i < arr.length; i++) {
				float val = propagateTaint(arr[i], execIdx);
				arr[i] = val;
			}
		} else if (type == double.class) {
			double[] arr = (double[])obj;
			for (int i = 0; i < arr.length; i++) {
				double val = propagateTaint(arr[i], execIdx);
				arr[i] = val;
			}
		} else if (type == long.class) {
			long[] arr = (long[])obj;
			for (int i = 0; i < arr.length; i++) {
				long val = propagateTaint(arr[i], execIdx);
				arr[i] = val;
			}
		} else {
			Object[] arr = (Object[])obj;
			for (int i = 0; i < arr.length; i++) {
				Object val = arr[i];
				//propagateTaint(val, execIdx, depth - 1);
				objHelper(val, execIdx);
			}
		}
	}
	
	public static void collectionHelper(Collection collection, long execIdx) {
		if (execIdx == -1) {
			return ;
		}
		
		if (collection == null) {
			return ;
		}
		
		for (Object o: collection) {
			//propagateTaint(o, execIdx, depth - 1);
			objHelper(o, execIdx);
		}
	}
		
	public static void objHelper(Object obj, long execIdx) {
		if (execIdx == -1) {
			return ;
		}
		
		if (obj == null) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(obj);
		if (t == null) {
			ArrayList<HitoLabel> labels = newLabels(obj, execIdx);
			t = new Taint(labels);
			synchronized(labels) {
				MultiTainter.taintedObject(obj, t);
			}
		} else {
			ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
			if (labels == null) {
				labels = newLabels(obj, execIdx);
				t.lbl = labels;
			} else {
				synchronized(labels) {
					HitoLabel label = new HitoLabel();
					label.val = obj;
					label.execIdx = execIdx;
					labels.add(label);
				}
			}
		}
		System.out.println("Tainted obj: " + obj + " " + execIdx + " " + MultiTainter.getTaint(obj));
		
		/*HashSet<Field> fields = FieldController.collectInstanceFields(obj.getClass());				
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
				} else {
					//check if the object has been tainted
					Object val = f.get(obj);
					//objHelper(val, execIdx, depth - 1);
					_propagateTaint(val, execIdx, depth - 1);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}*/
	}
	
	public static int cleanAndPropagate(int val, long execIdx) {
		ArrayList<HitoLabel> labels = newLabels(val, execIdx);
		val = MultiTainter.taintedInt(val, labels);
		
		return val;
	}
	
	public static short cleanAndPropagate(short val, long execIdx) {
		ArrayList<HitoLabel> labels = newLabels(val, execIdx);
		val = MultiTainter.taintedShort(val, labels);
		
		return val;
	}
	
	public static byte cleanAndPropagate(byte val, long execIdx) {
		ArrayList<HitoLabel> labels = newLabels(val, execIdx);
		val = MultiTainter.taintedByte(val, labels);
		
		return val;
	}
	
	public static boolean cleanAndPropagate(boolean val, long execIdx) {
		ArrayList<HitoLabel> labels = newLabels(val, execIdx);
		val = MultiTainter.taintedBoolean(val, labels);
		
		return val;
	}
	
	public static char cleanAndPropagate(char val, long execIdx) {
		ArrayList<HitoLabel> labels = newLabels(val, execIdx);
		val = MultiTainter.taintedChar(val, labels);
		
		return val;
	}
	
	public static float cleanAndPropagate(float val, long execIdx) {
		ArrayList<HitoLabel> labels = newLabels(val, execIdx);
		val = MultiTainter.taintedFloat(val, labels);
		
		return val;
	}
	
	public static double cleanAndPropagate(double val, long execIdx) {
		ArrayList<HitoLabel> labels = newLabels(val, execIdx);
		val = MultiTainter.taintedDouble(val, labels);
		
		return val;
	}
	
	public static long cleanAndPropagate(long val, long execIdx) {
		ArrayList<HitoLabel> labels = newLabels(val, execIdx);
		val = MultiTainter.taintedLong(val, labels);
		
		return val;
	}
	
	public static void cleanAndPropagate(Object val, long execIdx) {
		if ((val instanceof String)) {
			Taint t = MultiTainter.getTaint(val);
			ArrayList<HitoLabel> labels = newLabels(val, execIdx);
			if (t == null) {
				
				t = new Taint(labels);
				MultiTainter.taintedObject(val, t);
			} else {
				t.lbl = labels;
				t.dependencies = new LinkedList<Object>();
			}
		}
	}
				
	public static class HitoLabel {
		
		public long execIdx;
		
		public Object val;
		
		@Override
		public String toString() {
			return "Exec: " + this.execIdx + " Val: " + this.val.toString();
		}
		
	}
}
