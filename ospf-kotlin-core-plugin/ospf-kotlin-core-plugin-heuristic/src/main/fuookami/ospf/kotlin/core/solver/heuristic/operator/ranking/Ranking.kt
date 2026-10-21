/**
 * 排序算子 / Ranking operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.ranking

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly

/**
 * 排序算子接口，把个体序列整理为算法期望的顺序。 / Ranking operator interface that arranges an individual sequence into the order expected by the algorithm.
*/
interface Ranking {

    /**
     * 对个体排序 / Rank individuals
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param T 个体类型 / Individual type
     * @param model 回调模型接口 / Callback model interface
     * @param individuals 待排序个体 / Individuals to rank
     * @return 排序后的个体 / Ranked individuals
    */
    operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        individuals: List<T>
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V>
}

/**
 * 目标值升序排序（越靠前越优）/ Ascending objective ranking (better individuals first)
*/
data object ObjectiveRanking : Ranking {
    override operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        individuals: List<T>
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return individuals.sortedWith { lhs, rhs ->
            compareStrictly(model, lhs.fitness, rhs.fitness)
        }
    }
}

/**
 * 目标值降序排序（越靠前越差）/ Descending objective ranking (worse individuals first)
*/
data object ReversedObjectiveRanking : Ranking {
    override operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        individuals: List<T>
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return individuals.sortedWith { lhs, rhs ->
            -compareStrictly(model, lhs.fitness, rhs.fitness)
        }
    }
}

/**
 * 保持输入顺序的恒等排序 / Identity ranking that preserves the input order
*/
data object IdentityRanking : Ranking {
    override operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        individuals: List<T>
    ): List<T> where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return individuals
    }
}
