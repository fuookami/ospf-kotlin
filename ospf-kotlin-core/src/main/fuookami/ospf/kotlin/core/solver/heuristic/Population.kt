package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.utils.functional.Order

interface Individual<V> {
    val solution: Solution
    val fitness: V
}

data class PopulationBuilder(
    val eliteAmount: UInt64,
    val densityRange: ValueRange<UInt64>,
    val mutationRateRange: ValueRange<Flt64>,
    val parentAmountRange: ValueRange<UInt64>
)

data class Population<T : Individual<V>, V>(
    val individuals: List<T>,
    val elites: List<T>,
    val best: T,
    val eliteAmount: UInt64,
    val densityRange: ValueRange<UInt64>,
    val mutationRateRange: ValueRange<Flt64>,
    val parentAmountRange: ValueRange<UInt64>
) {
    val density by individuals::size
}

data class SolutionWithFitness<V>(
    override val solution: Solution,
    override val fitness: V
) : Individual<V>

fun <T : Individual<V>, V> refreshGoodIndividuals(
    goodIndividuals: MutableList<T>,
    newIndividuals: List<T>,
    model: AbstractCallBackModelInterface<*, V>,
    solutionAmount: UInt64 = goodIndividuals.usize
) {
    var i = 0
    var j = 0
    while (i != goodIndividuals.size && j != newIndividuals.size) {
        if (model.compareObjective(newIndividuals[j].fitness, goodIndividuals[i].fitness) is Order.Less) {
            goodIndividuals.add(i, newIndividuals[j])
            ++i
            ++j
        } else {
            ++i
        }
    }
    if (j != newIndividuals.size) {
        goodIndividuals.addAll(
            newIndividuals.subList(
                j,
                minOf(newIndividuals.size, maxOf(j, solutionAmount.toInt() - goodIndividuals.size))
            )
        )
    }
}




