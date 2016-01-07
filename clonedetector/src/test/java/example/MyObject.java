package example;

import java.util.List;
import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MyObject extends MyParent implements MyInter{
	
	public static String staticString;
	
	public boolean bool;
	
	public String name;
	
	public int age;
		
	public MyObject[] friends;
	
	public MyObject(String name, int age) {
		this.name = name;
		this.age = age;
		staticPrivate(this.age);
	}
	
	public MyObject(int age) {
		this("default", age);
	}
	
	private static void staticPrivate(int i) {
		
	}
	
	public static void main(String[] args) {
		//String tmp = MyObject.staticString;
		
		boolean b = false;
		int i = 0;
		
		MyObject mo = new MyObject("123", 58);
		//System.out.println(mo.parentInt);
		/*int age = mo.age;
		boolean bool = mo.bool;
		Type retType = Type.getType("I");
		System.out.println(retType.getSort());
		System.out.println("int sort: " + Type.INT);
		Type methodType = Type.getMethodType("([Ljava/lang/String;)V");
		System.out.println(methodType.getClassName());
		System.out.println(methodType.getArgumentTypes()[0].getSort());
		System.out.println(methodType.getReturnType().getSort());
		System.out.println(Type.VOID);
		Integer.valueOf(5);
		
		int[] arr = new int[5];
		System.out.println(arr.length);
		Class c = MyObject.class;
		
		int k = 43214;
		
		byte by = (byte)1;
		byte test = by;
		short s = 2;
		char ch = 'c';
		
		List list = new ArrayList();
		list.add(null);
		System.out.println(null == null);*/
	}

}
