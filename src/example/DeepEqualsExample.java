package example;

import java.util.List;
import java.util.ArrayList;

import com.cedarsoftware.util.DeepEquals;

public class DeepEqualsExample {
	
	public static void main(String[] args) {
		MyObject mo = new MyObject("abc", 15);
		YourObject yo = new YourObject();
		yo.theName = "abc";
		yo.theAge = 15;
		
		System.out.println(DeepEquals.deepEquals(mo, yo));
		System.out.println(DeepEquals.deepHashCode(mo));
		System.out.println(DeepEquals.deepHashCode(yo));
		
		List moList = new ArrayList();
		moList.add(mo);
		
		List yoList = new ArrayList();
		yoList.add(yo);
		yoList.add(mo);
		yoList.add(yo);

		System.out.println(DeepEquals.deepHashCode(moList));
		System.out.println(DeepEquals.deepHashCode(yoList));
		System.out.println(yoList.hashCode());
		
		List fList = new ArrayList();
		fList.add(2);
		fList.add(2);
		fList.add(5);
		
		List sList = new ArrayList();
		sList.add(2);
		sList.add(5);
		System.out.println(DeepEquals.deepHashCode(fList));
		System.out.println(DeepEquals.deepHashCode(sList));
	}

}
