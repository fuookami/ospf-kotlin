package fuookami.ospf.kotlin.core.backend.plugins.heuristic.ga

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

typealias Chromosome<V> = SolutionWithFitness<V>
val <V> Chromosome<V>.gene: List<Flt64> get() = solution

typealias AbstractPopulation<T, V> = fuookami.ospf.kotlin.core.backend.solver.heuristic.Population<T, V>
typealias Population = fuookami.ospf.kotlin.core.backend.solver.heuristic.Population<Chromosome<Flt64>, Flt64>
typealias MulObjPopulation = fuookami.ospf.kotlin.core.backend.solver.heuristic.Population<Chromosome<List<Flt64>>, List<Flt64>>
