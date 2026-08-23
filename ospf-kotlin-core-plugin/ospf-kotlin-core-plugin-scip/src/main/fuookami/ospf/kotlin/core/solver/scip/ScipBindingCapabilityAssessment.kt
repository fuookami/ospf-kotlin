/** JSCIP binding capability assessment. / JSCIP binding 能力评估。 */
package fuookami.ospf.kotlin.core.solver.scip

/** Incremental capability exposed by the current JSCIP binding. / 当前 JSCIP binding 暴露的增量能力。
 *
 * @property bindingVersion Binding version assessed. / 已评估的 binding 版本。
 * @property incrementalBoundUpdates Whether native bound updates are available. / 是否支持原生边界增量更新。
 * @property probing Whether probing transactions are available. / 是否支持 probing 事务。
 * @property conflictGraph Whether a native conflict graph is available. / 是否支持原生冲突图。
 * @property fallback Fallback strategy used when native support is absent. / 缺少原生能力时使用的降级策略。
 * @property reasons Reasons for the capability decisions. / 能力判断原因。
 */
data class ScipBindingCapabilityAssessment(
    val bindingVersion: String,
    val incrementalBoundUpdates: Boolean,
    val probing: Boolean,
    val conflictGraph: Boolean,
    val fallback: String,
    val reasons: List<String>
)

/**
 * 返回当前 binding 的事实能力，不把模型重建误报为原生增量能力。 /
 * Reports factual binding capabilities without presenting model rebuilds as native incremental support.
 */
object ScipBindingCapabilityAssessmentProvider {
    /** Assess the JSCIP API available to this plugin. / 评估插件当前可用的 JSCIP API。
     *
     * @return Factual binding capability assessment. / 事实性的 binding 能力评估。
     */
    fun current(): ScipBindingCapabilityAssessment {
        return ScipBindingCapabilityAssessment(
            bindingVersion = "jscip-1.0.0",
            incrementalBoundUpdates = false,
            probing = false,
            conflictGraph = false,
            fallback = "model-rebuild",
            reasons = listOf(
                "当前 JSCIP binding 未暴露 safe incremental bound mutation / " +
                    "The current JSCIP binding exposes no safe incremental bound mutation",
                "当前 JSCIP binding 未暴露 probing transaction / " +
                    "The current JSCIP binding exposes no probing transaction",
                "当前 JSCIP binding 未暴露 conflict graph API / " +
                    "The current JSCIP binding exposes no conflict graph API"
            )
        )
    }
}
