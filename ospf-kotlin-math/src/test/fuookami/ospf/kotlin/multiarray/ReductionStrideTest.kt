package fuookami.ospf.kotlin.multiarray

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class ReductionStrideTest {
    private fun assertEquivalent(
        expected: Ret<MultiArray<Flt64, DynShape>>,
        actual: Ret<MultiArray<Flt64, DynShape>>
    ) {
        when (expected) {
            is Ok -> {
                val value = assertIs<Ok<MultiArray<Flt64, DynShape>, *, *>>(actual).value
                assertEquals(expected.value.shape, value.shape)
                assertEquals(
                    expected.value.map { it.toDouble().toRawBits() },
                    value.map { it.toDouble().toRawBits() }
                )
            }
            is Failed -> {
                val failure = assertIs<Failed<*, *, *>>(actual)
                assertEquals(expected.error.code, failure.error.code)
                assertEquals(expected.error.message, failure.error.message)
            }
            is Fatal -> error("Unexpected reference fatal: ${expected.errors}")
        }
    }

    @Test
    fun allAxesAndStorageOrdersRetainSourceLinearFoldsAndOriginalOutputLayout() {
        val random = Random(72024)
        val samples = listOf(1e16, 1.0, -1e16, -0.0, 0.0, 3.0, -2.0, Double.POSITIVE_INFINITY, Double.NaN)
        val shapes: List<Shape> = listOf(
            Shape1(7), Shape2(3, 5), Shape3(
                d1 = 2,
                d2 = 3,
                d3 = 4
            ), Shape4(
                d1 = 2,
                d2 = 3,
                d3 = 1,
                d4 = 2
            ),
            DynShape(intArrayOf()), DynShape(intArrayOf(2, 0, 3)), DynShape(intArrayOf(2, 3, 1, 2, 2))
        )
        for (base in shapes) {
            for (storage in listOf(StorageOrder.RowMajor, StorageOrder.ColumnMajor)) {
                val dimensions = IntArray(base.dimension) { base[it] }
                val shape = when (base) {
                    is Shape1 -> base.withStorageOrder(storage)
                    is Shape2 -> base.withStorageOrder(storage)
                    is Shape3 -> base.withStorageOrder(storage)
                    is Shape4 -> base.withStorageOrder(storage)
                    else -> (base as DynShape).withStorageOrder(storage)
                }
                val array = MultiArray.newBy(shape) { _, _ -> Flt64(samples[random.nextInt(samples.size)]) }
                for (axis in dimensions.indices) {
                    assertEquivalent(
                        FastSumReference.run { array.sumAxisSafe(axis, Flt64.zero) },
                        array.sumAxisSafe(axis, Flt64.zero)
                    )
                    assertEquivalent(
                        FastSumReference.run { array.cumsumAxisSafe(axis, Flt64.zero) },
                        array.cumsumAxisSafe(axis, Flt64.zero)
                    )
                }
                for (mask in 0 until (1 shl dimensions.size)) {
                    val selected = dimensions.indices.filter { mask and (1 shl it) != 0 }.toIntArray()
                    val axes = selected + selected.reversedArray()
                    assertEquivalent(
                        FastSumReference.run { array.sumAxesSafe(axes, Flt64(-0.0)) },
                        array.sumAxesSafe(axes, Flt64(-0.0))
                    )
                }
            }
        }
    }

    @Test
    fun customCoordinateFailuresAndPublicOffsetChangesRetainFallbackBehavior() {
        val custom = object : Shape by Shape2(2, 3) {
            override fun vector(index: Int): Ret<IntArray> = Failed(ErrorCode.DataNotFound, "custom vector failure")
        }
        val modified = Shape2(2, 2).also { it.offsets[0] = 1 }
        for (shape in listOf(custom, modified)) {
            val array = MultiArray.newBy(shape) { index, _ -> Flt64(index.toDouble()) }
            for (axis in 0 until shape.dimension) {
                assertEquivalent(
                    FastSumReference.run { array.sumAxisSafe(axis, Flt64.zero) },
                    array.sumAxisSafe(axis, Flt64.zero)
                )
                assertEquivalent(
                    FastSumReference.run { array.cumsumAxisSafe(axis, Flt64.zero) },
                    array.cumsumAxisSafe(axis, Flt64.zero)
                )
            }
            for (axes in listOf(intArrayOf(), intArrayOf(0, 1, 0), intArrayOf(-1), intArrayOf(2))) {
                assertEquivalent(
                    FastSumReference.run { array.sumAxesSafe(axes, Flt64.zero) },
                    array.sumAxesSafe(axes, Flt64.zero)
                )
            }
        }
    }
}
