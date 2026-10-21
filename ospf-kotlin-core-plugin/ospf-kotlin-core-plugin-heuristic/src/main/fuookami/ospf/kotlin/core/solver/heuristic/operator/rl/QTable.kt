/**
 * Q 表 / Q-table
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.rl

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Q 表，维护「状态 × 动作」的价值估计。 / Q-table maintaining value estimates over "state × action".
 *
 * 与旧实现的三处关键差异： / Three key differences from the legacy implementation:
 *
 * 1. 动作维度真实存在。旧实现用 `mutableListOf(Flt64(9))` 构造动作表，得到的是**只含一个元素的列表**，
 *    Q 表实际退化为标量；本实现按 [actionAmount] 构造完整的一行。 /
 *    The action dimension really exists. The legacy code built the action list with `mutableListOf(Flt64(9))`,
 *    which is a **single-element list**, collapsing the table into a scalar; this implementation allocates a full row.
 * 2. 时序差分使用**下一状态** [nextState] 的最大值，而不是当前状态自身的最大值。 /
 *    The temporal-difference target uses the maximum over [nextState], not over the current state itself.
 * 3. 越界与超参数非法通过 [Ret] 显式返回，不抛异常。 / Out-of-range indices and illegal hyper-parameters are
 *    returned explicitly as [Ret] instead of being thrown.
 *
 * @property stateAmount 状态数量 / Number of states
 * @property actionAmount 动作数量 / Number of actions
*/
class QTable(
    val stateAmount: Int,
    val actionAmount: Int
) {

    private val entries: MutableList<MutableList<Flt64>> =
        MutableList(stateAmount.coerceAtLeast(0)) {
            MutableList(actionAmount.coerceAtLeast(0)) { Flt64.zero }
        }

    /**
     * 读取某状态下的全部动作价值 / Read all action values of one state
     *
     * @param state 状态 / State
     * @return 动作价值列表；状态越界时为空 / Action values; empty when the state is out of range
    */
    fun valuesOf(state: Int): List<Flt64> {
        return if (state in 0 until stateAmount) entries[state].toList() else emptyList()
    }

    /**
     * 读取单个动作价值 / Read one action value
     *
     * @param state 状态 / State
     * @param action 动作 / Action
     * @return 动作价值；越界时为零 / Action value; zero when out of range
    */
    fun valueOf(state: Int, action: Int): Flt64 {
        return if (state in 0 until stateAmount && action in 0 until actionAmount) {
            entries[state][action]
        } else {
            Flt64.zero
        }
    }

    /**
     * 取某状态下的最优动作 / Take the best action of one state
     *
     * 并列时取下标最小者，保证同种子下可复现。 / Ties resolve to the lowest index so runs are reproducible under the same seed.
     *
     * @param state 状态 / State
     * @return 最优动作下标 / Best action index
    */
    fun bestAction(state: Int): Int {
        if (state !in 0 until stateAmount || actionAmount <= 0) {
            return 0
        }
        var best = 0
        var bestValue = entries[state][0]
        for (action in 1 until actionAmount) {
            if (entries[state][action] gr bestValue) {
                bestValue = entries[state][action]
                best = action
            }
        }
        return best
    }

    /**
     * 时序差分更新 / Temporal-difference update
     *
     * `Q(s, a) ← Q(s, a) + α · (r + γ · max_a' Q(s', a') − Q(s, a))`
     *
     * @param state 当前状态 / Current state
     * @param action 已执行动作 / Executed action
     * @param reward 奖励 / Reward
     * @param nextState 下一状态 / Next state
     * @param alpha 学习率，须位于 [0, 1] / Learning rate, must lie in [0, 1]
     * @param gamma 折扣因子，须位于 [0, 1] / Discount factor, must lie in [0, 1]
     * @return 更新后的动作价值；参数非法时返回 `Failed` / Updated action value; `Failed` when an argument is illegal
    */
    fun update(
        state: Int,
        action: Int,
        reward: Flt64,
        nextState: Int,
        alpha: Flt64,
        gamma: Flt64
    ): Ret<Flt64> {
        if (state !in 0 until stateAmount) {
            return Failed(ErrorCode.IllegalArgument, "state out of range: $state / 状态越界：$state")
        }
        if (action !in 0 until actionAmount) {
            return Failed(ErrorCode.IllegalArgument, "action out of range: $action / 动作越界：$action")
        }
        if (nextState !in 0 until stateAmount) {
            return Failed(ErrorCode.IllegalArgument, "nextState out of range: $nextState / 下一状态越界：$nextState")
        }
        val alphaValue = alpha.toDouble()
        if (alphaValue < 0.0 || alphaValue > 1.0) {
            return Failed(ErrorCode.IllegalArgument, "alpha must lie in [0, 1], got $alphaValue / 学习率须位于 [0, 1]")
        }
        val gammaValue = gamma.toDouble()
        if (gammaValue < 0.0 || gammaValue > 1.0) {
            return Failed(ErrorCode.IllegalArgument, "gamma must lie in [0, 1], got $gammaValue / 折扣因子须位于 [0, 1]")
        }

        // 下一状态的最大动作价值，而非当前状态自身的最大值。 / The maximum over the next state, not over the current one.
        val nextBest = entries[nextState].maxOrNull() ?: Flt64.zero
        val target = reward + gamma * nextBest
        val current = entries[state][action]
        val updated = current + alpha * (target - current)
        entries[state][action] = updated
        return ok(updated)
    }

    /** 清空 Q 表 / Clear the table */
    fun clear() {
        for (state in entries.indices) {
            for (action in entries[state].indices) {
                entries[state][action] = Flt64.zero
            }
        }
    }
}
