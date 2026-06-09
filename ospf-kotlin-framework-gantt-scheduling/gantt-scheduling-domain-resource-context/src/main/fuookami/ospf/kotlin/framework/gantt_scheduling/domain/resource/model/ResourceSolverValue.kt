package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.schedulingSolverValueAdapter
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange

internal val resourceSolverValueAdapter = schedulingSolverValueAdapter

internal fun <V> V.solverResourceQuantity() where V : RealNumber<V>, V : NumberField<V> = toFlt64()

internal fun <V> AbstractResourceCapacity<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.lowerBound.value.unwrap().solverResourceQuantity()

internal fun <V> AbstractResourceCapacity<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.upperBound.value.unwrap().solverResourceQuantity()

internal fun <V> ValueRange<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    lowerBound.value.unwrap().solverResourceQuantity()

internal fun <V> ValueRange<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    upperBound.value.unwrap().solverResourceQuantity()

internal fun <V> AbstractResourceCapacity<V>.solverLessQuantity() where V : RealNumber<V>, V : NumberField<V> =
    lessQuantityValue?.value?.solverResourceQuantity() ?: Flt64.zero

internal fun <V> AbstractResourceCapacity<V>.solverOverQuantity() where V : RealNumber<V>, V : NumberField<V> =
    overQuantityValue?.value?.solverResourceQuantity() ?: Flt64.zero

internal fun <V> AbstractResourceCapacity<V>.solverValueRange() where V : RealNumber<V>, V : NumberField<V> =
    ValueRange(
        solverLowerBound() - solverLessQuantity(),
        solverUpperBound() + solverOverQuantity()
    ).value!!

internal fun <C, V> Resource<C, V>.solverInitialQuantity()
        where C : AbstractResourceCapacity<V>, V : RealNumber<V>, V : NumberField<V> =
    initialQuantity().value.solverResourceQuantity()
