package edu.columbia.cs.psl.ioclones.analysis.dynamic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintPropagater.HitoLabel;
import edu.columbia.cs.psl.ioclones.config.IOCloneConfig;
import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LinkedList;
import edu.columbia.cs.psl.phosphor.struct.LinkedList.Node;

public class HitoTaintChecker {
	
	private static final Logger logger = LogManager.getLogger(HitoTaintChecker.class);
	
	private static final int DEPTH = IOCloneConfig.getInstance().getDepth();
	
	private static final boolean CONTROL = IOCloneConfig.getInstance().isControl();
	
	private static final boolean WRITER = IOCloneConfig.getInstance().isWriter();
	
	private static final boolean DEBUG = IOCloneConfig.getInstance().isDebug();
	
	public static void debugMsg(String msg, boolean warn) {
		if (DEBUG) {
			if (warn)
				logger.warn(msg);
			else
				logger.info(msg);
		}
	}
	
	public static boolean shouldCheck(Object val, long execIdx) {
		if (execIdx == -1) {
			return false;
		}
		
		if (val == null) {
			return false;
		}
		
		if (val.getClass().isArray()) {
			return true;
		}
		
		if (Collection.class.isAssignableFrom(val.getClass())) {
			return true;
		}
		
		if (Map.class.isAssignableFrom(val.getClass())) {
			return true;
		}
		
		if (!shouldSerialize(val)) {
			return false;
		} else {
			Taint t = MultiTainter.getTaint(val);
			
			if (t == null) {
				return false;
			}
			
			ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
			if (labels == null) {
				return false;
			}
			
			boolean shouldCheck = false;
			for (HitoLabel l: labels) {
				if (l.execIdx >= execIdx) {
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
		if (record.getId() == -1) {
			return ;
		}
		
		debugMsg("Summarizing written inputs: " + record.getMethodKey() + " " + record.getId(), false);
		
		for (Object o: record.preload.values()) {
			if (HitoTaintChecker.shouldCheck(o, record.getId())) {
				HitoTaintChecker.analyzeTaint(o, record, depth, false, true);
			}
		}
		
		debugMsg("\n", false);
	}
	
	public static void recordControl(int val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!CONTROL) {
			return ;
		}
		
		record.registerInput(val, false);
	}
	
	public static void recordControl(Object obj, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!CONTROL) {
			return ;
		}
		
		if (obj == null) {
			return ;
		}
		
		if (String.class.isAssignableFrom(obj.getClass())) {
			record.registerInput(obj, false);
		} else {
			record.registerInput(obj, true);
		}
		return ;
	}
	
	public static void recordWriter(int val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		analyzeTaint(val, record, true, false);
	}
	
	public static void recordWriter(short val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		analyzeTaint(val, record, true, false);
	}
	
	public static void recordWriter(byte val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		analyzeTaint(val, record, true, false);
	}
	
	public static void recordWriter(boolean val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		analyzeTaint(val, record, true, false);
	}
	
	public static void recordWriter(char val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		analyzeTaint(val, record, true, false);
	}
	
	public static void recordWriter(long val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		analyzeTaint(val, record, true, false);
	}
	
	public static void recordWriter(float val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		analyzeTaint(val, record, true, false);
	}
	
	public static void recordWriter(double val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		analyzeTaint(val, record, true, false);
	}
		
	public static void recordWriter(Object val, IORecord record) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (!WRITER) {
			return ;
		}
		
		if (val == null) {
			return ;
		}
		
		analyzeTaint(val, record, DEPTH, true, false);
	}
	
	public static boolean interpretDeps(Object val, 
			Taint t, 
			IORecord record, 
			boolean writeObj) {
		if (record.getId() == -1) {
			return false;
		}
		
		if (val == null) {
			return false;
		}
		
		if (t == null) {
			//System.err.println("Value with no taint on method: " + record.getId() + " val: " + val);
			debugMsg("Value with no taint on method: " + record.getId() + " val: " + val, true);
			return false;
		}
		
		long execIdx = record.getId();
		debugMsg("Analyzing method " + record.getMethodKey() + " " + execIdx + " val: " + val, false);
		
		LinkedList deps = t.getDependencies();
		//Some taints are copied, so also need to analyze labels...
		ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
		
		HashSet<Object> inputs = new HashSet<Object>();
		
		debugMsg("Analyzing deps", false);
		
		if (deps != null && deps.getFirst() != null) {
			Node<Object> curNode = deps.getFirst();
			while (curNode != null) {
				ArrayList<HitoLabel> dep = (ArrayList<HitoLabel>)curNode.entry;
				for (HitoLabel d: dep) {
					if (d.execIdx == execIdx) {
						//From input param
						debugMsg("Dep input: " + d, false);
						inputs.add(d.val);
					} else if (d.execIdx == Long.MAX_VALUE) {
						//From class field
						debugMsg("Dep class: " + d, false);
						inputs.add(d.val);
					} else if (d.execIdx > execIdx && writeObj) {
						//From write-to-object
						debugMsg("Dep w-t-o: " + d, false);
						inputs.add(d.val);
					} else {
						debugMsg("Dep outside: " + d, false);
					}
				}
				curNode = curNode.next;
			}
		} else {
			debugMsg("No deps", false);
		}
		
		debugMsg("Analyzing labels: " + labels, false);
		if (labels != null && labels.size() > 0) {
			for (HitoLabel hl: labels) {
				if (hl.val.equals(val)) {
					continue ;
				}
				
				//k = i + j, if j is not tatined,, k inherit i's taint directly...
				if (hl.execIdx == execIdx) {						
					debugMsg("lbl input: " + hl, false);
					inputs.add(hl.val);
				} else if (hl.execIdx == Long.MAX_VALUE) {
					debugMsg("lbl class: " + hl, false);
					inputs.add(hl.val);
				} else if (hl.execIdx > execIdx && writeObj) {
					debugMsg("lbl w-t-o: " + hl, false);
					inputs.add(hl.val);
				} else {
					debugMsg("lbl outside: " + hl, false);
				}
			}
		} else {
			debugMsg("No lbls", false);
		}
		
		debugMsg("\n", false);
		
		for (Object i: inputs) {
			record.registerInput(i, shouldSerialize(i));
		}
		
		return inputs.size() > 0;
	}
		
	public static void analyzeTaint(int val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		//System.out.println("Check taint: " + val + " " + t);
		boolean hasDeps = interpretDeps(val, t, record, writeObj);
		if (register || hasDeps)
			record.registerOutput(val, false);
	}
	
	public static void analyzeTaint(short val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		boolean hasDeps = interpretDeps(val, t, record, writeObj);
		if (register || hasDeps)
			record.registerOutput(val, false);
	}
	
	public static void analyzeTaint(byte val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		boolean hasDeps = interpretDeps(val, t, record, writeObj);
		if (register || hasDeps)
			record.registerOutput(val, false);
	}
	
	public static void analyzeTaint(boolean val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		boolean hasDeps = interpretDeps(val, t, record, writeObj);
		if (register || hasDeps)
			record.registerOutput(val, false);
	}
	
	public static void analyzeTaint(char val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		boolean hasDeps = interpretDeps(val, t, record, writeObj);
		if (register || hasDeps)
			record.registerOutput(val, false);
	}
	
	public static void analyzeTaint(long val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		boolean hasDeps = interpretDeps(val, t, record, writeObj);
		if (register || hasDeps)
			record.registerOutput(val, false);
	}
	
	public static void analyzeTaint(float val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		boolean hasDeps = interpretDeps(val, t, record, writeObj);
		if (register || hasDeps)
			record.registerOutput(val, false);
	}
	
	public static void analyzeTaint(double val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		boolean hasDeps = interpretDeps(val, t, record, writeObj);
		if (register || hasDeps)
			record.registerOutput(val, false);
	}
	
	public static void analyzeTaint(String val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (val == null) {
			return ;
		}
		
		long execIdx = record.getId();		
		debugMsg("Analyzing method " + record.getMethodKey() + " " + execIdx + " val: " + val, false);
				
		//After concatenation, char's label will be merged into dependencies
		HashSet<Object> inputs = new HashSet<Object>();
		for (int i = 0; i < val.length(); i++) {
			Taint t = MultiTainter.getTaint(val.charAt(i));
			debugMsg("Cur char: " + val.charAt(i) + " " + t, false);
			
			if (t == null) {
				continue ;
			}
			
			if (t.lbl == null && t.dependencies == null) {
				continue ;
			}
			
			LinkedList<Object> deps = t.dependencies;
			if (deps != null && deps.getFirst() != null) {
				Node<Object> curNode = deps.getFirst();
				while (curNode != null) {
					ArrayList<HitoLabel> dep = (ArrayList<HitoLabel>)curNode.entry;
					for (HitoLabel d: dep) {
						if (d.val.equals(val)) {
							continue ;
						}
						
						if (d.execIdx == execIdx) {
							debugMsg("Dep input: " + d, false);
							inputs.add(d.val);
						} else if (d.execIdx == Long.MAX_VALUE) {
							debugMsg("Dep class: " + d, false);
							inputs.add(d.val);
						} else if (d.execIdx > execIdx && writeObj) {
							debugMsg("Dep w-t-o: " + d, false);
							inputs.add(d.val);
						} else {
							debugMsg("Dep outside: " + d, false);
						}
					}
					curNode = curNode.next;
				}
			} else {
				debugMsg("No deps", false);
			}
			
			if (t.lbl != null) {
				ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
				for (HitoLabel hl : labels) {
					if (hl.val.equals(val)) {
						continue ;
					}
					
					if (hl.execIdx == execIdx) {						
						//The label of each char is the string it belongs to
						debugMsg("lbl input: " + hl, false);
						inputs.add(hl.val);
					} else if (hl.execIdx == Long.MAX_VALUE) {
						debugMsg("lbl class: " + hl, false);
						inputs.add(hl.val);
					} else if (hl.execIdx > execIdx && writeObj) {
						debugMsg("lbl w-t-o: " + hl, false);
						inputs.add(hl.val);
					} else {
						debugMsg("lbl outside: " + hl, false);
					} 
				}
			} else {
				debugMsg("No lbl", false);
			}
		}
				
		if (inputs.size() > 0) {
			for (Object i: inputs) {
				//should be string?
				/*if (String.class.isAssignableFrom(i.getClass())) {
					debugMsg("Final dep: " + i, false);
				} else {
					logger.error("Suspicious input: " + i + " " + i.getClass());
				}*/
				debugMsg("Final dep: " + i, false);
				record.registerInput(i, false);
			}
		} else {
			debugMsg("No inputs", false);
		}
		
		//System.out.println("Value of " + execIdx + " val: " + val);
		//System.out.println("Deps: " + inputs);
		if (register || inputs.size() > 0) {
			record.registerOutput(val, false);
		}
		debugMsg("\n", false);
	}
	
	public static void analyzeTaint(Object obj, IORecord record, int depth, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (obj == null) {
			return ;
		}
		
		if ((obj instanceof Integer)) {
			int i = ((Integer)obj).intValue();
			analyzeTaint(i, record, register, writeObj);
		} else if ((obj instanceof Boolean)) {
			boolean b = ((Boolean)obj).booleanValue();
			analyzeTaint(b, record, register, writeObj);
		} else if ((obj instanceof Byte)) {
			byte b = ((Byte)obj).byteValue();
			analyzeTaint(b, record, register, writeObj);
		} else if ((obj instanceof Short)) {
			short s = ((Short)obj).shortValue();
			analyzeTaint(s, record, register, writeObj);
		} else if ((obj instanceof Character)) {
			char c = ((Character)obj).charValue();
			analyzeTaint(c, record, register, writeObj);
		} else if ((obj instanceof Float)) {
			float f = ((Float)obj).floatValue();
			analyzeTaint(f, record, register, writeObj);
		} else if ((obj instanceof Double)) {
			double d= ((Double)obj).doubleValue();
			analyzeTaint(d, record, register, writeObj);
		} else if ((obj instanceof Long)) {
			long l = ((Long)obj).longValue();
			analyzeTaint(l, record, register, writeObj);
		} else if (String.class.isAssignableFrom(obj.getClass())) {
			String string = (String)obj;
			analyzeTaint(string, record, register, writeObj);
		} else {
			//Analyze taint on array itself not useful, element is useful
			if (!obj.getClass().isArray()) {
				Taint t = MultiTainter.getTaint(obj);
				boolean hasDep = false;
				if (t != null) {
					hasDep = interpretDeps(obj, t, record, writeObj);
				}
				
				if (register || hasDep) {
					record.registerOutput(obj, shouldSerialize(obj));
				}
			}
			
			if (depth == 0) {			
				return ;
			}
			
			if (obj.getClass().isArray()) {
				arrayChecker(obj, depth, record, register, writeObj);
			} else if (Collection.class.isAssignableFrom(obj.getClass())) {
				Collection collection = (Collection)obj;
				collectionChecker(collection, depth, record, register, writeObj);
			} else if (Map.class.isAssignableFrom(obj.getClass())) {
				Map map = (Map)obj;
				collectionChecker(map.values(), depth, record, register, writeObj);
			} else {
				objChecker(obj, depth, record, register, writeObj);
			}
		}
	}
	
	public static void objChecker(Object obj, int depth, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (obj == null) {
			return ;
		}
		
		Class clazz = obj.getClass();
		HashSet<Field> fields = FieldController.collectInstanceFields(clazz);
		try {
			for (Field f: fields) {
				if (f.getType() == int.class) {
					int val = f.getInt(obj);
					analyzeTaint(val, record, register, writeObj);
				} else if (f.getType() == short.class) {
					short val = f.getShort(obj);
					analyzeTaint(val, record, register, writeObj);
				} else if (f.getType() == boolean.class) {
					boolean val = f.getBoolean(obj);
					analyzeTaint(val, record, register, writeObj);
				} else if (f.getType() == byte.class) {
					byte val = f.getByte(obj);
					analyzeTaint(val, record, register, writeObj);
				} else if (f.getType() == char.class) {
					char val = f.getChar(obj);
					analyzeTaint(val, record, register, writeObj);
				} else if (f.getType() == float.class) {
					float val = f.getFloat(obj);
					analyzeTaint(val, record, register, writeObj);
				} else if (f.getType() == long.class) {
					long val = f.getLong(obj);
					analyzeTaint(val, record, register, writeObj);
				} else if (f.getType() == double.class) {
					double val = f.getDouble(obj);
					analyzeTaint(val, record, register, writeObj);
				} else {
					Object val = f.get(obj);
					analyzeTaint(val, record, depth - 1, register, writeObj);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void arrayChecker(Object obj, int depth, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (obj == null) {
			return ;
		}
				
		int length = Array.getLength(obj);
		if (length == 0) {
			return ;
		}
		
		Class type = obj.getClass().getComponentType();
		if (type == int.class) {
			int[] arr = (int[])obj;
			for (int i = 0; i < arr.length; i++) {
				int val = arr[i];
				analyzeTaint(val, record, register, writeObj);
			}
		} else if (type == short.class) {
			short[] arr = (short[])obj;
			for (int i = 0; i < arr.length; i++) {
				short val = arr[i];
				analyzeTaint(val, record, register, writeObj);
			}
		} else if (type == char.class) {
			char[] arr = (char[])obj;
			for (int i = 0; i < arr.length; i++) {
				char val = arr[i];
				analyzeTaint(val, record, register, writeObj);
			}
		} else if (type == boolean.class) {
			boolean[] arr = (boolean[])obj;
			for (int i = 0; i < arr.length; i++) {
				boolean val = arr[i];
				analyzeTaint(val, record, register, writeObj);
			}
		} else if (type == byte.class) {
			byte[] arr = (byte[])obj;
			for (int i = 0; i < arr.length; i++) {
				byte val = arr[i];
				analyzeTaint(val, record, register, writeObj);
			}
		} else if (type == float.class) {
			float[] arr = (float[])obj;
			for (int i = 0; i < arr.length; i++) {
				float val = arr[i];
				analyzeTaint(val, record, register, writeObj);
			}
		} else if (type == long.class) {
			long[] arr = (long[])obj;
			for (int i = 0; i < arr.length; i++) {
				long val = arr[i];
				analyzeTaint(val, record, register, writeObj);
			}
		} else if (type == double.class) {
			double[] arr = (double[])obj;
			for (int i = 0; i < arr.length; i++) {
				double val = arr[i];
				analyzeTaint(val, record, register, writeObj);
			}
		} else {
			Object[] arr = (Object[])obj;
			for (int i = 0; i < arr.length; i++) {
				Object val = arr[i];
				analyzeTaint(val, record, depth - 1, register, writeObj);
			}
		}
	}
	
	public static void collectionChecker(Collection collection, int depth, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (collection == null) {
			return ;
		}
		
		for (Object o: collection) {
			analyzeTaint(o, record, depth - 1, register, writeObj);
		}
	}
}
