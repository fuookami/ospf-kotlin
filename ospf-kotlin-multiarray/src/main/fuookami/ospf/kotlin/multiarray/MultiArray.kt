package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.utils.concept.Indexed

/**
 * 抽象多维数组基类
 * Abstract multi-dimensional array base class
 *
 * @param T 元素类型
 * @param S 形状类型
 */
sealed class AbstractMultiArray<out T : Any, S : Shape>(
    val shape: S,
    ctor: ((Int, IntArray) -> @UnsafeVariance T)? = null
) : Collection<T> {

    internal lateinit var list: MutableList<@UnsafeVariance T>

    init {
        if (ctor != null) {
            init(ctor)
        }
    }

    /**
     * 维度数量
     * Number of dimensions
     */
    val dimension by shape::dimension

    /**
     * 存储顺序
     * Storage order
     */
    val storageOrder: StorageOrder get() = shape.storageOrder

    /**
     * 元素总数
     * Total number of elements
     */
    override val size get() = list.size

    /**
     * 初始化数组
     * Initialize the array
     */
    protected fun init(ctor: (Int, IntArray) -> @UnsafeVariance T) {
        if (!::list.isInitialized) {
            list = (0..<shape.size).map { ctor(it, shape.vector(it)) }.toMutableList()
        }
    }

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean {
        return list.containsAll(elements)
    }

    override fun contains(element: @UnsafeVariance T): Boolean {
        return list.contains(element)
    }

    override fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return list.iterator()
    }

    /**
     * 通过线性索引获取元素
     * Get element by linear index
     */
    operator fun get(i: Int): T {
        return list[i]
    }

    /**
     * 通过 ULong 线性索引获取元素
     * Get element by ULong linear index
     */
    operator fun get(i: ULong): T {
        return get(i.toInt())
    }

    /**
     * 通过 Indexed 接口获取元素
     * Get element by Indexed interface
     */
    operator fun get(e: Indexed): T {
        return list[e.index]
    }

    /**
     * 通过向量索引获取元素
     * Get element by vector index
     */
    @JvmName("getByIntArray")
    operator fun get(v: IntArray): T {
        return list[shape.index(v)]
    }

    /**
     * 通过可变参数获取元素
     * Get element by vararg
     */
    @JvmName("getByInts")
    operator fun get(vararg v: Int): T {
        return list[shape.index(v)]
    }

    /**
     * 通过 ULong 迭代获取元素
     * Get element by ULong iterable
     */
    operator fun get(v: Iterable<ULong>): T {
        return get(v.map { it.toInt() }.toIntArray())
    }

    /**
     * 通过 Indexed 可变参数获取元素
     * Get element by Indexed vararg
     */
    operator fun get(vararg v: Indexed): T {
        return list[shape.index(v.map { it.index }.toIntArray())]
    }

    /**
     * 通过任意类型数组创建视图
     * Create view by any type array
     */
    operator fun get(vararg v: Any): MultiArrayView<T, S> {
        return MultiArrayView(this, shape.dummyVector(*v))
    }

    /**
     * 创建视图
     * Create a view
     *
     * @param dummyVector 虚拟向量
     * @return 多维数组视图
     */
    fun view(dummyVector: DummyVector): MultiArrayView<T, S> {
        return MultiArrayView(this, dummyVector)
    }

    /**
     * 获取枚举迭代器
     * Get enumerate iterator
     *
     * @return 枚举迭代器，产生 (线性索引, 向量坐标, 元素引用) 三元组
     */
    fun enumerate(): Sequence<Triple<Int, IntArray, T>> = sequence {
        for (i in 0 until shape.size) {
            yield(Triple(i, shape.vector(i), list[i]))
        }
    }
}

/**
 * 不可变多维数组
 * Immutable multi-dimensional array
 *
 * @param T 元素类型
 * @param S 形状类型
 */
open class MultiArray<out T : Any, S : Shape>(
    shape: S,
    ctor: ((Int, IntArray) -> T)? = null
) : AbstractMultiArray<T, S>(shape, ctor) {

    companion object {
        /**
         * 使用默认值创建多维数组
         * Create multi-dimensional array with default values
         */
        inline fun <reified T : Any, S : Shape> new(shape: S): MultiArray<T, S> {
            return MultiArray(shape) { _, _ ->
                @Suppress("UNCHECKED_CAST")
                when (T::class) {
                    Int::class -> 0 as T
                    Long::class -> 0L as T
                    Double::class -> 0.0 as T
                    Float::class -> 0.0f as T
                    Boolean::class -> false as T
                    String::class -> "" as T
                    else -> throw IllegalArgumentException("Type ${T::class} does not have a default value")
                }
            }
        }

        /**
         * 使用指定值创建多维数组
         * Create multi-dimensional array with specified value
         */
        fun <T : Any, S : Shape> newWith(shape: S, value: T): MultiArray<T, S> {
            return MultiArray(shape) { _, _ -> value }
        }

        /**
         * 使用生成器创建多维数组
         * Create multi-dimensional array with generator
         */
        fun <T : Any, S : Shape> newBy(shape: S, generator: (Int, IntArray) -> T): MultiArray<T, S> {
            return MultiArray(shape, generator)
        }
    }

    /**
     * 转换存储顺序
     * Convert storage order
     *
     * @param order 目标存储顺序
     * @return 具有新存储顺序的数组
     */
    fun toStorageOrder(order: StorageOrder): MultiArray<T, DynShape> {
        if (shape.storageOrder == order) {
            // Same order - no reordering needed
            val dims = (0 until shape.dimension).map { shape[it] }.toIntArray()
            val newShape = DynShape.withOrder(dims, order)
            return MultiArray(newShape) { i, _ -> list[i] }
        }

        // Different order - need to reorder data
        val dims = (0 until shape.dimension).map { shape[it] }.toIntArray()
        val newShape = DynShape.withOrder(dims, order)

        return MultiArray(newShape) { newLinearIndex, _ ->
            // Convert new linear index to vector coordinates
            val vector = newShape.vector(newLinearIndex)
            // Use same vector to access original array (preserves value)
            val oldLinearIndex = shape.index(vector)
            list[oldLinearIndex]
        }
    }

    /**
     * 重塑数组形状
     * Reshape array
     *
     * @param newShape 新形状
     * @param fillValue 填充值（如果新形状更大）
     * @return 重塑后的数组
     */
    fun <NS : Shape> reshape(newShape: NS, fillValue: @UnsafeVariance T): MultiArray<T, NS> {
        return MultiArray(newShape) { i, _ ->
            if (i < size) list[i] else fillValue
        }
    }

    /**
     * 重塑数组形状（使用生成器填充）
     * Reshape array (fill with generator)
     */
    fun <NS : Shape> reshapeBy(newShape: NS, generator: (Int, IntArray) -> @UnsafeVariance T): MultiArray<T, NS> {
        return MultiArray(newShape) { i, v ->
            if (i < size) list[i] else generator(i, v)
        }
    }

    /**
     * 转换为可变数组
     * Convert to mutable array
     */
    fun toMutable(): MutableMultiArray<@UnsafeVariance T, S> {
        return MutableMultiArray(shape) { i, _ -> list[i] }
    }

    /**
     * 转换为列表
     * Convert to list
     */
    fun toList(): List<T> = list.toList()
}

/**
 * 可变多维数组
 * Mutable multi-dimensional array
 *
 * @param T 元素类型
 * @param S 形状类型
 */
open class MutableMultiArray<T : Any, S : Shape>(
    shape: S,
    ctor: ((Int, IntArray) -> T)? = null
) : AbstractMultiArray<T, S>(shape, ctor) {

    companion object {
        /**
         * 使用默认值创建可变多维数组
         * Create mutable multi-dimensional array with default values
         */
        inline fun <reified T : Any, S : Shape> new(shape: S): MutableMultiArray<T, S> {
            return MutableMultiArray(shape) { _, _ ->
                @Suppress("UNCHECKED_CAST")
                when (T::class) {
                    Int::class -> 0 as T
                    Long::class -> 0L as T
                    Double::class -> 0.0 as T
                    Float::class -> 0.0f as T
                    Boolean::class -> false as T
                    String::class -> "" as T
                    else -> throw IllegalArgumentException("Type ${T::class} does not have a default value")
                }
            }
        }

        /**
         * 使用指定值创建可变多维数组
         * Create mutable multi-dimensional array with specified value
         */
        fun <T : Any, S : Shape> newWith(shape: S, value: T): MutableMultiArray<T, S> {
            return MutableMultiArray(shape) { _, _ -> value }
        }

        /**
         * 使用生成器创建可变多维数组
         * Create mutable multi-dimensional array with generator
         */
        fun <T : Any, S : Shape> newBy(shape: S, generator: (Int, IntArray) -> T): MutableMultiArray<T, S> {
            return MutableMultiArray(shape, generator)
        }
    }

    /**
     * 通过线性索引设置元素
     * Set element by linear index
     */
    operator fun set(i: Int, value: T) {
        list[i] = value
    }

    /**
     * 通过 ULong 线性索引设置元素
     * Set element by ULong linear index
     */
    operator fun set(i: ULong, value: T) {
        set(i.toInt(), value)
    }

    /**
     * 通过 Indexed 接口设置元素
     * Set element by Indexed interface
     */
    operator fun set(e: Indexed, value: T) {
        list[e.index] = value
    }

    /**
     * 通过向量索引设置元素
     * Set element by vector index
     */
    @JvmName("setByIntArray")
    operator fun set(v: IntArray, value: T) {
        list[shape.index(v)] = value
    }

    /**
     * 通过可变参数设置元素
     * Set element by vararg
     */
    @JvmName("setByInts")
    operator fun set(vararg v: Int, value: T) {
        list[shape.index(v)] = value
    }

    /**
     * 通过 ULong 迭代设置元素
     * Set element by ULong iterable
     */
    operator fun set(v: Iterable<ULong>, value: T) {
        set(v.map { it.toInt() }.toIntArray(), value)
    }

    /**
     * 通过 Indexed 可变参数设置元素
     * Set element by Indexed vararg
     */
    operator fun set(vararg v: Indexed, value: T) {
        list[shape.index(v.map { it.index }.toIntArray())] = value
    }

    /**
     * 填充所有元素
     * Fill all elements
     */
    fun fill(value: T) {
        for (i in 0 until size) {
            list[i] = value
        }
    }

    /**
     * 使用生成器填充所有元素
     * Fill all elements with generator
     */
    fun fillBy(generator: (Int, IntArray) -> T) {
        for (i in 0 until size) {
            list[i] = generator(i, shape.vector(i))
        }
    }

    /**
     * 转换为不可变数组
     * Convert to immutable array
     */
    fun toImmutable(): MultiArray<T, S> {
        return MultiArray(shape) { i, _ -> list[i] }
    }
}

// 类型别名
typealias MultiArray1<T> = MultiArray<T, Shape1>
typealias MultiArray2<T> = MultiArray<T, Shape2>
typealias MultiArray3<T> = MultiArray<T, Shape3>
typealias MultiArray4<T> = MultiArray<T, Shape4>
typealias DynMultiArray<T> = MultiArray<T, DynShape>

typealias MutableMultiArray1<T> = MutableMultiArray<T, Shape1>
typealias MutableMultiArray2<T> = MutableMultiArray<T, Shape2>
typealias MutableMultiArray3<T> = MutableMultiArray<T, Shape3>
typealias MutableMultiArray4<T> = MutableMultiArray<T, Shape4>
typealias DynMutableMultiArray<T> = MutableMultiArray<T, DynShape>

/**
 * 便捷函数：创建一维数组
 * Convenience function: Create 1D array
 */
fun <T : Any> multiArrayOf(d1: Int, value: T): MultiArray1<T> {
    return MultiArray.newWith(Shape1(d1), value)
}

/**
 * 便捷函数：创建二维数组
 * Convenience function: Create 2D array
 */
fun <T : Any> multiArrayOf(d1: Int, d2: Int, value: T): MultiArray2<T> {
    return MultiArray.newWith(Shape2(d1, d2), value)
}

/**
 * 便捷函数：创建三维数组
 * Convenience function: Create 3D array
 */
fun <T : Any> multiArrayOf(d1: Int, d2: Int, d3: Int, value: T): MultiArray3<T> {
    return MultiArray.newWith(Shape3(d1, d2, d3), value)
}

/**
 * 便捷函数：创建可变一维数组
 * Convenience function: Create mutable 1D array
 */
fun <T : Any> mutableMultiArrayOf(d1: Int, value: T): MutableMultiArray1<T> {
    return MutableMultiArray.newWith(Shape1(d1), value)
}

/**
 * 便捷函数：创建可变二维数组
 * Convenience function: Create mutable 2D array
 */
fun <T : Any> mutableMultiArrayOf(d1: Int, d2: Int, value: T): MutableMultiArray2<T> {
    return MutableMultiArray.newWith(Shape2(d1, d2), value)
}

/**
 * 便捷函数：创建可变三维数组
 * Convenience function: Create mutable 3D array
 */
fun <T : Any> mutableMultiArrayOf(d1: Int, d2: Int, d3: Int, value: T): MutableMultiArray3<T> {
    return MutableMultiArray.newWith(Shape3(d1, d2, d3), value)
}