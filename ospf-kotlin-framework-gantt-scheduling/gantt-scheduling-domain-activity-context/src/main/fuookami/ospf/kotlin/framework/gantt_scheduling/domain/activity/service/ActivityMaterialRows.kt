@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service

import java.util.TreeMap
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 逐事件累计库存约束，不允许用未来产出补偿过去缺料。 / Event-wise cumulative stock rows; future supply cannot cover past shortages. */
internal class ActivityMaterialRows<V : RealNumber<V>>(private val data: CompiledActivityProblem<V>) {
    private data class Event(
        val choice: Int,
        val order: Int,
        val value: CompiledActivityMaterialEvent
    )

    fun rows(): List<CompiledActivityRow> {
        val rows = mutableListOf<CompiledActivityRow>()
        val indexedEvents = ArrayList<Event>()
        for (event in data.materialEvents) indexedEvents.add(Event(
            choice = -1,
            order = indexedEvents.size,
            value = event
        ))
        for ((choice, item) in data.choices.withIndex()) {
            for (event in item.materialEvents) indexedEvents.add(Event(
                choice = choice,
                order = indexedEvents.size,
                value = event
            ))
        }
        val byMaterial = indexedEvents.groupBy { it.value.material }
        for ((materialIndex, material) in data.materials.values.withIndex()) {
            val id = material.source.id
            rows.add(CompiledActivityRow(
                id = "stock_${materialIndex}_opening",
                coefficients = emptyMap(),
                lower = material.minimum - material.initial,
                upper = material.maximum?.let { it - material.initial }
            ))
            val events = byMaterial[id].orEmpty().groupBy { it.value.time }
            val times = (listOf(data.problem.horizon.start, data.problem.horizon.end) + events.keys).distinct().sorted()
            val fixedSum = OrderedActivitySum(material.initial)
            val sums = HashMap<Int, OrderedActivitySum>()
            val coefficients = TreeMap<Int, Flt64>()
            for ((eventIndex, time) in times.withIndex()) {
                for ((choice, entries) in events[time].orEmpty().groupBy { it.choice }) {
                    val sum = if (choice == -1) fixedSum else sums.getOrPut(choice) { OrderedActivitySum() }
                    sum.update(entries.map { it.order to it.value.quantity })
                    if (choice >= 0) {
                        if (sum.value.toDouble() != 0.0) coefficients[choice] = sum.value else coefficients.remove(choice)
                    }
                }
                val fixed = fixedSum.value
                rows.add(CompiledActivityRow(
                    id = "stock_${materialIndex}_$eventIndex",
                    coefficients = LinkedHashMap(coefficients),
                    lower = material.minimum - fixed,
                    upper = material.maximum?.let { it - fixed }
                ))
            }
        }
        return rows
    }
}
