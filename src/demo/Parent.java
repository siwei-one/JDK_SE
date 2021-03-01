package demo;

import java.util.Random;

/**
 * 用来测试访问权限还有多态等
 * @author Yuer
 *
 */
public class Parent {
	
	public int a = 1;
	protected int b = 2;
	int c = 3;
	private int d = 4;
	
	// 测试另外的
	public Random ran = new Random();
	
	
	public static void main(String[] args) {
		Parent p = new Parent();
		
		System.out.println(p.a);
		System.out.println(p.b);
		System.out.println(p.c);
		System.out.println(p.d);
	}
	
	public static void test() {
		
		System.out.println("父类的静态方法");
	}
	
	public void test1() {
		System.out.println("父类的普通方法");

	}

}
