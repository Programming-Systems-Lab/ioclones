package example;

public class SimpleExample {
	int field;
	
	/**
	 * Outputs: k, j (no instruction)
	 * Inputs: this.field (twice), in
	 * @param in
	 * @return
	 */
	int foo(int in) {
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
	}
	
	/**
	 * The input is only iaload this instruction
	 * @param arr
	 * @return
	 */
	/*public int sumArr(int[] arr) {
		int ret = 0;
		for (int i = 0; i < arr.length; i++) {
			ret += arr[i];
		}
		return ret;
	}*/
	
	/*public int trickyInput(int input) {
		int ret = 0;
		for (int i = 0; i < 5; i++) {
			ret = (input += 5);
		}
		
		return ret;
	}*/
	
	/**
	 * The inputs are two getfields
	 * @param mo1
	 * @param mo2
	 * @return
	 */
	/*public int simpleGetField(MyObject mo1, MyObject mo2) {
		return mo1.age + mo2.age;
	}*/
	
	/**
	 * Only toPut is the output, no input (toPut has no dependents)
	 * @param mo1
	 * @param toPut
	 */
	/*public void simplePutField(MyObject mo1, int toPut) {
		toPut += 5;
		mo1.age = toPut;
	}*/
	
	/**
	 * Outputs: fName
	 * @param mo1
	 * @param friendId
	 * @param fName
	 */
	/*public void complexPutField(MyObject mo1, int friendId, String fName) {
		MyObject targetF = mo1.friends[friendId];
		targetF.name = fName;
	}*/
	
	/**
	 * Outputs: MyObject
	 * Inputs: name, age
	 * @param name
	 * @param age
	 * @return
	 */
	/*public MyObject newObjeExample(String name, int age) {
		MyObject ret = new MyObject(name, age);
		return ret;
	}*/
	
	/**
	 * Outputs: int[]
	 * Inputs: int i and j (no instruction)
	 * @param i
	 * @return
	 */
	/*public int[] newArrayExample(int i) {
		int[] ret = new int[i];
		for (int j = 0; j < ret.length; j++) {
			ret[j] = j;
		}
		return ret;
	}*/
	
	/**
	 * Outputs: int[][][] ret
	 * Inputs: i, j, k, new MyObject, "abc" (no instruction), 12 (no instruction)
	 * @param i
	 * @param j
	 * @param k
	 * @return
	 */
	/*public MyObject[][][] newMultiObjArrayExample(int i, int j, int k) {
		MyObject[][][] ret = new MyObject[i][j][k];
		ret[1][2][3] = new MyObject("abc", 12);
		return ret;
	}*/
	
	/**
	 * Outputs: this.field
	 * Inputs: i, j
	 * @param i
	 * @param j
	 */
	/*public void ifExample(int i, int j) {
		if (i > 5) {
			int toPut = Examples.add(i, j);
			this.field = toPut;
		} else {
			this.field = 0;
		}
	}*/
	
	/*public float rewritePrimitive(float f) {
		f = 0.5f;
		return f + 1;
	}
	
	public MyObject rewriteObject(MyObject mo) {
		mo = new MyObject(15);
		return mo;
	}
	
	public int iincExample(int input) {
		for (int i = 0; i < 5; i++) {
			input++;
		}
		return input;
	}*/
	
	public int ifTest(int i, int j) {
		int ret = - 1;
		if (i > j) {
			ret = 0;
		} else {
			ret = 1;
		}
		
		System.out.println("Do somethingelse");
		return ret;
	}
}
