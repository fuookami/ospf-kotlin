package fuookami.ospf.kotlin.multiarray.einsum

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class ContractPreparationTest {
    @Test
    fun outputBasePreparationPreservesOriginalLinearAddressesAndRawBits() {
        val random = Random(7124)
        val values = listOf(1e16, 1.0, -1e16, 0.0, -0.0, 2.0, -3.0, Double.POSITIVE_INFINITY)
        for (aRank in 1..3) {
            for (bRank in 1..3) {
                for (axisA in 0 until aRank) {
                    for (axisB in 0 until bRank) {
                        for (order in listOf(StorageOrder.RowMajor, StorageOrder.ColumnMajor)) {
                            val aDims = IntArray(aRank) { if (it == axisA) 5 else 2 }
                            val bDims = IntArray(bRank) { if (it == axisB) 5 else 3 }
                            val a = MultiArray.newBy(DynShape.withOrder(aDims, order)) { _, _ -> Flt64(values[random.nextInt(values.size)]) }
                            val b = MultiArray.newBy(DynShape.withOrder(bDims, order)) { _, _ -> Flt64(values[random.nextInt(values.size)]) }
                            val actual = assertIs<Ok<MultiArray<Flt64, DynShape>, *, *>>(contract(
                                a = a,
                                axisA = axisA,
                                b = b,
                                axisB = axisB,
                                zero = Flt64.zero
                            )).value
                            val outputDims = aDims.filterIndexed { index, _ -> index != axisA } +
                                bDims.filterIndexed { index, _ -> index != axisB }
                            assertEquals(outputDims, (0 until actual.shape.dimension).map { actual.shape[it] })
                            for (outLinear in 0 until actual.size) {
                                var expected = Flt64.zero
                                for (k in 0 until 5) {
                                    // 按原实现每次从输出解码行主序线性地址，含列主序输入的原有行为。
                                    // Re-decode original row-major linear addresses, including legacy column-major input behavior.
                                    val output = actual.shape.vector(outLinear).value!!
                                    var cursor = 0
                                    val aCoords = IntArray(aRank) { if (it == axisA) k else output[cursor++] }
                                    val bCoords = IntArray(bRank) { if (it == axisB) k else output[cursor++] }
                                    var aLinear = 0
                                    var bLinear = 0
                                    for (i in aDims.indices) aLinear = aLinear * aDims[i] + aCoords[i]
                                    for (i in bDims.indices) bLinear = bLinear * bDims[i] + bCoords[i]
                                    expected += a[aLinear] * b[bLinear]
                                }
                                assertEquals(expected.toDouble().toRawBits(), actual[outLinear].toDouble().toRawBits())
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun invalidAxesAndIncompatibleDimensionsRetainReturnedFailures() {
        val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
        val b = MultiArray.newWith(Shape2(4, 2), Flt64.one)
        assertIs<Failed<*, *, *>>(contract(
            a = a,
            axisA = -1,
            b = b,
            axisB = 0,
            zero = Flt64.zero
        ))
        assertIs<Failed<*, *, *>>(contract(
            a = a,
            axisA = 1,
            b = b,
            axisB = 0,
            zero = Flt64.zero
        ))
    }
}
