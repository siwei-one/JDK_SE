package test;

public class Singleton {

	private static class InstanceHolder {
		public static Singleton uniqueInstance = new Singleton();
	}

	private Singleton() {}

	public static Singleton getUniqueInstance() {
		return InstanceHolder.uniqueInstance; // 这里会导致InstanceHolder类被初始化
	}
}