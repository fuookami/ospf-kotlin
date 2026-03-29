package fuookami.ospf.kotlin.core.frontend.model.status

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.algebra.number.UInt64

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

typealias ModelBuildingStatusCallBack = (ModelBuildingStatus) -> Try

