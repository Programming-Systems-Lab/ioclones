package example;

public class SimilaritySubject {
	
	public static int addInt(int i, int j) {
		return i + j;
	}
	
	public static double addDouble(double d1, double d2) {
		return d1 + d2;
	}
	
	public static void main(String[] args) {
		System.out.println(addInt(1, 2));
		System.out.println(addDouble(1.0, 2.0));
	}

}
