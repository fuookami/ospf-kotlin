/**
 * Q 学习调参 / Q-learning parameter tuning
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.rl

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.runner.IterationObserver

/**
 * 用 Q 学习在多个候选参数演化源之间选择，实现「按搜索态势自适应调参」。 /
 * Selects among several candidate parameter sources by Q-learning, achieving "adaptive parameter tuning driven by
 * the search situation".
 *
 * 它同时是 [ParameterSource] 与 [IterationObserver]：骨架按 [ParameterSource] 取值，按 [IterationObserver]
 * 回传奖励。因此 Q 学习调参可以挂在任何接受 `ParameterSource` 的位置上——PSO 的惯性权重、学习因子，
 * SA 的温度，GWO 的收敛因子，都无需改动算法代码。 /
 * It is both a [ParameterSource] and an [IterationObserver]: the skeleton reads values through the former and
 * feeds rewards through the latter. Q-learning can therefore tune anything that accepts a `ParameterSource` — a PSO
 * inertia weight or learning factor, an SA temperature, a GWO convergence factor — with no change to the algorithm.
 *
 * @property candidates 候选参数演化源，即动作集合 / Candidate parameter sources, i.e. the action set
 * @property agent Q 学习代理 / Q-learning agent
*/
class QlearningSource(
    val candidates: List<ParameterSource>,
    val agent: QlearningAgent
) : ParameterSource, IterationObserver {

    override fun invoke(iteration: Iteration): Flt64 {
        if (candidates.isEmpty()) {
            return Flt64.zero
        }
        val index = agent.action.coerceIn(0, candidates.size - 1)
        return candidates[index](iteration)
    }

    override fun <ObjValue, V> observe(
        iteration: Iteration,
        members: List<Individual<ObjValue, V>>,
        improved: Boolean,
        converter: IntoValue<V>
    ) where V : RealNumber<V>, V : NumberField<V> {
        agent.step(iteration, members, improved, converter)
    }

    companion object {

        /**
         * 构造一个调参用的 Q 学习源 / Build a Q-learning source for parameter tuning
         *
         * @param candidates 候选参数演化源 / Candidate parameter sources
         * @param randomGenerator 均匀分布熵源 / Uniform entropy source
         * @param discretizer 状态离散化算子 / State discretization operator
         * @param selector 动作选择算子 / Action selection operator
         * @param reward 奖励算子 / Reward operator
         * @param alpha 学习率 / Learning rate
         * @param gamma 折扣因子 / Discount factor
         * @return Q 学习参数源 / Q-learning parameter source
        */
        operator fun invoke(
            candidates: List<ParameterSource>,
            randomGenerator: Generator<Flt64>,
            discretizer: StateDiscretizer = DiversityStagnationDiscretizer(),
            selector: ActionSelector = EpsilonGreedyActionSelector(),
            reward: Reward = ImprovementReward(),
            alpha: Flt64 = Flt64(0.5),
            gamma: Flt64 = Flt64(0.9)
        ): QlearningSource {
            return QlearningSource(
                candidates = candidates,
                agent = QlearningAgent(
                    discretizer = discretizer,
                    actionAmount = candidates.size,
                    selector = selector,
                    reward = reward,
                    alpha = alpha,
                    gamma = gamma,
                    randomGenerator = randomGenerator
                )
            )
        }
    }
}
