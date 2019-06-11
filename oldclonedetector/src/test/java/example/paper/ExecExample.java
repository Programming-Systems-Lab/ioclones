package example.paper;

public class ExecExample {
	
	public long[] longs = {10, 100, 1000};
	
	public int myField;
	
	public void method(AbstractClass clazz) {
		this.myField = clazz.objField + 8;
		AbstractClass.staticField = this.longs[2] + 15;
	}
	
	public long sum() {
		long ret = 0;
		for (int i = 0; i < longs.length; i++) {
			ret += longs[i];
		}
		
		return ret;
	}
	
	public static long staticSum(ExecExample ee) {
		long ret = 0;
		for (int i = 0; i < ee.longs.length; i++) {
			ret += ee.longs[i];
		}
		
		return ret;
	}
	
	public static long staticSum2() {
		ExecExample ee = new ExecExample();
		long ret = 0;
		for (int i = 0; i < ee.longs.length; i++) {
			ret += ee.longs[i];
		}
		
		return ret;
	}
	
	public static long blackStaticSum(ExecExample ee) {
		ee = new ExecExample();
		long ret = 0;
		for (int i = 0; i < ee.longs.length; i++) {
			ret += ee.longs[i];
		}
		
		return ret;
	}
	
	public static long blackStaticSum2(ExecExample ee, int i) {
		long ret = 0;
		if (i > 5) {
			for (int j = 0; j < ee.longs.length; j++) {
				ret += ee.longs[j];
			}
		} else {
			ee = new ExecExample();
			for (int j = 0; j < ee.longs.length; j++) {
				ret += ee.longs[j];
			}
		}
		return ret;
	}
	
	public static void main(String[] args) {
		ExecExample ee = new ExecExample();
		ee.sum();
		staticSum(ee);
		staticSum2();
		blackStaticSum(ee);
		blackStaticSum2(ee, 6);
		blackStaticSum2(ee, 1);
	}
	
	public static class AbstractClass {
		public int objField;
		public static long staticField;
	}

}
