@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.time.Duration

data class InfeasibleUnifiedFields(
    val iterations: UInt64?,
    val nodeCount: UInt64?,
    val bestBound: Flt64?,
    val mipGap: Flt64?,
    val solveTime: Duration
)

fun resolveInfeasibleUnifiedFields(
    latestStatus: SolvingStatus?,
    fallbackSolveTime: Duration
): InfeasibleUnifiedFields {
    return InfeasibleUnifiedFields(
        iterations = latestStatus?.iterations,
        nodeCount = latestStatus?.nodeCount,
        bestBound = latestStatus?.bestBound,
        mipGap = latestStatus?.mipGap,
        solveTime = latestStatus?.solveTime ?: fallbackSolveTime
    )
}