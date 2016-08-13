package newtest;

/*
 * Test case to induce Manual Null pointer exception and see how system behaves
 */
public class BreakTest4 {
	
	
	public static void errorMethod(String err) {
		
		String sub = err.substring(0, 5);
		
		System.out.println(sub);
	}
	
	public static void rightMethod(String corr) {
		String sub = corr.substring(0, 5);
		
		System.out.println(sub);
	}
	

	public static void main(String[] args) {
		
		rightMethod("hellothere");
		errorMethod(null);

	}

}
