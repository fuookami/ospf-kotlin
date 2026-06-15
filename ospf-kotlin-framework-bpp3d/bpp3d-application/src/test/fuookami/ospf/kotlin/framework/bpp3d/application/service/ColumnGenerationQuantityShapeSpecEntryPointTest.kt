/**
 * 列生成物理量形状规格入口测试。
 * Column generation quantity shape spec entry point test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class ColumnGenerationQuantityShapeSpecEntryPointTest {
    private object Cargo : AbstractCargoAttribute

    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX(0.0)),
            hangingPolicy = AbsoluteHangingPolicy(FltX(0.0)),
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
        assertTrue(cylinderSpec.radius eq (FltX(0.5) * Meter))
        assertEquals(Axis3.Y, cylinderSpec.axis)
        assertEquals(0, cylinderSpec.radiusCandidates.size)
        assertTrue(cylinderSpec.radiusMin!! eq (FltX(0.5) * Meter))
        assertTrue(cylinderSpec.radiusMax!! eq (FltX(0.6) * Meter))
        assertEquals("quantity-shape-spec-v1", cylinderSpec.radiusWeightFunctionKey)
        assertEquals(null, cylinderSpec.radiusStep)
        assertTrue(cylinderSpec.diameterMin!! eq (FltX(1.0) * Meter))
        assertTrue(cylinderSpec.diameterMax!! eq (FltX(1.2) * Meter))
        assertEquals(null, cylinderSpec.diameterStep)
        assertEquals(1, cylinderSpec.resolvedRadiusCandidates.size)
        val selectedRadius = assertNotNull(cylinderSpec.continuousRadiusSelectionResult())
        assertEquals("quantity-shape-spec-v1", selectedRadius.key)
        assertTrue(selectedRadius.selectedRadius eq (FltX(0.5) * Meter))
        val solverPrototype = assertNotNull(cylinderSpec.continuousRadiusSolverPrototype(source = "quantity DTO"))
        assertEquals("quantity-shape-spec-v1", solverPrototype.radiusWeightFunctionKey)
        assertEquals("cylinder_radius_quantity_DTO_quantity_shape_spec_v1_Y", solverPrototype.variableName)
        assertTrue(solverPrototype.initialRadius!! eq (FltX(0.5) * Meter))
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
