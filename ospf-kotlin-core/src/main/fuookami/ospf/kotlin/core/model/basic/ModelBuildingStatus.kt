/**
 * 模型构建状态
 * Model building status
 */
package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 模型构建状态，包含模型名称、当前阶段和进度信息。
 * Model building status including model name, current stage, and progress.
 *
 * @property modelName 模型名称 / The model name
 * @property stage     当前构建阶段 / Current building stage
 * @property ready     已就绪数量 / Number of ready items
 * @property total     总数量 / Total number of items
 */
data class ModelBuildingStatus(
    val modelName: String,
    val stage: ModelBuildingStage,
    val ready: UInt64,
    val total: UInt64
) {
    val progress: Flt64
        get() = if (total neq UInt64.zero) {
            ready.toFlt64() / total.toFlt64()
        } else {
            Flt64.one
        }
}

/** 模型构建状态回调 / Model building status callback */
typealias ModelBuildingStatusCallBack = (ModelBuildingStatus) -> Try
