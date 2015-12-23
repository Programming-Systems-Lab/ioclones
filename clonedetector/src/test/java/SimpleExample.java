
public class SimpleExample {
	int field;
	
	int foo(int in) {
		int i = this.field;
		int j = 0;
//		int k = i + j;
		int k = 10;
//		in = 5;
		if(j == 0) {
			int l = this.field + in;
			k = i + l + in;
		}
		return k;
	}
}
