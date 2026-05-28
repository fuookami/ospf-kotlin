/**
 * 种群与个体数据结构
 * Population and individual data structures
 */
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface

/**
 * 个体接口，表示启发式搜索中的一个解及其适应度。
 * Individual interface, representing a solution and its fitness in heuristic search.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 */
interface Individual<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {
    val solution: Solution<V>
    val fitness: ObjValue
}

/**
 * 种群构建器，封装种群构建参数。
 * Population builder, encapsulating population construction parameters.
 *
 * @property eliteAmount 精英数量 / Elite amount
 * @property densityRange 密度范围 / Density range
 * @property mutationRateRange 变异率范围 / Mutation rate range
 * @property parentAmountRange 父代数量范围 / Parent amount range
 */
data class PopulationBuilder(
    val eliteAmount: UInt64,
    val densityRange: ValueRange<UInt64>,
    val mutationRateRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    val parentAmountRange: ValueRange<UInt64>
)

/**
 * 种群数据结构，包含个体列表、精英和最优个体。
 * Population data structure, containing individual list, elites, and best individual.
 *
 * @param T 个体类型 / Individual type
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property individuals 个体列表 / Individual list
 * @property elites 精英列表 / Elite list
 * @property best 最优个体 / Best individual
 * @property eliteAmount 精英数量 / Elite amount
 * @property densityRange 密度范围 / Density range
 * @property mutationRateRange 变异率范围 / Mutation rate range
 * @property parentAmountRange 父代数量范围 / Parent amount range
 */
data class Population<T : Individual<ObjValue, V>, ObjValue, V>(
    val individuals: List<T>,
    val elites: List<T>,
    val best: T,
    val eliteAmount: UInt64,
    val densityRange: ValueRange<UInt64>,
    val mutationRateRange: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    val parentAmountRange: ValueRange<UInt64>
) where V : RealNumber<V>, V : NumberField<V> {
    val density by individuals::size
}

/**
 * 带适应度的解，实现 [Individual] 接口。
 * Solution with fitness, implementing [Individual] interface.
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

/**
 * 刷新优良个体列表，合并新个体并保持有序。
 * Refresh the good individuals list, merging new individuals while maintaining order.
 *
 * @param T 个体类型 / Individual type
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param goodIndividuals 现有优良个体列表（可变）/ Existing good individuals list (mutable)
 * @param newIndividuals 新个体列表 / New individuals list
 * @param model 回调模型接口 / Callback model interface
 * @param solutionAmount 期望保留的解数量 / Desired number of solutions to keep
 */
fun <T : Individual<ObjValue, V>, ObjValue, V> refreshGoodIndividuals(
    goodIndividuals: MutableList<T>,
    newIndividuals: List<T>,
    model: AbstractCallBackModelInterface<*, ObjValue, V>,
    solutionAmount: UInt64 = goodIndividuals.usize
) where V : RealNumber<V>, V : NumberField<V> {
    var i = 0
    var j = 0
    while (i != goodIndividuals.size && j != newIndividuals.size) {
        if (model.compareObjective(newIndividuals[j].fitness, goodIndividuals[i].fitness) is Order.Less) {
            goodIndividuals.add(i, newIndividuals[j])
            ++i
            ++j
        } else {
            ++i
        }
    }
    if (j != newIndividuals.size) {
        goodIndividuals.addAll(
            newIndividuals.subList(
                j,
                minOf(newIndividuals.size, maxOf(j, solutionAmount.toInt() - goodIndividuals.size))
            )
        )
    }
}
