/**
 * 中间模型转储状态
 * Intermediate model dumping status
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStage
import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStatus

/**
 * 中间模型转储进度状态，跟踪约束的准备进度。
 * Intermediate model dumping progress status tracking constraint readiness.
 *
 * @property readyConstraintAmount 已就绪约束数量 / Number of ready constraints
 * @property totalConstraintAmount 约束总数 / Total number of constraints
 */
data class IntermediateModelDumpingStatus(
    val readyConstraintAmount: UInt64,
    val totalConstraintAmount: UInt64,
) {
    val totalProgress: Flt64 get() = readyConstraintAmount.toFlt64() / totalConstraintAmount.toFlt64()
}

/** 中间模型转储状态回调 / Intermediate model dumping status callback */
typealias IntermediateModelDumpingStatusCallBack = (IntermediateModelDumpingStatus) -> Try

/**
 * 将中间模型转储状态转换为模型构建状态。
 * Convert intermediate model dumping status to model building status.
 */
fun IntermediateModelDumpingStatus.toModelBuildingStatus(
    modelName: String,
    quadratic: Boolean = false
): ModelBuildingStatus {
    return ModelBuildingStatus(
        modelName = modelName,
        stage = if (quadratic) {
            ModelBuildingStage.FlattenQuadraticModel
        } else {
            ModelBuildingStage.FlattenLinearModel
        },
        ready = readyConstraintAmount,
        total = totalConstraintAmount
    )
}
