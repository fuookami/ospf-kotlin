package fuookami.ospf.kotlin.core.backend.plugins.heuristic.gwo

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.backend.solver.heuristic.*

typealias Wolf<V> = SolutionWithFitness<V>
val <V> Wolf<V>.location: List<Flt64> get() = solution

typealias AbstractPopulation<V> = fuookami.ospf.kotlin.core.backend.solver.heuristic.Population<Wolf<V>, V>
typealias Population = fuookami.ospf.kotlin.core.backend.solver.heuristic.Population<Wolf<Flt64>, Flt64>
typealias MulObjPopulation = fuookami.ospf.kotlin.core.backend.solver.heuristic.Population<Wolf<List<Flt64>>, List<Flt64>>

val <V> AbstractPopulation<V>.alpha: Wolf<V> get() = individuals[0]
val <V> AbstractPopulation<V>.beta: Wolf<V> get() = individuals[1]
val <V> AbstractPopulation<V>.delta: Wolf<V> get() = individuals[2]

val <V> List<Wolf<V>>.alpha: Wolf<V> get() = this[0]
val <V> List<Wolf<V>>.beta: Wolf<V> get() = this[1]
val <V> List<Wolf<V>>.delta: Wolf<V> get() = this[2]
