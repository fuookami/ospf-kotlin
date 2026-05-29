@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.iis

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * IIS 计算状态
 * IIS computing status
 */

/**
 * IIS 计算进度状态，包含边界和约束的剩余/总量信息。
 * IIS computing progress status, containing remaining/total information for bounds and constraints.
 *
 * @property restBoundAmount 剩余边界数量 / Remaining bound amount
 * @property totalBoundAmount 总边界数量 / Total bound amount
 * @property restConstraintAmount 剩余约束数量 / Remaining constraint amount
 * @property totalConstraintAmount 总约束数量 / Total constraint amount
 */
data class IISComputingStatus(
    val restBoundAmount: UInt64,
    val totalBoundAmount: UInt64,
    val restConstraintAmount: UInt64,
    val totalConstraintAmount: UInt64
) {
    /** 边界计算进度 / Bound computation progress */
    val boundProgress: Flt64 get() = restBoundAmount.toFlt64() / totalBoundAmount.toFlt64()
    /** 约束计算进度 / Constraint computation progress */
    val constraintProgress: Flt64 get() = restConstraintAmount.toFlt64() / totalConstraintAmount.toFlt64()
    /** 总进度 / Total progress */
    val totalProgress: Flt64 get() = (restBoundAmount.toFlt64() + restConstraintAmount.toFlt64()) / (totalBoundAmount.toFlt64() + totalConstraintAmount.toFlt64())
}

/** IIS 计算状态回调函数类型 / IIS computing status callback function type */
typealias IISComputingStatusCallBack = (Boolean, Duration, IISComputingStatus) -> Try
