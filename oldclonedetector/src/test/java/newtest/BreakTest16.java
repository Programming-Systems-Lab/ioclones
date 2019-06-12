package newtest;

/*
 * In below example, 3 methods are considered cones, returnRectangle(a,b) returnRectangle2(a,b) and rectangle.findArea().
 * findArea() input parameters are different and there is extra computation happening inside thread created by returnRectangle methods
 */
public class BreakTest16 {

	int returnRectangle(int a, int b) {
		
		MyThread thread = new MyThread(new Rectangle(a,b));
		thread.run();
		
		return a * b;
	}
	
	int returnRectangle2(int a, int b) {
		
		MyThread thread = new MyThread(new Rectangle(a,b));
		thread.run();
		
		return a * b;
	}

	public static void main(String[] args) {
		
		BreakTest16 test = new BreakTest16();
		test.returnRectangle(1,2);
		test.returnRectangle2(1,2);
		
	}
	
	class Rectangle {
		
		public Rectangle(int height, int width) {
			this.height = height;
			this.width = width;
		}
		
		public int width;
		public int height;
		
		public int findArea() {
			return width*height;
		}
		
		public int findPerimeter() {
			return width + height;
		}
	}
	
	 class MyThread implements Runnable {
		 
		 private Rectangle r;

		   public MyThread(Rectangle parameter) {
			   this.r = parameter;
		   }

		   public void run() {
			   System.out.println("Area is " + r.findArea());
			   System.out.println("Perimeter is " + r.findPerimeter());
		   }
		}
	

}
