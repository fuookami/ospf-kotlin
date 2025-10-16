package fuookami.ospf.kotlin.core.backend.solver.iis

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class IISComputingStatus(
    val restBoundAmount: UInt64,
    val totalBoundAmount: UInt64,
    val restConstraintAmount: UInt64,
    val totalConstraintAmount: UInt64
) {
    val boundProgress: Flt64 get() = restBoundAmount.toFlt64() / totalBoundAmount.toFlt64()
    val constraintProgress: Flt64 get() = restConstraintAmount.toFlt64() / totalConstraintAmount.toFlt64()
    val totalProgress: Flt64 get() = (restBoundAmount.toFlt64() + restConstraintAmount.toFlt64()) / (totalBoundAmount.toFlt64() + totalConstraintAmount.toFlt64())
}

typealias IISComputingStatusCallBack = (Boolean, Duration, IISComputingStatus) -> Try
