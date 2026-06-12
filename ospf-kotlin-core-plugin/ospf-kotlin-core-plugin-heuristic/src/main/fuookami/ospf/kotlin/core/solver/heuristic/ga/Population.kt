/** 遗传算法种群相关类型定义 / Genetic algorithm population type definitions */
package fuookami.ospf.kotlin.core.solver.heuristic.ga

import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness

/** 染色体类型，带有适应度的解 / Chromosome type, solution with fitness */
typealias Chromosome<ObjValue, V> = SolutionWithFitness<ObjValue, V>

/** 染色体的基因序列 / Gene sequence of chromosome */
val <ObjValue, V> Chromosome<ObjValue, V>.gene: List<V> where V : RealNumber<V>, V : NumberField<V>
    get() = solution

/** 抽象种群类型 / Abstract population type */
typealias AbstractPopulation<ObjValue, V> = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<ObjValue, V>, ObjValue, V>
/** 单目标种群类型 / Single-objective population type */
typealias Population = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<Flt64, Flt64>, Flt64, Flt64>
/** 多目标种群类型 / Multi-objective population type */
typealias MulObjPopulation = fuookami.ospf.kotlin.core.solver.heuristic.Population<Chromosome<List<Flt64>, Flt64>, List<Flt64>, Flt64>


