package edu.columbia.cs.psl.ioclones.analysis;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cedarsoftware.util.DeepEquals;

public class ComplexHasher extends AbstractSim {
	
	public void unrollObj(Object e, List<Object> recorder) {
		if (e.getClass().isArray()) {
			int length = Array.getLength(e);
			for (int i = 0; i < length; i++) {
				Object o = Array.get(e, i);
				this.unrollObj(o, recorder);
			}
		} else if (Collection.class.isAssignableFrom(e.getClass())) {
			Collection tmp = (Collection)e;
			Iterator tmpIt = tmp.iterator();
			while (tmpIt.hasNext()) {
				Object o = tmpIt.next();
				this.unrollObj(o, recorder);
			}
		} else if (Map.class.isAssignableFrom(e.getClass())) {
			Map tmp = (Map)e;
			Iterator<Entry> tmpIt = tmp.entrySet().iterator();
			while (tmpIt.hasNext()) {
				Entry ele = tmpIt.next();
				this.unrollObj(ele.getKey(), recorder);
				this.unrollObj(ele.getValue(), recorder);
			}
		} else {
			recorder.add(e);
		}
	}

	@Override
	public double computeIOSim(List l1, List l2) {
		// TODO Auto-generated method stub
		List<Object> l1Record = new ArrayList<Object>();
		List<Object> l2Record = new ArrayList<Object>();
		
		this.unrollObj(l1, l1Record);
		this.unrollObj(l2, l2Record);
		
		if (l1Record.size() != l2Record.size()) {
			return 0.0;
		} else {
			int hash1 = DeepEquals.deepHashCode(l1Record);
			int hash2 = DeepEquals.deepHashCode(l2Record);
			
			if (hash1 == hash2) {
				return 1.0;
			} else {
				return 0.0;
			}
		}
	}
}
