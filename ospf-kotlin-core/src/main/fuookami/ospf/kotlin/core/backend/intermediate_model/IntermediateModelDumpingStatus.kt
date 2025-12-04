package fuookami.ospf.kotlin.core.backend.intermediate_model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class IntermediateModelDumpingStatus(
    val readyConstraintAmount: UInt64,
    val totalConstraintAmount: UInt64,
) {
    val totalProgress: Flt64 get() = readyConstraintAmount.toFlt64() / totalConstraintAmount.toFlt64()
}

typealias IntermediateModelDumpingStatusCallBack = (IntermediateModelDumpingStatus) -> Try
