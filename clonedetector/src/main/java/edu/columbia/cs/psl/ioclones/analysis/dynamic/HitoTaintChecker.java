package edu.columbia.cs.psl.ioclones.analysis.dynamic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintPropagater.HitoLabel;
import edu.columbia.cs.psl.ioclones.analysis.dynamic.HitoTaintPropagater.WriteSignal;
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
		
		/*if (val.getClass().isArray()) {
			return true;
		}
		
		if (Collection.class.isAssignableFrom(val.getClass())) {
			return true;
		}
		
		if (Map.class.isAssignableFrom(val.getClass())) {
			return true;
		}*/
		
		if (!shouldSerialize(val)) {
			return false;
		} else {
			return true;
			/*Taint t = MultiTainter.getTaint(val);
			
			if (t == null) {
				return false;
			}
			
			ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
			if (labels == null) {
				return false;
			}
			
			boolean shouldCheck = false;
			synchronized(labels) {
				for (HitoLabel l: labels) {
					if (l.execIdx >= execIdx) {
						shouldCheck = true;
						break ;
					}
				}
			}
			return shouldCheck;*/
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
	
	public static HashSet<Object> interpretStringDeps(String val, 
			IORecord record, 
			boolean writeObj) {
		
		if (record.getId() == -1) {
			return null;
		}
		
		if (val == null) {
			return null;
		}
		
		long execIdx = record.getId();
		debugMsg("Analyzing string " + record.getMethodKey() + " " + execIdx + " val: " + val, false);
		
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
				synchronized(deps) {
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
								debugMsg("Dep static: " + d, false);
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
				}
			} else {
				debugMsg("No deps", false);
			}
			
			if (t.lbl != null) {
				ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
				synchronized(labels) {
					for (HitoLabel hl : labels) {
						if (hl.val.equals(val)) {
							continue ;
						}
						
						if (hl.execIdx == execIdx) {						
							//The label of each char is the string it belongs to
							debugMsg("lbl input: " + hl, false);
							inputs.add(hl.val);
						} else if (hl.execIdx == Long.MAX_VALUE) {
							debugMsg("lbl static: " + hl, false);
							inputs.add(hl.val);
						} else if (hl.execIdx > execIdx && writeObj) {
							debugMsg("lbl w-t-o: " + hl, false);
							inputs.add(hl.val);
						} else {
							debugMsg("lbl outside: " + hl, false);
						} 
					}
				}
			} else {
				debugMsg("No lbl", false);
			}
		}
		
		return inputs;
	}
		
	public static HashSet<Object> interpretSelfDeps(Object val, 
			Taint t, 
			IORecord record, 
			boolean writeObj) {
		if (record.getId() == -1) {
			return null;
		}
		
		if (val == null) {
			return null;
		}
		
		if (t == null) {
			//System.err.println("Value with no taint on method: " + record.getId() + " val: " + val);
			debugMsg("Value with no taint on method: " + record.getId() + " val: " + val, true);
			return null;
		}
		
		long execIdx = record.getId();
		debugMsg("Analyzing self-val " + record.getMethodKey() + " " + execIdx + " val: " + val, false);
		
		LinkedList deps = t.getDependencies();
		//Some taints are copied, so also need to analyze labels
		ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.getLabel();
		
		HashSet<Object> inputs = new HashSet<Object>();
		
		debugMsg("Analyzing deps", false);
		
		if (deps != null && deps.getFirst() != null) {
			synchronized(deps) {
				Node<Object> curNode = deps.getFirst();
				while (curNode != null) {
					ArrayList<HitoLabel> dep = (ArrayList<HitoLabel>)curNode.entry;
					for (HitoLabel d: dep) {
						if (d.execIdx == execIdx) {
							//From input param
							debugMsg("Dep input: " + d, false);
							inputs.add(d.val);
						} else if (d.execIdx == Long.MAX_VALUE) {
							debugMsg("Dep static: " + d, false);
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
			}
		} else {
			debugMsg("No deps", false);
		}
		
		debugMsg("Analyzing labels", false);
		if (labels != null && labels.size() > 0) {
			synchronized(labels) {
				for (HitoLabel hl: labels) {
					if (hl.val.equals(val)) {
						continue ;
					}
					
					//k = i + j, if j is not tatined,, k inherit i's taint directly...
					if (hl.execIdx == execIdx) {						
						debugMsg("lbl input: " + hl, false);
						inputs.add(hl.val);
					} else if (hl.execIdx == Long.MAX_VALUE) {
						debugMsg("lbl static: " + hl, false);
						inputs.add(hl.val);
					} else if (hl.execIdx > execIdx && writeObj) {
						debugMsg("lbl w-t-o: " + hl, false);
						inputs.add(hl.val);
					} else {
						debugMsg("lbl outside: " + hl, false);
					}
				}
			}
		} else {
			debugMsg("No lbls", false);
		}
		
		debugMsg("\n", false);
		
		/*for (Object i: inputs) {
			record.registerInput(i, shouldSerialize(i));
		}*/
		
		return inputs;
	}
		
	public static void analyzeTaint(int val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		HashSet<Object> inputs = interpretSelfDeps(val, t, record, writeObj);
		
		if (register) {
			record.registerOutput(val, false);
		}
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
		}
	}
	
	public static void analyzeTaint(short val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		HashSet<Object> inputs = interpretSelfDeps(val, t, record, writeObj);
		
		if (register) {
			record.registerOutput(val, false);
		}
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
		}
	}
	
	public static void analyzeTaint(byte val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		HashSet<Object> inputs = interpretSelfDeps(val, t, record, writeObj);
		
		if (register) {
			record.registerOutput(val, false);
		}
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
		}
	}
	
	public static void analyzeTaint(boolean val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		HashSet<Object> inputs = interpretSelfDeps(val, t, record, writeObj);
		
		if (register) {
			record.registerOutput(val, false);
		}
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
		}
	}
	
	public static void analyzeTaint(char val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		HashSet<Object> inputs = interpretSelfDeps(val, t, record, writeObj);
		
		if (register) {
			record.registerOutput(val, false);
		}
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
		}
	}
	
	public static void analyzeTaint(long val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		HashSet<Object> inputs = interpretSelfDeps(val, t, record, writeObj);
		
		if (register) {
			record.registerOutput(val, false);
		}
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
		}
	}
	
	public static void analyzeTaint(float val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		HashSet<Object> inputs = interpretSelfDeps(val, t, record, writeObj);
		
		if (register) {
			record.registerOutput(val, false);
		}
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
		}
	}
	
	public static void analyzeTaint(double val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		Taint t = MultiTainter.getTaint(val);
		HashSet<Object> inputs = interpretSelfDeps(val, t, record, writeObj);
		
		if (register) {
			record.registerOutput(val, false);
		}
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
		}
	}
	
	public static void analyzeTaint(String val, IORecord record, boolean register, boolean writeObj) {
		if (record.getId() == -1) {
			return ;
		}
		
		if (val == null) {
			return ;
		}
		
		long execIdx = record.getId();		
		debugMsg("Analyzing string taint " + record.getMethodKey() + " " + execIdx + " val: " + val, false);
				
		//After concatenation, char's label will be merged into dependencies
		HashSet<Object> inputs = interpretStringDeps(val, record, writeObj);
				
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
		
		if (inputs != null && inputs.size() > 0) {
			for (Object o: inputs) {
				record.registerInput(o, shouldSerialize(o));
			}
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
		
		debugMsg("Analyzing obj taint " + record.getMethodKey() + " " + record.getId() + " val: " + obj, false);
		//System.out.println("Depth: " + depth);
		HashSet<Object> inputs = interpretObjectDeps(obj, depth, record, register, writeObj);
		boolean shouldOutput = register;
		if (inputs != null && inputs.size() > 0) {
			shouldOutput = true;
			for (Object o: inputs) {
				debugMsg("Final deps:" + o, false);
				if (o instanceof WriteSignal) {
					//WriteSignal is for those vals flow to object without any deps
					continue ;
				}
				record.registerInput(o, shouldSerialize(o));
			}
		} else {
			debugMsg("No inputs", false);
		}
		
		if (shouldOutput) {
			record.registerOutput(obj, true);
		}
	}
	
	public static HashSet<Object> interpretObjectDeps(Object obj, 
			int depth, 
			IORecord record, 
			boolean register, 
			boolean writeObj) {
		if (record.getId() == -1) {
			return null;
		}
		
		if (obj == null) {
			return null;
		}
				
		//Handle non-field collection object
		if ((obj instanceof Integer)) {
			int i = ((Integer)obj).intValue();			
			Taint t = MultiTainter.getTaint(i);
			return interpretSelfDeps(i, t, record, writeObj);
		} else if ((obj instanceof Boolean)) {
			boolean b = ((Boolean)obj).booleanValue();
			Taint t = MultiTainter.getTaint(b);
			return interpretSelfDeps(b, t, record, writeObj);
		} else if ((obj instanceof Byte)) {
			byte b = ((Byte)obj).byteValue();
			Taint t = MultiTainter.getTaint(b);
			return interpretSelfDeps(b, t, record, writeObj);
		} else if ((obj instanceof Short)) {
			short s = ((Short)obj).shortValue();
			Taint t = MultiTainter.getTaint(s);
			return interpretSelfDeps(s, t, record, writeObj);
		} else if ((obj instanceof Character)) {
			char c = ((Character)obj).charValue();
			Taint t = MultiTainter.getTaint(c);
			return interpretSelfDeps(c, t, record, writeObj);
		} else if ((obj instanceof Float)) {
			float f = ((Float)obj).floatValue();
			Taint t = MultiTainter.getTaint(f);
			return interpretSelfDeps(f, t, record, writeObj);
		} else if ((obj instanceof Double)) {
			double d= ((Double)obj).doubleValue();
			Taint t = MultiTainter.getTaint(d);
			return interpretSelfDeps(d, t, record, writeObj);
		} else if ((obj instanceof Long)) {
			long l = ((Long)obj).longValue();
			Taint t = MultiTainter.getTaint(l);
			return interpretSelfDeps(l, t, record, writeObj);
		} else if (String.class.isAssignableFrom(obj.getClass())) {
			String string = (String)obj;
			return interpretStringDeps(string, record, writeObj);
		}
		
		if (depth == 0) {
			if (!obj.getClass().isArray()) {
				//Get the taint of arr itself not useful
				Taint t = MultiTainter.getTaint(obj);
				return interpretSelfDeps(obj, t, record, writeObj);
			} else {
				return null;
			}
		}
		
		Class clazz = obj.getClass();
		if (clazz.isArray()) {
			return interpretArrDeps(obj, depth, record, register, writeObj);
		} else if (Collection.class.isAssignableFrom(clazz)) {
			Collection collection = (Collection)obj;
			return interpretCollectionDeps(collection, depth, record, register, writeObj);
		} else if (Map.class.isAssignableFrom(clazz)) {
			Map map = (Map)obj;
			return interpretCollectionDeps(map.values(), depth, record, register, writeObj);
		} else {
			HashSet<Object> inputs = new HashSet<Object>();
			
			//The taint of object itself is not useful for now...
			Taint selfT = MultiTainter.getTaint(obj);
			HashSet<Object> selfInputs = interpretSelfDeps(obj, selfT, record, writeObj);
			if (selfInputs != null) {
				inputs.addAll(selfInputs);
			}
			
			HashSet<Field> fields = FieldController.collectInstanceFields(clazz);
			try {
				//System.out.println("Check flows: " + fields);
				for (Field f: fields) {
					if (f.getType() == int.class) {
						//int val = f.getInt(obj);
						
						Taint t = MultiTainter.getTaint(f.getInt(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						//System.out.println("Best flowTo taint: " + val + " " + t);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretSelfDeps(flowTo.val, t, record,  writeObj);
							
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.getInt(obj), false);
						}
					} else if (f.getType() == short.class) {
						//short val = f.getShort(obj);
						
						Taint t = MultiTainter.getTaint(f.getShort(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretSelfDeps(flowTo.val, t, record,  writeObj);
							
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.getShort(obj), false);
						}
					} else if (f.getType() == boolean.class) {
						//boolean val = f.getBoolean(obj);
						
						Taint t = MultiTainter.getTaint(f.getBoolean(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretSelfDeps(flowTo.val, t, record,  writeObj);
							
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.getBoolean(obj), false);
						}
					} else if (f.getType() == byte.class) {
						//byte val = f.getByte(obj);
						
						Taint t = MultiTainter.getTaint(f.getByte(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretSelfDeps(flowTo.val, t, record,  writeObj);
							
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.getByte(obj), false);
						}
					} else if (f.getType() == char.class) {
						//char val = f.getChar(obj);
						
						Taint t = MultiTainter.getTaint(f.getChar(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretSelfDeps(flowTo.val, t, record,  writeObj);
							
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.getChar(obj), false);
						}
					} else if (f.getType() == float.class) {
						//float val = f.getFloat(obj);
						
						Taint t = MultiTainter.getTaint(f.getFloat(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretSelfDeps(flowTo.val, t, record,  writeObj);
							
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.getFloat(obj), false);
						}
					} else if (f.getType() == long.class) {
						//long val = f.getLong(obj);
						
						Taint t = MultiTainter.getTaint(f.getLong(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretSelfDeps(flowTo.val, t, record,  writeObj);
							
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.getLong(obj), false);
						}
					} else if (f.getType() == double.class) {
						//double val = f.getDouble(obj);
						
						Taint t = MultiTainter.getTaint(f.getDouble(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretSelfDeps(flowTo.val, t, record,  writeObj);
							
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.getDouble(obj), false);
						}
					} else {
						//Object val = f.get(obj);
						Taint t = MultiTainter.getTaint(f.get(obj));
						HitoLabel flowTo = interpretFlowTo(t, record);
						
						if (flowTo != null) {
							HashSet<Object> flowInputs = interpretObjectDeps(f.get(obj), depth - 1, record, register, writeObj);
							if (flowInputs != null) {
								inputs.addAll(flowInputs);
							} else {
								inputs.add(new WriteSignal());
							}
							
							debugMsg("Flows: " + flowInputs + "->" + flowTo.val, false);
						} else {
							debugMsg("No flow: " + f.get(obj), false);
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return inputs;
		}
	}
	
	public static HashSet<Object> interpretArrDeps(Object obj, 
			int depth, 
			IORecord record, 
			boolean register, 
			boolean writeObj) {
		if (record.getId() == -1) {
			return null;
		}
		
		if (obj == null) {
			return null;
		}
				
		int length = Array.getLength(obj);
		if (length == 0) {
			return null;
		}
		
		Class type = obj.getClass().getComponentType();
		HashSet<Object> inputs = new HashSet<Object>();
		if (type == int.class) {
			int[] arr = (int[])obj;
			for (int i = 0; i < arr.length; i++) {
				int val = arr[i];
				
				Taint t = MultiTainter.getTaint(val);
				HashSet<Object> eInputs = interpretSelfDeps(val, t, record, writeObj);
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		} else if (type == short.class) {
			short[] arr = (short[])obj;
			for (int i = 0; i < arr.length; i++) {
				short val = arr[i];
				
				Taint t = MultiTainter.getTaint(val);
				HashSet<Object> eInputs = interpretSelfDeps(val, t, record, writeObj);
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		} else if (type == char.class) {
			char[] arr = (char[])obj;
			for (int i = 0; i < arr.length; i++) {
				char val = arr[i];
				
				Taint t = MultiTainter.getTaint(val);
				HashSet<Object> eInputs = interpretSelfDeps(val, t, record, writeObj);
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		} else if (type == boolean.class) {
			boolean[] arr = (boolean[])obj;
			for (int i = 0; i < arr.length; i++) {
				boolean val = arr[i];
				
				Taint t = MultiTainter.getTaint(val);
				HashSet<Object> eInputs = interpretSelfDeps(val, t, record, writeObj);
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		} else if (type == byte.class) {
			byte[] arr = (byte[])obj;
			for (int i = 0; i < arr.length; i++) {
				byte val = arr[i];
				
				Taint t = MultiTainter.getTaint(val);
				HashSet<Object> eInputs = interpretSelfDeps(val, t, record, writeObj);
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		} else if (type == float.class) {
			float[] arr = (float[])obj;
			for (int i = 0; i < arr.length; i++) {
				float val = arr[i];
				
				Taint t = MultiTainter.getTaint(val);
				HashSet<Object> eInputs = interpretSelfDeps(val, t, record, writeObj);
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		} else if (type == long.class) {
			long[] arr = (long[])obj;
			for (int i = 0; i < arr.length; i++) {
				long val = arr[i];
				
				Taint t = MultiTainter.getTaint(val);
				HashSet<Object> eInputs = interpretSelfDeps(val, t, record, writeObj);
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		} else if (type == double.class) {
			double[] arr = (double[])obj;
			for (int i = 0; i < arr.length; i++) {
				double val = arr[i];
				
				Taint t = MultiTainter.getTaint(val);
				HashSet<Object> eInputs = interpretSelfDeps(val, t, record, writeObj);
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		} else {
			Object[] arr = (Object[])obj;
			for (int i = 0; i < arr.length; i++) {
				Object val = arr[i];
				HashSet<Object> eInputs = interpretObjectDeps(val, depth - 1, record, register, writeObj);
				
				if (eInputs != null) {
					inputs.addAll(eInputs);
				}
			}
		}
		
		return inputs;
	}
	
	public static HashSet<Object> interpretCollectionDeps(Collection collection, 
			int depth, 
			IORecord record, 
			boolean register, 
			boolean writeObj) {
		if (record.getId() == -1) {
			return null;
		}
		
		if (collection == null) {
			return null;
		}
		
		HashSet<Object> inputs = new HashSet<Object>();
		for (Object o: collection) {
			HashSet<Object> eInputs = interpretObjectDeps(o, depth - 1, record, register, writeObj);
			if (eInputs != null) {
				inputs.addAll(eInputs);
			}
		}
		
		return inputs;
	}
	
	public static HitoLabel interpretFlowTo(Taint t, IORecord record) {
		if (record.getId() == - 1) {
			return null;
		}
		
		if (t == null) {
			return null;
		}
		
		if (t.lbl == null) {
			return null;
		}
		
		long execIdx = record.getId();
		synchronized(t.lbl) {
			HitoLabel latestLabel = null;
			ArrayList<HitoLabel> labels = (ArrayList<HitoLabel>)t.lbl;
			//System.out.println("Flow labels: " + t.lbl);
			
			for (HitoLabel lbl: labels) {
				if (lbl.flowTo >= execIdx) {
					if (latestLabel == null) {
						latestLabel = lbl;
					} else if (lbl.flowTo > latestLabel.flowTo) {
						latestLabel = lbl;
					}
				}
			} 
			return latestLabel;
		}
		
		
	}
	
	public static void checker(int val) {
		System.out.println("Checker: " + MultiTainter.getTaint(val));
	}
}
