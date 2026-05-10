package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStage
import fuookami.ospf.kotlin.core.model.basic.ModelBuildingStatus
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

data class IntermediateModelDumpingStatus(
    val readyConstraintAmount: UInt64,
    val totalConstraintAmount: UInt64,
) {
    val totalProgress: Flt64 get() = readyConstraintAmount.toFlt64() / totalConstraintAmount.toFlt64()
}

typealias IntermediateModelDumpingStatusCallBack = (IntermediateModelDumpingStatus) -> Try

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



