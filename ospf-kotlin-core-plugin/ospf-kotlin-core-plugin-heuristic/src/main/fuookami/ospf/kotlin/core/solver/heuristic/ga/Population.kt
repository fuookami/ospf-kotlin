package fuookami.ospf.kotlin.core.solver.heuristic.ga

import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

typealias Chromosome<ObjValue, V> = SolutionWithFitness<ObjValue, V>

val <ObjValue, V> Chromosome<ObjValue, V>.gene: List<V> where V : RealNumber<V>, V : NumberField<V>
    get() = solution

typealias AbstractPopulation<ObjValue, V> = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<ObjValue, V>, ObjValue, V>
typealias Population = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<Flt64, Flt64>, Flt64, Flt64>
typealias MulObjPopulation = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<List<Flt64>, Flt64>, List<Flt64>, Flt64>


