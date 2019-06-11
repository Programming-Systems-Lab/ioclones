package newtest;

import newtest.dto.Person;

/*
 * Simple Test case of creating a custom object and setting values to the object
 */
public class BreakTest1 {
	
	int createPerson1(int i, int j) {
		Person p = new Person();
		p.setId(1);
		p.setName("there");
		p.setPhone("123-456-7890");
		return i + j;
	}
	
	int createPerson2(int i, int j) {
		Person p = new Person();
		p.setId(2);
		p.setName("hithe");
		p.setPhone("123-456-7890");
		
		if(i == 3) {
			return i ;
		} else if(j == 4) {
			return j;
		}
		
		return i + j;
	}
	
	int testAdd1(int i, int j) {
		return i + j;
	}
	
	int testAdd2(int a, int b) {
		if(b > a) {
			return b;
		} else {
			return a + b;
		}
		
	}

	public static void main(String[] args) {
		BreakTest1 test = new BreakTest1();
		test.createPerson1(1, 5);
		test.createPerson2(1, 2);
		test.testAdd1(2, 2);
		test.testAdd2(1, 2);
	}

}
