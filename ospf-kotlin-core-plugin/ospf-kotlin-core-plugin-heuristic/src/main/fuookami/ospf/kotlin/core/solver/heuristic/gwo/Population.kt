package fuookami.ospf.kotlin.core.solver.heuristic.gwo

import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField

typealias Wolf<V> = SolutionWithFitness<V>

val <V> Wolf<V>.location: List<V> get() = solution

typealias AbstractPopulation<V> = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<V>, V>
typealias Population = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<Flt64>, Flt64>
typealias MulObjPopulation = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<List<Flt64>>, List<Flt64>>

fun <V> AbstractPopulation<V>.alpha(): Wolf<V> where V : RealNumber<V>, V : NumberField<V> = individuals[0]
fun <V> AbstractPopulation<V>.beta(): Wolf<V> where V : RealNumber<V>, V : NumberField<V> = individuals[1]
fun <V> AbstractPopulation<V>.delta(): Wolf<V> where V : RealNumber<V>, V : NumberField<V> = individuals[2]

val <V> List<Wolf<V>>.alpha: Wolf<V> get() = this[0]
val <V> List<Wolf<V>>.beta: Wolf<V> get() = this[1]
val <V> List<Wolf<V>>.delta: Wolf<V> get() = this[2]
