package fuookami.ospf.kotlin.core.solver.heuristic.operator.archive

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective.*

/** 将 Pareto 档案接入统一生命周期 / Adapt a Pareto archive to the common lifecycle.
 * @property capacity 容量 / Capacity
 * @property dominance 支配规则 / Dominance predicate
 */
class ParetoSearchArchive<V>(
    val capacity: Int,
    val dominance: Dominance
) : Archive<List<Flt64>, V> where V : RealNumber<V>, V : NumberField<V> {
    private fun fresh() = ParetoArchive<Individual<List<Flt64>, V>>(
        capacity = capacity,
        objectivesOf = { it.fitness },
        dominance = dominance
    )

    private var entries = fresh()
    override val individuals: List<Individual<List<Flt64>, V>> get() = entries.items

    override fun update(
        model: AbstractCallBackModelInterface<*, List<Flt64>, V>,
        candidates: List<Individual<List<Flt64>, V>>
    ): Boolean {
        val previous = entries.items.map { it.fitness }
        entries.offer(candidates)
        return entries.items.any { candidate ->
            previous.none { it == candidate.fitness || dominance.dominates(it, candidate.fitness) }
        }
    }

    override fun clear() {
        entries = fresh()
    }
}
