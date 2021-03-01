
package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 插入、获取的时间复杂度基本是 O(1)（前提是有适当的哈希函数，让元素分布在均匀的位置） 还有关于红黑树的操作后续再看并总结吧
 * 
 * 再看完构造方法和put方法后可以发现 事实上，new HashMap();完成后，如果没有put操作，是不会分配存储空间的。
 * 
 * 添加操作： 1.当桶数组 table 为空时，通过扩容的方式初始化 table 2.查找要插入的键值对是否已经存在，存在的话根据条件判断是否用新值替换旧值
 * 3.如果不存在，则将键值对链入链表中，并根据链表长度决定是否将链表转为红黑树 4.判断键值对数量是否大于阈值，大于的话则进行扩容操作
 * 
 * 还说下注意点： 1.HashMap有个MIN_TREEIFY_CAPACITY代表：桶中结构转化为红黑树对应的table的最小大小。 当需要将解决
 * hash 冲突的链表转变为红黑树时，需要判断下此时数组容量，若是由于数组容量太小（小于 MIN_TREEIFY_CAPACITY ） 导致的 hash
 * 冲突太多，则不进行链表转变为红黑树操作，转为利用 resize() 函数对 hashMap 扩容。
 * 所以并不是桶子上有8位元素的时候它就能变成红黑树，它得同时满足我们的散列表容量大于64才行的
 * 
 * 2.请问HashMap在什么时候扩容？
 * 一定是当size达到总容量的0.75时会扩容吗？这个不一定，得看jdk的版本，1.8以上put操作时确实对是否扩容只有loadFactor这个因素
 * 在1.7的源码中的put操作时扩容的条件为“(size >= threshold) && (null !=
 * table[bucketIndex])”，也就是说还需要同时满足后面条件，
 * 那么bucketIndex又是什么呢？直译为“桶的下标”，即下一个存放Entry的桶的位置。简而言之， 仅当size >=
 * threshold且发生Hash值%(length-1)冲突（或修改已存在的值或）时，才会进行扩容。
 * 
 * 3.还有关于1.8和1.7的一些改动： 数据结构： JDK1.7使用数组+链表的数据结构，而1.8使用数组+链表+红黑树。
 * 如果插入key的hashcode相同，使用链表方式解决冲突，当链表长度达到8个（默认设置的阈值）时，
 * 调用treeifyBin函数，将链表转换为红黑树。红黑树的时间复杂度为O(log n)，即put/get最坏时间复杂度为O(log
 * n)。而使用链表的话，则是O(n) 数据存储机制： 发生hash冲突时，JDK1.7采用链地址法+头插法，而1.8采用链地址法+尾插法+红黑树。
 * 头插入法插入效率较高，但容易出现逆序且环形链表死循环问题，尾插法可避免此问题。
 * 
 * 4.为什么要用红黑树，而不用平衡二叉树？ 插入效率比平衡二叉树高，查询效率比普通二叉树高。所以选择性能相对折中的红黑树
 * `
 * 5. JDK1.7是基于数组+单链表实现（为什么不用双链表）
 * 首先，用链表是为了解决hash冲突。单链表能实现为什么要用双链表呢?(双链表需要更大的存储空间)
 * 
 * 6.再谈下和Hashtable的区别及多线程情况下使用什么：
 * 从存储结构和实现来讲基本上都是相同的。它和HashMap的最大的不同是它是线程安全的，另外它不允许key和value为null。
 * Hashtable是个过时的集合类，不建议在新代码中使用，不需要线程安全的场合可以用HashMap替换，需要线程安全的场合可以用ConcurrentHashMap替换
 * 还有一种就是 Map m = Collections.synchronizedMap(new HashMap(...));
 * 
 * 7.重写对象的Equals方法时，要重写hashCode方法，为什么？跟HashMap有什么关系？ equals与hashcode间的关系:
 * 
 * 如果两个对象相同（即用equals比较返回true），那么它们的hashCode值一定要相同；
 * 如果两个对象的hashCode相同，它们并不一定相同(即用equals比较返回false)
 * 
 * 因为在 HashMap 的链表结构中遍历判断的时候，特定情况下重写的 equals
 * 方法比较对象是否相等的业务逻辑比较复杂，循环下来更是影响查找效率。所以这里把 hashcode 的判断放在前面，只要 hashcode
 * 不相等就玩儿完，不用再去调用复杂的 equals 了。很多程度地提升 HashMap 的使用效率。
 * 
 * 所以重写 hashcode 方法是为了让我们能够正常使用 HashMap 等集合类，因为 HashMap 判断对象是否相等既要比较 hashcode
 * 又要使用 equals 比较。而这样的实现是为了提高 HashMap 的效率。
 * 
 * 8. 既然红黑树那么好，为啥hashmap不直接采用红黑树，而是当大于8个的时候才转换红黑树？ 因为红黑树需要进行左旋，右旋操作， 而单链表不需要。
 * 以下都是单链表与红黑树结构对比。 如果元素小于8个，查询成本高，新增成本低。 如果元素大于8个，查询成本低，新增成本高。
 * 至于为什么选数字8，是大佬折中衡量的结果-.-，就像loadFactor默认值0.75一样。
 * 
 * 9.其他 扩容后是原先容量的两倍
 * 
 * 底层数组的长度要求2的次方(即使不是2的次方也会经过tableSizeFor转为2的次方): 首先，capacity 为 2的整数次幂的话，计算桶的位置
 * hash值&(length-1) 就相当于对 length 取模，提升了计算效率； 其次，capacity 为 2 的整数次幂的话，为偶数，这样
 * capacity-1 为奇数，奇数的最后一位是 1， 这样便保证了 h&(capacity-1) 的最后一位可能为 0，也可能为
 * 1（这取决于h的值），即与后的结果可能为偶数，也可能为奇数，这样便可以保证散列的均匀性； 而如果 capacity 为奇数的话，很明显
 * capacity-1 为偶数，它的最后一位是 0，这样 h&(capacity-1) 的最后一位肯定为 0， 即只能为偶数，这样任何 hash
 * 值都只会被散列到数组的偶数下标位置上，这便浪费了近一半的空间。
 * 
 * 怎样通过key获得数组中的索引呢？i=(length - 1) & hash 类似于取余，但是效率高一些
 * 
 * 
 * 
 * 
 * 
 * 
 * Hash table based implementation of the <tt>Map</tt> interface. This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.（这里说的允许key和value为空） (The
 * <tt>HashMap</tt> class is roughly equivalent to <tt>Hashtable</tt>, except
 * that it is unsynchronized and permits nulls.) This class makes no guarantees
 * as to the order of the map; in particular, it does not guarantee that the
 * order will remain constant over time.
 * 上面一段主要讲了允许key和value为null，且说了几乎等同于Hashtable除了不同步和允许为null外
 * 然后还说了此类不保证映射的顺序，特别是它不保证该顺序恒久不变。(个人理解是rehash时会导致顺序变化)
 *
 * <p>
 * This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets. Iteration over collection
 * views requires time proportional to the "capacity" of the <tt>HashMap</tt>
 * instance (the number of buckets) plus its size (the number of key-value
 * mappings). Thus, it's very important not to set the initial capacity too high
 * (or the load factor too low) if iteration performance is important.
 * 此实现假定哈希函数将元素适当地分布在各桶之间，可为基本操作（get 和 put）提供稳定的性能。 迭代 collection 视图所需的时间与
 * HashMap 实例的“容量”（桶的数量）及其大小（键-值映射关系数）成比例。
 * 所以，如果迭代性能很重要，则不要将初始容量设置得太高（或将加载因子设置得太低）。
 *
 * <p>
 * An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>. The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created. The
 * <i>load factor</i> is a measure of how full the hash table is allowed to get
 * before its capacity is automatically increased. When the number of entries in
 * the hash table exceeds the product of the load factor and the current
 * capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets. HashMap 的实例有两个参数影响其性能：初始容量 和加载因子。容量 是哈希表中桶的数量，
 * 初始容量只是哈希表在创建时的容量。加载因子 是哈希表在其容量自动增加之前可以达到多满的一种尺度。 当哈希表中的条目数超出了加载因子与当前容量的乘积时，
 * 则要对该哈希表进行 rehash 操作（即重建内部数据结构），从而哈希表将具有大约两倍的桶数。
 *
 * <p>
 * As a general rule, the default load factor (.75) offers a good tradeoff
 * between time and space costs. Higher values decrease the space overhead but
 * increase the lookup cost (reflected in most of the operations of the
 * <tt>HashMap</tt> class, including <tt>get</tt> and <tt>put</tt>). The
 * expected number of entries in the map and its load factor should be taken
 * into account when setting its initial capacity, so as to minimize the number
 * of rehash operations. If the initial capacity is greater than the maximum
 * number of entries divided by the load factor, no rehash operations will ever
 * occur. 默认的装载因子为0.75，过高的装载因子虽然会降低空间消耗，但是会增加查找的时间消耗
 * 在设置初始化参数时，应该考虑好装载因子和实体数目，以便最大限度地减少 rehash 操作次数。 如果初始容量大于最大条目数除以加载因子，则不会发生
 * rehash 操作。
 *
 * <p>
 * If many mappings are to be stored in a <tt>HashMap</tt> instance, creating it
 * with a sufficiently large capacity will allow the mappings to be stored more
 * efficiently than letting it perform automatic rehashing as needed to grow the
 * table. Note that using many keys with the same {@code hashCode()} is a sure
 * way to slow down performance of any hash table. To ameliorate impact, when
 * keys are {@link Comparable}, this class may use comparison order among keys
 * to help break ties. 如果许多映射要存储在HashMap中，那么创建一个足够大的容量将让映射被更有效地存储，而不是让它执行再hash。
 * 
 *
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access a hash map concurrently, and at least one of the
 * threads modifies the map structurally, it <i>must</i> be synchronized
 * externally. (A structural modification is any operation that adds or deletes
 * one or more mappings; merely changing the value associated with a key that an
 * instance already contains is not a structural modification.) This is
 * typically accomplished by synchronizing on some object that naturally
 * encapsulates the map. 这里强调的是不同步
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap} method. This
 * is best done at creation time, to prevent accidental unsynchronized access to
 * the map:
 * 
 * <pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));
 * </pre>
 * 
 * 这里强调的是怎样使其成为一个同步的容器
 *
 *
 * 下面的基本在介绍迭代器和fail-fast
 * <p>
 * The iterators returned by all of this class's "collection view methods" are
 * <i>fail-fast</i>: if the map is structurally modified at any time after the
 * iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a
 * {@link ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
 * is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification. Fail-fast iterators throw
 * <tt>ConcurrentModificationException</tt> on a best-effort basis. Therefore,
 * it would be wrong to write a program that depended on this exception for its
 * correctness: <i>the fail-fast behavior of iterators should be used only to
 * detect bugs.</i>
 *
 * <p>
 * This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html"> Java
 * Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author Doug Lea
 * @author Josh Bloch
 * @author Arthur van Hoff
 * @author Neal Gafter
 * @see Object#hashCode()
 * @see Collection
 * @see Map
 * @see TreeMap
 * @see Hashtable
 * @since 1.2
 */
public class HashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Cloneable, Serializable {

	private static final long serialVersionUID = 362498820763181265L;

	/**
	 * The default initial capacity - MUST be a power of two.、 必须是 2 的整数次方
	 * 默认初始化容量，<<代表向左四位，所以1变为16
	 * 
	 * 至于为什么是2的次方，后总结https://blog.csdn.net/oqqYeYi/article/details/39831029
	 * 或者https://www.ibm.com/developerworks/cn/java/j-lo-hash/?open&cm_mmc=6505-_-n-_-vrm_newsletter-_-10104_142587&cmibm_em=dm:0:10631101
	 * 
	 * HashMap 底层数组的长度总是 2 的 n 次方，这一点可参看后面关于 HashMap 构造器的介绍。 当 length 总是 2 的倍数时，h &
	 * (length-1)将是一个非常巧妙的设计：假设 h=5,length=16, 那么 h & length - 1 将得到 5； 如果
	 * h=6,length=16, 那么 h & length - 1 将得到 6 ……如果 h=15,length=16, 那么 h & length - 1
	 * 将得到 15； 但是当 h=16 时 , length=16 时，那么 h & length - 1 将得到 0 了；当 h=17 时 ,
	 * length=16 时， 那么 h & length - 1 将得到 1 了……这样保证计算得到的索引值总是位于 table 数组的索引之内。
	 */
	static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

	/**
	 * The maximum capacity, used if a higher value is implicitly specified by
	 * either of the constructors with arguments. MUST be a power of two <= 1<<30.
	 * 最大容量（必须是2的幂且小于2的30次方，传入容量过大将被这个值替换）
	 * 
	 * 这里相当于2的30次方，不用Math的方法进行运算是因为位运算效率高点 还有明明int是4个字节即32位，不应该是2的31次方吗？
	 * 原因在于第一位是用作符号位的。用来表示正负之分（0为正，1为负）
	 * 
	 */
	static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The load factor used when none specified in constructor. 默认装载因子
	 * 结合时间和空间效率考虑得到的一个折中方案
	 */
	static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * The bin(容器) count threshold for using a tree rather than list for a bin. Bins
	 * are converted to trees when adding an element to a bin with at least this
	 * many nodes. The value must be greater than 2 and should be at least 8 to mesh
	 * with assumptions in tree removal about conversion back to plain bins upon
	 * shrinkage.
	 * 
	 * 这个常量名的意思根据英文名看出是链表转为红黑树的门槛 当同一个桶中超过8个元素即进行转换
	 * 
	 * 链表转成树的阈值，当桶中链表长度大于8时转成树 threshold = capacity * loadFactor
	 * 
	 */
	static final int TREEIFY_THRESHOLD = 8;

	/**
	 * The bin count threshold for untreeifying a (split) bin during a resize
	 * operation. Should be less than TREEIFY_THRESHOLD, and at most 6 to mesh with
	 * shrinkage detection under removal.
	 * 
	 * 这个和上一个常量意思相反，即树转为list的界限。 进行resize操作时，当桶中数量小于6转为链表
	 */
	static final int UNTREEIFY_THRESHOLD = 6;

	/**
	 * The smallest table capacity for which bins may be treeified. (Otherwise the
	 * table is resized if too many nodes in a bin.) Should be at least 4 *
	 * TREEIFY_THRESHOLD to avoid conflicts between resizing and treeification
	 * thresholds.
	 * 
	 * 最小树化容量（桶的数目即数组的长度）
	 * 
	 * 
	 * 桶中结构转化为红黑树对应的table的最小大小 当需要将解决 hash 冲突的链表转变为红黑树时， 需要判断下此时数组容量， 若是由于数组容量太小（小于
	 * MIN_TREEIFY_CAPACITY ） 导致的 hash 冲突太多，则不进行链表转变为红黑树操作， 转为利用 resize() 函数对
	 * hashMap 扩容
	 */
	static final int MIN_TREEIFY_CAPACITY = 64;

	/**
	 * Basic hash bin node, used for most entries. (See below for TreeNode subclass,
	 * and in LinkedHashMap for its Entry subclass.)
	 */
	static class Node<K, V> implements Map.Entry<K, V> {
		// hash值代表位置
		final int hash;
		final K key;
		V value;
		// 下一个节点的指针
		Node<K, V> next;

		Node(int hash, K key, V value, Node<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}

		@Override
		public final K getKey() {
			return key;
		}

		@Override
		public final V getValue() {
			return value;
		}

		@Override
		public final String toString() {
			return key + "=" + value;
		}

		@Override
		public final int hashCode() {
			// 这个是按位异或
			// Objects中的hashCode方法会进行null判断,为null时hashCode直接为0
			// 不为null才去执行Object中的本地hashCode方法
			// 由 Object 类定义的 hashCode 方法确实会针对不同的对象返回不同的整数。
			// （这一般是通过将该对象的内部地址转换成一个整数来实现的，但是 Java 编程语言不需要这种实现技巧。）
			return Objects.hashCode(key) ^ Objects.hashCode(value);
		}

		/**
		 * 设置成新值返回旧值
		 */
		@Override
		public final V setValue(V newValue) {
			V oldValue = value;
			value = newValue;
			return oldValue;
		}

		@Override
		public final boolean equals(Object o) {
			// 先判断引用是否一致。不一致再看key和value的hashCode值
			if (o == this) {
				return true;
			}
			// 先判断类型
			if (o instanceof Map.Entry) {
				// 这里不知道为什么还要使用一个临时变量
				Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

				// 必须key和value的hashCode值同时相同才返回true
				if (Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue())) {
					return true;
				}
			}
			return false;
		}
	}

	/* ---------------- Static utilities -------------- */

	/**
	 * Computes key.hashCode() and spreads (XORs) higher bits of hash to lower.
	 * Because the table uses power-of-two masking, sets of hashes that vary only in
	 * bits above the current mask will always collide. (Among known examples are
	 * sets of Float keys holding consecutive whole numbers in small tables.) So we
	 * apply a transform that spreads the impact of higher bits downward. There is a
	 * tradeoff between speed, utility, and quality of bit-spreading. Because many
	 * common sets of hashes are already reasonably distributed (so don't benefit
	 * from spreading), and because we use trees to handle large sets of collisions
	 * in bins, we just XOR some shifted bits in the cheapest possible way to reduce
	 * systematic lossage, as well as to incorporate impact of the highest bits that
	 * would otherwise never be used in index calculations because of table bounds.
	 */
	static final int hash(Object key) {
		int h;
		// h >>> 16是用来取出h的高16 由于int只有32位，无符号向右移16位就代表取最先的16位即高16位
		// 如0000 0100 1011 0011 1101 1111 1110 0001 -> 0000 0000 0000 0000 0000 0100
		// 1011 0011
		// 那么为什么(h = key.hashCode())要与(h >>> 16)进行亦或呢？
		// 将键的hashcode的高16位异或低16位(高位运算)，这样即使数组table的length比较小的时候，也能保证高低Bit都参与到Hash的计算中，同时不会有太大的开销；
		// 另一个解释(为什么要先高16位异或低16位再取余运算)：
		// 当我们的length为16(即长度比较小)的时候，哈希码(字符串“abcabcabcabcabc”的key对应的哈希码)对(16-1)&操作，对于多个key生成的hashCode，只要哈希码的后4位为0，
		// 不论不论高位怎么变化，最终的结果均为0。也就是说，如果只取后四位(低位)的话，这个时候产生"碰撞"的几率就非常大(当然&运算中产生碰撞的原因很多，
		// 这里只是举个例子)。为了解决低位与操作碰撞的问题，于是便有了第二步中高16位异或低16位的“扰动函数”。
		// 右移16位，自己的高半区和低半区异或，就是为了混合原始哈希码的高位和低位，以此来加大低位随机性。减少了碰撞冲突的可能性！
		return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
	}

	/**
	 * Returns x's Class if it is of the form "class C implements Comparable<C>",
	 * else null.
	 */
	static Class<?> comparableClassFor(Object x) {
		if (x instanceof Comparable) {
			Class<?> c;
			Type[] ts, as;
			Type t;
			ParameterizedType p;
			if ((c = x.getClass()) == String.class) {
				return c;
			}
			if ((ts = c.getGenericInterfaces()) != null) {
				for (int i = 0; i < ts.length; ++i) {
					if (((t = ts[i]) instanceof ParameterizedType)
							&& ((p = (ParameterizedType) t).getRawType() == Comparable.class)
							&& (as = p.getActualTypeArguments()) != null && as.length == 1 && as[0] == c) {
						return c;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns k.compareTo(x) if x matches kc (k's screened comparable class), else
	 * 0.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" }) // for cast to Comparable
	static int compareComparables(Class<?> kc, Object k, Object x) {
		return (x == null || x.getClass() != kc ? 0 : ((Comparable) k).compareTo(x));
	}

	/**
	 * Returns a power of two size for the given target capacity. 根据指定的容量计算出合适的阈值
	 * 经过若干次无符号右移、求异运算，得出最接近指定参数 cap 的 2 的 N 次方容量。假如你传入的是 5，返回的初始容量为 8 。
	 * 刚刚自己测试的结果是返回大于等于当前值的2的N次方
	 */
	static final int tableSizeFor(int cap) {
		int n = cap - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
	}

	/* ---------------- Fields -------------- */

	/**
	 * The table, initialized on first use, and resized as necessary. When
	 * allocated, length is always a power of two. (We also tolerate length zero in
	 * some operations to allow bootstrapping mechanics that are currently not
	 * needed.)
	 * 
	 * 哈希表中的链表数组，代表桶的集合，每一个桶又是一个链表
	 */
	transient Node<K, V>[] table;

	/**
	 * Holds cached entrySet(). Note that AbstractMap fields are used for keySet()
	 * and values(). 缓存的 键值对集合（另外两个视图：keySet 和 values 是在 AbstractMap 中声明的）
	 */
	transient Set<Map.Entry<K, V>> entrySet;

	/**
	 * The number of key-value mappings contained in this map.
	 * 键值对的数量(不是数组中包含得键值对，而是指得所有得)
	 */
	transient int size;

	/**
	 * The number of times this HashMap has been structurally modified Structural
	 * modifications are those that change the number of mappings in the HashMap or
	 * otherwise modify its internal structure (e.g., rehash). This field is used to
	 * make iterators on Collection-views of the HashMap fail-fast. (See
	 * ConcurrentModificationException).
	 * 
	 * 当前 HashMap 修改的次数，这个变量用来保证 fail-fast 机制
	 */
	transient int modCount;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 *
	 * 阈值 下次需要扩容时的值，等于 容量*加载因子 threshold = capacity * load factor
	 * 
	 * @serial
	 */
	// (The javadoc description is true upon serialization.
	// Additionally, if the table array has not been allocated, this
	// field holds the initial array capacity, or zero signifying
	// DEFAULT_INITIAL_CAPACITY.)
	int threshold;

	/**
	 * The load factor for the hash table. 加载因子，默认得加载因子也是在构造函数里要赋值给这个得
	 * 
	 * @serial
	 */
	final float loadFactor;

	/* ---------------- Public operations -------------- */

	/**
	 * Constructs an empty <tt>HashMap</tt> with the specified initial capacity and
	 * load factor.
	 * 
	 * 构建一个带有指定初始化容量和加载因子的空的HashMap
	 *
	 * @param initialCapacity the initial capacity
	 * @param loadFactor      the load factor
	 * @throws IllegalArgumentException if the initial capacity is negative or the
	 *                                  load factor is nonpositive
	 */
	public HashMap(int initialCapacity, float loadFactor) {
		// 初始化容量小于0会抛出异常
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}

		// 如果传的容量值太过高大于MAXIMUM_CAPACITY，则令初始化参数为最大值
		if (initialCapacity > MAXIMUM_CAPACITY) {
			initialCapacity = MAXIMUM_CAPACITY;
		}
		// 如果加载因子小于等于0或是加载因子为nan(Not-a-Number)这种就会抛出异常
		// JDK中float和double有一个方法isNan,该方法用于描述非法的float,经过多次运算float值可能会出现非法情况，如除数为0.0
		// 在Float中NaN实际上是引用类型，而不是值类型，每一个NaN都是不同的对象(即nan！=nan)。
		if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
		}
		// 如果没有异常则赋值
		this.loadFactor = loadFactor;
		// 根据初始化容量设置一个合适的阈值
		this.threshold = tableSizeFor(initialCapacity);

		// 不过这里好像没有成员变量Capacity还有疑问的是threshold不应该是initialCapacity*loadFactor吗
		// 在1.7的源码是有一个局部变量capacity的，也是用来计算threshold并初始化table
		/*
		 * // 计算出大于 initialCapacity 的最小的 2 的 n 次方值。 int capacity = 1; //
		 * 这个就类似于jdk1.8的tableSizeFor方法 while (capacity < initialCapacity) capacity <<=
		 * 1; this.loadFactor = loadFactor; // 设置容量极限等于容量 * 负载因子 threshold =
		 * (int)(capacity * loadFactor); // 初始化 table 数组 table = new Entry[capacity];
		 * init();
		 */

		// 然后关于1.8中为啥是将2的整数幂的数赋给threshold？
		// threshold这个成员变量是阈值，决定了是否要将散列表再散列。它的值应该是：capacity * load factor才对的。
		// 原因：其实这里仅仅是一个初始化，当创建哈希表的时候，它会重新赋值的即第一次的resize()中

	}

	/**
	 * Constructs an empty <tt>HashMap</tt> with the specified initial capacity and
	 * the default load factor (0.75).
	 * 
	 * 构建一个带有指定初始化容量和默认加载因子0.75的空的HashMap
	 *
	 * @param initialCapacity the initial capacity.
	 * @throws IllegalArgumentException if the initial capacity is negative.
	 */
	public HashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs an empty <tt>HashMap</tt> with the default initial capacity (16)
	 * and the default load factor (0.75). 构建一个带有指定默认容量16和默认加载因子0.75的空的HashMap
	 */
	public HashMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
	}

	/**
	 * Constructs a new <tt>HashMap</tt> with the same mappings as the specified
	 * <tt>Map</tt>. The <tt>HashMap</tt> is created with default load factor (0.75)
	 * and an initial capacity sufficient to hold the mappings in the specified
	 * <tt>Map</tt>.
	 * 
	 * 创建一个内容为参数 m 的内容的哈希表
	 *
	 * @param m the map whose mappings are to be placed in this map
	 * @throws NullPointerException if the specified map is null
	 */
	public HashMap(Map<? extends K, ? extends V> m) {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		putMapEntries(m, false);
	}

	/**
	 * Implements Map.putAll and Map constructor
	 *
	 * @param m     the map
	 * @param evict false when initially constructing this map, else true (relayed
	 *              to method afterNodeInsertion). evict当初始化map时为false，否则为true
	 */
	final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
		int s = m.size();
		if (s > 0) {
			// 数组还是空，初始化参数
			if (table == null) { // pre-size
				float ft = ((float) s / loadFactor) + 1.0F;
				int t = ((ft < (float) MAXIMUM_CAPACITY) ? (int) ft : MAXIMUM_CAPACITY);
				if (t > threshold) {
					threshold = tableSizeFor(t);
				}
			}
			// 数组不为空，超过阈值就扩容
			else if (s > threshold) {
				resize();
			}
			for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
				K key = e.getKey();
				V value = e.getValue();
				// 先经过 hash() 计算位置，然后复制指定 map 的内容
				putVal(hash(key), key, value, false, evict);
			}
		}
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	@Override
	public int size() {
		return size;
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings
	 */
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns the value to which the specified key is mapped, or {@code null} if
	 * this map contains no mapping for the key.
	 *
	 * <p>
	 * More formally, if this map contains a mapping from a key {@code k} to a value
	 * {@code v} such that {@code (key==null ? k==null :
	 * key.equals(k))}, then this method returns {@code v}; otherwise it returns
	 * {@code null}. (There can be at most one such mapping.)
	 *
	 * <p>
	 * A return value of {@code null} does not <i>necessarily</i> indicate that the
	 * map contains no mapping for the key; it's also possible that the map
	 * explicitly maps the key to {@code null}. The {@link #containsKey containsKey}
	 * operation may be used to distinguish these two cases.
	 *
	 * @see #put(Object, Object)
	 */
	@Override
	public V get(Object key) {
		Node<K, V> e;
		// 根据hash(key)计算出位置，再调用getNode找到节点
		return (e = getNode(hash(key), key)) == null ? null : e.value;
	}

	/**
	 * Implements Map.get and related methods
	 * 
	 * 实现map的get及其相关方法
	 *
	 * @param hash hash for key
	 * @param key  the key
	 * @return the node, or null if none
	 */
	final Node<K, V> getNode(int hash, Object key) {
		Node<K, V>[] tab;
		Node<K, V> first, e;
		int n;
		K k;
		if ((tab = table) != null && (n = tab.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
			// first是桶中的元素，如果只有这一个节点(不是链表)就可以直接返回了
			// 或者是链表但是首节点与传来的key一致(同一条链表上的节点主要是key的hash值相同)
			if (first.hash == hash && // always check first node
					((k = first.key) == key || (key != null && key.equals(k)))) {
				return first;
			}
			if ((e = first.next) != null) { // 这里就需要在链表或红黑树了就麻烦点
				if (first instanceof TreeNode) {
					return ((TreeNode<K, V>) first).getTreeNode(hash, key);
				}
				do { // 链表 遍历找到equal相等的，先使用hash判断排除大部分相比于直接使用eauals判断提高一些效率
					if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
						return e;
					}
				} while ((e = e.next) != null);
			}
		}
		return null;
	}

	/**
	 * Returns <tt>true</tt> if this map contains a mapping for the specified key.
	 * 相当于使用get方法，看是否取得到
	 * 
	 * @param key The key whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map contains a mapping for the specified key.
	 */
	@Override
	public boolean containsKey(Object key) {
		return getNode(hash(key), key) != null;
	}

	/**
	 * Associates the specified value with the specified key in this map. If the map
	 * previously contained a mapping for the key, the old value is replaced.
	 *
	 * @param key   key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if
	 *         there was no mapping for <tt>key</tt>. (A <tt>null</tt> return can
	 *         also indicate that the map previously associated <tt>null</tt> with
	 *         <tt>key</tt>.)
	 */
	@Override
	public V put(K key, V value) {
		return putVal(hash(key), key, value, false, true);
	}

	/**
	 * Implements Map.put and related methods 实现map的put和相关的方法
	 * 
	 * 将值存入map的实际方法
	 *
	 * @param hash         hash for key // 存储的位置
	 * @param key          the key
	 * @param value        the value to put
	 * @param onlyIfAbsent if true, don't change existing value // 如果为true,则不改变存在的值
	 * @param evict        if false, the table is in creation mode. //
	 *                     这个之前说过，当为true代表是在用另一个map进行初始化(最后一种构造方法)
	 *                     除了这种情况，其他时候evict都为false
	 * @return previous value, or null if none 如果之前存在返回之前的值，不存在则返回null
	 */
	final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
		Node<K, V>[] tab;
		Node<K, V> p;
		int n, i;

		// 当散列表为空时，调用resize初始化散列表
		if ((tab = table) == null || (n = tab.length) == 0) {
			n = (tab = resize()).length;
		}

		// 在jdk1.7中有indexFor(int h, int length)方法。jdk1.8里没有，但原理没变。]
		// 1.8中用tab[(n - 1) & hash]代替但原理一样。

		// “模”运算的消耗还是比较大的所以使用h & (length-1) 其实就是取余
		// 这个方法非常巧妙，它通过 h & (table.length -1) 来得到该对象的保存位，而HashMap底层数组的长度总是 2 的 n
		// 次方，这是HashMap在速度上的优化
		// 当length总是 2 的n次方时，h& (length-1)运算等价于对length取模，也就是h%length，但是&比%具有更高的效率。
		// 取模(取余)运算转化成位运算公式:a%(2^n) 等价于 a&(2^n-1),而&操作比%操作具有更高的效率。

		// 如果要插入的位置没有元素，新建个节点并放进去即没有发生碰撞时，直接添加元素到散列表中
		if ((p = tab[i = (n - 1) & hash]) == null) {
			tab[i] = newNode(hash, key, value, null);
		} else { // 发生了碰撞
			Node<K, V> e;
			K k;

			// 如果要插入的元素，桶的key和hash都相等(即与首节点匹配)，记录下来
			if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
				e = p;
			} else if (p instanceof TreeNode) {
				// 放入树中
				e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
			} else {
				// 链表结构，找到了key映射的节点，就记录这个节点，退出循环
				// 如果没有找到，在链表尾部插入节点。插入后如果发现临界值大于TREEIFY_THRESHOLD
				// ，转为红黑树

				// 对链表进行遍历，并统计链表长度
				for (int binCount = 0;; ++binCount) {

					// 到达链表底部
					if ((e = p.next) == null) {
						// 在尾部插入新节点
						p.next = newNode(hash, key, value, null);
						// 如果节点数量达到阈值，则转化为红黑树
						if (binCount >= TREEIFY_THRESHOLD - 1) {
							treeifyBin(tab, hash);
						}
						break;
					}
					// 判断链表中节点的key值与插入的元素的key是否相等
					if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
						break;
					}
					p = e;
				}
			}
			// 判断要插入的键值对是否存在HashMap中
			// 新值覆盖旧值，返回旧值
			if (e != null) { // existing mapping for key
				V oldValue = e.value;
				// onlyIfAbsent表示是否仅在lodValue为null情况下更新键值对的值
				if (!onlyIfAbsent || oldValue == null) {
					e.value = value;
				}
				afterNodeAccess(e);
				return oldValue;
			}
		}
		++modCount;
		if (++size > threshold) {
			resize();
		}
		afterNodeInsertion(evict);
		return null;
	}

	/**
	 * Initializes or doubles table size. If null, allocates in accord with initial
	 * capacity target held in field threshold. Otherwise, because we are using
	 * power-of-two expansion, the elements from each bin must either stay at same
	 * index, or move with a power of two offset in the new table.
	 * 初始化时需要调用，当散列表元素大于阈值threshold时也要调用
	 *
	 * @return the table
	 */
	final Node<K, V>[] resize() {
		Node<K, V>[] oldTab = table;
		int oldCap = (oldTab == null) ? 0 : oldTab.length;
		int oldThr = threshold;
		int newCap, newThr = 0;
		// 这里就代表不是第一次扩容
		if (oldCap > 0) {
			// 如果旧的容量比最大容量还要大，那不能散列，返回旧的散列表
			if (oldCap >= MAXIMUM_CAPACITY) {
				threshold = Integer.MAX_VALUE;
				return oldTab;
			}
			// 新的阈值是旧的两倍 double threshold 且容量也为旧的两倍
			else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY) {
				newThr = oldThr << 1; // double threshold
			}
		}
		// 如果旧容量<=0 而旧阈值>0，数组的新容量设置为老数组扩容的阈值
		// 这个应该使用的是带参数的构造函数(这个已经将阈值弄为2的次方了(使用tableSizeFor方法)，所以不用担心容量不为2的次方了)
		else if (oldThr > 0) {
			newCap = oldThr;
		} else { // zero initial threshold signifies using defaults
			newCap = DEFAULT_INITIAL_CAPACITY;
			newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
		}
		if (newThr == 0) {
			float ft = (float) newCap * loadFactor; // 这里就是对使用的带参数的构造函数的后续处理
			newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ? (int) ft : Integer.MAX_VALUE);
		}
		// 这里回答了为什么阈值之前不是容量和加载因子的乘积了，在resize初始化table时会重新赋值
		threshold = newThr;
		@SuppressWarnings({ "unchecked" })
		Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
		table = newTab;

		// 将旧散列表复制到新散列表中
		if (oldTab != null) {
			for (int j = 0; j < oldCap; ++j) {
				Node<K, V> e;
				if ((e = oldTab[j]) != null) {
					oldTab[j] = null;
					if (e.next == null) {
						newTab[e.hash & (newCap - 1)] = e;
					} else if (e instanceof TreeNode) {
						// 重新映射时，需要对红黑树进行拆分
						((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
					} else { // preserve order 链表
						Node<K, V> loHead = null, loTail = null;
						Node<K, V> hiHead = null, hiTail = null;
						Node<K, V> next;
						// 遍历链表，并将链表节点按原顺序进行分组
						do {
							next = e.next;
							if ((e.hash & oldCap) == 0) {
								if (loTail == null) {
									loHead = e;
								} else {
									loTail.next = e;
								}
								loTail = e;
							} else {
								if (hiTail == null) {
									hiHead = e;
								} else {
									hiTail.next = e;
								}
								hiTail = e;
							}
						} while ((e = next) != null);
						// 将分组后的链表映射到新桶中
						if (loTail != null) {
							loTail.next = null;
							newTab[j] = loHead;
						}
						if (hiTail != null) {
							hiTail.next = null;
							newTab[j + oldCap] = hiHead;
						}
					}
				}
			}
		}
		return newTab;
	}

	/**
	 * Replaces all linked nodes in bin at index for given hash unless table is too
	 * small, in which case resizes instead.
	 */
	final void treeifyBin(Node<K, V>[] tab, int hash) {
		int n, index;
		Node<K, V> e;
		if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY) {
			resize();
		} else if ((e = tab[index = (n - 1) & hash]) != null) {
			TreeNode<K, V> hd = null, tl = null;
			do {
				TreeNode<K, V> p = replacementTreeNode(e, null);
				if (tl == null) {
					hd = p;
				} else {
					p.prev = tl;
					tl.next = p;
				}
				tl = p;
			} while ((e = e.next) != null);
			if ((tab[index] = hd) != null) {
				hd.treeify(tab);
			}
		}
	}

	/**
	 * Copies all of the mappings from the specified map to this map. These mappings
	 * will replace any mappings that this map had for any of the keys currently in
	 * the specified map.
	 *
	 * @param m mappings to be stored in this map
	 * @throws NullPointerException if the specified map is null
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		putMapEntries(m, true);
	}

	/**
	 * Removes the mapping for the specified key from this map if present.
	 * 根据键值删除键值对，如果哈希表中存在该键，那么返回键对应的值，否则返回null
	 * 
	 * @param key key whose mapping is to be removed from the map
	 * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if
	 *         there was no mapping for <tt>key</tt>. (A <tt>null</tt> return can
	 *         also indicate that the map previously associated <tt>null</tt> with
	 *         <tt>key</tt>.)
	 */
	@Override
	public V remove(Object key) {
		Node<K, V> e;
		// 同样也是找到先用key算出hash再在removeNode中使用(n - 1) & hash进行其他运算
		return (e = removeNode(hash(key), key, null, false, true)) == null ? null : e.value;
	}

	/**
	 * Implements Map.remove and related methods
	 *
	 * @param hash       hash for key
	 * @param key        the key
	 * @param value      the value to match if matchValue, else ignored 值如果不匹配就忽略
	 * @param matchValue if true only remove if value is equal 如果值相等是这个为true
	 * @param movable    if false do not move other nodes while removing
	 * @return the node, or null if none
	 */
	final Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
		Node<K, V>[] tab;
		Node<K, V> p;
		int n, index;
		// 桶不为空，映射的hash值也存在
		if ((tab = table) != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null) {
			Node<K, V> node = null, e;
			K k;
			V v;
			// 在桶的首位就找到了对应的元素了，记录下来
			if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k)))) {
				node = p;
			} else if ((e = p.next) != null) {
				if (p instanceof TreeNode) {
					node = ((TreeNode<K, V>) p).getTreeNode(hash, key);
				} else { // 头节点不匹配，且头节点为Node
					// 遍历链表，一旦匹配，跳出循环
					do {
						if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
							node = e;
							break;
						}
						p = e;
					} while ((e = e.next) != null);
				}
			}
			// 找到了对应的节点，并且要么没传value要么value也能匹配，那么就分三种情况去删除了
			// 1.链表 2.红黑树 3.在桶的首位
			if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
				// 如果待删除节点是TreeNode,使用红黑树的方法
				if (node instanceof TreeNode) {
					((TreeNode<K, V>) node).removeTreeNode(this, tab, movable);
				} else if (node == p) {
					tab[index] = node.next;
					// 在链表遍历过程中，p代表node节点的前驱节点
				} else {
					p.next = node.next;
				}
				++modCount;
				--size;
				// 子类实现
				afterNodeRemoval(node);
				return node;
			}
		}
		return null;
	}

	/**
	 * Removes all of the mappings from this map. The map will be empty after this
	 * call returns.
	 */
	@Override
	public void clear() {
		Node<K, V>[] tab;
		modCount++;
		if ((tab = table) != null && size > 0) {
			size = 0;
			for (int i = 0; i < tab.length; ++i) {
				tab[i] = null;
			}
		}
	}

	/**
	 * Returns <tt>true</tt> if this map maps one or more keys to the specified
	 * value.
	 *
	 * @param value value whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map maps one or more keys to the specified
	 *         value
	 */
	@Override
	public boolean containsValue(Object value) {
		Node<K, V>[] tab;
		V v;
		if ((tab = table) != null && size > 0) {
			for (int i = 0; i < tab.length; ++i) {
				for (Node<K, V> e = tab[i]; e != null; e = e.next) {
					if ((v = e.value) == value || (value != null && value.equals(v))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map. The set is
	 * backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa. If the map is modified while an iteration over the set is in
	 * progress (except through the iterator's own <tt>remove</tt> operation), the
	 * results of the iteration are undefined. The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
	 * <tt>retainAll</tt>, and <tt>clear</tt> operations. It does not support the
	 * <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public Set<K> keySet() {
		Set<K> ks;
		return (ks = keySet) == null ? (keySet = new KeySet()) : ks;
	}

	final class KeySet extends AbstractSet<K> {
		@Override
		public final int size() {
			return size;
		}

		@Override
		public final void clear() {
			HashMap.this.clear();
		}

		@Override
		public final Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public final boolean contains(Object o) {
			return containsKey(o);
		}

		@Override
		public final boolean remove(Object key) {
			return removeNode(hash(key), key, null, false, true) != null;
		}

		@Override
		public final Spliterator<K> spliterator() {
			return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
		}

		@Override
		public final void forEach(Consumer<? super K> action) {
			Node<K, V>[] tab;
			if (action == null) {
				throw new NullPointerException();
			}
			if (size > 0 && (tab = table) != null) {
				int mc = modCount;
				for (int i = 0; i < tab.length; ++i) {
					for (Node<K, V> e = tab[i]; e != null; e = e.next) {
						action.accept(e.key);
					}
				}
				if (modCount != mc) {
					throw new ConcurrentModificationException();
				}
			}
		}
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map. The
	 * collection is backed by the map, so changes to the map are reflected in the
	 * collection, and vice-versa. If the map is modified while an iteration over
	 * the collection is in progress (except through the iterator's own
	 * <tt>remove</tt> operation), the results of the iteration are undefined. The
	 * collection supports element removal, which removes the corresponding mapping
	 * from the map, via the <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations. It does
	 * not support the <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a view of the values contained in this map
	 */
	@Override
	public Collection<V> values() {
		Collection<V> vs;
		return (vs = values) == null ? (values = new Values()) : vs;
	}

	final class Values extends AbstractCollection<V> {
		@Override
		public final int size() {
			return size;
		}

		@Override
		public final void clear() {
			HashMap.this.clear();
		}

		@Override
		public final Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public final boolean contains(Object o) {
			return containsValue(o);
		}

		@Override
		public final Spliterator<V> spliterator() {
			return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
		}

		@Override
		public final void forEach(Consumer<? super V> action) {
			Node<K, V>[] tab;
			if (action == null) {
				throw new NullPointerException();
			}
			if (size > 0 && (tab = table) != null) {
				int mc = modCount;
				for (int i = 0; i < tab.length; ++i) {
					for (Node<K, V> e = tab[i]; e != null; e = e.next) {
						action.accept(e.value);
					}
				}
				if (modCount != mc) {
					throw new ConcurrentModificationException();
				}
			}
		}
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map. The set is
	 * backed by the map, so changes to the map are reflected in the set, and
	 * vice-versa. If the map is modified while an iteration over the set is in
	 * progress (except through the iterator's own <tt>remove</tt> operation, or
	 * through the <tt>setValue</tt> operation on a map entry returned by the
	 * iterator) the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding mapping from the map, via
	 * the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
	 * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not support the
	 * <tt>add</tt> or <tt>addAll</tt> operations.
	 *
	 * @return a set view of the mappings contained in this map
	 */
	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> es;
		return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
	}

	final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public final int size() {
			return size;
		}

		@Override
		public final void clear() {
			HashMap.this.clear();
		}

		@Override
		public final Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public final boolean contains(Object o) {
			if (!(o instanceof Map.Entry)) {
				return false;
			}
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			Object key = e.getKey();
			Node<K, V> candidate = getNode(hash(key), key);
			return candidate != null && candidate.equals(e);
		}

		@Override
		public final boolean remove(Object o) {
			if (o instanceof Map.Entry) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
				Object key = e.getKey();
				Object value = e.getValue();
				return removeNode(hash(key), key, value, true, true) != null;
			}
			return false;
		}

		@Override
		public final Spliterator<Map.Entry<K, V>> spliterator() {
			return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
		}

		@Override
		public final void forEach(Consumer<? super Map.Entry<K, V>> action) {
			Node<K, V>[] tab;
			if (action == null) {
				throw new NullPointerException();
			}
			if (size > 0 && (tab = table) != null) {
				int mc = modCount;
				for (int i = 0; i < tab.length; ++i) {
					for (Node<K, V> e = tab[i]; e != null; e = e.next) {
						action.accept(e);
					}
				}
				if (modCount != mc) {
					throw new ConcurrentModificationException();
				}
			}
		}
	}

	// Overrides of JDK8 Map extension methods

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		Node<K, V> e;
		return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
	}

	@Override
	public V putIfAbsent(K key, V value) {
		return putVal(hash(key), key, value, true, true);
	}

	@Override
	public boolean remove(Object key, Object value) {
		return removeNode(hash(key), key, value, true, true) != null;
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		Node<K, V> e;
		V v;
		if ((e = getNode(hash(key), key)) != null && ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
			e.value = newValue;
			afterNodeAccess(e);
			return true;
		}
		return false;
	}

	@Override
	public V replace(K key, V value) {
		Node<K, V> e;
		if ((e = getNode(hash(key), key)) != null) {
			V oldValue = e.value;
			e.value = value;
			afterNodeAccess(e);
			return oldValue;
		}
		return null;
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		if (mappingFunction == null) {
			throw new NullPointerException();
		}
		int hash = hash(key);
		Node<K, V>[] tab;
		Node<K, V> first;
		int n, i;
		int binCount = 0;
		TreeNode<K, V> t = null;
		Node<K, V> old = null;
		if (size > threshold || (tab = table) == null || (n = tab.length) == 0) {
			n = (tab = resize()).length;
		}
		if ((first = tab[i = (n - 1) & hash]) != null) {
			if (first instanceof TreeNode) {
				old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
			} else {
				Node<K, V> e = first;
				K k;
				do {
					if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
						old = e;
						break;
					}
					++binCount;
				} while ((e = e.next) != null);
			}
			V oldValue;
			if (old != null && (oldValue = old.value) != null) {
				afterNodeAccess(old);
				return oldValue;
			}
		}
		V v = mappingFunction.apply(key);
		if (v == null) {
			return null;
		} else if (old != null) {
			old.value = v;
			afterNodeAccess(old);
			return v;
		} else if (t != null) {
			t.putTreeVal(this, tab, hash, key, v);
		} else {
			tab[i] = newNode(hash, key, v, first);
			if (binCount >= TREEIFY_THRESHOLD - 1) {
				treeifyBin(tab, hash);
			}
		}
		++modCount;
		++size;
		afterNodeInsertion(true);
		return v;
	}

	@Override
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		if (remappingFunction == null) {
			throw new NullPointerException();
		}
		Node<K, V> e;
		V oldValue;
		int hash = hash(key);
		if ((e = getNode(hash, key)) != null && (oldValue = e.value) != null) {
			V v = remappingFunction.apply(key, oldValue);
			if (v != null) {
				e.value = v;
				afterNodeAccess(e);
				return v;
			} else {
				removeNode(hash, key, null, false, true);
			}
		}
		return null;
	}

	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		if (remappingFunction == null) {
			throw new NullPointerException();
		}
		int hash = hash(key);
		Node<K, V>[] tab;
		Node<K, V> first;
		int n, i;
		int binCount = 0;
		TreeNode<K, V> t = null;
		Node<K, V> old = null;
		if (size > threshold || (tab = table) == null || (n = tab.length) == 0) {
			n = (tab = resize()).length;
		}
		if ((first = tab[i = (n - 1) & hash]) != null) {
			if (first instanceof TreeNode) {
				old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
			} else {
				Node<K, V> e = first;
				K k;
				do {
					if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
						old = e;
						break;
					}
					++binCount;
				} while ((e = e.next) != null);
			}
		}
		V oldValue = (old == null) ? null : old.value;
		V v = remappingFunction.apply(key, oldValue);
		if (old != null) {
			if (v != null) {
				old.value = v;
				afterNodeAccess(old);
			} else {
				removeNode(hash, key, null, false, true);
			}
		} else if (v != null) {
			if (t != null) {
				t.putTreeVal(this, tab, hash, key, v);
			} else {
				tab[i] = newNode(hash, key, v, first);
				if (binCount >= TREEIFY_THRESHOLD - 1) {
					treeifyBin(tab, hash);
				}
			}
			++modCount;
			++size;
			afterNodeInsertion(true);
		}
		return v;
	}

	@Override
	public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		if (value == null) {
			throw new NullPointerException();
		}
		if (remappingFunction == null) {
			throw new NullPointerException();
		}
		int hash = hash(key);
		Node<K, V>[] tab;
		Node<K, V> first;
		int n, i;
		int binCount = 0;
		TreeNode<K, V> t = null;
		Node<K, V> old = null;
		if (size > threshold || (tab = table) == null || (n = tab.length) == 0) {
			n = (tab = resize()).length;
		}
		if ((first = tab[i = (n - 1) & hash]) != null) {
			if (first instanceof TreeNode) {
				old = (t = (TreeNode<K, V>) first).getTreeNode(hash, key);
			} else {
				Node<K, V> e = first;
				K k;
				do {
					if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
						old = e;
						break;
					}
					++binCount;
				} while ((e = e.next) != null);
			}
		}
		if (old != null) {
			V v;
			if (old.value != null) {
				v = remappingFunction.apply(old.value, value);
			} else {
				v = value;
			}
			if (v != null) {
				old.value = v;
				afterNodeAccess(old);
			} else {
				removeNode(hash, key, null, false, true);
			}
			return v;
		}
		if (value != null) {
			if (t != null) {
				t.putTreeVal(this, tab, hash, key, value);
			} else {
				tab[i] = newNode(hash, key, value, first);
				if (binCount >= TREEIFY_THRESHOLD - 1) {
					treeifyBin(tab, hash);
				}
			}
			++modCount;
			++size;
			afterNodeInsertion(true);
		}
		return value;
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		Node<K, V>[] tab;
		if (action == null) {
			throw new NullPointerException();
		}
		if (size > 0 && (tab = table) != null) {
			int mc = modCount;
			for (int i = 0; i < tab.length; ++i) {
				for (Node<K, V> e = tab[i]; e != null; e = e.next) {
					action.accept(e.key, e.value);
				}
			}
			if (modCount != mc) {
				throw new ConcurrentModificationException();
			}
		}
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		Node<K, V>[] tab;
		if (function == null) {
			throw new NullPointerException();
		}
		if (size > 0 && (tab = table) != null) {
			int mc = modCount;
			for (int i = 0; i < tab.length; ++i) {
				for (Node<K, V> e = tab[i]; e != null; e = e.next) {
					e.value = function.apply(e.key, e.value);
				}
			}
			if (modCount != mc) {
				throw new ConcurrentModificationException();
			}
		}
	}

	/* ------------------------------------------------------------ */
	// Cloning and serialization

	/**
	 * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and values
	 * themselves are not cloned.
	 *
	 * @return a shallow copy of this map
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		HashMap<K, V> result;
		try {
			result = (HashMap<K, V>) super.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError(e);
		}
		result.reinitialize();
		result.putMapEntries(this, false);
		return result;
	}

	// These methods are also used when serializing HashSets
	final float loadFactor() {
		return loadFactor;
	}

	final int capacity() {
		return (table != null) ? table.length : (threshold > 0) ? threshold : DEFAULT_INITIAL_CAPACITY;
	}

	/**
	 * Save the state of the <tt>HashMap</tt> instance to a stream (i.e., serialize
	 * it).
	 *
	 * @serialData The <i>capacity</i> of the HashMap (the length of the bucket
	 *             array) is emitted (int), followed by the <i>size</i> (an int, the
	 *             number of key-value mappings), followed by the key (Object) and
	 *             value (Object) for each key-value mapping. The key-value mappings
	 *             are emitted in no particular order.
	 */
	private void writeObject(java.io.ObjectOutputStream s) throws IOException {
		int buckets = capacity();
		// Write out the threshold, loadfactor, and any hidden stuff
		s.defaultWriteObject();
		s.writeInt(buckets);
		s.writeInt(size);
		internalWriteEntries(s);
	}

	/**
	 * Reconstitute the {@code HashMap} instance from a stream (i.e., deserialize
	 * it).
	 */
	private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
		// Read in the threshold (ignored), loadfactor, and any hidden stuff
		s.defaultReadObject();
		reinitialize();
		if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new InvalidObjectException("Illegal load factor: " + loadFactor);
		}
		s.readInt(); // Read and ignore number of buckets
		int mappings = s.readInt(); // Read number of mappings (size)
		if (mappings < 0) {
			throw new InvalidObjectException("Illegal mappings count: " + mappings);
		} else if (mappings > 0) { // (if zero, use defaults)
			// Size the table using given load factor only if within
			// range of 0.25...4.0
			float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
			float fc = (float) mappings / lf + 1.0f;
			int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ? DEFAULT_INITIAL_CAPACITY
					: (fc >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : tableSizeFor((int) fc));
			float ft = (float) cap * lf;
			threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ? (int) ft : Integer.MAX_VALUE);
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Node<K, V>[] tab = (Node<K, V>[]) new Node[cap];
			table = tab;

			// Read the keys and values, and put the mappings in the HashMap
			for (int i = 0; i < mappings; i++) {
				@SuppressWarnings("unchecked")
				K key = (K) s.readObject();
				@SuppressWarnings("unchecked")
				V value = (V) s.readObject();
				putVal(hash(key), key, value, false, false);
			}
		}
	}

	/* ------------------------------------------------------------ */
	// iterators

	abstract class HashIterator {
		Node<K, V> next; // next entry to return
		Node<K, V> current; // current entry
		int expectedModCount; // for fast-fail
		int index; // current slot

		HashIterator() {
			expectedModCount = modCount;
			Node<K, V>[] t = table;
			current = next = null;
			index = 0;
			if (t != null && size > 0) { // advance to first entry
				do {
				} while (index < t.length && (next = t[index++]) == null);
			}
		}

		public final boolean hasNext() {
			return next != null;
		}

		final Node<K, V> nextNode() {
			Node<K, V>[] t;
			Node<K, V> e = next;
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			if (e == null) {
				throw new NoSuchElementException();
			}
			if ((next = (current = e).next) == null && (t = table) != null) {
				do {
				} while (index < t.length && (next = t[index++]) == null);
			}
			return e;
		}

		public final void remove() {
			Node<K, V> p = current;
			if (p == null) {
				throw new IllegalStateException();
			}
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
			current = null;
			K key = p.key;
			removeNode(hash(key), key, null, false, false);
			expectedModCount = modCount;
		}
	}

	final class KeyIterator extends HashIterator implements Iterator<K> {
		@Override
		public final K next() {
			return nextNode().key;
		}
	}

	final class ValueIterator extends HashIterator implements Iterator<V> {
		@Override
		public final V next() {
			return nextNode().value;
		}
	}

	final class EntryIterator extends HashIterator implements Iterator<Map.Entry<K, V>> {
		@Override
		public final Map.Entry<K, V> next() {
			return nextNode();
		}
	}

	/* ------------------------------------------------------------ */
	// spliterators

	static class HashMapSpliterator<K, V> {
		final HashMap<K, V> map;
		Node<K, V> current; // current node
		int index; // current index, modified on advance/split
		int fence; // one past last index
		int est; // size estimate
		int expectedModCount; // for comodification checks

		HashMapSpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
			this.map = m;
			this.index = origin;
			this.fence = fence;
			this.est = est;
			this.expectedModCount = expectedModCount;
		}

		final int getFence() { // initialize fence and size on first use
			int hi;
			if ((hi = fence) < 0) {
				HashMap<K, V> m = map;
				est = m.size;
				expectedModCount = m.modCount;
				Node<K, V>[] tab = m.table;
				hi = fence = (tab == null) ? 0 : tab.length;
			}
			return hi;
		}

		public final long estimateSize() {
			getFence(); // force init
			return (long) est;
		}
	}

	static final class KeySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<K> {
		KeySpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}

		@Override
		public KeySpliterator<K, V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid || current != null) ? null
					: new KeySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
		}

		@Override
		public void forEachRemaining(Consumer<? super K> action) {
			int i, hi, mc;
			if (action == null) {
				throw new NullPointerException();
			}
			HashMap<K, V> m = map;
			Node<K, V>[] tab = m.table;
			if ((hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = (tab == null) ? 0 : tab.length;
			} else {
				mc = expectedModCount;
			}
			if (tab != null && tab.length >= hi && (i = index) >= 0 && (i < (index = hi) || current != null)) {
				Node<K, V> p = current;
				current = null;
				do {
					if (p == null) {
						p = tab[i++];
					} else {
						action.accept(p.key);
						p = p.next;
					}
				} while (p != null || i < hi);
				if (m.modCount != mc) {
					throw new ConcurrentModificationException();
				}
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super K> action) {
			int hi;
			if (action == null) {
				throw new NullPointerException();
			}
			Node<K, V>[] tab = map.table;
			if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null) {
						current = tab[index++];
					} else {
						K k = current.key;
						current = current.next;
						action.accept(k);
						if (map.modCount != expectedModCount) {
							throw new ConcurrentModificationException();
						}
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public int characteristics() {
			return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) | Spliterator.DISTINCT;
		}
	}

	static final class ValueSpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<V> {
		ValueSpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}

		@Override
		public ValueSpliterator<K, V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid || current != null) ? null
					: new ValueSpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
		}

		@Override
		public void forEachRemaining(Consumer<? super V> action) {
			int i, hi, mc;
			if (action == null) {
				throw new NullPointerException();
			}
			HashMap<K, V> m = map;
			Node<K, V>[] tab = m.table;
			if ((hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = (tab == null) ? 0 : tab.length;
			} else {
				mc = expectedModCount;
			}
			if (tab != null && tab.length >= hi && (i = index) >= 0 && (i < (index = hi) || current != null)) {
				Node<K, V> p = current;
				current = null;
				do {
					if (p == null) {
						p = tab[i++];
					} else {
						action.accept(p.value);
						p = p.next;
					}
				} while (p != null || i < hi);
				if (m.modCount != mc) {
					throw new ConcurrentModificationException();
				}
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super V> action) {
			int hi;
			if (action == null) {
				throw new NullPointerException();
			}
			Node<K, V>[] tab = map.table;
			if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null) {
						current = tab[index++];
					} else {
						V v = current.value;
						current = current.next;
						action.accept(v);
						if (map.modCount != expectedModCount) {
							throw new ConcurrentModificationException();
						}
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public int characteristics() {
			return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
		}
	}

	static final class EntrySpliterator<K, V> extends HashMapSpliterator<K, V> implements Spliterator<Map.Entry<K, V>> {
		EntrySpliterator(HashMap<K, V> m, int origin, int fence, int est, int expectedModCount) {
			super(m, origin, fence, est, expectedModCount);
		}

		@Override
		public EntrySpliterator<K, V> trySplit() {
			int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
			return (lo >= mid || current != null) ? null
					: new EntrySpliterator<>(map, lo, index = mid, est >>>= 1, expectedModCount);
		}

		@Override
		public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
			int i, hi, mc;
			if (action == null) {
				throw new NullPointerException();
			}
			HashMap<K, V> m = map;
			Node<K, V>[] tab = m.table;
			if ((hi = fence) < 0) {
				mc = expectedModCount = m.modCount;
				hi = fence = (tab == null) ? 0 : tab.length;
			} else {
				mc = expectedModCount;
			}
			if (tab != null && tab.length >= hi && (i = index) >= 0 && (i < (index = hi) || current != null)) {
				Node<K, V> p = current;
				current = null;
				do {
					if (p == null) {
						p = tab[i++];
					} else {
						action.accept(p);
						p = p.next;
					}
				} while (p != null || i < hi);
				if (m.modCount != mc) {
					throw new ConcurrentModificationException();
				}
			}
		}

		@Override
		public boolean tryAdvance(Consumer<? super Map.Entry<K, V>> action) {
			int hi;
			if (action == null) {
				throw new NullPointerException();
			}
			Node<K, V>[] tab = map.table;
			if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
				while (current != null || index < hi) {
					if (current == null) {
						current = tab[index++];
					} else {
						Node<K, V> e = current;
						current = current.next;
						action.accept(e);
						if (map.modCount != expectedModCount) {
							throw new ConcurrentModificationException();
						}
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public int characteristics() {
			return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) | Spliterator.DISTINCT;
		}
	}

	/* ------------------------------------------------------------ */
	// LinkedHashMap support

	/*
	 * The following package-protected methods are designed to be overridden by
	 * LinkedHashMap, but not by any other subclass. Nearly all other internal
	 * methods are also package-protected but are declared final, so can be used by
	 * LinkedHashMap, view classes, and HashSet.
	 */

	// Create a regular (non-tree) node
	Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
		return new Node<>(hash, key, value, next);
	}

	// For conversion from TreeNodes to plain nodes
	Node<K, V> replacementNode(Node<K, V> p, Node<K, V> next) {
		return new Node<>(p.hash, p.key, p.value, next);
	}

	// Create a tree bin node
	TreeNode<K, V> newTreeNode(int hash, K key, V value, Node<K, V> next) {
		return new TreeNode<>(hash, key, value, next);
	}

	// For treeifyBin
	TreeNode<K, V> replacementTreeNode(Node<K, V> p, Node<K, V> next) {
		return new TreeNode<>(p.hash, p.key, p.value, next);
	}

	/**
	 * Reset to initial default state. Called by clone and readObject.
	 */
	void reinitialize() {
		table = null;
		entrySet = null;
		keySet = null;
		values = null;
		modCount = 0;
		threshold = 0;
		size = 0;
	}

	// Callbacks to allow LinkedHashMap post-actions
	void afterNodeAccess(Node<K, V> p) {
	}

	void afterNodeInsertion(boolean evict) {
	}

	void afterNodeRemoval(Node<K, V> p) {
	}

	// Called only from writeObject, to ensure compatible ordering.
	void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
		Node<K, V>[] tab;
		if (size > 0 && (tab = table) != null) {
			for (int i = 0; i < tab.length; ++i) {
				for (Node<K, V> e = tab[i]; e != null; e = e.next) {
					s.writeObject(e.key);
					s.writeObject(e.value);
				}
			}
		}
	}

	/* ------------------------------------------------------------ */
	// Tree bins

	/**
	 * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn extends Node)
	 * so can be used as extension of either regular or linked node.
	 */
	static final class TreeNode<K, V> extends LinkedHashMap.Entry<K, V> {
		TreeNode<K, V> parent; // red-black tree links
		TreeNode<K, V> left;
		TreeNode<K, V> right;
		TreeNode<K, V> prev; // needed to unlink next upon deletion
		boolean red;

		TreeNode(int hash, K key, V val, Node<K, V> next) {
			super(hash, key, val, next);
		}

		/**
		 * Returns root of tree containing this node. 根据当前节点找到根节点
		 */
		final TreeNode<K, V> root() {
			// 遍历，直到parent为null为止
			for (TreeNode<K, V> r = this, p;;) {
				if ((p = r.parent) == null) {
					return r;
				}
				r = p;
			}
		}

		/**
		 * Ensures that the given root is the first node of its bin.
		 */
		static <K, V> void moveRootToFront(Node<K, V>[] tab, TreeNode<K, V> root) {
			int n;
			if (root != null && tab != null && (n = tab.length) > 0) {
				int index = (n - 1) & root.hash;
				TreeNode<K, V> first = (TreeNode<K, V>) tab[index];
				if (root != first) {
					Node<K, V> rn;
					tab[index] = root;
					TreeNode<K, V> rp = root.prev;
					if ((rn = root.next) != null) {
						((TreeNode<K, V>) rn).prev = rp;
					}
					if (rp != null) {
						rp.next = rn;
					}
					if (first != null) {
						first.prev = root;
					}
					root.next = first;
					root.prev = null;
				}
				assert checkInvariants(root);
			}
		}

		/**
		 * Finds the node starting at root p with the given hash and key. The kc
		 * argument caches comparableClassFor(key) upon first use comparing keys.
		 * 
		 * 从根节点开始，根据指定的key查找节点 至于kc这个参数暂时也没弄清楚是干啥的，后面看到其他地方使用再理解吧，默认使用null即可
		 */
		final TreeNode<K, V> find(int h, Object k, Class<?> kc) {
			TreeNode<K, V> p = this;
			do {
				int ph, dir;
				K pk;
				TreeNode<K, V> pl = p.left, pr = p.right, q;
				// 根据大小判断在此节点还是在此节点的左右枝
				if ((ph = p.hash) > h) {
					p = pl;
				} else if (ph < h) {
					p = pr;
				} else if ((pk = p.key) == k || (k != null && k.equals(pk))) {
					return p;
				} else if (pl == null) {
					p = pr;
				} else if (pr == null) {
					p = pl;
				} else if ((kc != null || (kc = comparableClassFor(k)) != null)
						&& (dir = compareComparables(kc, k, pk)) != 0) {
					p = (dir < 0) ? pl : pr;
				} else if ((q = pr.find(h, k, kc)) != null) {
					return q;
				} else {
					p = pl;
				}
			} while (p != null);
			return null;
		}

		/**
		 * Calls find for root node. 从根节点开始查找 先判断当前节点的parent是否为null即判断当前节点是否为根节点。
		 * 如果不为根节点再调用root()方法找到根节点 再调用根节点的find方法根据指定的key查找键值对
		 */
		final TreeNode<K, V> getTreeNode(int h, Object k) {
			return ((parent != null) ? root() : this).find(h, k, null);
		}

		/**
		 * Tie-breaking utility for ordering insertions when equal hashCodes and
		 * non-comparable. We don't require a total order, just a consistent insertion
		 * rule to maintain equivalence across rebalancings. Tie-breaking further than
		 * necessary simplifies testing a bit.
		 */
		static int tieBreakOrder(Object a, Object b) {
			int d;
			if (a == null || b == null || (d = a.getClass().getName().compareTo(b.getClass().getName())) == 0) {
				d = (System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1);
			}
			return d;
		}

		/**
		 * Forms tree of the nodes linked from this node.
		 * 
		 * @return root of tree
		 */
		final void treeify(Node<K, V>[] tab) {
			TreeNode<K, V> root = null;
			for (TreeNode<K, V> x = this, next; x != null; x = next) {
				next = (TreeNode<K, V>) x.next;
				x.left = x.right = null;
				if (root == null) {
					x.parent = null;
					x.red = false;
					root = x;
				} else {
					K k = x.key;
					int h = x.hash;
					Class<?> kc = null;
					for (TreeNode<K, V> p = root;;) {
						int dir, ph;
						K pk = p.key;
						if ((ph = p.hash) > h) {
							dir = -1;
						} else if (ph < h) {
							dir = 1;
						} else if ((kc == null && (kc = comparableClassFor(k)) == null)
								|| (dir = compareComparables(kc, k, pk)) == 0) {
							dir = tieBreakOrder(k, pk);
						}

						TreeNode<K, V> xp = p;
						if ((p = (dir <= 0) ? p.left : p.right) == null) {
							x.parent = xp;
							if (dir <= 0) {
								xp.left = x;
							} else {
								xp.right = x;
							}
							root = balanceInsertion(root, x);
							break;
						}
					}
				}
			}
			moveRootToFront(tab, root);
		}

		/**
		 * Returns a list of non-TreeNodes replacing those linked from this node.
		 */
		final Node<K, V> untreeify(HashMap<K, V> map) {
			Node<K, V> hd = null, tl = null;
			for (Node<K, V> q = this; q != null; q = q.next) {
				Node<K, V> p = map.replacementNode(q, null);
				if (tl == null) {
					hd = p;
				} else {
					tl.next = p;
				}
				tl = p;
			}
			return hd;
		}

		/**
		 * Tree version of putVal.
		 */
		final TreeNode<K, V> putTreeVal(HashMap<K, V> map, Node<K, V>[] tab, int h, K k, V v) {
			Class<?> kc = null;
			boolean searched = false;
			TreeNode<K, V> root = (parent != null) ? root() : this;
			for (TreeNode<K, V> p = root;;) {
				int dir, ph;
				K pk;
				if ((ph = p.hash) > h) {
					dir = -1;
				} else if (ph < h) {
					dir = 1;
				} else if ((pk = p.key) == k || (k != null && k.equals(pk))) {
					return p;
				} else if ((kc == null && (kc = comparableClassFor(k)) == null)
						|| (dir = compareComparables(kc, k, pk)) == 0) {
					if (!searched) {
						TreeNode<K, V> q, ch;
						searched = true;
						if (((ch = p.left) != null && (q = ch.find(h, k, kc)) != null)
								|| ((ch = p.right) != null && (q = ch.find(h, k, kc)) != null)) {
							return q;
						}
					}
					dir = tieBreakOrder(k, pk);
				}

				TreeNode<K, V> xp = p;
				if ((p = (dir <= 0) ? p.left : p.right) == null) {
					Node<K, V> xpn = xp.next;
					TreeNode<K, V> x = map.newTreeNode(h, k, v, xpn);
					if (dir <= 0) {
						xp.left = x;
					} else {
						xp.right = x;
					}
					xp.next = x;
					x.parent = x.prev = xp;
					if (xpn != null) {
						((TreeNode<K, V>) xpn).prev = x;
					}
					moveRootToFront(tab, balanceInsertion(root, x));
					return null;
				}
			}
		}

		/**
		 * Removes the given node, that must be present before this call. This is
		 * messier than typical red-black deletion code because we cannot swap the
		 * contents of an interior node with a leaf successor that is pinned by "next"
		 * pointers that are accessible independently during traversal. So instead we
		 * swap the tree linkages. If the current tree appears to have too few nodes,
		 * the bin is converted back to a plain bin. (The test triggers somewhere
		 * between 2 and 6 nodes, depending on tree structure).
		 */
		final void removeTreeNode(HashMap<K, V> map, Node<K, V>[] tab, boolean movable) {
			int n;
			if (tab == null || (n = tab.length) == 0) {
				return;
			}
			int index = (n - 1) & hash;
			TreeNode<K, V> first = (TreeNode<K, V>) tab[index], root = first, rl;
			TreeNode<K, V> succ = (TreeNode<K, V>) next, pred = prev;
			if (pred == null) {
				tab[index] = first = succ;
			} else {
				pred.next = succ;
			}
			if (succ != null) {
				succ.prev = pred;
			}
			if (first == null) {
				return;
			}
			if (root.parent != null) {
				root = root.root();
			}
			if (root == null || root.right == null || (rl = root.left) == null || rl.left == null) {
				tab[index] = first.untreeify(map); // too small
				return;
			}
			TreeNode<K, V> p = this, pl = left, pr = right, replacement;
			if (pl != null && pr != null) {
				TreeNode<K, V> s = pr, sl;
				while ((sl = s.left) != null) {
					s = sl;
				}
				boolean c = s.red;
				s.red = p.red;
				p.red = c; // swap colors
				TreeNode<K, V> sr = s.right;
				TreeNode<K, V> pp = p.parent;
				if (s == pr) { // p was s's direct parent
					p.parent = s;
					s.right = p;
				} else {
					TreeNode<K, V> sp = s.parent;
					if ((p.parent = sp) != null) {
						if (s == sp.left) {
							sp.left = p;
						} else {
							sp.right = p;
						}
					}
					if ((s.right = pr) != null) {
						pr.parent = s;
					}
				}
				p.left = null;
				if ((p.right = sr) != null) {
					sr.parent = p;
				}
				if ((s.left = pl) != null) {
					pl.parent = s;
				}
				if ((s.parent = pp) == null) {
					root = s;
				} else if (p == pp.left) {
					pp.left = s;
				} else {
					pp.right = s;
				}
				if (sr != null) {
					replacement = sr;
				} else {
					replacement = p;
				}
			} else if (pl != null) {
				replacement = pl;
			} else if (pr != null) {
				replacement = pr;
			} else {
				replacement = p;
			}
			if (replacement != p) {
				TreeNode<K, V> pp = replacement.parent = p.parent;
				if (pp == null) {
					root = replacement;
				} else if (p == pp.left) {
					pp.left = replacement;
				} else {
					pp.right = replacement;
				}
				p.left = p.right = p.parent = null;
			}

			TreeNode<K, V> r = p.red ? root : balanceDeletion(root, replacement);

			if (replacement == p) { // detach
				TreeNode<K, V> pp = p.parent;
				p.parent = null;
				if (pp != null) {
					if (p == pp.left) {
						pp.left = null;
					} else if (p == pp.right) {
						pp.right = null;
					}
				}
			}
			if (movable) {
				moveRootToFront(tab, r);
			}
		}

		/**
		 * Splits nodes in a tree bin into lower and upper tree bins, or untreeifies if
		 * now too small. Called only from resize; see above discussion about split bits
		 * and indices.
		 * 
		 * 将红黑树转为链表
		 *
		 * @param map   the map
		 * @param tab   the table for recording bin heads
		 * @param index the index of the table being split
		 * @param bit   the bit of hash to split on
		 */
		final void split(HashMap<K, V> map, Node<K, V>[] tab, int index, int bit) {
			TreeNode<K, V> b = this;
			// Relink into lo and hi lists, preserving order
			TreeNode<K, V> loHead = null, loTail = null;
			TreeNode<K, V> hiHead = null, hiTail = null;
			int lc = 0, hc = 0;
			for (TreeNode<K, V> e = b, next; e != null; e = next) {
				next = (TreeNode<K, V>) e.next;
				e.next = null;
				if ((e.hash & bit) == 0) {
					if ((e.prev = loTail) == null) {
						loHead = e;
					} else {
						loTail.next = e;
					}
					loTail = e;
					++lc;
				} else {
					if ((e.prev = hiTail) == null) {
						hiHead = e;
					} else {
						hiTail.next = e;
					}
					hiTail = e;
					++hc;
				}
			}

			if (loHead != null) {
				if (lc <= UNTREEIFY_THRESHOLD) {
					tab[index] = loHead.untreeify(map);
				} else {
					tab[index] = loHead;
					if (hiHead != null) {
						loHead.treeify(tab);
					}
				}
			}
			if (hiHead != null) {
				if (hc <= UNTREEIFY_THRESHOLD) {
					tab[index + bit] = hiHead.untreeify(map);
				} else {
					tab[index + bit] = hiHead;
					if (loHead != null) {
						hiHead.treeify(tab);
					}
				}
			}
		}

		/* ------------------------------------------------------------ */
		// Red-black tree methods, all adapted from CLR

		static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> p) {
			TreeNode<K, V> r, pp, rl;
			if (p != null && (r = p.right) != null) {
				if ((rl = p.right = r.left) != null) {
					rl.parent = p;
				}
				if ((pp = r.parent = p.parent) == null) {
					(root = r).red = false;
				} else if (pp.left == p) {
					pp.left = r;
				} else {
					pp.right = r;
				}
				r.left = p;
				p.parent = r;
			}
			return root;
		}

		static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> p) {
			TreeNode<K, V> l, pp, lr;
			if (p != null && (l = p.left) != null) {
				if ((lr = p.left = l.right) != null) {
					lr.parent = p;
				}
				if ((pp = l.parent = p.parent) == null) {
					(root = l).red = false;
				} else if (pp.right == p) {
					pp.right = l;
				} else {
					pp.left = l;
				}
				l.right = p;
				p.parent = l;
			}
			return root;
		}

		static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root, TreeNode<K, V> x) {
			x.red = true;
			for (TreeNode<K, V> xp, xpp, xppl, xppr;;) {
				if ((xp = x.parent) == null) {
					x.red = false;
					return x;
				} else if (!xp.red || (xpp = xp.parent) == null) {
					return root;
				}
				if (xp == (xppl = xpp.left)) {
					if ((xppr = xpp.right) != null && xppr.red) {
						xppr.red = false;
						xp.red = false;
						xpp.red = true;
						x = xpp;
					} else {
						if (x == xp.right) {
							root = rotateLeft(root, x = xp);
							xpp = (xp = x.parent) == null ? null : xp.parent;
						}
						if (xp != null) {
							xp.red = false;
							if (xpp != null) {
								xpp.red = true;
								root = rotateRight(root, xpp);
							}
						}
					}
				} else {
					if (xppl != null && xppl.red) {
						xppl.red = false;
						xp.red = false;
						xpp.red = true;
						x = xpp;
					} else {
						if (x == xp.left) {
							root = rotateRight(root, x = xp);
							xpp = (xp = x.parent) == null ? null : xp.parent;
						}
						if (xp != null) {
							xp.red = false;
							if (xpp != null) {
								xpp.red = true;
								root = rotateLeft(root, xpp);
							}
						}
					}
				}
			}
		}

		static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root, TreeNode<K, V> x) {
			for (TreeNode<K, V> xp, xpl, xpr;;) {
				if (x == null || x == root) {
					return root;
				} else if ((xp = x.parent) == null) {
					x.red = false;
					return x;
				} else if (x.red) {
					x.red = false;
					return root;
				} else if ((xpl = xp.left) == x) {
					if ((xpr = xp.right) != null && xpr.red) {
						xpr.red = false;
						xp.red = true;
						root = rotateLeft(root, xp);
						xpr = (xp = x.parent) == null ? null : xp.right;
					}
					if (xpr == null) {
						x = xp;
					} else {
						TreeNode<K, V> sl = xpr.left, sr = xpr.right;
						if ((sr == null || !sr.red) && (sl == null || !sl.red)) {
							xpr.red = true;
							x = xp;
						} else {
							if (sr == null || !sr.red) {
								if (sl != null) {
									sl.red = false;
								}
								xpr.red = true;
								root = rotateRight(root, xpr);
								xpr = (xp = x.parent) == null ? null : xp.right;
							}
							if (xpr != null) {
								xpr.red = (xp == null) ? false : xp.red;
								if ((sr = xpr.right) != null) {
									sr.red = false;
								}
							}
							if (xp != null) {
								xp.red = false;
								root = rotateLeft(root, xp);
							}
							x = root;
						}
					}
				} else { // symmetric
					if (xpl != null && xpl.red) {
						xpl.red = false;
						xp.red = true;
						root = rotateRight(root, xp);
						xpl = (xp = x.parent) == null ? null : xp.left;
					}
					if (xpl == null) {
						x = xp;
					} else {
						TreeNode<K, V> sl = xpl.left, sr = xpl.right;
						if ((sl == null || !sl.red) && (sr == null || !sr.red)) {
							xpl.red = true;
							x = xp;
						} else {
							if (sl == null || !sl.red) {
								if (sr != null) {
									sr.red = false;
								}
								xpl.red = true;
								root = rotateLeft(root, xpl);
								xpl = (xp = x.parent) == null ? null : xp.left;
							}
							if (xpl != null) {
								xpl.red = (xp == null) ? false : xp.red;
								if ((sl = xpl.left) != null) {
									sl.red = false;
								}
							}
							if (xp != null) {
								xp.red = false;
								root = rotateRight(root, xp);
							}
							x = root;
						}
					}
				}
			}
		}

		/**
		 * Recursive invariant check
		 */
		static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
			TreeNode<K, V> tp = t.parent, tl = t.left, tr = t.right, tb = t.prev, tn = (TreeNode<K, V>) t.next;
			if (tb != null && tb.next != t) {
				return false;
			}
			if (tn != null && tn.prev != t) {
				return false;
			}
			if (tp != null && t != tp.left && t != tp.right) {
				return false;
			}
			if (tl != null && (tl.parent != t || tl.hash > t.hash)) {
				return false;
			}
			if (tr != null && (tr.parent != t || tr.hash < t.hash)) {
				return false;
			}
			if (t.red && tl != null && tl.red && tr != null && tr.red) {
				return false;
			}
			if (tl != null && !checkInvariants(tl)) {
				return false;
			}
			if (tr != null && !checkInvariants(tr)) {
				return false;
			}
			return true;
		}
	}

}
