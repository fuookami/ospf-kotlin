package fuookami.ospf.kotlin.utils.multi_array

class DimensionMismatchingException(
    val dimension: Int,
    val vectorDimension: Int
) : Throwable() {
    override val message: String = "Dimension should be $dimension, not $vectorDimension."
}

class OutOfShapeException(
    val dimension: Int,
    val vectorIndex: Int,
    val length: Int,
) : Throwable() {
    override val message: String = "Length of dimension $dimension is $length, but it get $vectorIndex."
}

interface Shape {
    val dimension: Int
    val size: Int

    operator fun get(index: Int): Int
    fun index(vector: IntArray): Int
    fun vector(index: Int): IntArray
}

data class Shape1(private val d1: Int) : Shape {
    override val dimension = 1
    override val size get() = d1

    @Throws(ArrayIndexOutOfBoundsException::class)
    override operator fun get(index: Int): Int {
        return when (index) {
            1 -> d1
            else -> throw ArrayIndexOutOfBoundsException("Total size is $d1, but it get $index")
        }
    }

    @Throws(DimensionMismatchingException::class)
    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            1 -> if (vector[0] > d1) {
                throw OutOfShapeException(1, vector[0], d1)
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
    private val totalSize = d1 * d2
    override val dimension = 2
    override val size get() = totalSize

    override operator fun get(index: Int): Int {
        return when (index) {
            1 -> d1
            2 -> d2
            else -> throw ArrayIndexOutOfBoundsException("")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            2 -> if (vector[0] > d1) {
                throw OutOfShapeException(1, vector[0], d1)
            } else if (vector[1] > d2) {
                throw OutOfShapeException(2, vector[1], d2)
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
            1 -> d1
            2 -> d2
            3 -> d3
            else -> throw ArrayIndexOutOfBoundsException("")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            3 -> if (vector[0] > d1) {
                throw OutOfShapeException(1, vector[0], d1)
            } else if (vector[1] > d2) {
                throw OutOfShapeException(2, vector[1], d2)
            } else if (vector[2] > d3) {
                throw OutOfShapeException(3, vector[2], d3)
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
            1 -> d1
            2 -> d2
            3 -> d3
            4 -> d4
            else -> throw ArrayIndexOutOfBoundsException("")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            4 -> if (vector[0] > d1) {
                throw OutOfShapeException(1, vector[0], d1)
            } else if (vector[1] > d2) {
                throw OutOfShapeException(2, vector[1], d2)
            } else if (vector[2] > d3) {
                throw OutOfShapeException(3, vector[2], d3)
            } else if (vector[3] > d4) {
                throw OutOfShapeException(4, vector[3], d4)
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
                throw OutOfShapeException(i + 1, vector[i], shape[i])
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
