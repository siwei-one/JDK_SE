package demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 对集合框架的一些学习
 * @author Yuer
 *
 */
public class showCollection {
	
	
	public static void main(String[] args) {
		// 先看下Object的数组能不能插入不同的类型
		Object[] ob = new Object[8];
		
		ob[0] = 1;
		ob[1] = "gfhk";
		
		System.out.println(Arrays.toString(ob));
		
		// 再看看集合（其每个元素都为object时）能不能插入不同的类型
		// ArrayList的底层是一个Object数组，但是通过传入确定的类型最后取出来时进行相应的转化
		ArrayList<Object> list = new ArrayList<Object>();
		Iterator<Object> it = list.iterator();
		
		
		list.add(1);
		list.add("fgh");
		
		System.out.println(list.get(0).getClass());
		System.out.println(list);
		
		
		
		
	}

}
