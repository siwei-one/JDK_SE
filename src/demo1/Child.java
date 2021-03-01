package demo1;

import demo.Parent;

public class Child extends Parent {
	
	@Override
	public void test1() {
		
		System.out.println("子类的普通方法");
	}
	
	
	private void test_() {
		// 这两个指向同一个对象
		System.out.println(this.ran);
		System.out.println(super.ran);
		
		System.out.println("public:");
		System.out.println(this.a);
		System.out.println(super.a);
		
		// 虽然是外包但有继承关系所以可以访问
		System.out.println("protected:");
		System.out.println(this.b);
		System.out.println(super.b);
		
		System.out.println("default:");
//		System.out.println(this.c);
//		System.out.println(super.c);
		
		
	}

//	@Override 这里不是重写，所以不是继承
	public static  void test() {
		System.out.println("子类的静态方法");

	}
	
	public static void main(String[] args) {
		Child c = new Child();
		c.test_();
		
		System.out.println("------");

		c.test();
		c.test1();
		
		System.out.println("-----------");
		
		Parent p = new Child();
		p.test(); // 静态的，这里不会调用子类重写的 原因子类不能重写父类的test静态方法，只能隐藏
		p.test1(); // 这里会调用子类重写的
		
		
	}
}

/**
 * 讲下static
 * static：static方法可以被子类继承，但是不能被子类重写（覆盖），但是可以被子类隐藏。
 * （这里意思是说如果父类里有一个static方法，它的子类里如果没有对应的方法，那么当子类对象调用这个方法时就会使用父类中的方法。
 * 而如果子类中定义了相同的方法，则会调用子类的中定义的方法。唯一的不同就是，当子类对象上转型为父类对象时，
 * 不论子类中有没有定义这个静态方法，该对象都会使用父类中的静态方法。因此这里说静态方法可以被隐藏而不能被覆盖。
 * 这与子类隐藏父类中的成员变量是一样的。隐藏和覆盖的区别在于，子类对象转换成父类对象后，
 * 能够访问父类被隐藏的变量和方法，而不能访问父类被覆盖的方法）。
 * @author Yuer
 *
 */

class A {
	private void test() {
		Parent p = new Parent();
		
		// 外包且没有继承关系无法访问protected
		System.out.println(p.a);
//		System.out.println(p.b);
//		System.out.println(p.c);

	}
}
