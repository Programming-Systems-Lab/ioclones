package newtest;

import java.lang.reflect.Method;

/*
 * Test Reflection
 */
public class BreakTest7 {
	
	static void printSome(Class cls, Object obj) throws Exception {
		
		//no paramater
		Class noparams[] = {};

		//String parameter
		Class[] paramString = new Class[1];
		paramString[0] = String.class;

		//int parameter
		Class[] paramInt = new Class[1];
		paramInt[0] = Integer.TYPE;
		
		//call the printIt method
		Method method = cls.getDeclaredMethod("printIt", noparams);
		method.invoke(obj, null);

		//call the printItString method, pass a String param
		method = cls.getDeclaredMethod("printItString", paramString);
		method.invoke(obj, new String("mkyong"));

		//call the printItInt method, pass a int param
		method = cls.getDeclaredMethod("printItInt", paramInt);
		method.invoke(obj, 123);
	}
	
	static void printSomeOther(Class cls, Object obj) throws Exception {
		
		//no paramater
		Class noparams[] = {};

		//String parameter
		Class[] paramString = new Class[1];
		paramString[0] = String.class;

		//int parameter
		Class[] paramInt = new Class[1];
		paramInt[0] = Integer.TYPE;
		
		//call the setCounter method, pass a int param
		Method method = cls.getDeclaredMethod("setCounter", paramInt);
		method.invoke(obj, 999);

		//call the printCounter method
		method = cls.getDeclaredMethod("printCounter", noparams);
		method.invoke(obj, null);
	}

	public static void main(String[] args) {
		

		try{
		        //load the AppTest at runtime
			Class cls = Class.forName("newtest.dto.ReflectionTest");
			Object obj = cls.newInstance();
			printSome(cls, obj);
			printSomeOther(cls, obj);
			
		} catch(Exception ex){
			ex.printStackTrace();
		}

	}

}
