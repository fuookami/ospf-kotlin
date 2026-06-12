/**
 * 求解器数值转换工具 / Solver value conversion utilities
 *
 * 本文件提供 MaterialDemand 和 MaterialReserves 与求解器数值之间的转换扩展函数。
 * This file provides extension functions for converting between MaterialDemand/MaterialReserves and solver values.
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.toSolverValue

internal fun <V> MaterialDemand<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.lowerBound.value.unwrap().toSolverValue()

internal fun <V> MaterialDemand<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.upperBound.value.unwrap().toSolverValue()

internal fun <V> MaterialDemand<V>.solverLessQuantity() where V : RealNumber<V>, V : NumberField<V> =
    lessQuantityValue?.value?.toSolverValue() ?: Flt64.zero

internal fun <V> MaterialDemand<V>.solverOverQuantity() where V : RealNumber<V>, V : NumberField<V> =
    overQuantityValue?.value?.toSolverValue() ?: Flt64.zero

internal fun <V> MaterialDemand<V>.solverRangeLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    solverLowerBound() - solverLessQuantity()

internal fun <V> MaterialDemand<V>.solverRangeUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    solverUpperBound() + solverOverQuantity()

internal fun <V> MaterialDemand<V>.solverValueRange() where V : RealNumber<V>, V : NumberField<V> =
    ValueRange(solverRangeLowerBound(), solverRangeUpperBound()).value!!

internal fun <V> MaterialReserves<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.lowerBound.value.unwrap().toSolverValue()

internal fun <V> MaterialReserves<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.upperBound.value.unwrap().toSolverValue()

internal fun <V> MaterialReserves<V>.solverLessQuantity() where V : RealNumber<V>, V : NumberField<V> =
    lessQuantityValue?.value?.toSolverValue() ?: Flt64.zero

internal fun <V> MaterialReserves<V>.solverOverQuantity() where V : RealNumber<V>, V : NumberField<V> =
    overQuantityValue?.value?.toSolverValue() ?: Flt64.zero

internal fun <V> MaterialReserves<V>.solverRangeLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    solverLowerBound() - solverLessQuantity()

internal fun <V> MaterialReserves<V>.solverRangeUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    solverUpperBound() + solverOverQuantity()

internal fun <V> MaterialReserves<V>.solverValueRange() where V : RealNumber<V>, V : NumberField<V> =
    ValueRange(solverRangeLowerBound(), solverRangeUpperBound()).value!!
