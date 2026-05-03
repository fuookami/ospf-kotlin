package fuookami.ospf.kotlin.core.solver.heuristic.ga

import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness
import fuookami.ospf.kotlin.math.algebra.number.Flt64

typealias Chromosome<V> = SolutionWithFitness<V>

val <V> Chromosome<V>.gene: List<V> get() = solution

typealias AbstractPopulation<V> = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<V>, V>
typealias Population = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<Flt64>, Flt64>
typealias MulObjPopulation = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<List<Flt64>>, List<Flt64>>



