package test;

import java.util.Collections;
import java.util.HashMap;

public class T {
	
	public static void main(String[] args) {
		
		// 测试下按位异或
		System.out.println(3^4); // 7   011^100 = 111
		System.out.println(true^false); // true
		System.out.println(true^true); // false
		System.out.println(false^false); // false
		
		// 测试下Map的put
		HashMap<Object, Object> map = new HashMap<>();
		
		System.out.println(map.put("a", 1)); // null
		System.out.println(map.put("b", 2)); // null
		System.out.println(map.put("c", 3)); // null
		System.out.println(map.put("a", 4)); // 1
		
		System.out.println(map);
		
		// 这里测试的是阈值计算方法
		System.out.println(tableSizeFor(1)); // 1 2的0
		System.out.println(tableSizeFor(2)); // 2 2的1
		System.out.println(tableSizeFor(3)); // 4 2的1多一点
		System.out.println(tableSizeFor(4)); // 4 2的2
		System.out.println(tableSizeFor(5)); // 8 2的2多一点
		System.out.println(tableSizeFor(6)); // 8 2的2多一点
		System.out.println(tableSizeFor(7)); // 8 2的2多一点
		System.out.println(tableSizeFor(8)); // 8 2的3
		System.out.println(tableSizeFor(9)); // 16 2的3多一点
		System.out.println(tableSizeFor(10)); // 16 2的3多一点
		
//		String nu = null;
//		String s = new String(nu);
		
		System.out.println("测试String的hashCode方法");
		
		String s = "ab";
		System.out.println('a' + 0); // 97
		System.out.println('b' + 0); // 98
		System.out.println('a' * 31 + 'b');
		
		System.out.println(s.hashCode());
		System.out.println("a".hashCode());
		String s1 = s.replace('a', 'b');
		
		System.out.println("------");
		System.out.println(s1);
		System.out.println(s.hashCode());
		
		System.out.println("-----");
		System.out.println(s.compareTo(s1)); // -1
		System.out.println(s.compareTo(s)); // 0
		System.out.println("cb".compareTo(s1)); // 0
		
		Integer i = 0;
		
		// 私有的
//		Collections s = null;
		
		
		
		
		
	}
	
	
	static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= 1 << 30) ? 1 << 30 : n + 1;
    }

}


interface E {
	
}

interface G {
	
}
interface K {
	
}

/**
 * 接口可以多继承
 * @author Yuer
 *
 */
interface J extends E, G {
	
}
