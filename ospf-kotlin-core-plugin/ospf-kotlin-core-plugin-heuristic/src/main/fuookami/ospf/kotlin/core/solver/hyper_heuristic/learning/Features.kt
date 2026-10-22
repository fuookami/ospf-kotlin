package fuookami.ospf.kotlin.core.solver.hyper_heuristic.learning

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.selection.SelectionContext

/** 搜索进度快照 / Search progress snapshot.
 * @property decision 已派发决策 / Dispatched decisions
 * @property evaluations 已用评估 / Consumed evaluations
 * @property evaluationLimit 评估上限 / Evaluation limit
 * @property stagnation 连续无进展次数 / Consecutive stagnant outcomes
 * @property diversity 工作解集的不同解比例 / Distinct-solution ratio in the working set
 * @property successRate 已归并动作成功率 / Success ratio of merged actions
 * @property cost 上一动作的相对评估成本 / Relative evaluation cost of the last action
 */
data class SearchProgress(
    val decision: Long,
    val evaluations: Long,
    val evaluationLimit: Long,
    val stagnation: Long,
    val diversity: Double,
    val successRate: Flt64 = Flt64.zero,
    val cost: Flt64 = Flt64.zero
)

/** 可替换的无量纲特征提取 / Replaceable dimensionless feature extraction. */
interface FeatureExtractor {
    /** 特征协议版本 / Feature protocol version. */
    val version: String

    /** 离散状态空间大小，null 表示不设限 / Discrete state-space size, null means unrestricted. */
    val stateCount: Int? get() = null

    /** 提取选择状态 / Extract selection state.
     * @param progress 搜索进度 / Search progress
     * @return 选择上下文 / Selection context
     */
    fun extract(progress: SearchProgress): SelectionContext
}

/** 进度与停滞的 8 x 8 离散化 / An 8 x 8 progress/stagnation discretizer. */
object ProgressFeatures : FeatureExtractor {
    override val version = "progress:1"
    override val stateCount = 64

    override fun extract(progress: SearchProgress): SelectionContext {
        val ratio = if (progress.evaluationLimit == Long.MAX_VALUE) 0.0 else progress.evaluations.toDouble() / progress.evaluationLimit.coerceAtLeast(1)
        val bounded = ratio.coerceIn(0.0, 1.0)
        return SelectionContext(
            state = (bounded * 7).toInt() * 8 + progress.stagnation.coerceIn(0, 7).toInt(),
            features = listOf(bounded, progress.stagnation.coerceAtMost(1000).toDouble() / 1000, progress.diversity).map { Flt64(it) } + listOf(progress.successRate, progress.cost),
            decision = progress.decision,
            stagnation = progress.stagnation
        )
    }
}

/** 奖励输入，不混淆接受与档案进展 / Reward input separating acceptance from archive progress.
 * @property improved 相对参考档案是否改进 / Improvement against the reference archive
 * @property contribution 有效新贡献比例 / Effective novel contribution ratio
 * @property evaluations 评估成本 / Evaluation cost
 */
data class RewardContext(val improved: Boolean, val contribution: Flt64, val evaluations: Long)

/** 可替换奖励 / Replaceable reward. */
fun interface RewardPolicy {
    /** 计算有限奖励 / Compute a finite reward.
     * @param context 动作结果 / Action outcome
     * @return 无量纲奖励 / Dimensionless reward
     */
    fun reward(context: RewardContext): Flt64
}

/** 单目标改善或 Pareto 新贡献奖励 / Improvement or novel Pareto contribution reward. */
object ImprovementRewardPolicy : RewardPolicy {
    override fun reward(context: RewardContext): Flt64 = if (context.improved) Flt64.one else Flt64.zero
}

/** 按评估成本归一化贡献 / Contribution normalized by evaluation cost. */
object CostAwareRewardPolicy : RewardPolicy {
    override fun reward(context: RewardContext): Flt64 = context.contribution / Flt64(context.evaluations.coerceAtLeast(1).toDouble())
}
