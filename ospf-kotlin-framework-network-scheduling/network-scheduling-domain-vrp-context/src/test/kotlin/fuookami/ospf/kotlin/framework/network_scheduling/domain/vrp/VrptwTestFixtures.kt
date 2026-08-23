@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlin.test.assertNotNull
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ServiceTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.VrptwValidator

internal object VrptwTestFixtures {
    fun oneCustomerInstance(
        demand: Flt64 = Flt64(5.0),
        capacity: Flt64 = Flt64(5.0)
    ): Ret<VrptwInstance<Flt64>> {
        return VrptwValidator.create(
            name = "one-customer",
            startDepot = Depot(
                node = node("start", Flt64.zero),
                timeWindow = window(Flt64.zero, Flt64(100.0))
            ),
            endDepot = Depot(
                node = node("end", Flt64(2.0)),
                timeWindow = window(Flt64.zero, Flt64(100.0))
            ),
            customers = listOf(
                assertNotNull(
                    Customer(
                        id = CustomerId("customer"),
                        node = node("customer", Flt64.one),
                        demand = Quantity(demand, Kilogram),
                        timeWindow = window(Flt64(10.0), Flt64(10.0)),
                        serviceTime = 2.toDuration(DurationUnit.SECONDS)
                    ).value
                )
            ),
            vehicleTypes = listOf(
                assertNotNull(
                    VehicleType(
                        id = VehicleTypeId("vehicle"),
                        capacity = Quantity(capacity, Kilogram),
                        fixedCost = Quantity(Flt64(10.0), NoneUnit),
                        amount = 1
                    ).value
                )
            ),
            units = units,
            schedulingWindow = schedulingWindow,
            tolerances = VrptwTolerances.default
        )
    }

    val units: VrptwUnits = assertNotNull(
        VrptwUnits(
            distanceUnit = Meter,
            loadUnit = Kilogram,
            costUnit = NoneUnit
        ).value
    )

    val schedulingWindow = SchedulingTimeWindow(
        window = TimeRange(
            start = Instant.parse("2026-01-01T00:00:00Z"),
            end = Instant.parse("2026-01-01T01:00:00Z")
        ),
        durationUnit = DurationUnit.SECONDS,
        fromDouble = { Flt64(it) },
        toDouble = { it.toDouble() }
    )

    fun node(id: String, x: Flt64): NetworkNode<Flt64> {
        return assertNotNull(
            NetworkNode(
                id = NetworkNodeId(id),
                attributes = mapOf("x" to Quantity(x, Meter), "y" to Quantity(Flt64.zero, Meter))
            ).value
        )
    }

    fun instant(value: Flt64): Instant = schedulingWindow.instantOf(value)

    fun duration(value: Flt64): Duration = schedulingWindow.durationOf(value)

    fun window(ready: Flt64, due: Flt64): ServiceTimeWindow {
        return assertNotNull(
            ServiceTimeWindow(
                readyTime = instant(ready),
                dueTime = instant(due)
            ).value
        )
    }
}

internal object SmallVrptwFixture {
    val instance: VrptwInstance<Flt64> by lazy {
        val customers = (1..6).map { index ->
            assertNotNull(
                Customer(
                    id = CustomerId("c$index"),
                    node = VrptwTestFixtures.node("c$index", Flt64(index)),
                    demand = Quantity(Flt64.one, Kilogram),
                    timeWindow = VrptwTestFixtures.window(Flt64.zero, Flt64(100.0)),
                    serviceTime = Duration.ZERO
                ).value
            )
        }
        assertNotNull(
            VrptwInstance(
                name = "enumerable-six-customers",
                startDepot = Depot(
                    node = VrptwTestFixtures.node("start", Flt64.zero),
                    timeWindow = VrptwTestFixtures.window(Flt64.zero, Flt64(100.0))
                ),
                endDepot = Depot(
                    node = VrptwTestFixtures.node("end", Flt64(7.0)),
                    timeWindow = VrptwTestFixtures.window(Flt64.zero, Flt64(100.0))
                ),
                customers = customers,
                vehicleTypes = listOf(
                    assertNotNull(
                        VehicleType(
                            id = VehicleTypeId("vehicle"),
                            capacity = Quantity(Flt64(3.0), Kilogram),
                            fixedCost = Quantity(Flt64.zero, NoneUnit),
                            amount = 2
                        ).value
                    )
                ),
                units = VrptwTestFixtures.units,
                schedulingWindow = VrptwTestFixtures.schedulingWindow,
                tolerances = VrptwTolerances.default
            ).value
        )
    }

    fun enumeratedSingleVehicleSequences(): List<List<CustomerId>> {
        val result = mutableListOf<List<CustomerId>>()
        val ids = instance.customers.map { it.id }

        fun extend(prefix: List<CustomerId>) {
            if (prefix.isNotEmpty()) {
                result.add(prefix)
            }
            if (prefix.size == 3) {
                return
            }
            ids.filterNot(prefix::contains).forEach { extend(prefix + it) }
        }

        extend(emptyList())
        return result
    }
}
