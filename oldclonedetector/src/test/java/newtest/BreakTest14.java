package newtest;

/*
 * System doesn't detect returnSum and returnSum2 as clones
 */
public class BreakTest14 {

	String returnSum(int a, int b) {
		int sum = a + b;
		return "Sum is " + sum;
	}
	
	String returnSum2(int a, int b) {
		int sum = a + b;
		return "Sum is " + sum;
	}

	public static void main(String[] args) {
		
		BreakTest14 test = new BreakTest14();
		test.returnSum(1,2);
		test.returnSum2(1,2);
		
	}
	

}
