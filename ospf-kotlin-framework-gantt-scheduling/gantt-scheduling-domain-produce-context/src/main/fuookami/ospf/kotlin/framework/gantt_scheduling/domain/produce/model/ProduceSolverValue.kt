package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.schedulingSolverValueAdapter
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange

internal val produceSolverValueAdapter = schedulingSolverValueAdapter

internal fun <V> V.solverMaterialQuantity() where V : RealNumber<V>, V : NumberField<V> = toFlt64()

internal fun <V> MaterialDemand<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.lowerBound.value.unwrap().solverMaterialQuantity()

internal fun <V> MaterialDemand<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.upperBound.value.unwrap().solverMaterialQuantity()

internal fun <V> MaterialDemand<V>.solverLessQuantity() where V : RealNumber<V>, V : NumberField<V> =
    lessQuantityValue?.value?.solverMaterialQuantity() ?: Flt64.zero

internal fun <V> MaterialDemand<V>.solverOverQuantity() where V : RealNumber<V>, V : NumberField<V> =
    overQuantityValue?.value?.solverMaterialQuantity() ?: Flt64.zero

internal fun <V> MaterialDemand<V>.solverRangeLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    solverLowerBound() - solverLessQuantity()

internal fun <V> MaterialDemand<V>.solverRangeUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    solverUpperBound() + solverOverQuantity()

internal fun <V> MaterialDemand<V>.solverValueRange() where V : RealNumber<V>, V : NumberField<V> =
    ValueRange(solverRangeLowerBound(), solverRangeUpperBound()).value!!

internal fun <V> MaterialReserves<V>.solverLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.lowerBound.value.unwrap().solverMaterialQuantity()

internal fun <V> MaterialReserves<V>.solverUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    quantityRangeValue.value.upperBound.value.unwrap().solverMaterialQuantity()

internal fun <V> MaterialReserves<V>.solverLessQuantity() where V : RealNumber<V>, V : NumberField<V> =
    lessQuantityValue?.value?.solverMaterialQuantity() ?: Flt64.zero

internal fun <V> MaterialReserves<V>.solverOverQuantity() where V : RealNumber<V>, V : NumberField<V> =
    overQuantityValue?.value?.solverMaterialQuantity() ?: Flt64.zero

internal fun <V> MaterialReserves<V>.solverRangeLowerBound() where V : RealNumber<V>, V : NumberField<V> =
    solverLowerBound() - solverLessQuantity()

internal fun <V> MaterialReserves<V>.solverRangeUpperBound() where V : RealNumber<V>, V : NumberField<V> =
    solverUpperBound() + solverOverQuantity()

internal fun <V> MaterialReserves<V>.solverValueRange() where V : RealNumber<V>, V : NumberField<V> =
    ValueRange(solverRangeLowerBound(), solverRangeUpperBound()).value!!
