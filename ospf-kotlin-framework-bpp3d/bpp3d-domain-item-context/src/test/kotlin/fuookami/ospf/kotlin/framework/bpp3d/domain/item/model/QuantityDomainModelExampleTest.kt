package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuantityDomainModelExampleTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    @Test
    fun infraNumberQuantityDomainObjectsShouldBuildDirectly() {
        val material: QuantityMaterial<FltX> = QuantityMaterial(
            no = MaterialNo("MAT-64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "MAT-64",
            weight = fltX(0.5) * Kilogram
        )
        val pack: QuantityPackage<FltX> = QuantityPackage.innerPackage(
            shape = QuantityPackageShape(
                width = fltX(1.0) * Meter,
                height = fltX(2.0) * Meter,
                depth = fltX(3.0) * Meter,
                weight = fltX(1.2) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64(2))
        )
        val item: QuantityItem<FltX> = QuantityItem(
            id = "FltX-item",
            name = "FltX-item",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("BATCH-64"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer: QuantityBinLayer<FltX> = QuantityBinLayer(
            iteration = Int64.zero,
            from = QuantityDomainModelExampleTest::class,
            width = fltX(5.0) * Meter,
            height = fltX(5.0) * Meter,
            depth = fltX(5.0) * Meter,
            units = listOf(
                QuantityItemPlacement(item, fltX(0.0) * Meter, fltX(0.0) * Meter, fltX(0.0) * Meter)
            )
        )

        assertTrue(layer.width eq (fltX(5.0) * Meter))
        assertEquals(1, layer.units.size)
        assertTrue(layer.toModel().shape.width eq (fltX(5.0) * Meter))
    }

    @Test
    fun fltXQuantityDomainObjectsShouldBuildDirectly() {
        val material: QuantityMaterial<FltX> = QuantityMaterial(
            no = MaterialNo("MAT-X"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "MAT-X",
            weight = FltX(0.5) * Kilogram
        )
        val pack: QuantityPackage<FltX> = QuantityPackage.innerPackage(
            shape = QuantityPackageShape(
                width = FltX.one * Meter,
                height = FltX(2.0) * Meter,
                depth = FltX(3.0) * Meter,
                weight = FltX(1.2) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64(2))
        )
        val item: QuantityItem<FltX> = QuantityItem(
            id = "fltx-item",
            name = "fltx-item",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("BATCH-X"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer: QuantityBinLayer<FltX> = QuantityBinLayer(
            iteration = Int64.zero,
            from = QuantityDomainModelExampleTest::class,
            width = FltX(5.0) * Meter,
            height = FltX(5.0) * Meter,
            depth = FltX(5.0) * Meter,
            units = listOf(
                QuantityItemPlacement(item, FltX.zero * Meter, FltX.zero * Meter, FltX.zero * Meter)
            )
        )

        assertEquals(Meter, layer.width.unit)
        assertEquals(5.0, layer.width.value.toDouble(), 1e-10)
        assertEquals(1, layer.units.size)
        assertTrue(layer.toModel().shape.width eq (fltX(5.0) * Meter))
    }

    @Test
    fun quantityPackageShapeShouldMapVerticalCylinderSpecToModel() {
        val quantityShape = QuantityPackageShape(
            width = FltX(1.0) * Meter,
            height = FltX(1.2) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(1.5) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = QuantityPackageShapeSpec.VerticalCylinder(
                radius = FltX(0.5) * Meter,
                axis = Axis3.Y,
                radiusMin = FltX(0.5) * Meter,
                radiusMax = FltX(0.6) * Meter,
                radiusWeightFunctionKey = "radius-weight-v1",
                diameterMin = FltX(1.0) * Meter,
                diameterMax = FltX(1.2) * Meter
            )
        )

        val modelShape = quantityShape.toModel()
        val cylinderSpec = modelShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
        assertNotNull(cylinderSpec)
        assertTrue(cylinderSpec.radius eq (fltX(0.5) * Meter))
        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals("radius-weight-v1", cylinderSpec.radiusWeightFunctionKey)
        assertTrue(cylinderSpec.diameterMin!! eq (fltX(1.0) * Meter))
        assertTrue(cylinderSpec.diameterMax!! eq (fltX(1.2) * Meter))
        assertEquals(1, cylinderSpec.resolvedRadiusCandidates.size)
    }
}
