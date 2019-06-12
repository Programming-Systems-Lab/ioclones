package example;

public class HitoshiIOExample {
	
	public class Person {
		public String name;
		public int age;
		public Person[] relatives;
	}
	
	public int addRelative(Person me, String rName, int rAge) {
		Person newRelative = new Person();
		newRelative.name = rName;
		newRelative.age = rAge;
		
		me.relatives[10] = newRelative;
		
		int ret = sumAge(me.relatives);
		return ret;
	}
	
	public static int sumAge(Person[] people) {
		int sum = 0;
		for (Person p: people) {
			sum += p.age;
		}
		
		return sum;
	}
}
