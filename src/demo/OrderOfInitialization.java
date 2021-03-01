package demo;

// housekeeping/OrderOfInitialization.java
// Demonstrates initialization order
// When the constructor is called to create a
// Window object, you'll see a message:
class Window {
	Window(int marker) {
		System.out.println("Window(" + marker + ")");
	}
}

class House {
	Window w1 = new Window(1); // Before constructor

	House() {
		// Show that we're in the constructor:
		System.out.println("House()");
		w3 = new Window(33); // Reinitialize w3
	}

	Window w2 = new Window(2); // After constructor

	void f() {
		System.out.println("f()");
	}

	Window w3 = new Window(3); // At end
}

/**
 * 展示非静态的成员初始化的顺序
 * 在类中变量定义的顺序决定了它们初始化的顺序。即使变量定义散布在方法定义之间，
 * 它们仍会在任何方法（包括构造器）被调用之前得到初始化。
 * 
 * 接下来展示静态成员的初始化和常量的初始化位置
 * @author Yuer
 *
 */
public class OrderOfInitialization {
	public static void main(String[] args) {
		House h = new House();
		h.f(); // Shows that construction is done
	}
}