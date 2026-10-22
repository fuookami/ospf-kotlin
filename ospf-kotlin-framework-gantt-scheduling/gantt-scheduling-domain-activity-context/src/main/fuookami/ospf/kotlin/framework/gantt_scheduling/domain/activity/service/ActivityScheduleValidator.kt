@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 不读取模型行的独立排程复核。 / Independent schedule validation without reading generated model rows. */
internal class ActivityScheduleValidator<V : RealNumber<V>>(private val data: CompiledActivityProblem<V>) {
    fun validate(keys: List<ActivityChoiceKey>): ActivityScheduleValidation {
        val violations = mutableListOf<String>()
        if (keys.toSet().size != keys.size) violations.add("duplicate_choice")
        if (keys.any { it !in data.indices }) violations.add("unknown_choice")
        val indices = keys.mapNotNull { data.indices[it] }.toSet()
        val selected = indices.map { data.choices[it] }
        val placements = selected.mapNotNull { it.placement }
        val byTask = placements.groupBy { it.key.task }
        for (activity in data.problem.activities) {
            val count = byTask[activity.id].orEmpty().size
            if (count > 1) violations.add("multiple_modes:${activity.id}")
            if (count == 0 && activity.cancellationCost == null) violations.add("missing_required:${activity.id}")
        }
        for (baseline in data.problem.baselines) {
            if (baseline.frozen && baseline.choice !in keys) violations.add("frozen:${baseline.choice.task}")
        }
        val times = (placements + data.problem.executions.map { it.placement }).associate { it.key.task to it.time }
        for ((index, dependency) in data.problem.dependencies.withIndex()) {
            val predecessor = times[dependency.predecessor]
            val successor = times[dependency.successor]
            if (successor != null && predecessor == null && dependency.presence != ActivityDependencyPresence.WhenBothPresent ||
                predecessor != null && successor == null && dependency.presence == ActivityDependencyPresence.RequireBoth
            ) {
                violations.add("dependency_presence:$index")
            }
            if (predecessor != null && successor != null && !data.compatible(dependency, predecessor, successor)) {
                violations.add("dependency_time:$index")
            }
        }
        val horizon = data.problem.horizon
        val reservations = selected.flatMap { it.reservations } + data.fixedReservations
        for ((id, resource) in data.resources) {
            val uses = reservations.filter { it.resource == id }
            val intervals = uses.map { it.time } + resource.source.unavailableTimes + resource.capacities.map { it.first }
            val events = (listOf(horizon.start) + intervals.flatMap { listOf(it.start, it.end) })
                .filter { it >= horizon.start && it < horizon.end }.distinct().sorted()
            for (time in events) {
                val usage = uses.filter { time >= it.time.start && time < it.time.end }
                    .fold(Flt64.zero) { sum, use -> sum + use.quantity }
                if (!usage.toDouble().isFinite() || (usage gr resource.capacityAt(time))) {
                    violations.add("capacity:$id:$time")
                }
            }
        }
        val selectedWithCancellation = indices + data.cancellations.filterKeys { it !in byTask }.values
        for (row in data.extraRows) {
            val value = row.coefficients.filterKeys { it in selectedWithCancellation }.values.fold(Flt64.zero) { sum, amount -> sum + amount }
            if (!value.toDouble().isFinite() || row.lower?.let { value ls it } == true || row.upper?.let { value gr it } == true) {
                violations.add("extra_row:${row.id}")
            }
        }
        violations.addAll(ActivityMaterialAudit(data).violations(indices))
        return ActivityScheduleValidation(violations.toList())
    }
}
