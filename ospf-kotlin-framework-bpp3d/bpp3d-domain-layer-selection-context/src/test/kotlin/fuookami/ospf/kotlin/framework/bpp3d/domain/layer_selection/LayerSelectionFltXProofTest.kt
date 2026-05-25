package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_selection

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Package as GenericPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.PackageShape as GenericPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_selection.service.ColumnGenerationAlgorithm
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LayerSelectionFltXProofTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(Flt64.zero),
            hangingPolicy = AbsoluteHangingPolicy(Flt64.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun q(value: Double, unit: PhysicalUnit): Quantity<FltX> {
        return FltX(value) * unit
    }

    @Test
    fun fltXStatisticsShouldBeAvailableInLayerSelectionContext() {
        val context = LayerSelectionContext()
        val algorithm = ColumnGenerationAlgorithm()
        assertNotNull(context)
        assertNotNull(algorithm)

        val material = GenericMaterial(
            no = MaterialNo("M-LS"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-LS",
            weight = q(0.4, Kilogram)
        )
        val shape = GenericPackageShape(
            width = q(1.1, Meter),
            height = q(0.9, Meter),
            depth = q(0.7, Meter),
            weight = q(0.2, Kilogram),
            packageType = PackageType.CartonContainer
        )
        val pack = GenericPackage.innerPackage(
            shape = shape,
            materials = mapOf(material to UInt64(1))
        )
        val item = GenericItem(
            id = "item-ls",
            name = "item-ls",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-LS"),
            packageAttribute = defaultPackageAttribute()
        )

        val stats = item.statistics(
            mode = Bpp3dDemandMode.ItemMaterialWeight,
            amount = UInt64(2)
        )
        assertEquals(1, stats.size)
    }
}

