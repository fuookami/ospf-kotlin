package fuookami.ospf.kotlin.core.backend.solver.output

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

data class SolvingStatus(
    val solver: String,
    val solverIndex: UInt64 = UInt64.zero,
    val objectCategory: ObjectCategory?,
    val time: Duration,
    val obj: Flt64,
    val possibleBestObj: Flt64,
    val initialBestObj: Flt64,
    val gap: Flt64
)

typealias SolvingStatusCallBack = (SolvingStatus) -> Try
