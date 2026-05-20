package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.utils.functional.Order

interface Individual<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    val solution: Solution<V>
    val fitness: ObjValue
}

data class PopulationBuilder(
    val eliteAmount: UInt64,
    val densityRange: ValueRange<UInt64>,
    val mutationRateRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    val parentAmountRange: ValueRange<UInt64>
)

data class Population<T : Individual<ObjValue, V>, ObjValue, V>(
    val individuals: List<T>,
    val elites: List<T>,
    val best: T,
    val eliteAmount: UInt64,
    val densityRange: ValueRange<UInt64>,
    val mutationRateRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    val parentAmountRange: ValueRange<UInt64>
) where V : RealNumber<V>, V : NumberField<V> {
    val density by individuals::size
}

data class SolutionWithFitness<ObjValue, V>(
    override val solution: Solution<V>,
    override val fitness: ObjValue
) : Individual<ObjValue, V> where V : RealNumber<V>, V : NumberField<V>

fun <T : Individual<ObjValue, V>, ObjValue, V> refreshGoodIndividuals(
    goodIndividuals: MutableList<T>,
    newIndividuals: List<T>,
    model: AbstractCallBackModelInterface<*, ObjValue, V>,
    solutionAmount: UInt64 = goodIndividuals.usize
) where V : RealNumber<V>, V : NumberField<V> {
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



