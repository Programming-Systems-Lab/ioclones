package example;

public class Examples {
	
	public static double add(int i, double d) {
		double ret = i + d;
		return ret;
	}
	
	public static void main(String[] args) {
		System.out.println(add(3, 5.0));
		System.out.println(System.getProperty("java.class.path"));
	}

}
