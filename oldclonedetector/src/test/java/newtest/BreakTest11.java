package newtest;

/*
 * Below two methods should be clones, but as the input number is different, it doesn't show up as clones
 */
public class BreakTest11 {
	
	static int returnANumber(int i, int j) {
		return i + j;
	}
	
	static int returnANumber2(int i, int j) {
		return i + j;
	}

	public static void main(String[] args) {
		returnANumber(1,2);
		returnANumber2(2,3);
	}
}
