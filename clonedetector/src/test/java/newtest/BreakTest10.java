package newtest;

/*
 * Below methods does not accept input but return the same number. They are not considered as clones. 
 * I think both should be considered as clones
 */
public class BreakTest10 {
	
	static int returnANumber() {
		return 10;
	}
	
	static int returnANumber2() {
		return 10;
	}

	public static void main(String[] args) {
		returnANumber();
		returnANumber2();
	}
}
