package newtest;

/*
 * When computation of multiplication is done in a different class, methods doesn't seem to be considering as clones. But if returned as direct multiplication,
 * system considers it as clones
 */
public class BreakTest12 {

	int getRectangle(int h, int w) {
		Rectangle rect = new Rectangle(h,w);
		return rect.findArea();
	//	return h*w;
	}
	
	int getRectangle2(int h, int w) {
		Rectangle rect = new Rectangle(h,w);
		return rect.findArea();
	//	return h*w;
	}

	public static void main(String[] args) {
		
		BreakTest12 test = new BreakTest12();
		test.getRectangle(1,2);
		test.getRectangle2(1,2);
		
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
