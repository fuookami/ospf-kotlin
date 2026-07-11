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

/**
 * 获取物料需求的求解器下界 / Get solver lower bound for material demand
 *
 * @param V 数值类型 / Value type
 * @return 求解器下界值 / Solver lower bound value
*/
internal fun <V> MaterialDemand<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.lowerBound.value.unwrap().toSolverValue()

/**
 * 获取物料需求的求解器上界 / Get solver upper bound for material demand
 *
 * @param V 数值类型 / Value type
 * @return 求解器上界值 / Solver upper bound value
*/
internal fun <V> MaterialDemand<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.upperBound.value.unwrap().toSolverValue()

/**
 * 获取物料需求的求解器不足量 / Get solver less quantity for material demand
 *
 * @param V 数值类型 / Value type
 * @return 求解器不足量值，若为空则返回零 / Solver less quantity value, or zero if absent
*/
internal fun <V> MaterialDemand<V>.solverLessQuantity() where V : RealNumber<V>, V : NumberField<V> =
    lessQuantityValue?.value?.toSolverValue() ?: Flt64.zero

/**
 * 获取物料需求的求解器超限量 / Get solver over quantity for material demand
 *
 * @param V 数值类型 / Value type
 * @return 求解器超限量值，若为空则返回零 / Solver over quantity value, or zero if absent
*/
internal fun <V> MaterialDemand<V>.solverOverQuantity() where V : RealNumber<V>, V : NumberField<V> =
    overQuantityValue?.value?.toSolverValue() ?: Flt64.zero

/**
 * 获取物料需求的求解器范围下界（含不足量偏移）/ Get solver range lower bound for material demand (with less-quantity offset)
 *
 * @param V 数值类型 / Value type
 * @return 求解器范围下界值 / Solver range lower bound value
*/
internal fun <V> MaterialDemand<V>.solverRangeLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    solverLowerBound() - solverLessQuantity()

/**
 * 获取物料需求的求解器范围上界（含超限量偏移）/ Get solver range upper bound for material demand (with over-quantity offset)
 *
 * @param V 数值类型 / Value type
 * @return 求解器范围上界值 / Solver range upper bound value
*/
internal fun <V> MaterialDemand<V>.solverRangeUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    solverUpperBound() + solverOverQuantity()

/**
 * 获取物料需求的求解器数值范围 / Get solver value range for material demand
 *
 * @param V 数值类型 / Value type
 * @return 求解器数值范围 / Solver value range
*/
internal fun <V> MaterialDemand<V>.solverValueRange() where V : RealNumber<V>, V : NumberField<V> =
    ValueRange(solverRangeLowerBound(), solverRangeUpperBound()).value!!

/**
 * 获取物料储备的求解器下界 / Get solver lower bound for material reserves
 *
 * @param V 数值类型 / Value type
 * @return 求解器下界值 / Solver lower bound value
*/
internal fun <V> MaterialReserves<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.lowerBound.value.unwrap().toSolverValue()

/**
 * 获取物料储备的求解器上界 / Get solver upper bound for material reserves
 *
 * @param V 数值类型 / Value type
 * @return 求解器上界值 / Solver upper bound value
*/
internal fun <V> MaterialReserves<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.upperBound.value.unwrap().toSolverValue()

/**
 * 获取物料储备的求解器不足量 / Get solver less quantity for material reserves
 *
 * @param V 数值类型 / Value type
 * @return 求解器不足量值，若为空则返回零 / Solver less quantity value, or zero if absent
*/
internal fun <V> MaterialReserves<V>.solverLessQuantity() where V : RealNumber<V>, V : NumberField<V> =
    lessQuantityValue?.value?.toSolverValue() ?: Flt64.zero

/**
 * 获取物料储备的求解器超限量 / Get solver over quantity for material reserves
 *
 * @param V 数值类型 / Value type
 * @return 求解器超限量值，若为空则返回零 / Solver over quantity value, or zero if absent
*/
internal fun <V> MaterialReserves<V>.solverOverQuantity() where V : RealNumber<V>, V : NumberField<V> =
    overQuantityValue?.value?.toSolverValue() ?: Flt64.zero

/**
 * 获取物料储备的求解器范围下界（含不足量偏移）/ Get solver range lower bound for material reserves (with less-quantity offset)
 *
 * @param V 数值类型 / Value type
 * @return 求解器范围下界值 / Solver range lower bound value
*/
internal fun <V> MaterialReserves<V>.solverRangeLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    solverLowerBound() - solverLessQuantity()

/**
 * 获取物料储备的求解器范围上界（含超限量偏移）/ Get solver range upper bound for material reserves (with over-quantity offset)
 *
 * @param V 数值类型 / Value type
 * @return 求解器范围上界值 / Solver range upper bound value
*/
internal fun <V> MaterialReserves<V>.solverRangeUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    solverUpperBound() + solverOverQuantity()

/**
 * 获取物料储备的求解器数值范围 / Get solver value range for material reserves
 *
 * @param V 数值类型 / Value type
 * @return 求解器数值范围 / Solver value range
*/
internal fun <V> MaterialReserves<V>.solverValueRange() where V : RealNumber<V>, V : NumberField<V> =
    ValueRange(solverRangeLowerBound(), solverRangeUpperBound()).value!!
