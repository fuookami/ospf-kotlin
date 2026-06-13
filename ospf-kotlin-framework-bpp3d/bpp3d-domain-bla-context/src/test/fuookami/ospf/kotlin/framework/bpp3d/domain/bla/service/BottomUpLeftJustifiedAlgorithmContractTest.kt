/**
 * 底部向上左对齐算法契约测试。
 * Bottom-up left-justified algorithm contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.bla.service

import kotlin.test.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class BottomUpLeftJustifiedAlgorithmContractTest {
    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = fltX(1.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute()
        )
    }

    @Test
    fun singleProjectionShouldProduceAPlacement() = runBlocking {
        val projection = PlaneProjection(item("item-1").view(), Bottom)
        val algorithm = BottomUpLeftJustifiedAlgorithm(
            space = Container2Shape(length = fltX(5.0) * Meter, width = fltX(5.0) * Meter, plane = Bottom),
            plane = Bottom
        )

        val guard = algorithm(listOf(projection), this)
        val result = withTimeout(5_000) { guard.receive() }
        guard.close()

        assertEquals(1, result.size)
        val placement = assertNotNull(result.first())
        assertEquals(0.0, placement.x.value.toDouble(), 1e-10)
        assertEquals(0.0, placement.y.value.toDouble(), 1e-10)
    }
}
