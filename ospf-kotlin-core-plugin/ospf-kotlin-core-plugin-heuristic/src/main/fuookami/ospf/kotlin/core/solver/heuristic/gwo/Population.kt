/** 灰狼优化器种群相关类型定义 / Grey Wolf Optimizer population type definitions */
package fuookami.ospf.kotlin.core.solver.heuristic.gwo

import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness

/** 灰狼个体类型，带有适应度的解 / Wolf type, solution with fitness */
typealias Wolf<ObjValue, V> = SolutionWithFitness<ObjValue, V>

/** 灰狼个体的位置坐标 / Location coordinates of wolf */
val <ObjValue, V> Wolf<ObjValue, V>.location: List<V> where V : RealNumber<V>, V : NumberField<V>
    get() = solution

/** 抽象种群类型 / Abstract population type */
typealias AbstractPopulation<ObjValue, V> = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<ObjValue, V>, ObjValue, V>
/** 单目标种群类型 / Single-objective population type */
typealias Population = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<Flt64, Flt64>, Flt64, Flt64>
/** 多目标种群类型 / Multi-objective population type */
typealias MulObjPopulation = fuookami.ospf.kotlin.core.solver.heuristic.Population<Wolf<List<Flt64>, Flt64>, List<Flt64>, Flt64>

/**
 * 获取 alpha 狼 / Get alpha wolf
 *
 * @return alpha 狼 / alpha wolf
 */
fun <ObjValue, V> AbstractPopulation<ObjValue, V>.alpha(): Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> = individuals[0]
/**
 * 获取 beta 狼 / Get beta wolf
 *
 * @return beta 狼 / beta wolf
 */
fun <ObjValue, V> AbstractPopulation<ObjValue, V>.beta(): Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> = individuals[1]
/**
 * 获取 delta 狼 / Get delta wolf
 *
 * @return delta 狼 / delta wolf
 */
fun <ObjValue, V> AbstractPopulation<ObjValue, V>.delta(): Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> = individuals[2]

/** alpha 狼 / alpha wolf */
val <ObjValue, V> List<Wolf<ObjValue, V>>.alpha: Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V>
    get() = this[0]
/** beta 狼 / beta wolf */
val <ObjValue, V> List<Wolf<ObjValue, V>>.beta: Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V>
    get() = this[1]
/** delta 狼 / delta wolf */
val <ObjValue, V> List<Wolf<ObjValue, V>>.delta: Wolf<ObjValue, V> where V : RealNumber<V>, V : NumberField<V>
    get() = this[2]
