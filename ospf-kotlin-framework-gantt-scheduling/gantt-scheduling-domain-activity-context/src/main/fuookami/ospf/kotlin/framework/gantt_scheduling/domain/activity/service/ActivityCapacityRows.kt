@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service

import java.util.TreeMap
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 按资源扫描端点，仅重算发生变化的选择，保留各组原求和顺序。 / Sweep resource endpoints, recomputing only changed choices in their original summation order. */
internal class ActivityCapacityRows<V : RealNumber<V>>(private val data: CompiledActivityProblem<V>) {
    private data class Reservation(
        val choice: Int,
        val order: Int,
        val value: CompiledActivityReservation
    )

    fun rows(): List<CompiledActivityRow> {
        val reservations = ArrayList<Reservation>()
        for ((choice, item) in data.choices.withIndex()) {
            for (reservation in item.reservations) reservations.add(Reservation(
                choice = choice,
                order = reservations.size,
                value = reservation
            ))
        }
        for (reservation in data.fixedReservations) reservations.add(Reservation(
            choice = -1,
            order = reservations.size,
            value = reservation
        ))
        val byResource = reservations.groupBy { it.value.resource }
        val rows = ArrayList<CompiledActivityRow>()
        val horizon = data.problem.horizon
        for ((resourceIndex, entry) in data.resources.entries.withIndex()) {
            val (id, resource) = entry
            val events = sortedSetOf(horizon.start, horizon.end)
            val changes = HashMap<Instant, MutableList<Pair<Reservation, Boolean>>>()
            val initial = LinkedHashMap<Int, MutableList<Pair<Int, Flt64>>>()
            fun addEndpoint(
                time: Instant,
                reservation: Reservation? = null,
                starts: Boolean = false
            ) {
                if (time > horizon.start && time < horizon.end) {
                    events.add(time)
                    if (reservation != null) changes.getOrPut(time) { ArrayList() }.add(reservation to starts)
                }
            }
            for (reservation in byResource[id].orEmpty()) {
                val range = reservation.value.time
                val valid = range.start < range.end
                addEndpoint(
                    time = range.start,
                    reservation = if (valid) reservation else null,
                    starts = true
                )
                addEndpoint(
                    time = range.end,
                    reservation = if (valid) reservation else null,
                    starts = false
                )
                if (horizon.start >= range.start && horizon.start < range.end) {
                    initial.getOrPut(reservation.choice) { ArrayList() }.add(reservation.order to reservation.value.quantity)
                }
            }
            for (range in resource.source.unavailableTimes + resource.capacities.map { it.first }) {
                addEndpoint(range.start)
                addEndpoint(range.end)
            }
            val sums = HashMap<Int, OrderedActivitySum>()
            val coefficients = TreeMap<Int, Flt64>()
            fun update(
                choice: Int,
                added: List<Pair<Int, Flt64>>,
                removed: List<Int> = emptyList()
            ) {
                val sum = sums.getOrPut(choice) { OrderedActivitySum() }
                sum.update(added, removed)
                if (choice >= 0) {
                    if (sum.value.toDouble() != 0.0) coefficients[choice] = sum.value else coefficients.remove(choice)
                }
            }
            for ((choice, terms) in initial) update(choice, terms)
            for ((eventIndex, time) in events.toList().dropLast(1).withIndex()) {
                for ((choice, terms) in changes[time].orEmpty().groupBy { it.first.choice }) {
                    update(
                        choice = choice,
                        added = terms.filter { it.second }.map { it.first.order to it.first.value.quantity },
                        removed = terms.filter { !it.second }.map { it.first.order }
                    )
                }
                val fixed = sums[-1]?.value ?: Flt64.zero
                if (coefficients.isNotEmpty() || fixed.toDouble() != 0.0) {
                    rows.add(CompiledActivityRow(
                        id = "capacity_${resourceIndex}_$eventIndex",
                        coefficients = LinkedHashMap(coefficients),
                        upper = resource.capacityAt(time) - fixed
                    ))
                }
            }
        }
        return rows
    }
}
