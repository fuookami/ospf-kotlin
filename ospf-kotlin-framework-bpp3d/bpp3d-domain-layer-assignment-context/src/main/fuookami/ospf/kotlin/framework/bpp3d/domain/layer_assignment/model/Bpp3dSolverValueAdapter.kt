package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.toFlt64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

interface Bpp3dSolverValueAdapter {
    fun amountToSolver(value: UInt64): Flt64
    fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<Flt64> = value.toFlt64()
    fun lengthToSolver(value: Quantity<Flt64>): Flt64
    fun areaToSolver(value: Quantity<Flt64>): Flt64
    fun volumeToSolver(value: Quantity<Flt64>): Flt64
    fun depthToSolver(value: Quantity<Flt64>): Flt64 = lengthToSolver(value)
    fun weightToSolver(value: Quantity<Flt64>): Flt64

    fun toSolver(value: Bpp3dDemandValue): Flt64 {
        return when (value) {
            is Bpp3dDemandValue.Amount -> amountToSolver(value.value)
            is Bpp3dDemandValue.Weight -> weightToSolver(value.value)
        }
    }
}

typealias Bpp3dDemandValueAdapter = Bpp3dSolverValueAdapter

data object DefaultBpp3dDemandValueAdapter : Bpp3dSolverValueAdapter {
    override fun amountToSolver(value: UInt64): Flt64 = value.toFlt64()
    override fun lengthToSolver(value: Quantity<Flt64>): Flt64 = value.toFlt64()
    override fun areaToSolver(value: Quantity<Flt64>): Flt64 = value.toFlt64()
    override fun volumeToSolver(value: Quantity<Flt64>): Flt64 = value.toFlt64()
    override fun weightToSolver(value: Quantity<Flt64>): Flt64 = value.toFlt64()
}

val DefaultBpp3dSolverValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dDemandValueAdapter
