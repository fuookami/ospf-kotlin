package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar

class CylinderShapeContractTest {
    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(InfraNumber.zero),
            hangingPolicy = AbsoluteHangingPolicy(InfraNumber.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun cylinderShape(axis: Axis3): PackingShape3<InfraNumber> {
        val radius = infraScalar(0.5) * Meter
        val height = infraScalar(1.0) * Meter
        val shape = PackageShape(
            width = radius + radius,
            height = height,
            depth = radius + radius,
            weight = infraScalar(1.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = radius,
                axis = axis
            )
        )
        return assertNotNull(shape.toPackingShapeOrNull())
    }

    private fun cylinderItem(axis: Axis3): ActualItem {
        val radius = infraScalar(0.5) * Meter
        return ActualItem(
            id = "cylinder-$axis",
            name = "cylinder-$axis",
            width = radius + radius,
            height = infraScalar(1.0) * Meter,
            depth = radius + radius,
            weight = infraScalar(1.0) * Kilogram,
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
        val support = CylinderCapabilityPath.entries.filter { path ->
            path.status == CylinderCapabilityStatus.UprightVerticalSupportOnly
        }
        val finalValidation = CylinderCapabilityPath.entries.filter { path ->
            path.status == CylinderCapabilityStatus.KnownCoordinateFinalValidation
        }

        assertTrue(cuboidOnly.isNotEmpty())
        assertTrue(cuboidOnly.all { path -> path.pathPredicate != null })
        assertTrue(generated.any { path -> path == CylinderCapabilityPath.CirclePackingCandidate })
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
    fun verticalCandidatePathShouldRejectHorizontalCylinderAxis() {
        requireVerticalCylinderAxis(
            shape = cylinderShape(Axis3.Y),
            path = CylinderCapabilityPath.CirclePackingCandidate
        )

        val error = assertFailsWith<IllegalArgumentException> {
            requireVerticalCylinderAxis(
                shape = cylinderShape(Axis3.X),
                path = CylinderCapabilityPath.CirclePackingCandidate
            )
        }

        assertTrue(error.message?.contains("CirclePackingLayerGenerator") == true)
        assertTrue(error.message?.contains("only Axis3.Y is allowed") == true)
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
