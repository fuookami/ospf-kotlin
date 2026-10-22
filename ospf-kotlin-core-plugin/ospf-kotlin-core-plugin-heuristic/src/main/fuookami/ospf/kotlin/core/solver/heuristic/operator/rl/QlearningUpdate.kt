/**
 * Q 学习算子选择 / Q-learning operator selection
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.rl

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.runner.IterationObserver

/**
 * 用 Q 学习在多个候选解更新算子之间选择。 / Selects among several candidate solution-update operators by Q-learning.
 *
 * 这是「算子选择」而非「调参」：被选中的是整个更新规则本身，例如在灰狼更新与正弦余弦更新之间切换，
 * 或者在一组交叉算子之间切换。骨架只看到一个 [SolutionUpdate]，因而无需知道存在多个候选。 /
 * This is operator *selection* rather than parameter tuning: what gets selected is the update rule itself — switching
 * between a grey-wolf update and a sine-cosine update, say, or among a set of crossovers. The skeleton sees a single
 * [SolutionUpdate] and never learns that several candidates exist.
 *
 * @property candidates 候选解更新算子，即动作集合 / Candidate solution updates, i.e. the action set
 * @property agent Q 学习代理 / Q-learning agent
*/
class QlearningUpdate(
    val candidates: List<SolutionUpdate>,
    val agent: QlearningAgent
) : SolutionUpdate, IterationObserver {
    override fun reset() = agent.reset()


    override fun <V, T> invoke(
        iteration: Iteration,
        parents: List<T>,
        model: AbstractCallBackModelInterface<*, *, V>,
        converter: IntoValue<V>,
        entropy: Generator<Flt64>
    ): List<Solution<V>> where V : RealNumber<V>, V : NumberField<V>, T : Individual<*, V> {
        if (candidates.isEmpty()) {
            return emptyList()
        }
        val index = agent.action.coerceIn(0, candidates.size - 1)
        return candidates[index](iteration, parents, model, converter, entropy)
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
         * 构造一个算子选择用的 Q 学习更新 / Build a Q-learning update for operator selection
         *
         * @param candidates 候选解更新算子 / Candidate solution updates
         * @param randomGenerator 均匀分布熵源 / Uniform entropy source
         * @param discretizer 状态离散化算子 / State discretization operator
         * @param selector 动作选择算子 / Action selection operator
         * @param reward 奖励算子 / Reward operator
         * @param alpha 学习率 / Learning rate
         * @param gamma 折扣因子 / Discount factor
         * @return Q 学习解更新 / Q-learning solution update
        */
        operator fun invoke(
            candidates: List<SolutionUpdate>,
            randomGenerator: Generator<Flt64>,
            discretizer: StateDiscretizer = DiversityStagnationDiscretizer(),
            selector: ActionSelector = EpsilonGreedyActionSelector(),
            reward: Reward = ImprovementReward(),
            alpha: Flt64 = Flt64(0.5),
            gamma: Flt64 = Flt64(0.9)
        ): QlearningUpdate {
            return QlearningUpdate(
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
