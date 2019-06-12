package example.paper;

public class YourObject {
	
	public static String theString;
	
	public boolean theBool;
	
	public String theName;
	
	public int theAge;
	
	public YourObject other;
	
	/**
	 * expected input {0, 1, 2, 3, 2.0}
	 * expected output {5.0}
	 * @param i
	 * @param d
	 */
	public static void primitiveTest(int i1, double d2) {
		int k = 0;
		for (int j = 0; j < 3; j++) {
			k = i1++;
		}
		System.out.println(d2 + k);
	}
	
	/**
	 * expected input {0, 3, 5}
	 * expected output {5}
	 * expected input {5, 6}
	 * expected output {5}
	 * @param yo
	 * @param i
	 * @return
	 */
	public int objTest(YourObject yo, int i) {
		YourObject ptr = null;
		if (i > 5) {
			ptr = new YourObject();
		} else {
			ptr = yo;
		}
		return ptr.theAge + 5;
	}
	
	public static void main(String[] args) {
		YourObject yo = new YourObject();
		primitiveTest(1, 2.0);
		yo.objTest(yo, 3);
		yo.objTest(yo, 6);
	}

}
