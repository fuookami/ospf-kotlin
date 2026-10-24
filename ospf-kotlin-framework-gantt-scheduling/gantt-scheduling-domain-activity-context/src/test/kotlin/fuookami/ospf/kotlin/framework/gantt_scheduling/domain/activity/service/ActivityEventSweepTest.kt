@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service

import kotlin.random.Random
import kotlin.test.*
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

class ActivityEventSweepTest {
    private data class ResourceId(val index: Int) : ExecutorId
    private data class ActivityId(val index: Int) : TaskId
    private val epoch = Instant.parse("2026-10-24T00:00:00Z")
    private fun at(hour: Int) = epoch + hour.hours

    @Test
    fun `sweep and material prefixes preserve every row and raw floating result`() {
        repeat(40) { seed ->
            val random = Random(seed)
            var reservationReads = 0
            var materialReads = 0
            val numbers = listOf(0.0, -0.0, 1.0, 0.1, 1e16, -1e16, -0.1)
            val resources = (0..2).map { index ->
                val source = ActivityResource(
                    id = ResourceId(index),
                    capacity = Quantity(Flt64(10), NoneUnit),
                    unavailableTimes = listOf(TimeRange(at(4), at(5)))
                )
                CompiledActivityResource(
                    source = source,
                    capacity = Flt64(10),
                    capacities = listOf(TimeRange(at(2), at(4)) to Flt64(3))
                )
            }
            val materials = (0..2).map { index ->
                CompiledActivityMaterial(
                    source = ActivityMaterial("m$index", Quantity(Flt64.one, NoneUnit)),
                    initial = Flt64(numbers[random.nextInt(numbers.size)]),
                    minimum = Flt64.zero,
                    maximum = if (index == 2) null else Flt64(100)
                )
            }
            fun reservations(): List<CompiledActivityReservation> {
                val source = List(12) {
                    val start = random.nextInt(-2, 9)
                    CompiledActivityReservation(
                        resource = resources[random.nextInt(resources.size)].source.id,
                        time = TimeRange(at(start), at(start + random.nextInt(0, 5))),
                        quantity = Flt64(numbers[random.nextInt(numbers.size)])
                    )
                }
                return object : AbstractList<CompiledActivityReservation>() {
                    override val size get() = source.size
                    override fun get(index: Int): CompiledActivityReservation {
                        reservationReads++
                        return source[index]
                    }
                }
            }
            fun materialEvents(): List<CompiledActivityMaterialEvent> {
                val source = List(16) {
                    CompiledActivityMaterialEvent(
                        material = "m${random.nextInt(materials.size)}",
                        time = at(random.nextInt(-1, 10)),
                        quantity = Flt64(numbers[random.nextInt(numbers.size)])
                    )
                }
                return object : AbstractList<CompiledActivityMaterialEvent>() {
                    override val size get() = source.size
                    override fun get(index: Int): CompiledActivityMaterialEvent {
                        materialReads++
                        return source[index]
                    }
                }
            }
            val data = CompiledActivityProblem(
                problem = ActivitySchedulingProblem(
                    horizon = TimeRange(at(0), at(8)),
                    activities = emptyList(),
                    resources = resources.map { it.source },
                    costUnit = NoneUnit
                ),
                choices = List(8) { index ->
                    CompiledActivityChoice<Flt64>(
                        task = ActivityId(index),
                        placement = null,
                        reservations = reservations(),
                        cost = Flt64.zero,
                        materialEvents = materialEvents()
                    )
                },
                resources = resources.associateBy { it.source.id },
                fixedReservations = reservations(),
                extraRows = emptyList(),
                materials = materials.associateBy { it.source.id },
                materialEvents = materialEvents()
            )
            val capacities = capacityReference(data)
            reservationReads = 0
            assertRows(capacities, ActivityCapacityRows(data).rows())
            assertEquals(9 * 12, reservationReads)
            val stocks = materialReference(data)
            materialReads = 0
            assertRows(stocks, ActivityMaterialRows(data).rows())
            assertEquals(9 * 16, materialReads)
        }
    }

    @Test
    fun `ordered sums recover from removal and retain original order on earlier insertion`() {
        val sum = OrderedActivitySum()
        sum.update(listOf(0 to Flt64(1e16), 2 to Flt64(-1e16)))
        sum.update(listOf(1 to Flt64.one))
        assertEquals(0.0.toRawBits(), sum.value.toDouble().toRawBits())
        sum.update(emptyList(), listOf(0, 2))
        assertEquals(1.0.toRawBits(), sum.value.toDouble().toRawBits())
        sum.update(listOf(3 to Flt64(2), 4 to Flt64(3)))
        assertEquals(6.0.toRawBits(), sum.value.toDouble().toRawBits())
        sum.update(emptyList(), listOf(1, 3, 4))
        assertEquals(0.0.toRawBits(), sum.value.toDouble().toRawBits())
    }

    private fun assertRows(expected: List<CompiledActivityRow>, actual: List<CompiledActivityRow>) {
        assertEquals(expected.map { it.id }, actual.map { it.id })
        for ((reference, row) in expected.zip(actual)) {
            assertEquals(
                expected = reference.coefficients.keys.toList(),
                actual = row.coefficients.keys.toList(),
                message = row.id
            )
            assertEquals(
                expected = reference.coefficients.values.map { it.toDouble().toRawBits() },
                actual = row.coefficients.values.map { it.toDouble().toRawBits() },
                message = row.id
            )
            assertEquals(
                expected = reference.lower?.toDouble()?.toRawBits(),
                actual = row.lower?.toDouble()?.toRawBits(),
                message = row.id
            )
            assertEquals(
                expected = reference.upper?.toDouble()?.toRawBits(),
                actual = row.upper?.toDouble()?.toRawBits(),
                message = row.id
            )
        }
    }

    private fun capacityReference(data: CompiledActivityProblem<Flt64>): List<CompiledActivityRow> {
        val rows = ArrayList<CompiledActivityRow>()
        val horizon = data.problem.horizon
        for ((resourceIndex, entry) in data.resources.entries.withIndex()) {
            val (id, resource) = entry
            val events = sortedSetOf(horizon.start, horizon.end)
            fun add(time: Instant) {
                if (time > horizon.start && time < horizon.end) events.add(time)
            }
            for (reservation in (data.choices.flatMap { it.reservations } + data.fixedReservations).filter { it.resource == id }) {
                add(reservation.time.start)
                add(reservation.time.end)
            }
            for (range in resource.source.unavailableTimes + resource.capacities.map { it.first }) {
                add(range.start)
                add(range.end)
            }
            for ((eventIndex, time) in events.toList().dropLast(1).withIndex()) {
                val coefficients = linkedMapOf<Int, Flt64>()
                for ((index, choice) in data.choices.withIndex()) {
                    val usage = choice.reservations.filter { it.resource == id && time >= it.time.start && time < it.time.end }
                        .fold(Flt64.zero) { sum, reservation -> sum + reservation.quantity }
                    if (usage.toDouble() != 0.0) coefficients[index] = usage
                }
                val fixed = data.fixedReservations.filter { it.resource == id && time >= it.time.start && time < it.time.end }
                    .fold(Flt64.zero) { sum, reservation -> sum + reservation.quantity }
                if (coefficients.isNotEmpty() || fixed.toDouble() != 0.0) rows.add(CompiledActivityRow(
                    id = "capacity_${resourceIndex}_$eventIndex",
                    coefficients = coefficients,
                    upper = resource.capacityAt(time) - fixed
                ))
            }
        }
        return rows
    }

    private fun materialReference(data: CompiledActivityProblem<Flt64>): List<CompiledActivityRow> {
        val rows = ArrayList<CompiledActivityRow>()
        for ((materialIndex, material) in data.materials.values.withIndex()) {
            val id = material.source.id
            rows.add(CompiledActivityRow(
                id = "stock_${materialIndex}_opening",
                coefficients = emptyMap(),
                lower = material.minimum - material.initial,
                upper = material.maximum?.let { it - material.initial }
            ))
            val events = (data.materialEvents + data.choices.flatMap { it.materialEvents }).filter { it.material == id }
            val times = (listOf(data.problem.horizon.start, data.problem.horizon.end) + events.map { it.time }).distinct().sorted()
            for ((eventIndex, time) in times.withIndex()) {
                val fixed = data.materialEvents.filter { it.material == id && it.time <= time }
                    .fold(material.initial) { sum, event -> sum + event.quantity }
                val coefficients = data.choices.mapIndexed { index, choice ->
                    index to choice.materialEvents.filter { it.material == id && it.time <= time }
                        .fold(Flt64.zero) { sum, event -> sum + event.quantity }
                }.filter { it.second.toDouble() != 0.0 }.toMap()
                rows.add(CompiledActivityRow(
                    id = "stock_${materialIndex}_$eventIndex",
                    coefficients = coefficients,
                    lower = material.minimum - fixed,
                    upper = material.maximum?.let { it - fixed }
                ))
            }
        }
        return rows
    }
}
