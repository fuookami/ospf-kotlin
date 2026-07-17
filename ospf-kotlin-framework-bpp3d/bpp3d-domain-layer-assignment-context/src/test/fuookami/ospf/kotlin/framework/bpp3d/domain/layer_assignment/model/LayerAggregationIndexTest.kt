package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.asContainer3Shape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.binTypeIdOf

class LayerAggregationIndexTest {
    @Test
    fun addColumnsShouldRefreshIndexesWhenLayersAreReusedByNextModel() = runBlocking {
        val firstLayer = layer("BIN-1", FltX.one)
        val secondLayer = layer("BIN-2", FltX(2.0))
        LayerAggregation().addColumns(listOf(firstLayer))

        val added = LayerAggregation().addColumns(listOf(firstLayer, secondLayer))

        assertEquals(2, added.size)
        assertEquals(setOf(0, 1), added.map { it.index }.toSet())
    }

    private fun layer(code: String, width: FltX): BinLayer {
        val bin = BinType(
            width = width * Meter,
            height = FltX.one * Meter,
            depth = FltX.one * Meter,
            capacity = FltX(100.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf(code)
        )
        return BinLayer(
            iteration = Int64.zero,
            from = LayerAggregationIndexTest::class,
            bin = bin,
            shape = Container3Shape(bin.asContainer3Shape()),
            units = emptyList()
        )
    }
}
