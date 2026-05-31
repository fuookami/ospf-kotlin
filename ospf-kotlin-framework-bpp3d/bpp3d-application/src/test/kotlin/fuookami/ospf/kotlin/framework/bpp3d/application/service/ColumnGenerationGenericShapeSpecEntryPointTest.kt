package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ColumnGenerationGenericShapeSpecEntryPointTest {
    private object Cargo : AbstractCargoAttribute

    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(infraScalar(0.0)),
            hangingPolicy = AbsoluteHangingPolicy(infraScalar(0.0)),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    @Test
    fun genericRequestShouldKeepVerticalCylinderShapeSpecWhenMappingToModel() {
        val material = GenericMaterial(
            no = MaterialNo("M-SHAPE-SPEC"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-SHAPE-SPEC",
            weight = FltX(0.2) * Kilogram
        )
        val item = GenericItem(
            id = "item-shape-spec",
            name = "item-shape-spec",
            pack = GenericPackage.innerPackage(
                shape = GenericPackageShape(
                    width = FltX(1.0) * Meter,
                    height = FltX(1.2) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
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
                        radiusWeightFunctionKey = "generic-shape-spec-v1"
                    )
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-SHAPE-SPEC"),
            packageAttribute = packageAttribute()
        )
        val request = ColumnGenerationGenericApplicationRequest(
            itemDemands = listOf(item to UInt64.one)
        )

        val modelRequest = request.toModelRequest()
        val modelItem = modelRequest.itemDemands.single().first
        val cylinderSpec = modelItem.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
        assertNotNull(cylinderSpec)
        assertTrue(cylinderSpec.radius eq (infraScalar(0.5) * Meter))
        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals(2, cylinderSpec.radiusCandidates.size)
        assertEquals("generic-shape-spec-v1", cylinderSpec.radiusWeightFunctionKey)
    }
}
