package newtest;

/*
 * Works as expected
 */
public class BreakTest15 {

	Rectangle returnRectangle(int a, int b) {
		return new Rectangle(a,b);
	}
	
	Rectangle returnRectangle2(int a, int b) {
		return new Rectangle(a,b);
	}

	public static void main(String[] args) {
		
		BreakTest15 test = new BreakTest15();
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
	}
	

}
