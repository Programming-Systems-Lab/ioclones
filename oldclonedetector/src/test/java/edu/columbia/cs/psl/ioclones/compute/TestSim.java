package edu.columbia.cs.psl.ioclones.compute;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import edu.columbia.cs.psl.ioclones.sim.FastAnalyzer;
import edu.columbia.cs.psl.ioclones.testpojos.Obj1;
import edu.columbia.cs.psl.ioclones.testpojos.Obj2;

public class TestSim {
	
	public static final double EPSILON = Math.pow(10, -5);
	
	@Test
	public void testSimple() {
		Set<Object> set1 = new HashSet<Object>();
		set1.add(1);
		set1.add(2);
		set1.add(3);
		
		Set<Object> set2 = new HashSet<Object>();
		set2.add(1);
		set2.add(3);
		set2.add(2);
		
		FastAnalyzer fa = new FastAnalyzer();
		double sim = fa.similarity(set1, set2);
		assertEquals(sim, 1.0, EPSILON);
	}
	
	@Test
	public void testSimple2() {
		Set<Object> set1 = new HashSet<Object>();
		set1.add(1);
		set1.add(2);
		set1.add(3);
		
		Set<Object> set2 = new HashSet<Object>();
		set2.add(1);
		set2.add(4);
		set2.add(2);
		
		FastAnalyzer fa = new FastAnalyzer();
		double sim = fa.similarity(set1, set2);
		assertEquals(sim, 0.5, EPSILON);
	}
	
	@Test
	public void testComplex() {
		Set<Object> set1 = new HashSet<Object>();
		double[] d1 = {1.0, 2.0, 109.0};
		String s1 = "klp*()&";
		float f1 = 6.98f;
		set1.add(d1);
		set1.add(s1);
		set1.add(f1);
		
		Set<Object> set2 = new HashSet<Object>();
		double[] d2 = {109.0, 1.0, 2.0};
		String s2 = "klp*()&-";
		float f2 = 6.975f;
		set2.add(d2);
		set2.add(s2);
		set2.add(f2);
		
		FastAnalyzer fa = new FastAnalyzer();
		double sim = fa.similarity(set1, set2);
		assertEquals(sim, 0.5, EPSILON);
	}
	
	@Test
	public void testComplex2() {
		Set<Object> set1 = new HashSet<Object>();
		double[] d1 = {1.0, 2.0, 109.0};
		String s1 = "klp*()&";
		float f1 = 6.98f;
		Obj1 obj1 = new Obj1();
		set1.add(d1);
		set1.add(s1);
		set1.add(f1);
		set1.add(obj1);
		
		Set<Object> set2 = new HashSet<Object>();
		double[] d2 = {109.0, 1.0, 2.0};
		String s2 = "klp*()&-";
		float f2 = 6.975f;
		Obj2 obj2 = new Obj2();
		set2.add(d2);
		set2.add(s2);
		set2.add(f2);
		set2.add(obj2);
		
		FastAnalyzer fa = new FastAnalyzer();
		double sim = fa.similarity(set1, set2);
		assertEquals(sim, 0.6, EPSILON); //Union: 4 + 4 - 3, Intersect: 3
	}
	
	@Test
	public void testComplex3() {
		Set<Object> set1 = new HashSet<Object>();
		double[] d1 = {1.0, 2.0, 109.0};
		String s1 = "klp*()&";
		float f1 = 6.98f;
		Obj1 obj1 = new Obj1();
		set1.add(d1);
		set1.add(s1);
		set1.add(f1);
		set1.add(obj1);
		
		Set<Object> set2 = new HashSet<Object>();
		double[] d2 = {109.0, 1.0, 2.0};
		String s2 = "klp*()&-";
		float f2 = 6.975f;
		Obj2 obj2 = new Obj2();
		set2.add(d2);
		set2.add(s2);
		set2.add(f2);
		set2.add(obj2);
		set2.add(9876543);
		
		FastAnalyzer fa = new FastAnalyzer();
		double sim = fa.similarity(set1, set2);
		assertEquals(sim, 0.5, EPSILON); //Union: 4 + 5 - 3, Intersect: 3
	}

}
