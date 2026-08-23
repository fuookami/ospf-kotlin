@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.example.framework_demo.demo5

import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.VrptwUnits
import fuookami.ospf.kotlin.example.core_demo.Demo17
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.io.SolomonDataReader
import fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.adapter.*

class Demo5InputAdapterTest {
    @Test
    fun demo17AdapterShouldReuse25And100CustomerFixtures() {
        val schedulingWindow = flt64SchedulingWindow()
        val first25 = assertNotNull(Demo17InstanceAdapter.first25Customers(schedulingWindow).value)
        val all100 = assertNotNull(Demo17InstanceAdapter.all100Customers(schedulingWindow).value)
        val sourceCustomers = Demo17.nodes.filterIsInstance<Demo17.DemandNode>()

        assertEquals(100, sourceCustomers.size)
        assertEquals(25, first25.customers.size)
        assertEquals(100, all100.customers.size)
        assertEquals(Demo17.vehicles.size, all100.vehicleTypes.sumOf { it.amount })
        assertEquals(Flt64(200.0), all100.vehicleTypes.single().capacity.value)
        assertEquals(Flt64(500.0), all100.vehicleTypes.single().fixedCost.value)
        assertEquals(
            sourceCustomers.first().position.position,
            listOf("x", "y", "z").mapNotNull { first25.customers.first().node.attributes[it]?.value }
        )
        assertEquals(sourceCustomers.last().demand.toFlt64(), all100.customers.last().demand.value)
        assertNotEquals(all100.startDepot.node.id, all100.endDepot.node.id)
        assertTrue(
            Demo17InstanceAdapter.create(
                customerCount = 101,
                schedulingWindow = schedulingWindow
            ).failed
        )
    }

    @Test
    fun solomonAdapterShouldSupportFlt64AndFltX() {
        val data = assertNotNull(SolomonDataReader.read(validData).value)
        val units = assertNotNull(
            VrptwUnits(
                distanceUnit = Meter,
                loadUnit = Kilogram,
                costUnit = NoneUnit
            ).value
        )
        val flt64 = SolomonInstanceAdapter(SolomonFlt64ValueConverter).create(
            data = data,
            units = units,
            fixedVehicleCost = Quantity(Flt64(10.0), NoneUnit),
            schedulingWindow = flt64SchedulingWindow()
        )
        val fltX = SolomonInstanceAdapter(SolomonFltXValueConverter).create(
            data = data,
            units = units,
            fixedVehicleCost = Quantity(FltX("10"), NoneUnit),
            schedulingWindow = fltXSchedulingWindow()
        )

        assertEquals(3, flt64.value?.customers?.size)
        assertEquals(3, fltX.value?.customers?.size)
        assertTrue(assertNotNull(fltX.value).customers.first().demand.value eq FltX.one)
    }

    @Test
    fun solomonAdapterShouldMapRawTimesToAbsoluteInstantsAndDurations() {
        val data = assertNotNull(
            SolomonDataReader.read(
                validData.replace(
                    "3 3 0 1 0 100 0",
                    "3 3 0 1 10 20 12"
                )
            ).value
        )
        val schedulingWindow = flt64SchedulingWindow()
        val instance = assertNotNull(
            SolomonInstanceAdapter(SolomonFlt64ValueConverter).create(
                data = data,
                units = units(),
                fixedVehicleCost = Quantity(Flt64(10.0), NoneUnit),
                schedulingWindow = schedulingWindow
            ).value
        )
        val customer = instance.customers.last()

        assertEquals(schedulingWindow.start + 10.toDuration(DurationUnit.SECONDS), customer.timeWindow.readyTime)
        assertEquals(schedulingWindow.start + 20.toDuration(DurationUnit.SECONDS), customer.timeWindow.dueTime)
        assertEquals(12.toDuration(DurationUnit.SECONDS), customer.serviceTime)
    }

    @Test
    fun solomonAdapterShouldRejectTimeWindowOutsideBusinessTimeline() {
        val data = assertNotNull(
            SolomonDataReader.read(
                validData.replace(
                    "3 3 0 1 0 100 0",
                    "3 3 0 1 0 3601 0"
                )
            ).value
        )

        val result = SolomonInstanceAdapter(SolomonFlt64ValueConverter).create(
            data = data,
            units = units(),
            fixedVehicleCost = Quantity(Flt64(10.0), NoneUnit),
            schedulingWindow = flt64SchedulingWindow()
        )

        assertTrue(result.failed)
    }

    @Test
    fun solomonReaderShouldKeepEveryCustomerIncludingLastRow() {
        val data = assertNotNull(SolomonDataReader.read(validData).value)

        assertEquals("TINY", data.name)
        assertEquals(2, data.vehicle.amount)
        assertEquals("0", data.depot.id)
        assertEquals(listOf("1", "2", "3"), data.customers.map { it.id })
    }

    @Test
    fun solomonReaderShouldRejectMalformedRowsAndMissingDepot() {
        val wrongColumnCount = validData.replace("3 3 0 1 0 100 0", "3 3 0 1 0 100")
        val duplicate = validData.replace("3 3 0 1 0 100 0", "2 3 0 1 0 100 0")
        val missingDepot = validData.replace("0 0 0 0 0 100 0", "9 0 0 0 0 100 0")
        val invalidNumber = validData.replace("3 3 0 1 0 100 0", "3 invalid 0 1 0 100 0")
        val invalidWindow = validData.replace("3 3 0 1 0 100 0", "3 3 0 1 101 100 0")

        assertTrue(SolomonDataReader.read(wrongColumnCount).failed)
        assertTrue(SolomonDataReader.read(duplicate).failed)
        assertTrue(SolomonDataReader.read(missingDepot).failed)
        assertTrue(SolomonDataReader.read(invalidNumber).failed)
        assertTrue(SolomonDataReader.read(invalidWindow).failed)
    }

    companion object {
        private fun units(): VrptwUnits {
            return assertNotNull(
                VrptwUnits(
                    distanceUnit = Meter,
                    loadUnit = Kilogram,
                    costUnit = NoneUnit
                ).value
            )
        }

        private fun flt64SchedulingWindow(): SchedulingTimeWindow<Flt64> {
            return SchedulingTimeWindow(
                window = TimeRange(
                    start = Instant.parse("2026-01-01T00:00:00Z"),
                    end = Instant.parse("2026-01-01T01:00:00Z")
                ),
                durationUnit = DurationUnit.SECONDS,
                fromDouble = { Flt64(it) },
                toDouble = { it.toDouble() }
            )
        }

        private fun fltXSchedulingWindow(): SchedulingTimeWindow<FltX> {
            return SchedulingTimeWindow(
                window = TimeRange(
                    start = Instant.parse("2026-01-01T00:00:00Z"),
                    end = Instant.parse("2026-01-01T01:00:00Z")
                ),
                durationUnit = DurationUnit.SECONDS,
                fromDouble = { FltX(it.toString()) },
                toDouble = { it.toDouble() }
            )
        }

        private val validData = """
            TINY

            VEHICLE
            NUMBER CAPACITY
            2 3

            CUSTOMER
            CUST NO. XCOORD. YCOORD. DEMAND READY TIME DUE DATE SERVICE TIME
            0 0 0 0 0 100 0
            1 1 0 1 0 100 0
            2 2 0 1 0 100 0
            3 3 0 1 0 100 0
        """.trimIndent()
    }
}
