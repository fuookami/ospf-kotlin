/**
 * 奖励算子 / Reward operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.rl

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 奖励算子契约：把「这一轮有没有变好」翻译成标量奖励。 / Reward contract translating "did this round improve"
 * into a scalar reward.
 *
 * 奖励是 Q 学习与具体算法之间唯一的耦合点：不同算法可以换不同的奖励而不必改 Q 表与选择器。 /
 * The reward is the only coupling between Q-learning and a concrete algorithm: different algorithms can swap the
 * reward without touching the table or the selector.
*/
fun interface Reward {

    /**
     * 计算奖励 / Compute the reward
     *
     * @param improved 档案最优是否被改善 / Whether the archive best improved
     * @return 奖励值 / Reward value
    */
    operator fun invoke(improved: Boolean): Flt64
}

/**
 * 改进奖励：变好给正奖励，变差给负奖励。 / Improvement reward: positive when improved, negative otherwise.
 *
 * @property positive 改进时的奖励 / Reward when improved
 * @property negative 未改进时的奖励 / Reward when not improved
*/
data class ImprovementReward(
    val positive: Flt64 = Flt64.one,
    val negative: Flt64 = Flt64(-0.5)
) : Reward {

    override fun invoke(improved: Boolean): Flt64 = if (improved) positive else negative
}

/**
 * 对称奖励：改进给 [magnitude]，未改进给它的相反数。 / Symmetric reward: [magnitude] when improved, its
 * negation otherwise.
 *
 * @property magnitude 奖励幅度 / Reward magnitude
*/
data class SymmetricReward(
    val magnitude: Flt64 = Flt64.one
) : Reward {

    override fun invoke(improved: Boolean): Flt64 = if (improved) magnitude else -magnitude
}
