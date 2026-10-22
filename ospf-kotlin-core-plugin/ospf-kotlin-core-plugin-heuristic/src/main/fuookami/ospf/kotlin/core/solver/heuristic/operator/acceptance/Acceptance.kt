/**
 * 接受判据算子 / Acceptance criterion operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.compareIndividuals
import fuookami.ospf.kotlin.core.solver.heuristic.constraintHandling
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration

/**
 * 接受判据接口，决定候选解是否取代当前解。 / Acceptance criterion deciding whether a candidate replaces the current solution.
 *
 * 梯度下降、爬山、模拟退火、禁忌搜索的区别只在这一个算子。 / Hill climbing, simulated annealing, and tabu search differ only in this one operator.
*/
interface Acceptance {

    /**
     * 判断是否接受候选解 / Decide whether to accept the candidate
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param T 个体类型 / Individual type
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 当前迭代 / Current iteration
     * @param current 当前个体 / Current individual
     * @param candidate 候选个体 / Candidate individual
     * @return 是否接受 / Whether to accept
    */
    operator fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        current: T,
        candidate: T
    ): Boolean where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V>
}

/** 严格改进才接受，等价于爬山法 / Accept only on strict improvement, i.e. hill climbing */
data object BetterOnlyAcceptance : Acceptance {
    override fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        current: T,
        candidate: T
    ): Boolean where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return compareIndividuals(
            model = model,
            lhs = candidate,
            rhs = current
        ) < 0
    }
}

/** 不劣于当前解即接受，允许在平台上横向移动 / Accept anything not worse, allowing lateral moves across plateaus */
data object NotWorseAcceptance : Acceptance {
    override fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        current: T,
        candidate: T
    ): Boolean where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return compareIndividuals(
            model = model,
            lhs = candidate,
            rhs = current
        ) <= 0
    }
}

/** 无条件接受，用于纯随机游走 / Accept unconditionally, used for pure random walks */
data object AlwaysAcceptance : Acceptance {
    override fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        current: T,
        candidate: T
    ): Boolean where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        return (constraintHandling(model)?.compareTier(candidate, current) ?: 0) <= 0
    }
}
