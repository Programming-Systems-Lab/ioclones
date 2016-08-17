package newtest;

/*
 * Test Cloning
 */
public class BreakTest8 implements Cloneable{  
	int rollno;  
	String name;  
	  
	BreakTest8(int rollno,String name){  
		this.rollno=rollno;  
		this.name=name;  
	}  
	  
	public Object clone()throws CloneNotSupportedException{  
		return super.clone();  
	}  
	
	static void testClone1() throws CloneNotSupportedException{
		BreakTest8 s1=new BreakTest8(101,"Mike");  
		  
		BreakTest8 s2=(BreakTest8)s1.clone();  
	  
		System.out.println(s1.rollno+" "+s1.name);  
		System.out.println(s2.rollno+" "+s2.name); 
	}
	
	static void testClone2() throws CloneNotSupportedException{
		BreakTest8 s1=new BreakTest8(102,"harsha");  
		  
		BreakTest8 s2=(BreakTest8)s1.clone();  
	  
		System.out.println(s1.rollno+" "+s1.name);  
		System.out.println(s2.rollno+" "+s2.name); 
	}
	
	  
	public static void main(String args[]){  
		try{  
			testClone1();
			testClone2();
		  
		}catch(CloneNotSupportedException c){
			c.printStackTrace();
		}  
	  
	}  
}  
