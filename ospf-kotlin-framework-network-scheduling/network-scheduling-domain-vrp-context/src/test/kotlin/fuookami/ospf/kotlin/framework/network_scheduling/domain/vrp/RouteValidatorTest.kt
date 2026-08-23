@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.Flt64NetworkSchedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.RouteValidator

class RouteValidatorTest {
    @Test
    fun routeShouldValidateWaitingDueBoundaryAndCapacityBoundary() {
        val instance = assertNotNull(VrptwTestFixtures.oneCustomerInstance().value)
        val route = validRoute(instance)
        val distance = EuclideanDistanceCalculator<Flt64>(Meter)

        val result = RouteValidator.validate(
            instance = instance,
            route = route,
            distanceCalculator = distance,
            travelTimeCalculator = DistanceAsTravelTimeCalculator(distance, instance.schedulingWindow),
            arcCostCalculator = DistanceArcCostCalculator(Meter, NoneUnit),
            routeCostPolicy = Demo17CostPolicy(NoneUnit),
            valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
        )

        assertTrue(result.ok)
    }

    @Test
    fun routeShouldRejectRepeatedCustomer() {
        val instance = assertNotNull(VrptwTestFixtures.oneCustomerInstance().value)
        val valid = validRoute(instance)
        val repeated = assertNotNull(
            Route(
                vehicleTypeId = valid.vehicleTypeId,
                stops = listOf(valid.stops[0], valid.stops[1], valid.stops[1], valid.stops[2]),
                distance = Quantity(Flt64(3.0), Meter),
                cost = Quantity(Flt64(13.0), NoneUnit)
            ).value
        )
        val distance = EuclideanDistanceCalculator<Flt64>(Meter)

        assertTrue(
            RouteValidator.validate(
                instance = instance,
                route = repeated,
                distanceCalculator = distance,
                travelTimeCalculator = DistanceAsTravelTimeCalculator(distance, instance.schedulingWindow),
                arcCostCalculator = DistanceArcCostCalculator(Meter, NoneUnit),
                routeCostPolicy = Demo17CostPolicy(NoneUnit),
                valueAdapter = Flt64NetworkSchedulingSolverValueAdapter
            ).failed
        )
    }

    private fun validRoute(instance: VrptwInstance<Flt64>): Route<Flt64> {
        val customer = instance.customers.single()
        return assertNotNull(
            Route(
                vehicleTypeId = instance.vehicleTypes.single().id,
                stops = listOf(
                    RouteStop(
                        nodeId = instance.startDepot.node.id,
                        customerId = null,
                        arrival = VrptwTestFixtures.instant(Flt64.zero),
                        serviceStart = VrptwTestFixtures.instant(Flt64.zero),
                        departure = VrptwTestFixtures.instant(Flt64.zero),
                        accumulatedLoad = Quantity(Flt64.zero, Kilogram)
                    ),
                    RouteStop(
                        nodeId = customer.node.id,
                        customerId = customer.id,
                        arrival = VrptwTestFixtures.instant(Flt64.one),
                        serviceStart = VrptwTestFixtures.instant(Flt64(10.0)),
                        departure = VrptwTestFixtures.instant(Flt64(12.0)),
                        accumulatedLoad = Quantity(Flt64(5.0), Kilogram)
                    ),
                    RouteStop(
                        nodeId = instance.endDepot.node.id,
                        customerId = null,
                        arrival = VrptwTestFixtures.instant(Flt64(13.0)),
                        serviceStart = VrptwTestFixtures.instant(Flt64(13.0)),
                        departure = VrptwTestFixtures.instant(Flt64(13.0)),
                        accumulatedLoad = Quantity(Flt64(5.0), Kilogram)
                    )
                ),
                distance = Quantity(Flt64(2.0), Meter),
                cost = Quantity(Flt64(12.0), NoneUnit)
            ).value
        )
    }
}
