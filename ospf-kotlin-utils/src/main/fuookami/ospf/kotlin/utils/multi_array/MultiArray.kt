package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.concept.*

sealed class AbstractMultiArray<out T : Any, S : Shape>(
    val shape: S,
    ctor: ((Int, IntArray) -> T)? = null
) : Collection<T> {
    internal lateinit var list: MutableList<@UnsafeVariance T>

    init {
        if (ctor != null) {
            init(ctor)
        }
    }

    val dimension by shape::dimension
    override val size get() = list.size

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

    operator fun get(i: Int): T {
        return list[i]
    }

    operator fun get(e: Indexed): T {
        return list[e.index]
    }

    @JvmName("getByIntArray")
    operator fun get(v: IntArray): T {
        return list[shape.index(v)]
    }

    @JvmName("getByInts")
    operator fun get(vararg v: Int): T {
        return list[shape.index(v)]
    }

    operator fun get(vararg v: Indexed): T {
        return list[shape.index(v.map { it.index }.toIntArray())]
    }

    operator fun get(vararg v: Any): MultiArrayView<T, S> {
        return MultiArrayView(this, shape.dummyVector(*v))
    }
}

open class MultiArray<out T : Any, S : Shape>(
    shape: S,
    ctor: ((Int, IntArray) -> T)? = null
) : AbstractMultiArray<T, S>(shape, ctor)

open class MutableMultiArray<T : Any, S : Shape>(
    shape: S,
    ctor: ((Int, IntArray) -> T)? = null
) : AbstractMultiArray<T, S>(shape, ctor) {
    operator fun set(i: Int, value: T) {
        list[i] = value
    }

    operator fun set(e: Indexed, value: T) {
        list[e.index] = value
    }

    operator fun set(vararg v: Int, e: T) {
        list[shape.index(v)] = e
    }

    operator fun set(vararg v: Indexed, value: T) {
        list[shape.index(v.map { it.index }.toIntArray())] = value
    }
}

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
