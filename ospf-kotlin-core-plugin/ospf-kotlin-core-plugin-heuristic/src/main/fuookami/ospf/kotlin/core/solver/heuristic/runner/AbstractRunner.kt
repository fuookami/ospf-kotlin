/**
 * 搜索骨架 / Search skeleton
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.Archive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion

/**
 * 一级搜索骨架，只负责「迭代 / 档案 / 回调」这三件与算法无关的事。 / First-level search skeleton that owns only the three algorithm-agnostic concerns: iteration, archive, and callbacks.
 *
 * 具体算法只需实现三个钩子：[initialize] 建立初始状态、[advance] 推进一步、[membersOf] 把状态摊平成个体列表。
 * 这一步把「循环怎么转」与「一步算什么」彻底分开。 / A concrete algorithm implements three hooks only: [initialize] builds the initial state, [advance] performs one step,
 * and [membersOf] flattens the state into individuals. This fully separates "how the loop runs" from "what one step computes".
 *
 * @param State 算法状态类型 / Algorithm state type
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property archive 收敛档案 / Convergence archive
 * @property stopping 终止判据 / Stopping criterion
 * @property converter 值转换器 / Value converter
 * @property observers 迭代观测钩子，每轮结束后广播「是否改进」/ Iteration observers broadcasting "did this round improve" after each round
*/
abstract class AbstractRunner<State, ObjValue, V>(
    val archive: Archive<ObjValue, V>,
    val stopping: StoppingCriterion,
    val converter: IntoValue<V>,
    val observers: List<IterationObserver> = emptyList()
) where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 建立初始状态 / Build the initial state
     *
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 当前迭代 / Current iteration
     * @return 初始状态 / Initial state
    */
    protected abstract suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): State

    /**
     * 推进一步 / Advance one step
     *
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 当前迭代 / Current iteration
     * @param state 当前状态 / Current state
     * @return 推进后的状态 / Advanced state
    */
    protected abstract suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: State
    ): State

    /**
     * 把状态摊平为个体列表 / Flatten the state into individuals
     *
     * @param state 当前状态 / Current state
     * @return 个体列表 / Individual list
    */
    protected abstract fun membersOf(state: State): List<Individual<ObjValue, V>>

    /**
     * 执行搜索 / Run the search
     *
     * @param model 回调模型接口 / Callback model interface
     * @param iteration 迭代计数器 / Iteration counter
     * @param runningCallBack 每轮回调，返回非 `Ok` 时提前结束 / Per-round callback; returning non-`Ok` ends the search early
     * @return 档案中的个体，按优度排列 / Individuals in the archive, ordered by quality
    */
    open suspend fun run(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration = Iteration(),
        runningCallBack: ((Iteration, List<Individual<ObjValue, V>>) -> Try)? = null
    ): List<Individual<ObjValue, V>> {
        var state = initialize(model, iteration)
        archive.update(model, membersOf(state))
        iteration.next(true)

        while (!stopping(iteration)) {
            state = advance(model, iteration, state)
            val members = membersOf(state)
            val improved = archive.update(model, members)

            // 奖励信号只有骨架知道，因此在推进迭代前广播给观测者。自适应算子（如 Q 学习）据此更新自己的状态。 /
            // Only the skeleton knows the reward signal, so it is broadcast to observers before the iteration advances.
            // Adaptive operators (Q-learning, …) update their own state from it.
            for (observer in observers) {
                observer.observe(iteration, members, improved, converter)
            }

            model.flush()
            iteration.next(improved)

            if (runningCallBack != null && runningCallBack(iteration, members) is Failed) {
                break
            }
        }
        return archive.individuals
    }
}
