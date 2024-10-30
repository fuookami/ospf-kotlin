package fuookami.ospf.kotlin.utils.multi_array

import kotlin.reflect.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*

class DimensionMismatchingException(
    val dimension: Int,
    val vectorDimension: Int
) : Throwable() {
    override val message: String = "Dimension should be $dimension, not $vectorDimension."
}

class OutOfShapeException(
    val dimension: Int,
    val length: Int,
    val vectorIndex: Int
) : Throwable() {
    override val message: String = "Length of dimension $dimension is $length, but it get $vectorIndex."
}

class UnknownDummyIndexTypeException(
    val cls: KClass<*>
) : Throwable() {
    override val message: String = "Unknown dummy index type: $cls."
}

interface Shape {
    val dimension: Int
    val size: Int
    val indices get() = 0 until dimension

    operator fun get(index: Int): Int
    fun index(vector: IntArray): Int
    fun vector(index: Int): IntArray

    fun next(vector: IntArray): IntArray? {
        val temp = vector.clone()
        var i = dimension - 1
        while (i >= 0) {
            if (this[i] == 0 || temp[i] == (this[i] - 1)) {
                temp[i] = 0
            } else {
                temp[i] = temp[i] + 1
                return temp
            }
            --i
        }
        return null
    }

    fun dummyVector(vararg v: Any): DummyVector {
        if (v.size != dimension) {
            throw DimensionMismatchingException(dimension, v.size)
        }
        val vector = ArrayList<DummyIndex>()
        for (i in indices) {
            when (val index = v[i]) {
                _a -> {
                    vector.add(DummyIndex(null))
                }

                is IntRange -> {
                    vector.add(DummyIndex(ValueRange(UInt64(index.first), UInt64(index.last)).value!!))
                }

                is IntegerRange<*> -> {
                    vector.add(
                        DummyIndex(
                            ValueRange(
                                (index.first as RealNumber<*>).toUInt64(),
                                (index.last as RealNumber<*>).toUInt64() - UInt64.one
                            ).value!!
                        )
                    )
                }

                is Int -> {
                    vector.add(DummyIndex(ValueRange(UInt64(index), UInt64(index)).value!!))
                }

                is Indexed -> {
                    vector.add(DummyIndex(ValueRange(UInt64(index.index), UInt64(index.index)).value!!))
                }

                is Integer<*> -> {
                    vector.add(DummyIndex(ValueRange(index.toUInt64(), index.toUInt64() + UInt64.one).value!!))
                }

                else -> {
                    throw UnknownDummyIndexTypeException(index.javaClass.kotlin)
                }
            }
        }
        return vector
    }
}

data class Shape1(private val d1: Int) : Shape {
    override val dimension = 1
    override val size by ::d1

    @Throws(ArrayIndexOutOfBoundsException::class)
    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            else -> throw ArrayIndexOutOfBoundsException("Total size is $d1, but it get $index")
        }
    }

    @Throws(DimensionMismatchingException::class)
    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            1 -> if (vector[0] > d1) {
                throw OutOfShapeException(1, d1, vector[0])
            } else {
                vector[0]
            }

            else -> throw DimensionMismatchingException(1, vector.size)
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index > d1) {
            throw ArrayIndexOutOfBoundsException("Total size is $d1, but it get $index")
        } else {
            intArrayOf(index)
        }
    }
}

data class Shape2(private val d1: Int, private val d2: Int) : Shape {
    private val totalSize by lazy { d1 * d2 }
    override val dimension = 2
    override val size get() = totalSize

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            else -> throw ArrayIndexOutOfBoundsException("")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            2 -> if (vector[0] > d1) {
                throw OutOfShapeException(1, d1, vector[0])
            } else if (vector[1] > d2) {
                throw OutOfShapeException(2, d2, vector[1])
            } else {
                vector[0] * d2 + vector[1]
            }

            else -> throw DimensionMismatchingException(2, vector.size)
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index > totalSize) {
            throw ArrayIndexOutOfBoundsException("Total size is $totalSize, but it get $index")
        } else {
            intArrayOf(index / d2, index % d2)
        }
    }
}

data class Shape3(private val d1: Int, private val d2: Int, private val d3: Int) : Shape {
    private val totalSize = d1 * d2 * d3
    override val dimension = 3
    override val size get() = totalSize

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            2 -> d3
            else -> throw ArrayIndexOutOfBoundsException("")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            3 -> if (vector[0] > d1) {
                throw OutOfShapeException(1, d1, vector[0])
            } else if (vector[1] > d2) {
                throw OutOfShapeException(2, d2, vector[1])
            } else if (vector[2] > d3) {
                throw OutOfShapeException(3, d3, vector[2])
            } else {
                (vector[0] * d2 + vector[1]) * d3 + vector[2]
            }

            else -> throw DimensionMismatchingException(3, vector.size)
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index > totalSize) {
            throw ArrayIndexOutOfBoundsException("Total size is $totalSize, but it get $index")
        } else {
            var currentIndex = index
            val vector = ArrayList<Int>()
            vector.add(0, currentIndex % d3)
            currentIndex /= d3
            vector.add(0, currentIndex % d2)
            currentIndex /= d2
            vector.add(0, currentIndex % d1)
            currentIndex /= d1
            vector.toIntArray()
        }
    }
}

data class Shape4(private val d1: Int, private val d2: Int, private val d3: Int, private val d4: Int) : Shape {
    private val totalSize = d1 * d2 * d3 * d4
    override val dimension = 4
    override val size get() = totalSize

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            2 -> d3
            3 -> d4
            else -> throw ArrayIndexOutOfBoundsException("")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            4 -> if (vector[0] > d1) {
                throw OutOfShapeException(1, d1, vector[0])
            } else if (vector[1] > d2) {
                throw OutOfShapeException(2, d2, vector[1])
            } else if (vector[2] > d3) {
                throw OutOfShapeException(3, d3, vector[2])
            } else if (vector[3] > d4) {
                throw OutOfShapeException(4, d4, vector[3])
            } else {
                ((vector[0] * d2 + vector[1]) * d3 + vector[2]) * d4 + vector[3]
            }

            else -> throw DimensionMismatchingException(4, vector.size)
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index > totalSize) {
            throw ArrayIndexOutOfBoundsException("Total size is $totalSize, but it get $index")
        } else {
            var currentIndex = index
            val vector = ArrayList<Int>()
            vector.add(0, currentIndex % d4)
            currentIndex /= d4
            vector.add(0, currentIndex % d3)
            currentIndex /= d3
            vector.add(0, currentIndex % d2)
            currentIndex /= d2
            vector.add(0, currentIndex % d1)
            currentIndex /= d1
            vector.toIntArray()
        }
    }
}

data class DynShape(private val shape: IntArray) : Shape {
    private val totalSize = calculateTotalSize(shape)
    override val dimension get() = shape.size
    override val size get() = totalSize

    override operator fun get(index: Int): Int {
        return shape[index]
    }

    override fun index(vector: IntArray): Int {
        if (dimension != vector.size) {
            throw DimensionMismatchingException(dimension, vector.size)
        }
        var ret = 0
        for (i in 0 until (dimension - 1)) {
            if (vector[i] > shape[i]) {
                throw OutOfShapeException(i + 1, shape[i], vector[i])
            }
            ret += vector[i]
            ret *= shape[i + 1]
        }
        return ret + vector[dimension - 1]
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index > totalSize) {
            throw ArrayIndexOutOfBoundsException("Total size is $totalSize, but it get $index")
        } else {
            var currentIndex = index
            val vector = ArrayList<Int>()
            for (i in 0 until dimension) {
                vector.add(0, currentIndex % shape[dimension - 1 - i])
                currentIndex /= shape[dimension - 1 - i]
            }
            vector.toIntArray()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DynShape

        if (!shape.contentEquals(other.shape)) return false

        return true
    }

    override fun hashCode(): Int {
        return shape.contentHashCode()
    }

    companion object {
        @JvmStatic
        private fun calculateTotalSize(shape: IntArray): Int {
            var ret = 1
            for (l in shape) {
                ret *= l
            }
            return ret
        }
    }
}
