
public class SimpleExample {
	int field;
	
	/*int foo(int in) {
		int i = this.field;
		int j = 0;
//		int k = i + j;
		int k = 10;
//		in = 5;
		if(j == 0) {
			this.field = j;
			int l = this.field + in;
			k = i + l + in;
		}
		return k;
	}*/
	
	public int sumArr(int[] arr) {
		int ret = 0;
		for (int i = 0; i < arr.length; i++) {
			ret += arr[i];
		}
		return ret;
	}
}
