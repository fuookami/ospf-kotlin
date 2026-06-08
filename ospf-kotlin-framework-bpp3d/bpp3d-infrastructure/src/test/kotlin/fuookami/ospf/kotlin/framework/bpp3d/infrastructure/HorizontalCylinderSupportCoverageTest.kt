package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.geometry.Axis3

class HorizontalCylinderSupportCoverageTest {
    private fun geometry(
        minX: Double,
        maxX: Double,
        minY: Double,
        maxY: Double,
        minZ: Double,
        maxZ: Double,
        isCylinder: Boolean = false
    ): HorizontalCylinderSupportGeometry {
        return HorizontalCylinderSupportGeometry(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            minZ = minZ,
            maxZ = maxZ,
            isCylinder = isCylinder
        )
    }

    @Test
    fun floorHorizontalCylinderShouldBeAcceptedWithoutSupport() {
        val cylinder = geometry(
            minX = 0.0,
            maxX = 1.2,
            minY = 0.0,
            maxY = 1.0,
            minZ = 0.0,
            maxZ = 1.0,
            isCylinder = true
        )

        assertTrue(
            horizontalCylinderCuboidSupportCoverage(
                cylinder = cylinder,
                axis = Axis3.X,
                supports = emptyList()
            )
        )
    }

    @Test
    fun heterogeneousCuboidSupportShouldCoverHorizontalCylinderAxis() {
        val cylinder = geometry(
            minX = 0.0,
            maxX = 1.0,
            minY = 0.2,
            maxY = 1.2,
            minZ = 0.0,
            maxZ = 1.0,
            isCylinder = true
        )
        val supports = listOf(
            geometry(
                minX = 0.0,
                maxX = 0.4,
                minY = 0.0,
                maxY = 0.2,
                minZ = 0.0,
                maxZ = 1.0
            ),
            geometry(
                minX = 0.4,
                maxX = 1.0,
                minY = 0.0,
                maxY = 0.2,
                minZ = 0.0,
                maxZ = 1.0
            )
        )

        assertTrue(
            horizontalCylinderCuboidSupportCoverage(
                cylinder = cylinder,
                axis = Axis3.X,
                supports = supports
            )
        )
    }

    @Test
    fun horizontalCylinderSupportShouldRejectAxisGapRadialMismatchAndCylinderSupport() {
        val cylinder = geometry(
            minX = 0.0,
            maxX = 1.0,
            minY = 0.2,
            maxY = 1.2,
            minZ = 0.0,
            maxZ = 1.0,
            isCylinder = true
        )

        assertFalse(
            horizontalCylinderCuboidSupportCoverage(
                cylinder = cylinder,
                axis = Axis3.X,
                supports = listOf(
                    geometry(
                        minX = 0.0,
                        maxX = 0.4,
                        minY = 0.0,
                        maxY = 0.2,
                        minZ = 0.0,
                        maxZ = 1.0
                    ),
                    geometry(
                        minX = 0.6,
                        maxX = 1.0,
                        minY = 0.0,
                        maxY = 0.2,
                        minZ = 0.0,
                        maxZ = 1.0
                    )
                )
            )
        )
        assertFalse(
            horizontalCylinderCuboidSupportCoverage(
                cylinder = cylinder,
                axis = Axis3.X,
                supports = listOf(
                    geometry(
                        minX = 0.0,
                        maxX = 1.0,
                        minY = 0.0,
                        maxY = 0.2,
                        minZ = 0.0,
                        maxZ = 0.2
                    )
                )
            )
        )
        assertFalse(
            horizontalCylinderCuboidSupportCoverage(
                cylinder = cylinder,
                axis = Axis3.X,
                supports = listOf(
                    geometry(
                        minX = 0.0,
                        maxX = 1.0,
                        minY = 0.0,
                        maxY = 0.2,
                        minZ = 0.0,
                        maxZ = 1.0,
                        isCylinder = true
                    )
                )
            )
        )
    }
}
