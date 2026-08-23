@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure

import kotlin.test.*
import kotlin.time.Instant
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNodeId

class VrpInfrastructureTest {
    @Test
    fun branchMaskShouldApplyVehicleAndDepotAwareArcRequirements() {
        val start = NetworkNodeId("start")
        val end = NetworkNodeId("end")
        val a = NetworkNodeId("a")
        val b = NetworkNodeId("b")
        val mask = assertNotNull(
            BranchMask(
                startDepot = start,
                endDepot = end,
                requiredArcs = setOf(ResourceArc("small", a, b))
            ).value
        )

        assertTrue(mask.allowsArc("small", a, b))
        assertFalse(mask.allowsArc("small", a, end))
        assertFalse(mask.allowsNode("large", a))
        assertFalse(mask.allowsNode("large", b))

        val depotRequired = assertNotNull(
            BranchMask(
                startDepot = start,
                endDepot = end,
                requiredArcs = setOf(ResourceArc("small", start, a))
            ).value
        )
        assertTrue(depotRequired.allowsArc("small", start, b))
        assertTrue(depotRequired.isRouteCompatible("small", listOf(start, b, end)))
        assertFalse(depotRequired.allowsArc("large", start, a))
    }

    @Test
    fun branchMaskShouldRequireCustomerToEndDepotWithoutRestrictingOtherReturns() {
        val start = NetworkNodeId("start")
        val end = NetworkNodeId("end")
        val a = NetworkNodeId("a")
        val b = NetworkNodeId("b")
        val mask = assertNotNull(
            BranchMask(
                startDepot = start,
                endDepot = end,
                requiredArcs = setOf(ResourceArc("small", a, end))
            ).value
        )

        assertTrue(mask.allowsArc("small", a, end))
        assertFalse(mask.allowsArc("small", a, b))
        assertTrue(mask.allowsArc("small", b, end))
        assertFalse(mask.allowsArc("large", a, end))
        assertTrue(mask.isRouteCompatible("small", listOf(start, a, end)))
        assertFalse(mask.isRouteCompatible("small", listOf(start, a, b, end)))
        assertTrue(mask.isRouteCompatible("small", listOf(start, b, end)))
    }

    @Test
    fun branchMaskRouteCompatibilityShouldAgreeWithArcChecks() {
        val start = NetworkNodeId("start")
        val end = NetworkNodeId("end")
        val a = NetworkNodeId("a")
        val b = NetworkNodeId("b")
        val c = NetworkNodeId("c")
        val mask = assertNotNull(
            BranchMask(
                startDepot = start,
                endDepot = end,
                forbiddenArcs = setOf(ResourceArc("v1", a, b)),
                forbiddenNodes = mapOf("v1" to setOf(b)),
                requiredArcs = setOf(ResourceArc("v1", start, a))
            ).value
        )

        assertTrue(mask.isRouteCompatible("v1", listOf(start, a, c, end)))
        assertFalse(mask.isRouteCompatible("v1", listOf(start, b, end)))
        assertFalse(mask.isRouteCompatible("v1", listOf(start, a, b, end)))
        assertTrue(mask.isRouteCompatible("v2", listOf(start, b, c, end)))
    }

    @Test
    fun serviceTimeWindowShouldAcceptItsDueTime() {
        val readyTime = Instant.parse("2026-01-01T00:00:00Z")
        val dueTime = Instant.parse("2026-01-01T00:00:10Z")
        val window = assertNotNull(ServiceTimeWindow(readyTime, dueTime).value)

        assertTrue(window.contains(dueTime))
        assertTrue(ServiceTimeWindow(dueTime, readyTime).failed)
    }
}
