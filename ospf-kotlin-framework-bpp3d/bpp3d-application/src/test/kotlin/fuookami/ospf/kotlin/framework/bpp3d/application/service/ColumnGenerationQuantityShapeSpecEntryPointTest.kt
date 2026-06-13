package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.QuantityPackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.continuousRadiusSelectionResult
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.continuousRadiusSolverPrototype
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX
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

class ColumnGenerationQuantityShapeSpecEntryPointTest {
    private object Cargo : AbstractCargoAttribute

    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(fltX(0.0)),
            hangingPolicy = AbsoluteHangingPolicy(fltX(0.0)),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    @Test
    fun quantityRequestShouldKeepVerticalCylinderShapeSpecWhenMappingToModel() {
        val material = QuantityMaterial(
            no = MaterialNo("M-SHAPE-SPEC"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-SHAPE-SPEC",
            weight = FltX(0.2) * Kilogram
        )
        val item = QuantityItem(
            id = "item-shape-spec",
            name = "item-shape-spec",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(1.0) * Meter,
                    height = FltX(1.2) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer,
                    shapeSpec = QuantityPackageShapeSpec.VerticalCylinder(
                        radius = FltX(0.5) * Meter,
                        axis = Axis3.Y,
                        radiusMin = FltX(0.5) * Meter,
                        radiusMax = FltX(0.6) * Meter,
                        radiusWeightFunctionKey = "quantity-shape-spec-v1",
                        diameterMin = FltX(1.0) * Meter,
                        diameterMax = FltX(1.2) * Meter
                    )
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-SHAPE-SPEC"),
            packageAttribute = packageAttribute()
        )
        val request = ColumnGenerationQuantityApplicationRequest(
            itemDemands = listOf(item to UInt64.one)
        )

        val modelRequest = request.toModelRequest()
        val modelItem = modelRequest.itemDemands.single().first
        val cylinderSpec = modelItem.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder
        assertNotNull(cylinderSpec)
        assertTrue(cylinderSpec.radius eq (fltX(0.5) * Meter))
        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals(0, cylinderSpec.radiusCandidates.size)
        assertTrue(cylinderSpec.radiusMin!! eq (fltX(0.5) * Meter))
        assertTrue(cylinderSpec.radiusMax!! eq (fltX(0.6) * Meter))
        assertEquals("quantity-shape-spec-v1", cylinderSpec.radiusWeightFunctionKey)
        assertEquals(null, cylinderSpec.radiusStep)
        assertTrue(cylinderSpec.diameterMin!! eq (fltX(1.0) * Meter))
        assertTrue(cylinderSpec.diameterMax!! eq (fltX(1.2) * Meter))
        assertEquals(null, cylinderSpec.diameterStep)
        assertEquals(1, cylinderSpec.resolvedRadiusCandidates.size)
        val selectedRadius = assertNotNull(cylinderSpec.continuousRadiusSelectionResult())
        assertEquals("quantity-shape-spec-v1", selectedRadius.key)
        assertTrue(selectedRadius.selectedRadius eq (fltX(0.5) * Meter))
        val solverPrototype = assertNotNull(cylinderSpec.continuousRadiusSolverPrototype(source = "quantity DTO"))
        assertEquals("quantity-shape-spec-v1", solverPrototype.radiusWeightFunctionKey)
        assertEquals("cylinder_radius_quantity_DTO_quantity_shape_spec_v1_Y", solverPrototype.variableName)
        assertTrue(solverPrototype.initialRadius!! eq (fltX(0.5) * Meter))
        assertTrue(solverPrototype.isProductionReady)
    }

    @Test
    fun quantityRequestShouldKeepDepthBoundaryPolicyWhenMappingToModel() {
        val policy = DepthBoundaryLayerOrientationPolicy(
            firstLayerAllowedCylinderAxes = setOf(Axis3.Y),
            lastLayerAllowedCuboidOrientations = setOf(Orientation.Upright, Orientation.Side)
        )
        val request = ColumnGenerationQuantityApplicationRequest<FltX>(
            itemDemands = emptyList(),
            depthBoundaryLayerOrientationPolicy = policy
        )

        val modelRequest = request.toModelRequest()

        assertEquals(policy, modelRequest.depthBoundaryLayerOrientationPolicy)
        assertEquals(null, modelRequest.executorConfig.depthBoundaryLayerOrientationPolicy)
    }
}
