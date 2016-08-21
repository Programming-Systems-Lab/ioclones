package newtest;

import newtest.dto.Person;

/*
 * All 4 methods mentioned here show up as clones, but I think as there are other operations being done, it should not be the case
 */
public class BreakTest9 {

	int createPerson1(int i, int j) {
		Person p = new Person();
		p.setId(1);
		p.setName("there");
		p.setPhone("123-456-7890");
		return i + j + 2 - 2;
	}
	
	int createPerson2(int i, int j) {
		Person p = new Person();
		p.setId(2);
		p.setName("hithe");
		p.setPhone("123-456-7890");
		return i + j;
	}
	
	int testAdd1(int i, int j) {
		return i + j;
	}
	
	int testAdd2(int a, int b) {
		if(a > b) {
			return a - b;
		} else {
			return a + b;
		}
		
	}

	public static void main(String[] args) {
		BreakTest9 test = new BreakTest9();
		test.createPerson1(1, 2);
		test.createPerson2(1, 2);
		test.testAdd1(1, 2);
		test.testAdd2(1, 2);
	}

}
