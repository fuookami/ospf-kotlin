/**
 * Q 学习代理 / Q-learning agent
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.rl

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.Individual

/**
 * Q 学习代理，把「状态离散化 + Q 表 + 动作选择 + 奖励」四件事组装成一个可复用内核。 /
 * Q-learning agent assembling the four pieces "state discretization + Q-table + action selection + reward" into
 * one reusable kernel.
 *
 * 代理本身不知道动作是什么：对 [QlearningSource] 而言动作是一个候选参数演化源，对 [QlearningUpdate] 而言
 * 动作是一个候选解更新算子。因此同一份 Q 学习内核同时服务于「调参」与「算子选择」两种用途。 /
 * The agent does not know what an action *is*: for [QlearningSource] an action is a candidate parameter source,
 * while for [QlearningUpdate] it is a candidate solution update. One kernel therefore serves both "parameter
 * tuning" and "operator selection".
 *
 * 学习率与折扣因子在构造时被夹取到 [0, 1]，因此运行期不存在需要上抛的非法参数，[step] 也就没有失败分支。 /
 * The learning rate and discount factor are clamped into [0, 1] at construction, so no illegal value can reach the
 * loop and [step] needs no failure branch.
 *
 * @property discretizer 状态离散化算子 / State discretization operator
 * @property actionAmount 动作数量 / Number of actions
 * @property selector 动作选择算子 / Action selection operator
 * @property reward 奖励算子 / Reward operator
 * @property table Q 表 / Q-table
 * @property randomGenerator 均匀分布熵源 / Uniform entropy source
*/
class QlearningAgent(
    val discretizer: StateDiscretizer,
    val actionAmount: Int,
    val selector: ActionSelector = EpsilonGreedyActionSelector(),
    val reward: Reward = ImprovementReward(),
    alpha: Flt64 = Flt64(0.5),
    gamma: Flt64 = Flt64(0.9),
    private val randomGenerator: Generator<Flt64>
) {

    /** 学习率，已夹取到 [0, 1] / Learning rate, clamped into [0, 1] */
    val alpha: Flt64 = Flt64(alpha.toDouble().coerceIn(0.0, 1.0))

    /** 折扣因子，已夹取到 [0, 1] / Discount factor, clamped into [0, 1] */
    val gamma: Flt64 = Flt64(gamma.toDouble().coerceIn(0.0, 1.0))

    /** Q 表 / Q-table */
    val table: QTable = QTable(discretizer.stateAmount.coerceAtLeast(1), actionAmount.coerceAtLeast(1))

    /** 当前动作 / Current action */
    var action: Int = 0
        private set

    /** 当前状态 / Current state */
    var state: Int = 0
        private set

    /** 当前状态下的最优动作 / Best action of the current state */
    val bestAction: Int get() = table.bestAction(state)

    /** 是否已经历过至少一轮，用于区分首轮的「建表」与后续的「更新」 / Whether at least one round happened, separating table bootstrapping from later updates */
    var started: Boolean = false
        private set

    /** 清空独立搜索的学习状态 / Clear learning state for an independent search. */
    fun reset() {
        table.clear()
        action = 0
        state = 0
        started = false
    }

    /**
     * 推进一轮：先按上一轮的结果更新 Q 值，再为下一轮挑选动作。 / Advance one round: update the Q value from the
     * previous round's outcome, then pick the action for the next round.
     *
     * @param V 值类型 / Value type
     * @param iteration 当前迭代 / Current iteration
     * @param members 本轮成员 / Members of this round
     * @param improved 档案最优是否被改善 / Whether the archive best improved
     * @param converter 值转换器 / Value converter
     * @return 更新后的动作价值；首轮建表或动作数为零时为 `null` / Updated action value; `null` while bootstrapping
     * or when there is no action
    */
    fun <V> step(
        iteration: Iteration,
        members: List<Individual<*, V>>,
        improved: Boolean,
        converter: IntoValue<V>
    ): Flt64? where V : RealNumber<V>, V : NumberField<V> {
        if (actionAmount <= 0 || table.stateAmount <= 0) {
            return null
        }

        val nextState = discretizer(iteration, members, converter).coerceIn(0, table.stateAmount - 1)
        var updated: Flt64? = null

        if (started) {
            val rewardValue = reward(improved)
            when (val result = table.update(state, action, rewardValue, nextState, alpha, gamma)) {
                is Ok -> updated = result.value
                // 参数已在构造时夹取、下标已在下方夹取，此分支不可达；保留显式分支以避免吞掉未来改动引入的错误。 /
                // Unreachable today (arguments are clamped here and indices above); kept explicit so future changes
                // cannot silently swallow an error.
                else -> updated = null
            }
        } else {
            started = true
        }

        state = nextState
        action = selector(table.valuesOf(state), randomGenerator).coerceIn(0, actionAmount - 1)
        return updated
    }
}
