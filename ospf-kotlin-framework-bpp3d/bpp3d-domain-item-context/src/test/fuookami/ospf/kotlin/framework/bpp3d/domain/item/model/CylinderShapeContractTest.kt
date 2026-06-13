/**
 * 圆柱形状契约测试。
 * Cylinder shape contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

class CylinderShapeContractTest {
    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cylinderShape(axis: Axis3): PackingShape3<FltX> {
        val radius = fltX(0.5) * Meter
        val height = fltX(1.0) * Meter
        val shape = PackageShape(
            width = radius + radius,
            height = height,
            depth = radius + radius,
            weight = fltX(1.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis
            )
        )
        return assertNotNull(shape.toPackingShapeOrNull())
    }

    private fun cylinderItem(axis: Axis3): ActualItem {
        val radius = fltX(0.5) * Meter
        return ActualItem(
            id = "cylinder-$axis",
            name = "cylinder-$axis",
            width = radius + radius,
            height = fltX(1.0) * Meter,
            depth = radius + radius,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-CYL-$axis"),
            packageAttribute = packageAttribute(),
            shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis
            )
        )
    }

    @Test
    fun capabilityMatrixShouldClassifyEveryPath() {
        val cuboidOnly = CylinderCapabilityPath.entries.filter { path ->
            path.status == CylinderCapabilityStatus.CuboidOnly
        }
        val generated = CylinderCapabilityPath.entries.filter { path ->
            path.status == CylinderCapabilityStatus.VerticalCandidateOnly
        }
        val axisAware = CylinderCapabilityPath.entries.filter { path ->
            path.status == CylinderCapabilityStatus.AxisAwareCandidate
        }
        val verifiedPlacement = CylinderCapabilityPath.entries.filter { path ->
            path.status == CylinderCapabilityStatus.VerifiedGeneratedPlacement
        }
        val support = CylinderCapabilityPath.entries.filter { path ->
            path.status == CylinderCapabilityStatus.UprightVerticalSupportOnly
        }
        val finalValidation = CylinderCapabilityPath.entries.filter { path ->
            path.status == CylinderCapabilityStatus.KnownCoordinateFinalValidation
        }

        assertTrue(cuboidOnly.isNotEmpty())
        assertTrue(cuboidOnly.all { path -> path.pathPredicate != null })
        assertTrue(generated.any { path -> path == CylinderCapabilityPath.DefaultLayerCandidate })
        assertTrue(axisAware.any { path -> path == CylinderCapabilityPath.CirclePackingCandidate })
        assertEquals(
            expected = setOf(CylinderCapabilityPath.ApplicationLayerPlacementCandidate),
            actual = verifiedPlacement.toSet()
        )
        assertTrue(support.any { path -> path == CylinderCapabilityPath.PackageAttributeSupport })
        assertEquals(
            expected = setOf(
                CylinderCapabilityPath.KnownCoordinateFinalPacking,
                CylinderCapabilityPath.RendererFinalPacking
            ),
            actual = finalValidation.toSet()
        )
    }

    @Test
    fun verifiedGeneratedPlacementShouldRejectUnverifiedHorizontalCylinderCandidate() {
        requireVerifiedGeneratedCylinderCandidate(
            shape = cylinderShape(Axis3.Y),
            verifiedAxisAwareCandidate = false,
            path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
        )
        requireVerifiedGeneratedCylinderCandidate(
            shape = cylinderShape(Axis3.X),
            verifiedAxisAwareCandidate = true,
            path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
        )

        val error = assertFailsWith<IllegalArgumentException> {
            requireVerifiedGeneratedCylinderCandidate(
                shape = cylinderShape(Axis3.Z),
                verifiedAxisAwareCandidate = false,
                path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
            )
        }

        assertTrue(error.message?.contains("LayerPlacementAdapter.toLayerPlacement") == true)
        assertTrue(error.message?.contains("verified axis-aware generated candidates") == true)
    }

    @Test
    fun verticalCandidatePathShouldRejectHorizontalCylinderAxis() {
        requireVerticalCylinderAxis(
            shape = cylinderShape(Axis3.Y),
            path = CylinderCapabilityPath.DefaultLayerCandidate
        )

        val error = assertFailsWith<IllegalArgumentException> {
            requireVerticalCylinderAxis(
                shape = cylinderShape(Axis3.X),
                path = CylinderCapabilityPath.DefaultLayerCandidate
            )
        }

        assertTrue(error.message?.contains("LayerGeneration.defaultCandidate") == true)
        assertTrue(error.message?.contains("only Axis3.Y is allowed") == true)
    }

    @Test
    fun axisAwareCandidatePathShouldAcceptHorizontalCylinderAxis() {
        requireAxisAwareCylinderCandidate(
            shape = cylinderShape(Axis3.X),
            path = CylinderCapabilityPath.CirclePackingCandidate
        )
        requireAxisAwareCylinderCandidate(
            shape = cylinderShape(Axis3.Z),
            path = CylinderCapabilityPath.CirclePackingCandidate
        )
    }

    @Test
    fun supportPathShouldRejectHorizontalCylinderAxis() {
        val error = assertFailsWith<IllegalArgumentException> {
            requireUprightVerticalCylinderSupport(
                shape = cylinderShape(Axis3.Z),
                orientation = Orientation.Upright,
                path = CylinderCapabilityPath.PackageAttributeSupport
            )
        }

        assertTrue(error.message?.contains("PackageAttribute.supportPackingShape") == true)
        assertTrue(error.message?.contains("only upright Axis3.Y items are allowed") == true)
    }

    @Test
    fun cuboidOnlyPathShouldUseSharedCapabilityMessage() {
        val error = assertFailsWith<IllegalArgumentException> {
            requireNoCylinderItemsForCuboidOnlyPath(
                items = listOf(cylinderItem(Axis3.Y)),
                path = CylinderCapabilityPath.ItemMergeBlocks
            )
        }

        assertTrue(error.message?.contains("ItemMerger.mergeBlocks") == true)
        assertTrue(error.message?.contains("item merge paths are cuboid-only") == true)
    }
}
