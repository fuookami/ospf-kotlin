@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service

import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.TaskId
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 有限选择、精确事件容量和依赖行的装配管线。 / Assembly pipeline for finite choices, exact event capacity and dependency rows. */
internal class ActivitySchedulingRows<V : RealNumber<V>>(private val data: CompiledActivityProblem<V>) {
    fun rows(): List<CompiledActivityRow> {
        val rows = mutableListOf<CompiledActivityRow>()
        for ((index, activity) in data.problem.activities.withIndex()) {
            rows.add(CompiledActivityRow(
                id = "selection_$index",
                coefficients = data.choices.indices.filter { data.choices[it].task == activity.id }.associateWith { Flt64.one },
                lower = Flt64.one,
                upper = Flt64.one
            ))
        }
        for ((index, baseline) in data.problem.baselines.withIndex()) {
            if (baseline.frozen) {
                rows.add(CompiledActivityRow(
                    id = "frozen_$index",
                    coefficients = mapOf(data.indices.getValue(baseline.choice) to Flt64.one),
                    lower = Flt64.one,
                    upper = Flt64.one
                ))
            }
        }
        rows.addAll(ActivityCapacityRows(data).rows())
        val facts = data.problem.executions.associate { it.placement.key.task to it.placement.time }
        fun choices(task: TaskId): List<Pair<Int?, TimeRange>> = facts[task]?.let { listOf(null to it) }
            ?: data.choices.mapIndexedNotNull { index, choice ->
                choice.placement?.takeIf { choice.task == task }?.let { index to it.time }
            }
        for ((dependencyIndex, dependency) in data.problem.dependencies.withIndex()) {
            val predecessors = choices(dependency.predecessor)
            val successors = choices(dependency.successor)
            fun requirePresence(from: List<Pair<Int?, TimeRange>>, to: List<Pair<Int?, TimeRange>>, suffix: String) {
                val coefficients = linkedMapOf<Int, Flt64>()
                for ((index, _) in from) {
                    if (index != null) coefficients[index] = (coefficients[index] ?: Flt64.zero) + Flt64.one
                }
                for ((index, _) in to) {
                    if (index != null) coefficients[index] = (coefficients[index] ?: Flt64.zero) - Flt64.one
                }
                val constant = Flt64(from.count { it.first == null }.toDouble() - to.count { it.first == null }.toDouble())
                rows.add(CompiledActivityRow("presence_${dependencyIndex}_$suffix", coefficients, upper = -constant))
            }
            if (dependency.presence != ActivityDependencyPresence.WhenBothPresent) {
                requirePresence(successors, predecessors, "predecessor")
            }
            if (dependency.presence == ActivityDependencyPresence.RequireBoth) {
                requirePresence(predecessors, successors, "successor")
            }
            var pair = 0
            for ((fromIndex, predecessor) in predecessors) {
                for ((toIndex, successor) in successors) {
                    if (data.compatible(dependency, predecessor, successor)) continue
                    val coefficients = linkedMapOf<Int, Flt64>()
                    for (index in listOfNotNull(fromIndex, toIndex)) {
                        coefficients[index] = (coefficients[index] ?: Flt64.zero) + Flt64.one
                    }
                    val constants = listOf(fromIndex, toIndex).count { it == null }
                    rows.add(CompiledActivityRow(
                        id = "dependency_${dependencyIndex}_${pair++}",
                        coefficients = coefficients,
                        upper = Flt64((1 - constants).toDouble())
                    ))
                }
            }
        }
        rows.addAll(data.extraRows.mapIndexed { index, row -> row.copy(id = "extra_$index") })
        rows.addAll(ActivityMaterialRows(data).rows())
        return rows
    }
}

/** 单个线性边界管线。 / Single linear-bound pipeline. */
internal class ActivityRowPipeline(private val row: CompiledActivityRow) {
    fun register(model: LinearMetaModel<Flt64>, compilation: ActivityCompilation, name: String): Try {
        for ((index, coefficient) in row.coefficients) {
            if (!coefficient.toDouble().isFinite()) {
                return activityFailure("行系数溢出 / Row coefficient overflow")
            }
        }
        val expression = LinearPolynomial(
            monomials = row.coefficients.map { (index, coefficient) -> LinearMonomial(coefficient, compilation.variables[index]) },
            constant = Flt64.zero
        )
        if (listOfNotNull(row.lower, row.upper).any { !it.toDouble().isFinite() }) {
            return activityFailure("行边界溢出 / Row bound overflow")
        }
        val lower = row.lower
        val upper = row.upper
        if (lower != null && upper != null && lower == upper) {
            return model.addConstraint(expression eq lower, name = "${name}_${row.id}_eq")
        }
        return run(
            { lower?.let { model.addConstraint(expression geq it, name = "${name}_${row.id}_lower") } ?: ok },
            { upper?.let { model.addConstraint(expression leq it, name = "${name}_${row.id}_upper") } ?: ok }
        )
    }
}
