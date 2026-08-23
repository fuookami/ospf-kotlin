@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp

import kotlin.time.Instant
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ServiceTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.*

class VrptwValidatorTest {
    @Test
    fun instanceShouldRejectDemandAboveEveryVehicleCapacity() {
        assertTrue(
            VrptwTestFixtures.oneCustomerInstance(
                demand = Flt64(6.0),
                capacity = Flt64(5.0)
            ).failed
        )
    }

    @Test
    fun instanceShouldRejectIncompatibleDemandUnit() {
        val base = assertNotNull(VrptwTestFixtures.oneCustomerInstance().value)
        val invalidCustomer = assertNotNull(
            Customer(
                id = CustomerId("invalid"),
                node = VrptwTestFixtures.node("invalid", Flt64.one),
                demand = Quantity(Flt64.one, Second),
                timeWindow = VrptwTestFixtures.window(Flt64.zero, Flt64(10.0)),
                serviceTime = VrptwTestFixtures.duration(Flt64.zero)
            ).value
        )
        val invalid = VrptwValidator.create(
            name = "invalid-unit",
            startDepot = base.startDepot,
            endDepot = base.endDepot,
            customers = listOf(invalidCustomer),
            vehicleTypes = base.vehicleTypes,
            schedulingWindow = base.schedulingWindow,
            units = base.units,
            tolerances = base.tolerances
        )

        assertTrue(invalid.failed)
    }

    @Test
    fun instanceShouldRejectDuplicateCustomerId() {
        val base = assertNotNull(VrptwTestFixtures.oneCustomerInstance().value)
        val original = base.customers.single()
        val duplicate = assertNotNull(
            Customer(
                id = original.id,
                node = VrptwTestFixtures.node("duplicate-customer-node", Flt64(2.0)),
                demand = original.demand,
                timeWindow = original.timeWindow,
                serviceTime = original.serviceTime
            ).value
        )

        val result = VrptwValidator.create(
            name = "duplicate-customer-id",
            startDepot = base.startDepot,
            endDepot = Depot(
                node = VrptwTestFixtures.node("duplicate-test-end", Flt64(3.0)),
                timeWindow = base.endDepot.timeWindow
            ),
            customers = listOf(original, duplicate),
            vehicleTypes = base.vehicleTypes,
            schedulingWindow = base.schedulingWindow,
            units = base.units,
            tolerances = base.tolerances
        )

        assertTrue(result.failed)
    }

    @Test
    fun serviceTimeWindowShouldRejectReadyTimeAfterDueTime() {
        val result = ServiceTimeWindow(
            readyTime = Instant.parse("2026-01-01T00:00:11Z"),
            dueTime = Instant.parse("2026-01-01T00:00:10Z")
        )

        assertTrue(result.failed)
    }

    @Test
    fun instanceShouldRejectServiceTimeWindowOutsideBusinessTimeline() {
        val base = assertNotNull(VrptwTestFixtures.oneCustomerInstance().value)
        val original = base.customers.single()
        val outsideTimeline = assertNotNull(
            ServiceTimeWindow(
                readyTime = original.timeWindow.readyTime,
                dueTime = base.schedulingWindow.end + VrptwTestFixtures.duration(Flt64.one)
            ).value
        )
        val customer = assertNotNull(
            Customer(
                id = original.id,
                node = original.node,
                demand = original.demand,
                timeWindow = outsideTimeline,
                serviceTime = original.serviceTime
            ).value
        )

        val result = VrptwValidator.create(
            name = "outside-business-timeline",
            startDepot = base.startDepot,
            endDepot = base.endDepot,
            customers = listOf(customer),
            vehicleTypes = base.vehicleTypes,
            schedulingWindow = base.schedulingWindow,
            units = base.units,
            tolerances = base.tolerances
        )

        assertTrue(result.failed)
    }

    @Test
    fun instanceShouldRejectMismatchedCoordinateAxes() {
        val base = assertNotNull(VrptwTestFixtures.oneCustomerInstance().value)
        val mismatchedEnd = Depot(
            node = assertNotNull(
                NetworkNode(
                    id = base.endDepot.node.id,
                    attributes = mapOf(
                        "x" to Quantity(Flt64(2.0), Meter),
                        "z" to Quantity(Flt64.zero, Meter)
                    )
                ).value
            ),
            timeWindow = base.endDepot.timeWindow
        )

        val result = VrptwValidator.create(
            name = "mismatched-coordinate-axes",
            startDepot = base.startDepot,
            endDepot = mismatchedEnd,
            customers = base.customers,
            vehicleTypes = base.vehicleTypes,
            schedulingWindow = base.schedulingWindow,
            units = base.units,
            tolerances = base.tolerances
        )

        assertTrue(result.failed)
    }

    @Test
    fun distanceShouldRejectMismatchedCoordinateAxes() {
        val from = assertNotNull(
            NetworkNode(
                id = NetworkNodeId("from"),
                attributes = mapOf(
                    "x" to Quantity(Flt64.zero, Meter),
                    "z" to Quantity(Flt64.zero, Meter)
                )
            ).value
        )
        val to = assertNotNull(
            NetworkNode(
                id = NetworkNodeId("to"),
                attributes = mapOf(
                    "x" to Quantity(Flt64.one, Meter),
                    "y" to Quantity(Flt64.one, Meter)
                )
            ).value
        )

        assertTrue(EuclideanDistanceCalculator<Flt64>(Meter).distance(from, to).failed)
    }

    @Test
    fun enumerableFixtureShouldHaveKnownRouteCount() {
        assertEquals(6, SmallVrptwFixture.instance.customers.size)
        assertEquals(156, SmallVrptwFixture.enumeratedSingleVehicleSequences().size)
        assertEquals(156, SmallVrptwFixture.enumeratedSingleVehicleSequences().toSet().size)
    }
}
