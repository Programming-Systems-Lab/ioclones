package newtest;

/*
 * Test case works as expected
 */
public class BreakTest13 {

	int getPerimeter(Rectangle r) {
		return r.height + r.width;
	}
	
	int getPerimeter2(Rectangle r) {
		return r.height + r.width;
	}

	public static void main(String[] args) {
		
		BreakTest13 test = new BreakTest13();
		Rectangle rect = test.new Rectangle(1,2);
		test.getPerimeter(rect);
		test.getPerimeter2(rect);
		
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
