package fuookami.ospf.kotlin.core.backend.solver.output

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

data class SolvingStatus(
    val solver: String,
    val solverIndex: UInt64 = UInt64.zero,
    val time: Duration,
    val obj: Flt64,
    val possibleBestObj: Flt64,
    val gap: Flt64
)

typealias SolvingStatusCallBack = (SolvingStatus) -> Try
