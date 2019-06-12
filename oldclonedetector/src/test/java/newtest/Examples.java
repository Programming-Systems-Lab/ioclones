package newtest;

public class Examples {
	
	public static int addInts(int mo1, int mo2) {
		
		return mo1 + mo2;
	}
	
	public static int printInts(int a, int b) {
		
		System.out.println(a + b);
		return a +b;
	}
	
	
	public static void main(String[] args) {
		printInts(2,3);
		addInts(3,4);
	}

}
