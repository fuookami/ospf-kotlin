package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*

data class Chromosome(
    val fitness: Flt64?,
    val gene: List<Flt64>
) {
    val size by gene::size
}

data class Population(
    val chromosomes: List<Chromosome>,
    val elites: List<Chromosome>,
    val best: Chromosome,
    val densityRange: ValueRange<UInt64>,
    val mutationRange: ValueRange<Flt64>,
    val parentAmountRange: ValueRange<UInt64>
) {
    val density by chromosomes::size
}
