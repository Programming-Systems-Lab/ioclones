package example.paper;

public class PaperExample {
	
	public static class Person {
		public String name;
		public int age;
		public Person[] relatives;
	}
	
	public static int addRelative(Person me, String rName, int rAge, int pos, double useless) {
		Person newRel = new Person();
		newRel.name = rName;
		newRel.age = rAge;
		
		if (pos > 0) {
			insert(me, newRel, pos);
		}
		int ret = sum(me.relatives);
		
		double k = useless + 1;
		return ret;
	}
		
	public static void insert(Person me, Person rel, int pos) {
		if (me != null) {
			me.relatives[pos] = rel;
		}
	}
		
	public static int sum(Person[] relatives) {
		int sum = 0;
		for (Person r: relatives) {
			sum += r.age;
		}
		return sum;
	}
		
	public static void main(String[] args) {
		Person p = new Person();
		p.name = "Jack";
		p.age = 25;
		p.relatives = new Person[3];
		
		int totalAge = addRelative(p, "Mary", 23, 0, 3249);
		System.out.println("Total age: " + totalAge);
	}

}
