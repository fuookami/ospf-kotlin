package fuookami.ospf.kotlin.core.solver.heuristic.gwo

import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField

typealias Wolf<ObjValue, V> = SolutionWithFitness<ObjValue, V>

val <ObjValue, V> Wolf<ObjValue, V>.location: List<V> where V : RealNumber<V>, V : NumberField<V>
    get() = solution

typealias AbstractPopulation<ObjValue, V> = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<ObjValue, V>, ObjValue, V>
typealias Population = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<Flt64, Flt64>, Flt64, Flt64>
typealias MulObjPopulation = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<List<Flt64>, Flt64>, List<Flt64>, Flt64>

fun <ObjValue, V> AbstractPopulation<ObjValue, V>.alpha(): Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> = individuals[0]
fun <ObjValue, V> AbstractPopulation<ObjValue, V>.beta(): Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> = individuals[1]
fun <ObjValue, V> AbstractPopulation<ObjValue, V>.delta(): Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> = individuals[2]

val <ObjValue, V> List<Wolf<ObjValue, V>>.alpha: Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V>
    get() = this[0]
val <ObjValue, V> List<Wolf<ObjValue, V>>.beta: Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V>
    get() = this[1]
val <ObjValue, V> List<Wolf<ObjValue, V>>.delta: Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V>
    get() = this[2]
