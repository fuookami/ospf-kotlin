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

enum class StorageOrder {
    RowMajor,
    ColumnMajor;

    companion object {
        val Default = RowMajor
    }
}

interface Shape {
    val dimension: Int

    val udimension: UInt64 get() = UInt64(dimension)

    val size: Int

    val usize: UInt64 get() = UInt64(size)

    val indices: IntRange get() = 0 until dimension

    val storageOrder: StorageOrder get() = StorageOrder.Default

    val offsets: IntArray

    operator fun get(index: Int): Int

    fun index(vector: IntArray): Int

    fun vector(index: Int): IntArray

    fun isEmpty(): Boolean = size == 0

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

    fun offset(dimension: Int): Int {
        if (dimension >= this.dimension || dimension < 0) {
            throw DimensionMismatchingException(this.dimension, dimension)
        }
        return offsets[dimension]
    }

    fun actualIndex(dimension: Int, index: Int): Int? {
        val len = try {
            this[dimension]
        } catch (e: ArrayIndexOutOfBoundsException) {
            return null
        }

        return when {
            index >= len || index < -len -> null
            index >= 0 -> index
            else -> len + index
        }
    }

    fun dummyVector(vararg v: Any): DummyVector {
        if (v.size != dimension) {
            throw DimensionMismatchingException(
                dimension = dimension,
                vectorDimension = v.size
            )
        }
        val vector = ArrayList<DummyIndex>()
        for (i in indices) {
            when (val index = v[i]) {
                _a -> {
                    vector.add(DummyIndex(null))
                }

                is IntRange -> {
                    vector.add(
                        DummyIndex(
                            ValueRange(
                                UInt64(index.first),
                                UInt64(index.last + 1)
                            ).value!!
                        )
                    )
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
                    vector.add(
                        DummyIndex(
                            ValueRange(
                                UInt64(index),
                                UInt64(index)
                            ).value!!
                        )
                    )
                }

                is Indexed -> {
                    vector.add(
                        DummyIndex(
                            ValueRange(
                                UInt64(index.index),
                                UInt64(index.index)
                            ).value!!
                        )
                    )
                }

                is Integer<*> -> {
                    vector.add(
                        DummyIndex(
                            ValueRange(
                                (index as RealNumber<*>).toUInt64(),
                                (index as RealNumber<*>).toUInt64() + UInt64.one
                            ).value!!
                        )
                    )
                }

                else -> {
                    throw UnknownDummyIndexTypeException(
                        cls = index.javaClass.kotlin
                    )
                }
            }
        }
        return vector
    }
}

data class Shape1 private constructor(
    private val d1: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        operator fun invoke(d1: Int): Shape1 = Shape1(d1, StorageOrder.Default)

        operator fun invoke(d1: UInt64): Shape1 = Shape1(d1.toInt(), StorageOrder.Default)

        operator fun invoke(d1: Collection<*>): Shape1 = Shape1(d1.size, StorageOrder.Default)

        fun withOrder(d1: Int, order: StorageOrder): Shape1 = Shape1(d1, order)

        fun withOrder(d1: UInt64, order: StorageOrder): Shape1 = Shape1(d1.toInt(), order)
    }

    override val dimension = 1
    override val size by ::d1

    override val offsets: IntArray by lazy { intArrayOf(1) }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            else -> throw ArrayIndexOutOfBoundsException("Dimension index $index out of bounds for shape dimension $dimension")
        }
    }

    @Throws(DimensionMismatchingException::class, OutOfShapeException::class)
    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            1 -> if (vector[0] < 0 || vector[0] >= d1) {
                throw OutOfShapeException(
                    dimension = 0,
                    length = d1,
                    vectorIndex = vector[0]
                )
            } else {
                vector[0] * offsets[0]
            }

            else -> throw DimensionMismatchingException(
                dimension = 1,
                vectorDimension = vector.size
            )
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= d1) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $d1")
        } else {
            intArrayOf(index / offsets[0])
        }
    }

    fun withStorageOrder(order: StorageOrder): Shape1 = Shape1(d1, order)
}

data class Shape2 private constructor(
    private val d1: Int,
    private val d2: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        operator fun invoke(d1: Int, d2: Int): Shape2 = Shape2(d1, d2, StorageOrder.Default)

        operator fun invoke(d1: UInt64, d2: UInt64): Shape2 = Shape2(d1.toInt(), d2.toInt(), StorageOrder.Default)

        operator fun invoke(d1: Collection<*>, d2: Collection<*>): Shape2 = Shape2(d1.size, d2.size, StorageOrder.Default)

        fun withOrder(d1: Int, d2: Int, order: StorageOrder): Shape2 = Shape2(d1, d2, order)

        fun withOrder(d1: UInt64, d2: UInt64, order: StorageOrder): Shape2 = Shape2(d1.toInt(), d2.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 }
    override val dimension = 2
    override val size get() = totalSize

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1)
        }
    }

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            else -> throw ArrayIndexOutOfBoundsException("Dimension index $index out of bounds for shape dimension $dimension")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            2 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    throw OutOfShapeException(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    throw OutOfShapeException(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else {
                    vector[0] * offsets[0] + vector[1] * offsets[1]
                }
            }

            else -> throw DimensionMismatchingException(
                dimension = 2,
                vectorDimension = vector.size
            )
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= totalSize) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $totalSize")
        } else {
            intArrayOf(index / offsets[0], index % offsets[0] / offsets[1])
        }
    }

    fun withStorageOrder(order: StorageOrder): Shape2 = Shape2(d1, d2, order)
}

data class Shape3 private constructor(
    private val d1: Int,
    private val d2: Int,
    private val d3: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        operator fun invoke(d1: Int, d2: Int, d3: Int): Shape3 = Shape3(d1, d2, d3, StorageOrder.Default)

        operator fun invoke(d1: UInt64, d2: UInt64, d3: UInt64): Shape3 = Shape3(d1.toInt(), d2.toInt(), d3.toInt(), StorageOrder.Default)

        operator fun invoke(d1: Collection<*>, d2: Collection<*>, d3: Collection<*>): Shape3 = Shape3(d1.size, d2.size, d3.size, StorageOrder.Default)

        fun withOrder(d1: Int, d2: Int, d3: Int, order: StorageOrder): Shape3 = Shape3(d1, d2, d3, order)

        fun withOrder(d1: UInt64, d2: UInt64, d3: UInt64, order: StorageOrder): Shape3 = Shape3(d1.toInt(), d2.toInt(), d3.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 * d3 }
    override val dimension = 3
    override val size get() = totalSize

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2 * d3, d3, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1, d1 * d2)
        }
    }

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            2 -> d3
            else -> throw ArrayIndexOutOfBoundsException("Dimension index $index out of bounds for shape dimension $dimension")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            3 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    throw OutOfShapeException(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    throw OutOfShapeException(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else if (vector[2] < 0 || vector[2] >= d3) {
                    throw OutOfShapeException(
                        dimension = 2,
                        length = d3,
                        vectorIndex = vector[2]
                    )
                } else {
                    vector[0] * offsets[0] + vector[1] * offsets[1] + vector[2] * offsets[2]
                }
            }

            else -> throw DimensionMismatchingException(
                dimension = 3,
                vectorDimension = vector.size
            )
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= totalSize) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $totalSize")
        } else {
            var currentIndex = index
            intArrayOf(
                currentIndex / offsets[0],
                currentIndex % offsets[0] / offsets[1],
                currentIndex % offsets[1]
            )
        }
    }

    /**
     * Create a new shape with a different storage order / 创建具有不同存储顺序的新形状
     */
    fun withStorageOrder(order: StorageOrder): Shape3 = Shape3(d1, d2, d3, order)
}

data class Shape4 private constructor(
    private val d1: Int,
    private val d2: Int,
    private val d3: Int,
    private val d4: Int,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        operator fun invoke(d1: Int, d2: Int, d3: Int, d4: Int): Shape4 = Shape4(d1, d2, d3, d4, StorageOrder.Default)

        operator fun invoke(d1: UInt64, d2: UInt64, d3: UInt64, d4: UInt64): Shape4 = Shape4(d1.toInt(), d2.toInt(), d3.toInt(), d4.toInt(), StorageOrder.Default)

        operator fun invoke(d1: Collection<*>, d2: Collection<*>, d3: Collection<*>, d4: Collection<*>): Shape4 = Shape4(d1.size, d2.size, d3.size, d4.size, StorageOrder.Default)

        fun withOrder(d1: Int, d2: Int, d3: Int, d4: Int, order: StorageOrder): Shape4 = Shape4(d1, d2, d3, d4, order)

        fun withOrder(d1: UInt64, d2: UInt64, d3: UInt64, d4: UInt64, order: StorageOrder): Shape4 = Shape4(d1.toInt(), d2.toInt(), d3.toInt(), d4.toInt(), order)
    }

    private val totalSize by lazy { d1 * d2 * d3 * d4 }
    override val dimension = 4
    override val size get() = totalSize

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> intArrayOf(d2 * d3 * d4, d3 * d4, d4, 1)
            StorageOrder.ColumnMajor -> intArrayOf(1, d1, d1 * d2, d1 * d2 * d3)
        }
    }

    override operator fun get(index: Int): Int {
        return when (index) {
            0 -> d1
            1 -> d2
            2 -> d3
            3 -> d4
            else -> throw ArrayIndexOutOfBoundsException("Dimension index $index out of bounds for shape dimension $dimension")
        }
    }

    override fun index(vector: IntArray): Int {
        return when (vector.size) {
            4 -> {
                if (vector[0] < 0 || vector[0] >= d1) {
                    throw OutOfShapeException(
                        dimension = 0,
                        length = d1,
                        vectorIndex = vector[0]
                    )
                } else if (vector[1] < 0 || vector[1] >= d2) {
                    throw OutOfShapeException(
                        dimension = 1,
                        length = d2,
                        vectorIndex = vector[1]
                    )
                } else if (vector[2] < 0 || vector[2] >= d3) {
                    throw OutOfShapeException(
                        dimension = 2,
                        length = d3,
                        vectorIndex = vector[2]
                    )
                } else if (vector[3] < 0 || vector[3] >= d4) {
                    throw OutOfShapeException(
                        dimension = 3,
                        length = d4,
                        vectorIndex = vector[3]
                    )
                } else {
                    vector[0] * offsets[0] + vector[1] * offsets[1] + vector[2] * offsets[2] + vector[3] * offsets[3]
                }
            }

            else -> throw DimensionMismatchingException(
                dimension = 4,
                vectorDimension = vector.size
            )
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= totalSize) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $totalSize")
        } else {
            var currentIndex = index
            intArrayOf(
                currentIndex / offsets[0],
                currentIndex % offsets[0] / offsets[1],
                currentIndex % offsets[1] / offsets[2],
                currentIndex % offsets[2]
            )
        }
    }

    fun withStorageOrder(order: StorageOrder): Shape4 = Shape4(d1, d2, d3, d4, order)
}

data class DynShape private constructor(
    private val shape: IntArray,
    override val storageOrder: StorageOrder
) : Shape {
    companion object {
        @JvmStatic
        private fun calculateTotalSize(shape: IntArray): Int {
            var ret = 1
            for (l in shape) {
                ret *= l
            }
            return ret
        }

        @JvmStatic
        private fun calculateOffsetsRowMajor(shape: IntArray): IntArray {
            if (shape.isEmpty()) return intArrayOf()
            val offsets = IntArray(shape.size)
            offsets[shape.size - 1] = 1
            for (i in (shape.size - 2) downTo 0) {
                offsets[i] = offsets[i + 1] * shape[i + 1]
            }
            return offsets
        }

        @JvmStatic
        private fun calculateOffsetsColumnMajor(shape: IntArray): IntArray {
            if (shape.isEmpty()) return intArrayOf()
            val offsets = IntArray(shape.size)
            offsets[0] = 1
            for (i in 1 until shape.size) {
                offsets[i] = offsets[i - 1] * shape[i - 1]
            }
            return offsets
        }

        operator fun invoke(shape: IntArray): DynShape = DynShape(shape, StorageOrder.Default)

        @JvmName("constructByUInt64List")
        operator fun invoke(shape: Iterable<UInt64>): DynShape = DynShape(shape.map { it.toInt() }.toIntArray(), StorageOrder.Default)

        @JvmName("constructByCollectionList")
        operator fun invoke(shape: Iterable<Collection<*>>): DynShape = DynShape(shape.map { it.size }.toIntArray(), StorageOrder.Default)

        fun withOrder(shape: IntArray, order: StorageOrder): DynShape = DynShape(shape, order)

        @JvmName("withOrderFromUInt64List")
        fun withOrder(shape: Iterable<UInt64>, order: StorageOrder): DynShape = DynShape(shape.map { it.toInt() }.toIntArray(), order)

        @JvmName("withOrderFromCollectionList")
        fun withOrder(shape: Iterable<Collection<*>>, order: StorageOrder): DynShape = DynShape(shape.map { it.size }.toIntArray(), order)
    }

    private val totalSize by lazy { calculateTotalSize(shape) }
    override val dimension get() = shape.size
    override val size get() = totalSize

    override val offsets: IntArray by lazy {
        when (storageOrder) {
            StorageOrder.RowMajor -> calculateOffsetsRowMajor(shape)
            StorageOrder.ColumnMajor -> calculateOffsetsColumnMajor(shape)
        }
    }

    override operator fun get(index: Int): Int {
        return shape[index]
    }

    override fun index(vector: IntArray): Int {
        if (dimension != vector.size) {
            throw DimensionMismatchingException(dimension, vector.size)
        }
        var ret = 0
        for (i in shape.indices) {
            if (vector[i] < 0 || vector[i] >= shape[i]) {
                throw OutOfShapeException(
                    dimension = i,
                    length = shape[i],
                    vectorIndex = vector[i]
                )
            }
            ret += vector[i] * offsets[i]
        }
        return ret
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun vector(index: Int): IntArray {
        return if (index < 0 || index >= totalSize) {
            throw ArrayIndexOutOfBoundsException("Index $index out of bounds for shape size $totalSize")
        } else {
            var currentIndex = index
            IntArray(dimension) { i ->
                val dim = if (storageOrder == StorageOrder.RowMajor) dimension - 1 - i else i
                val result = currentIndex / offsets[dim]
                currentIndex %= offsets[dim]
                result
            }.let { result ->
                if (storageOrder == StorageOrder.RowMajor) result.reversedArray() else result
            }
        }
    }

    fun withStorageOrder(order: StorageOrder): DynShape = DynShape(shape.copyOf(), order)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DynShape

        if (!shape.contentEquals(other.shape)) return false
        if (storageOrder != other.storageOrder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.contentHashCode()
        result = 31 * result + storageOrder.hashCode()
        return result
    }
}
