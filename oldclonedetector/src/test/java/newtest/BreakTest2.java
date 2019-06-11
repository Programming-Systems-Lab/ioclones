package newtest;

/*
 * Test case to test inheritance and polymorphism
 */
public class BreakTest2 extends BreakTest1 implements Runnable {

	public static void main(String[] args) {
		BreakTest1 test = new BreakTest2();
		test.createPerson1(1,2);
		test.createPerson2(2,3);
	}

	@Override
	public void run() {
		BreakTest1 test = new BreakTest2();
		test.createPerson1(1,2);
		test.createPerson2(2,3);
	}

}
