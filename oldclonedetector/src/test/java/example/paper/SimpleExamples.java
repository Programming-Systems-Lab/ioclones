package example.paper;

import java.util.Collections;
import java.util.List;

public class SimpleExamples {
	
	public static int staticWritten;
	
	public double instanceWritten;
	
	public static long[] array;
	
	public long[] longs;
	
	public long[] a;
	
	public long b;
	
	public static String testTable(int i) {
		String ret = "";
		switch(i) {
			case 1:
				ret = "123";
				break ;
			case 2:
				ret = "456";
				break ;
			default:
				ret = "789";
				break ;
		}
		return ret;
	}
	
	public static String testLookup(int i) {
		String ret = "";
		switch(i) {
			case 1:
				ret = "123";
				break;
			case 100:
				ret ="100";
				break ;
			case 1000:
				ret = "1000";
				break ;
			default:
				ret = "10000";
				break ;
			
		}
		return ret;
	}
	
	public static int testIf(int i) {
		int ret = -1;
		if (i > 5) {
			ret = 10;
		} else {
			ret = 10000;
		}
		return ret;
	}
	
	public static int testLoop(int[] in) {
		int ret = 0;
		for (int i = 0; i < in.length; i++) {
			ret +=in[i];
		}
		return ret;
	}
	
	public static int testLength(double[] in) {
		int j = 5 + in.length;
		return j;
	}
	
	public static int testAdd(int i, int j) {
		return i + j + 5;
	}
	
	public static void testStaticWritten(int i, int j) {
		staticWritten = i + j;
	}
	
	public static int testStaticRead(int i) {
		return i + staticWritten;
	}
	
	public void testInstanceWritten(double d1, double d2) {
		double written = d1 + d2;
		this.instanceWritten = written;
	}
	
	public double testInstanceRead(double d1) {
		return d1 + this.instanceWritten;
	}
	
	public static double testInstanceRead2(SimpleExamples ex1) {
		return ex1.instanceWritten + staticWritten;
	}
	
	public static void testSort(List<Integer> data) {
		Collections.sort(data);
	}
	
	public long retLongs(int i, int j) {
		return longs[i] + j;
	}
}
