/**
 * 箱型混合目标契约测试。 / Mixed bin type objective contract test.
 *
 * 覆盖：余料体积口径、按箱型计价、箱型参与列去重。
 * / Covers the waste volume metric, per-bin-type pricing and bin type participating in column deduplication.
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.CubicMeter
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.asContainer3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.binTypeIdOf

class MixedBinObjectivesTest {

    /**
     * 构造箱型。 / Build a bin type.
     *
     * @param code 箱型编码 / bin type code
     * @param width 宽度 / width
     * @param height 高度 / height
     * @param depth 深度 / depth
     * @param isMain 是否主板型 / whether it is the main bin type
     * @return 箱型 / bin type
    */
    private fun binType(
        code: String,
        width: Double,
        height: Double,
        depth: Double,
        isMain: Boolean = false
    ): BinType<FltX> {
        return BinType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            depth = FltX(depth) * Meter,
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
            from = MixedBinObjectivesTest::class,
            bin = bin,
            shape = Container3Shape(bin.asContainer3Shape()),
            units = emptyList()
        )
    }

    @Test
    fun wasteVolumeIsContainerVolumeMinusLoadedVolume() {
        // 箱型 2×2×2 = 8 m³，装入 1.5 m³ 的层 → 余料 6.5 m³。
        // Container 2 by 2 by 2 equals 8 m³ with a 1.5 m³ layer loaded, so the waste is 6.5 m³.
        val type = binType("BIN-A", 2.0, 2.0, 2.0)
        val loaded = listOf<Quantity<FltX>>(FltX(1.5) * CubicMeter)
        assertEquals(8.0, type.volume.value.toDouble(), 1e-9)
        assertEquals(6.5, binWasteVolume(type, loaded).toDouble(), 1e-9)
        assertEquals(0.8125, binWasteRatio(type, loaded).toDouble(), 1e-9)
    }

    @Test
    fun wasteVolumeIsZeroWhenOverloaded() {
        val type = binType("BIN-A", 1.0, 1.0, 1.0)
        val loaded = listOf<Quantity<FltX>>(FltX(2.0) * CubicMeter)
        assertEquals(0.0, binWasteVolume(type, loaded).toDouble(), 1e-9)
    }

    @Test
    fun perTypeBinCostUsesConfiguredCostAndFallsBackToDefault() {
        val small = binType("SMALL", 1.0, 1.0, 1.0)
        val big = binType("BIG", 2.0, 2.0, 2.0, isMain = true)
        val cost = perTypeBinCost(
            typeCost = mapOf(binTypeIdOf("SMALL") to FltX(5.0)),
            defaultCost = FltX(9.0)
        )
        assertEquals(5.0, cost(small).toDouble(), 1e-9)
        assertEquals(9.0, cost(big).toDouble(), 1e-9)
    }

    @Test
    fun sameGeometryOnDifferentBinTypesIsNotDeduplicated() {
        // 两个箱型尺寸完全相同、只有编码不同，因此 shape 与 units 都相等；
        // 此时必须靠箱型区分，否则多箱型的列会被错误去重。
        // Both bin types share the same dimensions and differ only by code, so shape and units are equal; the bin
        // type must therefore decide identity, otherwise columns of different types would be deduplicated.
        val first = binType("TYPE-A", 2.0, 2.0, 2.0)
        val second = binType("TYPE-B", 2.0, 2.0, 2.0)
        assertNotEquals(layer(first), layer(second))
    }
}
