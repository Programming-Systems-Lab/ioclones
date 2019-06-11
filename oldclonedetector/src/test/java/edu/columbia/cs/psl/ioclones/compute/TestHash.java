package edu.columbia.cs.psl.ioclones.compute;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import edu.columbia.cs.psl.ioclones.testpojos.Obj1;
import edu.columbia.cs.psl.ioclones.testpojos.Obj2;
import edu.columbia.cs.psl.ioclones.utils.DeepHash;

public class TestHash {
	
	@Test
	public void testInt() {
		int i = 5;
		assertEquals(DeepHash.deepHash(i), DeepHash.deepHash(new Integer(5)));
	}
	
	@Test
	public void testInt2() {
		List<Integer> testList = new ArrayList<Integer>();
		testList.add(3);
		assertEquals(DeepHash.deepHash(testList), DeepHash.deepHash(3));
	}
	
	@Test
	public void testMultipleInt() {
		List<Integer> test = new ArrayList<Integer>();
		test.add(4);
		test.add(4);
		test.add(4);
		assertEquals(DeepHash.deepHash(test), DeepHash.deepHash(4));
	}
	
	@Test
	public void testDiffCollection() {
		List<Integer> test = new ArrayList<Integer>();
		test.add(4);
		test.add(4);
		test.add(4);
		
		int[] test2 = new int[]{4};
		assertEquals(DeepHash.deepHash(test), DeepHash.deepHash(test2));
	}
	
	@Test
	public void testString() {
		String test1 = "abc";
		Set<String> test2 = new HashSet<String>();
		test2.add("abc");
		assertEquals(DeepHash.deepHash(test1), DeepHash.deepHash(test2));
	}
	
	@Test
	public void testSort() {
		int[] test = {2, 3, 1};
		int[] test2 = new int[test.length];
		System.arraycopy(test, 0, test2, 0, test.length);
		Arrays.sort(test2);
		assertEquals(DeepHash.deepHash(test), DeepHash.deepHash(test2));
	}
	
	@Test
	public void testDouble() {
		double d = 5.2;
		assertEquals(DeepHash.deepHash(d), DeepHash.deepHash(new Double(5.2)));
	}
	
	@Test
	public void testDouble2() {
		double d1 = 5.138;
		double d2 = 5.139;
		int d1Hash = DeepHash.deepHash(d1);
		int d2Hash = DeepHash.deepHash(d2);
		int original = DeepHash.deepHash(5.14);
		
		assertEquals(d1Hash, original);
		assertEquals(d2Hash, original);
		assertEquals(d1Hash, d2Hash);
	}
	
	@Test
	public void testMultipleDouble() {
		int original = DeepHash.deepHash(5.14);
		int multiple = DeepHash.deepHash(new double[]{5.138, 5.14});
		assertEquals(original, multiple);
	}
	
	@Test
	public void testFloat() {
		float f = 100.83f;
		assertEquals(DeepHash.deepHash(f), DeepHash.deepHash(new Float(100.83)));
	}
	
	@Test
	public void testFloat2() {
		float d1 = 100.825f;		
		float d2 = 100.824f;
		int d1Hash = DeepHash.deepHash(d1);
		int d2Hash = DeepHash.deepHash(d2);
		int original = DeepHash.deepHash(100.83f);
		
		assertEquals(d1Hash, original);
		assertNotEquals(d2Hash, original);
		assertNotEquals(d1Hash, d2Hash);
	}
	
	@Test
	public void testObj() {
		Obj1 mo = new Obj1();
		int myDeepHash = DeepHash.deepHash(mo);
		int total = DeepHash.deepHash("abc") 
				+ DeepHash.deepHash(7) 
				+ DeepHash.deepHash(180.34) 
				+ DeepHash.deepHash(new float[]{4.0f, 5.2f, 6.8f});
		assertEquals(myDeepHash, total);
	}
	
	@Test
	public void testDiffObj() {
		Obj1 mo = new Obj1();
		int myDeepHash = DeepHash.deepHash(mo);
		Obj2 yo = new Obj2();
		int yourDeepHash = DeepHash.deepHash(yo);
		assertEquals(myDeepHash, yourDeepHash);
	}
}
