
public class SimpleExample {
	int field;
	int foo()
	{
		int i = field;
		int j = 0;
//		int k = i + j;
		int k = 10;
		if(j == 0)
			k = i+j;
		return k;
	}
}
