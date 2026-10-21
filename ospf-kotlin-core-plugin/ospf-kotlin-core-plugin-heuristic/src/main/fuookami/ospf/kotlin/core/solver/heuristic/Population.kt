/**
 * 个体数据结构 / Individual data structures
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution

/**
 * 个体接口，表示启发式搜索中的一个解及其适应度。 / Individual interface, representing a solution and its fitness in heuristic search.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
*/
interface Individual<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    /** Solution represented by this individual / 此个体表示的解 */
    val solution: Solution<V>

    /** Fitness value of this individual / 此个体的适应度值 */
    val fitness: ObjValue
}

/**
 * 带适应度的解，实现 [Individual] 接口。 / Solution with fitness, implementing [Individual] interface.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property solution 解 / Solution
 * @property fitness 适应度 / Fitness
*/
data class SolutionWithFitness<ObjValue, V>(
    override val solution: Solution<V>,
    override val fitness: ObjValue
) : Individual<ObjValue, V> where V : RealNumber<V>, V : NumberField<V>
