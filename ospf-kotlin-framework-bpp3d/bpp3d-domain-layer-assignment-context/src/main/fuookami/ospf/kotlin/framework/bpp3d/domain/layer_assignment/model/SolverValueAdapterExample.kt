package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.toFltX
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityFlt64
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.unit.CubicMeter
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.SquareMeter

data class Bpp3dSolverUnitSystem(
    val lengthUnit: PhysicalUnit = Meter,
    val areaUnit: PhysicalUnit = SquareMeter,
    val volumeUnit: PhysicalUnit = CubicMeter,
    val weightUnit: PhysicalUnit = Kilogram
)

data class Bpp3dSolverFltXScale(
    val amount: FltX = FltX.one,
    val length: FltX = FltX.one,
    val area: FltX = FltX.one,
    val volume: FltX = FltX.one,
    val depth: FltX = FltX.one,
    val weight: FltX = FltX.one
)

class ScaledBpp3dSolverValueAdapter(
    private val unitSystem: Bpp3dSolverUnitSystem = Bpp3dSolverUnitSystem(),
    private val scale: Bpp3dSolverFltXScale = Bpp3dSolverFltXScale()
) : Bpp3dSolverValueAdapter {
    override fun amountToSolver(value: UInt64): Flt64 {
        return (value.toFltX() * scale.amount).toFlt64()
    }

    override fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<Flt64> {
        val lower = amountToSolver(value.lowerBound.value.unwrap())
        val upper = amountToSolver(value.upperBound.value.unwrap())
        return ValueRange(
            lower,
            upper,
            value.lowerBound.interval,
            value.upperBound.interval,
            Flt64
        ).value!!
    }

    override fun lengthToSolver(value: QuantityFlt64): Flt64 {
        return quantityToSolver(value, unitSystem.lengthUnit, scale.length)
    }

    override fun areaToSolver(value: QuantityFlt64): Flt64 {
        return quantityToSolver(value, unitSystem.areaUnit, scale.area)
    }

    override fun volumeToSolver(value: QuantityFlt64): Flt64 {
        return quantityToSolver(value, unitSystem.volumeUnit, scale.volume)
    }

    override fun depthToSolver(value: QuantityFlt64): Flt64 {
        return quantityToSolver(value, unitSystem.lengthUnit, scale.depth)
    }

    override fun weightToSolver(value: QuantityFlt64): Flt64 {
        return quantityToSolver(value, unitSystem.weightUnit, scale.weight)
    }

    private fun quantityToSolver(
        value: QuantityFlt64,
        targetUnit: PhysicalUnit,
        factor: FltX
    ): Flt64 {
        val normalized = value.convertTo(targetUnit)
            ?: throw IllegalArgumentException("Incompatible unit: ${value.unit} vs $targetUnit")
        return (normalized.value.toFltX() * factor).toFlt64()
    }
}
