package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantityDomainAliasExampleTest {
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

    @Test
    fun flt64AliasShouldBuildGenericDomainObjects() {
        val material: Flt64Material = Material(
            no = MaterialNo("MAT-64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "MAT-64",
            weight = 0.5 * Kilogram
        )
        val pack: Flt64Package = Package.innerPackage(
            shape = PackageShape(
                width = 1.0 * Meter,
                height = 2.0 * Meter,
                depth = 3.0 * Meter,
                weight = 1.2 * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64(2))
        )
        val item: Flt64Item = Item(
            id = "flt64-item",
            name = "flt64-item",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("BATCH-64"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer: Flt64BinLayer = BinLayer(
            iteration = Int64.zero,
            from = QuantityDomainAliasExampleTest::class,
            width = 5.0 * Meter,
            height = 5.0 * Meter,
            depth = 5.0 * Meter,
            units = listOf(
                ItemPlacement(item, 0.0 * Meter, 0.0 * Meter, 0.0 * Meter)
            )
        )

        assertTrue(layer.width eq (5.0 * Meter))
        assertEquals(1, layer.units.size)
        assertTrue(layer.toLegacy().shape.width eq (5.0 * Meter))
    }

    @Test
    fun fltXAliasShouldBuildGenericDomainObjects() {
        val material: FltXMaterial = Material(
            no = MaterialNo("MAT-X"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "MAT-X",
            weight = FltX(0.5) * Kilogram
        )
        val pack: FltXPackage = Package.innerPackage(
            shape = PackageShape(
                width = FltX.one * Meter,
                height = FltX(2.0) * Meter,
                depth = FltX(3.0) * Meter,
                weight = FltX(1.2) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64(2))
        )
        val item: FltXItem = Item(
            id = "fltx-item",
            name = "fltx-item",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("BATCH-X"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer: FltXBinLayer = BinLayer(
            iteration = Int64.zero,
            from = QuantityDomainAliasExampleTest::class,
            width = FltX(5.0) * Meter,
            height = FltX(5.0) * Meter,
            depth = FltX(5.0) * Meter,
            units = listOf(
                ItemPlacement(item, FltX.zero * Meter, FltX.zero * Meter, FltX.zero * Meter)
            )
        )

        assertEquals(Meter, layer.width.unit)
        assertEquals(5.0, layer.width.value.toDouble(), 1e-10)
        assertEquals(1, layer.units.size)
        assertTrue(layer.toLegacy().shape.width eq (5.0 * Meter))
    }
}
