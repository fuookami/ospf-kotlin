package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class MechanismModelDumpingStatus(
    val readyConstraintAmount: UInt64,
    val totalConstraintAmount: UInt64,
) {
    val totalProgress: Flt64 get() = readyConstraintAmount.toFlt64() / totalConstraintAmount.toFlt64()
}

typealias MechanismModelDumpingStatusCallBack = (MechanismModelDumpingStatus) -> Try
