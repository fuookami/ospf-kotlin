/** 资源求解器数值转换：容量边界与数量范围映射 / Resource solver value conversion: capacity bounds and quantity range mapping */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 计算资源容量的求解器下界 / Calculate solver lower bound for resource capacity
 *
 * @param V 数值类型 / Value type
 * @return 求解器下界值 / Solver lower bound value
 */
internal fun <V> AbstractResourceCapacity<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.lowerBound.value.unwrap().toSolverValue()

/**
 * 计算资源容量的求解器上界 / Calculate solver upper bound for resource capacity
 *
 * @param V 数值类型 / Value type
 * @return 求解器上界值 / Solver upper bound value
 */
internal fun <V> AbstractResourceCapacity<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.upperBound.value.unwrap().toSolverValue()

/**
 * 计算数值范围的求解器下界 / Calculate solver lower bound for value range
 *
 * @param V 数值类型 / Value type
 * @return 求解器下界值 / Solver lower bound value
 */
internal fun <V> ValueRange<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    lowerBound.value.unwrap().toSolverValue()

/**
 * 计算数值范围的求解器上界 / Calculate solver upper bound for value range
 *
 * @param V 数值类型 / Value type
 * @return 求解器上界值 / Solver upper bound value
 */
internal fun <V> ValueRange<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    upperBound.value.unwrap().toSolverValue()

/**
 * 计算资源容量的求解器不足量 / Calculate solver less quantity for resource capacity
 *
 * @param V 数值类型 / Value type
 * @return 求解器不足量值 / Solver less quantity value
 */
internal fun <V> AbstractResourceCapacity<V>.solverLessQuantity() where V : RealNumber<V>, V : NumberField<V> =
    lessQuantityValue?.value?.toSolverValue() ?: Flt64.zero

/**
 * 计算资源容量的求解器超限量 / Calculate solver over quantity for resource capacity
 *
 * @param V 数值类型 / Value type
 * @return 求解器超限量值 / Solver over quantity value
 */
internal fun <V> AbstractResourceCapacity<V>.solverOverQuantity() where V : RealNumber<V>, V : NumberField<V> =
    overQuantityValue?.value?.toSolverValue() ?: Flt64.zero

/**
 * 计算资源容量的求解器数值范围 / Calculate solver value range for resource capacity
 *
 * @param V 数值类型 / Value type
 * @return 求解器数值范围 / Solver value range
 */
internal fun <V> AbstractResourceCapacity<V>.solverValueRange() where V : RealNumber<V>, V : NumberField<V> =
    ValueRange(
        solverLowerBound() - solverLessQuantity(),
        solverUpperBound() + solverOverQuantity()
    ).value!!

/**
 * 计算资源的求解器初始数量 / Calculate solver initial quantity for resource
 *
 * @param C 资源容量类型 / Resource capacity type
 * @param V 数值类型 / Value type
 * @return 求解器初始数量值 / Solver initial quantity value
 */
internal fun <C, V> Resource<C, V>.solverInitialQuantity()
        where C : AbstractResourceCapacity<V>, V : RealNumber<V>, V : NumberField<V> =
    initialQuantity().value.toSolverValue()
