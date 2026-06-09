package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.schedulingSolverValueAdapter
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

internal val capacitySolverValueAdapter = schedulingSolverValueAdapter

internal fun <V : RealNumber<V>> V.solverCapacityCoefficient() = toFlt64()

internal fun UInt64.solverCapacityAmount() = Flt64(toLong().toDouble())
