package newtest;

/*
 * Test case to test Lambda Expression
 */
public class BreakTest5 {
	
	static void test1() {
		BreakTest5 tester = new BreakTest5();
		
		//with type declaration
	      MathOperation addition = (int a, int b) -> a + b;
			
	      //with out type declaration
	      MathOperation subtraction = (a, b) -> a - b;
	      
	      System.out.println("10 + 5 = " + tester.operate(10, 5, addition));
	      System.out.println("10 - 5 = " + tester.operate(10, 5, subtraction));
	      
	    //with parenthesis
	      GreetingService greetService1 = message ->
	      System.out.println("Hello " + message);
	      
	      greetService1.sayMessage("Mahesh");
		
	}
	
	static void test2() {
		BreakTest5 tester = new BreakTest5();
		
		//with return statement along with curly braces
	      MathOperation multiplication = (int a, int b) -> { return a * b; };
			
	      //without return statement and without curly braces
	      MathOperation division = (int a, int b) -> a / b;
	      
	      System.out.println("10 x 5 = " + tester.operate(10, 5, multiplication));
	      System.out.println("10 / 5 = " + tester.operate(10, 5, division));
	      
	    //without parenthesis
	      GreetingService greetService2 = (message) ->
	      System.out.println("Hello " + message);
	      
	      greetService2.sayMessage("Suresh");
	}
	
	   public static void main(String args[]){
			
	      test1();
	      test2();
	      
	   }
		
	   interface MathOperation {
	      int operation(int a, int b);
	   }
		
	   interface GreetingService {
	      void sayMessage(String message);
	   }
		
	   private int operate(int a, int b, MathOperation mathOperation){
	      return mathOperation.operation(a, b);
	   }
	}
