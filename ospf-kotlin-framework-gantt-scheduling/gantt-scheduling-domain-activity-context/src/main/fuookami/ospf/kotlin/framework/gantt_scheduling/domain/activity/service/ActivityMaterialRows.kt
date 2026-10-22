@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 逐事件累计库存约束，不允许用未来产出补偿过去缺料。 / Event-wise cumulative stock rows; future supply cannot cover past shortages. */
internal class ActivityMaterialRows<V : RealNumber<V>>(private val data: CompiledActivityProblem<V>) {
    fun rows(): List<CompiledActivityRow> {
        val rows = mutableListOf<CompiledActivityRow>()
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
