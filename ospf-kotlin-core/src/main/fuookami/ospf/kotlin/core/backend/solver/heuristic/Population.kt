package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.core.frontend.model.*

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

data class Population<T: Individual<V>, V>(
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
