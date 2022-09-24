package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.concept.Indexed

open class MultiArray<T : Any, S : Shape>(
    val shape: S
) {
    var list = ArrayList<T?>((0 until shape.size).map { null })

    // getters and setters
    operator fun get(i: Int): T? {
        return list[i]
    }

    operator fun get(e: Indexed): T? {
        return list[e.index]
    }

    operator fun get(vararg v: Int): T? {
        return list[shape.index(v)]
    }

    operator fun get(vararg v: Indexed): T? {
        return list[shape.index(v.map { it.index }.toIntArray())]
    }

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

    operator fun iterator(): Iterator<T?> {
        return list.iterator()
    }
}

typealias MultiArray1<T> = MultiArray<T, Shape1>
typealias MultiArray2<T> = MultiArray<T, Shape2>
typealias MultiArray3<T> = MultiArray<T, Shape3>
typealias MultiArray4<T> = MultiArray<T, Shape4>
typealias DynMultiArray<T> = MultiArray<T, DynShape>
