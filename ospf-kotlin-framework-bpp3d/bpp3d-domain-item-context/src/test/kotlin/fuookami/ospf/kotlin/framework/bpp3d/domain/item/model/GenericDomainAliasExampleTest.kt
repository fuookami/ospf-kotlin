package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
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

class GenericDomainAliasExampleTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    @Test
    fun infraNumberAliasShouldBuildGenericDomainObjects() {
        val material: InfraNumberMaterial = GenericMaterial(
            no = MaterialNo("MAT-64"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "MAT-64",
            weight = infraScalar(0.5) * Kilogram
        )
        val pack: InfraNumberPackage = GenericPackage.innerPackage(
            shape = GenericPackageShape(
                width = infraScalar(1.0) * Meter,
                height = infraScalar(2.0) * Meter,
                depth = infraScalar(3.0) * Meter,
                weight = infraScalar(1.2) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64(2))
        )
        val item: InfraNumberItem = GenericItem(
            id = "InfraNumber-item",
            name = "InfraNumber-item",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("BATCH-64"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer: InfraNumberBinLayer = GenericBinLayer(
            iteration = Int64.zero,
            from = GenericDomainAliasExampleTest::class,
            width = infraScalar(5.0) * Meter,
            height = infraScalar(5.0) * Meter,
            depth = infraScalar(5.0) * Meter,
            units = listOf(
                GenericItemPlacement(item, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter, infraScalar(0.0) * Meter)
            )
        )

        assertTrue(layer.width eq (infraScalar(5.0) * Meter))
        assertEquals(1, layer.units.size)
        assertTrue(layer.toModel().shape.width eq (infraScalar(5.0) * Meter))
    }

    @Test
    fun fltXAliasShouldBuildGenericDomainObjects() {
        val material: FltXMaterial = GenericMaterial(
            no = MaterialNo("MAT-X"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "MAT-X",
            weight = FltX(0.5) * Kilogram
        )
        val pack: FltXPackage = GenericPackage.innerPackage(
            shape = GenericPackageShape(
                width = FltX.one * Meter,
                height = FltX(2.0) * Meter,
                depth = FltX(3.0) * Meter,
                weight = FltX(1.2) * Kilogram,
                packageType = PackageType.CartonContainer
            ),
            materials = mapOf(material to UInt64(2))
        )
        val item: FltXItem = GenericItem(
            id = "fltx-item",
            name = "fltx-item",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("BATCH-X"),
            packageAttribute = defaultPackageAttribute()
        )
        val layer: FltXBinLayer = GenericBinLayer(
            iteration = Int64.zero,
            from = GenericDomainAliasExampleTest::class,
            width = FltX(5.0) * Meter,
            height = FltX(5.0) * Meter,
            depth = FltX(5.0) * Meter,
            units = listOf(
                GenericItemPlacement(item, FltX.zero * Meter, FltX.zero * Meter, FltX.zero * Meter)
            )
        )

        assertEquals(Meter, layer.width.unit)
        assertEquals(5.0, layer.width.value.toDouble(), 1e-10)
        assertEquals(1, layer.units.size)
        assertTrue(layer.toModel().shape.width eq (infraScalar(5.0) * Meter))
    }

    @Test
    fun genericPackageShapeShouldMapVerticalCylinderSpecToModel() {
        val genericShape = GenericPackageShape(
            width = FltX(1.0) * Meter,
            height = FltX(1.2) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(1.5) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = GenericPackageShapeSpec.VerticalCylinder(
                radius = FltX(0.5) * Meter,
                axis = Axis3.Y,
                radiusCandidates = listOf(
                    FltX(0.5) * Meter,
                    FltX(0.6) * Meter
                ),
                radiusMin = FltX(0.5) * Meter,
                radiusMax = FltX(0.6) * Meter,
                radiusWeightFunctionKey = "radius-weight-v1",
                radiusStep = FltX(0.05) * Meter,
                diameterMin = FltX(1.0) * Meter,
                diameterMax = FltX(1.2) * Meter,
                diameterStep = FltX(0.1) * Meter
            )
        )

        val modelShape = genericShape.toModel()
        val cylinderSpec = modelShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
        assertNotNull(cylinderSpec)
        assertTrue(cylinderSpec.radius eq (infraScalar(0.5) * Meter))
        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals("radius-weight-v1", cylinderSpec.radiusWeightFunctionKey)
        assertEquals(2, cylinderSpec.radiusCandidates.size)
        assertTrue(cylinderSpec.radiusStep!! eq (infraScalar(0.05) * Meter))
        assertTrue(cylinderSpec.diameterMin!! eq (infraScalar(1.0) * Meter))
        assertTrue(cylinderSpec.diameterMax!! eq (infraScalar(1.2) * Meter))
        assertTrue(cylinderSpec.diameterStep!! eq (infraScalar(0.1) * Meter))
        assertEquals(2, cylinderSpec.resolvedRadiusCandidates.size)
    }
}
