@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.iis

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.time.Duration

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



