package newtest;

import newtest.dto.Person;

/*
 * Simple Test case of creating a custom object and setting values to the object
 */
public class BreakTest1 {
	
	void createPerson1() {
		Person p = new Person();
		p.setId(1);
		p.setName("there");
		p.setPhone("123-456-7890");
	}
	
	void createPerson2() {
		Person p = new Person();
		p.setId(2);
		p.setName("hithe");
		p.setPhone("123-456-7890");
	}
	
	int testAdd1(int i, int j) {
		return i + j;
	}
	
	int testAdd2(int a, int b) {
		return a + b;
	}

	public static void main(String[] args) {
		BreakTest1 test = new BreakTest1();
		//test.createPerson1();
		//test.createPerson2();
		test.testAdd1(1, 2);
		test.testAdd2(1, 2);
	}

}
