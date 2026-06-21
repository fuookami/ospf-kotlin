/**
 * 圆柱形状契约测试。
 * Cylinder shape contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

class CylinderShapeContractTest {
    private fun assertFailedMessage(
        result: Try,
        vararg expectedMessages: String
    ) {
        assertTrue(result is Failed)
        for (expectedMessage in expectedMessages) {
            assertTrue(result.message.contains(expectedMessage))
        }
    }

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
        val radius = FltX(0.5) * Meter
        val diameter = assertNotNull(radius + radius)
        val height = FltX(1.0) * Meter
        val shape = PackageShape(
            width = diameter,
            height = height,
            depth = diameter,
            weight = FltX(1.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis
            )
        )
        return assertNotNull(shape.toPackingShapeOrNull())
    }

    private fun cylinderItem(axis: Axis3): ActualItem {
        val radius = FltX(0.5) * Meter
        val diameter = assertNotNull(radius + radius)
        return ActualItem(
            id = "cylinder-$axis",
            name = "cylinder-$axis",
            width = diameter,
            height = FltX(1.0) * Meter,
            depth = diameter,
            weight = FltX(1.0) * Kilogram,
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
        assertTrue(requireVerifiedGeneratedCylinderCandidate(
            shape = cylinderShape(Axis3.Y),
            verifiedAxisAwareCandidate = false,
            path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
        ).ok)
        assertTrue(requireVerifiedGeneratedCylinderCandidate(
            shape = cylinderShape(Axis3.X),
            verifiedAxisAwareCandidate = true,
            path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
        ).ok)

        val result = requireVerifiedGeneratedCylinderCandidate(
            shape = cylinderShape(Axis3.Z),
            verifiedAxisAwareCandidate = false,
            path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
        )

        assertFailedMessage(
            result,
            "LayerPlacementAdapter.toLayerPlacement",
            "verified axis-aware generated candidates"
        )
    }

    @Test
    fun verticalCandidatePathShouldRejectHorizontalCylinderAxis() {
        assertTrue(requireVerticalCylinderAxis(
            shape = cylinderShape(Axis3.Y),
            path = CylinderCapabilityPath.DefaultLayerCandidate
        ).ok)

        val result = requireVerticalCylinderAxis(
            shape = cylinderShape(Axis3.X),
            path = CylinderCapabilityPath.DefaultLayerCandidate
        )

        assertFailedMessage(
            result,
            "LayerGeneration.defaultCandidate",
            "only Axis3.Y is allowed"
        )
    }

    @Test
    fun axisAwareCandidatePathShouldAcceptHorizontalCylinderAxis() {
        assertTrue(requireAxisAwareCylinderCandidate(
            shape = cylinderShape(Axis3.X),
            path = CylinderCapabilityPath.CirclePackingCandidate
        ).ok)
        assertTrue(requireAxisAwareCylinderCandidate(
            shape = cylinderShape(Axis3.Z),
            path = CylinderCapabilityPath.CirclePackingCandidate
        ).ok)
    }

    @Test
    fun supportPathShouldRejectHorizontalCylinderAxis() {
        val result = requireUprightVerticalCylinderSupport(
            shape = cylinderShape(Axis3.Z),
            orientation = Orientation.Upright,
            path = CylinderCapabilityPath.PackageAttributeSupport
        )

        assertFailedMessage(
            result,
            "PackageAttribute.supportPackingShape",
            "only upright Axis3.Y items are allowed"
        )
    }

    @Test
    fun cuboidOnlyPathShouldUseSharedCapabilityMessage() {
        val result = requireNoCylinderItemsForCuboidOnlyPath(
            items = listOf(cylinderItem(Axis3.Y)),
            path = CylinderCapabilityPath.ItemMergeBlocks
        )

        assertFailedMessage(
            result,
            "ItemMerger.mergeBlocks",
            "item merge paths are cuboid-only"
        )
    }
}
