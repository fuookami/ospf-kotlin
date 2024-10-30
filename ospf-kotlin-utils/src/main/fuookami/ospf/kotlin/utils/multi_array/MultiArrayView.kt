package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.math.*

class MultiArrayView<out T : Any, S : Shape>(
    private val origin: AbstractMultiArray<T, S>,
    private val vector: DummyVector
) : Collection<T> {
    private data class ElementIterator<out T : Any, S : Shape>(
        private val view: MultiArrayView<T, S>,
        private var current: IntArray? = null
    ) : Iterator<T> {
        override fun next(): T {
            current = if (current == null) {
                view.shape.indices.map { 0 }.toIntArray()
            } else {
                view.shape.next(current!!)
            }
            return view.origin[view.actualVector(current!!)]
        }

        override fun hasNext(): Boolean {
            return current == null || view.shape.next(current!!) != null
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ElementIterator<*, *>

            if (view != other.view) return false
            if (current != null) {
                if (other.current == null) return false
                if (!current.contentEquals(other.current)) return false
            } else if (other.current != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = view.hashCode()
            result = 31 * result + (current?.contentHashCode() ?: 0)
            return result
        }
    }

    val shape: DynShape
    private val dummyDimensions: Set<Int>

    init {
        val shape = ArrayList<Int>()
        val dummyDimensions = HashSet<Int>()
        for ((dimension, index) in vector.withIndex()) {
            when (val range = index.range) {
                null -> {
                    shape.add(origin.shape[dimension])
                    dummyDimensions.add(dimension)
                }

                else -> {
                    if (!range.fixed) {
                        shape.add((range.upperBound - range.lowerBound).value.unwrapOrNull()?.toInt() ?: origin.shape[dimension])
                        dummyDimensions.add(dimension)
                    }
                }
            }
        }
        this.shape = DynShape(shape.toIntArray())
        this.dummyDimensions = dummyDimensions
    }

    override val size by shape::size

    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean {
        return elements.all {
            val iterator = ElementIterator(this)
            while (iterator.hasNext()) {
                val thisElement = iterator.next()
                if (thisElement == it) {
                    return@all true
                }
            }
            false
        }
    }

    override fun contains(element: @UnsafeVariance T): Boolean {
        val iterator = ElementIterator(this)
        while (iterator.hasNext()) {
            val thisElement = iterator.next()
            if (thisElement == element) {
                return true
            }
        }
        return false
    }

    override fun isEmpty(): Boolean {
        return origin.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return ElementIterator(this)
    }

    operator fun get(i: Int): T {
        return origin[shape.vector(i)]
    }

    operator fun get(e: Indexed): T {
        return origin[shape.vector(e.index)]
    }

    @JvmName("getByIntArray")
    operator fun get(v: IntArray): T {
        return origin[actualVector(v)]
    }

    @JvmName("getByInts")
    operator fun get(vararg v: Int): T {
        return origin[actualVector(v)]
    }

    operator fun get(vararg v: Indexed): T {
        return origin[actualVector(v.map { it.index }.toIntArray())]
    }

    operator fun get(vararg v: Any): MultiArrayView<T, S> {
        val vector = ArrayList<DummyIndex>()
        val dummyVector = shape.dummyVector(*v)
        var j = 0
        for (i in origin.shape.indices) {
            if (!dummyDimensions.contains(i)) {
                vector.add(DummyIndex(this.vector[i].range!!.fixedValue!!))
            } else {
                vector.add(dummyVector[j])
                ++j
            }
        }
        return MultiArrayView(origin, vector)
    }

    private fun actualVector(v: IntArray): IntArray {
        val vector = ArrayList<Int>()
        var j = 0
        for (i in origin.shape.indices) {
            if (!dummyDimensions.contains(i)) {
                vector.add(this.vector[i].range!!.fixedValue!!.toInt())
            } else {
                val thisIndex = when (val range = this.vector[i].range) {
                    null -> {
                        v[j]
                    }

                    else -> {
                        (range.lowerBound.value.unwrapOrNull() ?: UInt64.zero).toInt() + v[j]
                    }
                }
                vector.add(thisIndex)
                ++j
            }
        }
        return vector.toIntArray()
    }
}
