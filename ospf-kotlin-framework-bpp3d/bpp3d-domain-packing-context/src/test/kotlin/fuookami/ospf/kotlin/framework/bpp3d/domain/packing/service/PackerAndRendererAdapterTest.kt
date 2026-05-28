package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingContext
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackerAndRendererAdapterTest {
    private object CargoAttr : fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute

    private fun packageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String, material: Material): ActualItem {
        val pack = Package.innerPackage(
            shape = PackageShape(
                width = infraScalar(1.0) * Meter,
                height = infraScalar(1.0) * Meter,
                depth = infraScalar(1.0) * Meter,
                weight = infraScalar(1.0) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64.one)
        )
        return ActualItem(
            id = id,
            name = id,
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = packageAttribute()
        )
    }

    private fun layerBin(items: List<ActualItem>): Bin<BinLayer> {
        val binType = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(3.0) * Meter,
            depth = infraScalar(3.0) * Meter,
            capacity = infraScalar(100.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-A"
        )
        val placements = items.mapIndexed { index, item ->
            QuantityPlacement3(
                view = item.view(Orientation.Upright),
                position = point3(
                    x = infraScalar(index.toDouble()) * Meter,
                    y = infraScalar(0.0) * Meter,
                    z = infraScalar(0.0) * Meter
                )
            )
        }
        val layer = BinLayer(
            iteration = Int64.zero,
            from = PackerAndRendererAdapterTest::class,
            bin = binType,
            shape = Container3Shape(binType),
            units = placements
        )
        return Bin(
            shape = binType,
            units = listOf(
                QuantityPlacement3(
                    view = layer.view(Orientation.Upright)!!,
                    position = point3()
                )
            )
        )
    }

    @Test
    fun packerShouldSummarizeMaterialsAndRendererShouldBuildSchema() = runBlocking {
        val material = Material(
            no = MaterialNo("M-1"),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = "M-1",
            weight = infraScalar(0.5) * Kilogram
        )
        val bin = layerBin(listOf(item("item-1", material), item("item-2", material)))

        val result = Packer().invoke(
            bins = listOf(bin),
            context = PackingContext(info = mapOf("source" to "unit-test"))
        )
        assertEquals(1, result.materialSummary.size)
        assertEquals(UInt64(2), result.materialSummary.first().amount)

        val schema = PackingRendererAdapter().toSchema(result)
        assertEquals("1", schema.kpi["bin_count"])
        assertEquals("1", schema.kpi["material_count"])
        assertEquals(1, schema.loadingPlans.size)
        assertEquals(2, schema.loadingPlans.first().items.size)
        assertTrue(schema.loadingPlans.first().loadingRate > InfraNumber.zero)
    }
}
