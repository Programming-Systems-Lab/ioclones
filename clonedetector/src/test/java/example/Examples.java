package example;

public class Examples {
	
	/*public static int[] array() {
		int i = 5;
		int[] ret = new int[3];
		ret[2] = i;
		return ret;
	}
	
	public static int[] array2() {
		return new int[]{1, 2, 3};
	}*/
	
	public static double moAdd(MyObject mo1, MyObject mo2) {
		double ret = mo1.age + mo2.age;
		return ret;
	}
	
	/*public static int add(int i, int j) {
		int ret = i + j;
		return ret;
	}
	
	public static double add(int i, double d) {
		double ret = i + d;
		return ret;
	}
	
	public static byte retByte() {
		return (byte)1;
	}*/
	
	/*public static int controlExample(int i, int j) {
		int base = 15;
		if (i < j) {
			base = 5;
		}
		
		switch(i) {
		case 1:
			System.out.println("1");
			break ;
		case 100:
			System.out.println("100");
			break ;
		default:
			System.out.println("Default");
		}
		
		return base;
	}*/
	
	public static void main(String[] args) {
		//System.out.println(add(3, 5.0));
		//System.out.println(System.getProperty("java.class.path"));
		//add(3, 5.0);
		//add(3, 5);
		MyObject mo1 = new MyObject(3);
		MyObject mo2 = new MyObject(5);
		for (int i = 0; i < 5; i++) {
			moAdd(mo1, mo2);
		}
	}

}
