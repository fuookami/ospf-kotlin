@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.network_scheduling.application

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.VrptwValidator
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.*

class BranchDecisionTest {

    private val startDepot = NetworkNodeId("start")
    private val endDepot = NetworkNodeId("end")
    private val customerNode = NetworkNodeId("c1")
    private val vehicleType = VehicleTypeId("v1")

    @Test
    fun forbitVehicleTypeAccumulatesCorrectly() {
        val decisions = listOf(
            BranchDecision.ForbidVehicleType(vehicleType, CustomerId("c1"), customerNode)
        )
        val mask = accumulateBranchMask(startDepot, endDepot, decisions)
        assertTrue(mask is Ok)
        val m = (mask as Ok).value
        assertFalse(m.allowsNode(vehicleType, customerNode))
    }

    @Test
    fun requireVehicleTypeAccumulatesCorrectly() {
        val decisions = listOf(
            BranchDecision.RequireVehicleType(vehicleType, CustomerId("c1"), customerNode)
        )
        val mask = accumulateBranchMask(startDepot, endDepot, decisions)
        assertTrue(mask is Ok)
        val m = (mask as Ok).value
        // customerNode is required to use vehicleType, so other types are forbidden
        val otherType = VehicleTypeId("v2")
        assertFalse(m.allowsNode(otherType, customerNode))
        assertTrue(m.allowsNode(vehicleType, customerNode))
    }

    @Test
    fun forbitArcAccumulatesCorrectly() {
        val nodeA = NetworkNodeId("a")
        val nodeB = NetworkNodeId("b")
        val decisions = listOf(
            BranchDecision.ForbidArc(vehicleType, nodeA, nodeB)
        )
        val mask = accumulateBranchMask(startDepot, endDepot, decisions)
        assertTrue(mask is Ok)
        val m = (mask as Ok).value
        assertFalse(m.allowsArc(vehicleType, nodeA, nodeB))
    }

    @Test
    fun requireArcAccumulatesCorrectly() {
        val nodeA = NetworkNodeId("a")
        val nodeB = NetworkNodeId("b")
        val decisions = listOf(
            BranchDecision.RequireArc(vehicleType, nodeA, nodeB)
        )
        val mask = accumulateBranchMask(startDepot, endDepot, decisions)
        assertTrue(mask is Ok)
        val m = (mask as Ok).value
        assertTrue(m.allowsArc(vehicleType, nodeA, nodeB))
    }

    @Test
    fun multipleDecisionsAccumulateCorrectly() {
        val nodeA = NetworkNodeId("a")
        val nodeB = NetworkNodeId("b")
        val nodeC = NetworkNodeId("c")
        val decisions = listOf(
            BranchDecision.ForbidVehicleType(vehicleType, CustomerId("c1"), nodeA),
            BranchDecision.ForbidArc(vehicleType, nodeB, nodeC)
        )
        val mask = accumulateBranchMask(startDepot, endDepot, decisions)
        assertTrue(mask is Ok)
        val m = (mask as Ok).value
        assertFalse(m.allowsNode(vehicleType, nodeA))
        assertFalse(m.allowsArc(vehicleType, nodeB, nodeC))
    }

    @Test
    fun emptyDecisionsProduceEmptyMask() {
        val mask = accumulateBranchMask(startDepot, endDepot, emptyList())
        assertTrue(mask is Ok)
        val m = (mask as Ok).value
        // Everything should be allowed
        val nodeA = NetworkNodeId("a")
        assertTrue(m.allowsNode(vehicleType, nodeA))
    }
}

class BranchNodeTest {

    private val startDepot = NetworkNodeId("start")
    private val endDepot = NetworkNodeId("end")

    @Test
    fun rootCreationSucceeds() {
        val root = BranchNode.root(startDepot, endDepot)
        assertTrue(root is Ok)
        val r = (root as Ok).value
        assertEquals(0, r.id)
        assertNull(r.parentId)
        assertEquals(0, r.depth)
        assertTrue(r.decisions.isEmpty())
        assertEquals(NodeStatus.Pending, r.status)
    }

    @Test
    fun childCreationSucceeds() {
        val root = (BranchNode.root(startDepot, endDepot) as Ok).value
        val decision = BranchDecision.ForbidVehicleType(
            VehicleTypeId("v1"), CustomerId("c1"), NetworkNodeId("c1")
        )
        val child = BranchNode.child(1, root, decision, startDepot, endDepot)
        assertTrue(child is Ok)
        val c = (child as Ok).value
        assertEquals(1, c.id)
        assertEquals(0, c.parentId)
        assertEquals(1, c.depth)
        assertEquals(listOf(decision), c.decisions)
    }

    @Test
    fun solveUpdatesLowerBound() {
        val root = (BranchNode.root(startDepot, endDepot) as Ok).value
        assertEquals(NodeStatus.Pending, root.status)
        root.solve(Flt64(42.0))
        assertEquals(NodeStatus.Solved, root.status)
        assertEquals(Flt64(42.0), root.lowerBound)
    }

    @Test
    fun markInfeasibleSetsStatus() {
        val root = (BranchNode.root(startDepot, endDepot) as Ok).value
        root.markInfeasible()
        assertEquals(NodeStatus.Infeasible, root.status)
    }

    @Test
    fun pruneSetsStatus() {
        val root = (BranchNode.root(startDepot, endDepot) as Ok).value
        root.prune()
        assertEquals(NodeStatus.Pruned, root.status)
    }

    @Test
    fun canImproveReturnsTrueWhenNoIncumbent() {
        val root = (BranchNode.root(startDepot, endDepot) as Ok).value
        assertTrue(root.canImprove(null, Flt64(1e-6)))
    }

    @Test
    fun canImproveReturnsFalseWhenBoundWorseThanIncumbent() {
        val root = (BranchNode.root(startDepot, endDepot, Flt64(100.0)) as Ok).value
        root.solve(Flt64(100.0))
        assertFalse(root.canImprove(Flt64(50.0), Flt64(1e-6)))
    }

    @Test
    fun effectiveLowerBoundUsesOwnBoundAfterSolve() {
        val root = (BranchNode.root(startDepot, endDepot, Flt64.negativeInfinity) as Ok).value
        root.solve(Flt64(42.0))
        // Solving replaces the inherited bound: effectiveLowerBound = 42.0.
        // 求解后自身下界替换继承下界：effectiveLowerBound = 42.0。
        assertEquals(Flt64(42.0), root.effectiveLowerBound)
    }

    @Test
    fun effectiveLowerBoundUsesOwnWhenBetter() {
        val root = (BranchNode.root(startDepot, endDepot, Flt64(50.0)) as Ok).value
        root.solve(Flt64(42.0))
        // The own LP bound replaces inheritedLowerBound even when it is smaller.
        // 自身 LP 下界即使更小，也会替换 inheritedLowerBound。
        assertEquals(Flt64(42.0), root.effectiveLowerBound)
    }

    @Test
    fun childInheritsParentLowerBound() {
        val root = (BranchNode.root(startDepot, endDepot) as Ok).value
        root.solve(Flt64(42.0))
        val decision = BranchDecision.ForbidVehicleType(
            VehicleTypeId("v1"), CustomerId("c1"), NetworkNodeId("c1")
        )
        val child = (BranchNode.child(1, root, decision, startDepot, endDepot) as Ok).value
        assertEquals(Flt64(42.0), child.inheritedLowerBound)
    }
}

class BranchAndPriceStatusTest {

    @Test
    fun allStatusesExist() {
        // Verify all termination statuses are defined per contract
        val optimal = BranchAndPriceStatus.Optimal
        val infeasible = BranchAndPriceStatus.Infeasible
        val timeLimit = BranchAndPriceStatus.TimeLimit
        val nodeLimit = BranchAndPriceStatus.NodeLimit
        val solverStopped = BranchAndPriceStatus.SolverStopped
        // Just verify they exist and are distinct
        assertNotEquals(optimal, infeasible)
        assertNotEquals(timeLimit, nodeLimit)
        assertNotEquals(solverStopped, optimal)
    }
}

class BranchAndPriceTraceTest {

    @Test
    fun traceCreation() {
        val trace = BranchAndPriceTrace(
            nodesExplored = 10,
            nodesPruned = 3,
            globalLowerBound = Flt64(100.0),
            globalUpperBound = Flt64(105.0),
            relativeGap = Flt64(0.05),
            totalTime = Duration.parse("PT1M"),
            totalIterations = 50
        )
        assertEquals(10, trace.nodesExplored)
        assertEquals(3, trace.nodesPruned)
        assertEquals(Flt64(100.0), trace.globalLowerBound)
        assertEquals(Flt64(105.0), trace.globalUpperBound)
        assertEquals(Flt64(0.05), trace.relativeGap)
    }
}
