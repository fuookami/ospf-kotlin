/**
 * 多箱型请求契约测试。 / Multi bin type request contract test.
 *
 * 验证请求能携带全部候选箱型，并验证箱型参与列去重（同几何不同箱型视为不同列）。
 * / Verifies that a request carries every candidate bin type, and that the bin type participates in column
 * deduplication (identical geometry on different bin types counts as different columns).
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.asContainer3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.binTypeIdOf

class MultiBinTypeRequestContractTest {

    /**
     * 构造箱型。 / Build a bin type.
     *
     * @param code 箱型编码 / bin type code
     * @param size 长宽高尺寸 / size applied to every axis
     * @param isMain 是否主板型 / whether it is the main bin type
     * @return 箱型 / bin type
    */
    private fun binType(code: String, size: Double, isMain: Boolean = false): BinType<FltX> {
        return BinType(
            width = FltX(size) * Meter,
            height = FltX(size) * Meter,
            depth = FltX(size) * Meter,
            capacity = FltX(10000.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf(code),
            isMain = isMain
        )
    }

    /**
     * 构造属于指定箱型的空层。 / Build an empty layer belonging to the given bin type.
     *
     * @param bin 箱型 / bin type
     * @return 层 / layer
    */
    private fun layer(bin: BinType<FltX>): BinLayer {
        return BinLayer(
            iteration = Int64.zero,
            from = MultiBinTypeRequestContractTest::class,
            bin = bin,
            shape = Container3Shape(bin.asContainer3Shape()),
            units = emptyList()
        )
    }

    @Test
    fun requestCarriesEveryBinTypeInOrder() {
        val big = binType("BIG", 2.0, isMain = true)
        val small = binType("SMALL", 1.0)
        val request = bpp3dLayerGenerationRequest<Nothing>(
            iteration = 0,
            bin = big,
            bins = listOf(big, small),
            items = emptyList()
        )
        assertEquals(2, request.bins.size)
        assertEquals(listOf("BIG", "SMALL"), request.bins.map { it.typeCode.toString() })
        assertEquals("BIG", request.bin?.typeCode?.toString())
    }

    @Test
    fun emptyBinsFallsBackToSingleBin() {
        val big = binType("BIG", 2.0, isMain = true)
        val request = bpp3dLayerGenerationRequest<Nothing>(
            iteration = 0,
            bin = big,
            items = emptyList()
        )
        assertEquals(0, request.bins.size)
        assertEquals("BIG", request.bin?.typeCode?.toString())
    }

    @Test
    fun sameGeometryOnDifferentBinTypesIsNotDeduplicated() {
        // 尺寸相同、仅编码不同的两个箱型：shape 与 units 相等，只有箱型能区分身份。
        // Two bin types with identical dimensions differing only by code: shape and units are equal, so only the bin
        // type can distinguish identity.
        val first = binType("TYPE-A", 2.0)
        val second = binType("TYPE-B", 2.0)
        assertNotEquals(layer(first), layer(second))
        assertEquals(layer(first), layer(first))
    }
}
