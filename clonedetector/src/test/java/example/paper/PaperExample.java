package example.paper;

public class PaperExample {
	
	public static Person p = new Person();
	
	public static class Person {
		public String name;
		public int age;
		public Person[] relatives;
		public int relAge;
		public Person check;
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
		
		System.out.println(pos);
		return ret;
	}
	
	public static void insert(Person[]to, Person rel, int pos) {
		to[pos] = rel;
	}
	
	public static void insert(Person me, Person rel, int pos) {
		me.relatives[pos] = rel;
	}
	
	public static int sum(Person me) {
		int sum = 0;
		for (Person p: me.relatives) {
			sum += p.age;
		}
		return sum;
	}
	
	public static int sum(Person[] relatives) {
		int sum = 0;
		for (Person r: relatives) {
			sum += r.age;
		}
		return sum;
	}
	
	/*public static void test(Person me) {
		me.check.age = 5;
	}
	
	public static int testStopRecord(Person p, int i) {
		//p = new Person();
		if (i > 0) {
			p = new Person();
		}
		
		return p.age + 5;
	}
	
	public static void testDeepStatic() {
		PaperExample.p.name = "abc";
	}
	
	public int[] testSingle(int num) {
		int[] ret = new int[num];
		return ret;
	}
	
	public Object[][] testMulti(int i, int j) {
		Object[][] ret = new Object[i][j];
		return ret;
	}
	
	public int testSwitch(int i) {
		int ret = 0;
		switch(i) {
		case 1:
			ret = 0;
			break; 
		case 2:
			ret = 1;
			break;
		default:
			ret = i;
			break ;
		}
		
		return ret;
	}
	
	public String testLookup(String s) {
		String ret = "";
		switch(s) {
		case "abc":
			ret = "abc";
			break ;
		case "cde":
			ret = "efg";
			break ;
		default:
			ret = "default";
			break ;
		}
		
		return ret;
	}*/
	
	/*public static void main(String[] args) {
		Person p = new Person();
		p.age = 3;
		testStopRecord(p, 1);
	}*/

}
