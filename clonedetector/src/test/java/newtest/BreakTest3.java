package newtest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import newtest.dto.Person;

/*
 * Test Case for running multiple threads at the same time using Executor
 */
public class BreakTest3 {
	
	static ExecutorService service = Executors.newFixedThreadPool(10);
	
	public static int createThread1() {
		
		int count = 0;
		for(int i=0;i<5;i++) {
			count ++;
			Runnable worker = new Runnable() {
				
				@Override
				public void run() {
					Person p1 = new Person();
					p1.setId(5);
					System.out.println(Math.random());
				}
			};
			service.execute(worker);
		}
		return count;
	}
	
	public static int createThread2() {
		int count = 0;
		for(int i=0;i<5;i++) {
			count ++;
			Runnable worker = new Runnable() {
				
				@Override
				public void run() {
					Person p1 = new Person();
					p1.setName("Hello");
					System.out.println(Math.random() + p1.getId());
				}
			};
			service.execute(worker);
		}
		return count;
	}
	
	public static int createThread3() {
		int count = 0;
		for(int i=0;i<5;i++) {
			count ++;
			service.execute(new BreakTest2());
		}
		return count;
	}

	public static void main(String[] args) {
		
		createThread1();
		createThread2();
		createThread3();

	}

}
