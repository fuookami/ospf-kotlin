/**
 * 带缩放的 BPP3D 求解器值适配器。
 * Scaled BPP3D solver value adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.toFltX
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.unit.CubicMeter
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.SquareMeter

/**
 * 求解器单位系统配置。
 * Solver unit system configuration.
 *
 * @property lengthUnit 长度单位 / length unit
 * @property areaUnit 面积单位 / area unit
 * @property volumeUnit 体积单位 / volume unit
 * @property weightUnit 重量单位 / weight unit
 */
data class Bpp3dSolverUnitSystem(
    val lengthUnit: PhysicalUnit = Meter,
    val areaUnit: PhysicalUnit = SquareMeter,
    val volumeUnit: PhysicalUnit = CubicMeter,
    val weightUnit: PhysicalUnit = Kilogram
)

/**
 * 求解器浮点缩放因子配置。
 * Solver floating-point scale factor configuration.
 *
 * @property amount 数量缩放因子 / amount scale factor
 * @property length 长度缩放因子 / length scale factor
 * @property area 面积缩放因子 / area scale factor
 * @property volume 体积缩放因子 / volume scale factor
 * @property depth 深度缩放因子 / depth scale factor
 * @property weight 重量缩放因子 / weight scale factor
 */
data class Bpp3dSolverFltXScale(
    val amount: FltX = FltX.one,
    val length: FltX = FltX.one,
    val area: FltX = FltX.one,
    val volume: FltX = FltX.one,
    val depth: FltX = FltX.one,
    val weight: FltX = FltX.one
)

/**
 * 带缩放的 BPP3D 求解器值适配器，支持单位转换和数值缩放。
 * Scaled BPP3D solver value adapter, supports unit conversion and numeric scaling.
 *
 * @property unitSystem 单位系统 / unit system
 * @property scale 缩放因子配置 / scale factor configuration
 */
class ScaledBpp3dSolverValueAdapter(
    private val unitSystem: Bpp3dSolverUnitSystem = Bpp3dSolverUnitSystem(),
    private val scale: Bpp3dSolverFltXScale = Bpp3dSolverFltXScale()
) : Bpp3dSolverValueAdapter {
    private fun FltX.toSolverInfraNumber(): InfraNumber = InfraNumber(this.toDouble())

    override fun amountToSolver(value: UInt64): InfraNumber {
        return (value.toFltX() * scale.amount).toSolverInfraNumber()
    }

    override fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<InfraNumber> {
        val lower = amountToSolver(value.lowerBound.value.unwrap())
        val upper = amountToSolver(value.upperBound.value.unwrap())
        return ValueRange(
            lower,
            upper,
            value.lowerBound.interval,
            value.upperBound.interval,
            InfraNumber
        ).value!!
    }

    override fun lengthToSolver(value: Quantity<InfraNumber>): InfraNumber {
        return quantityToSolver(value, unitSystem.lengthUnit, scale.length)
    }

    override fun areaToSolver(value: Quantity<InfraNumber>): InfraNumber {
        return quantityToSolver(value, unitSystem.areaUnit, scale.area)
    }

    override fun volumeToSolver(value: Quantity<InfraNumber>): InfraNumber {
        return quantityToSolver(value, unitSystem.volumeUnit, scale.volume)
    }

    override fun depthToSolver(value: Quantity<InfraNumber>): InfraNumber {
        return quantityToSolver(value, unitSystem.lengthUnit, scale.depth)
    }

    override fun weightToSolver(value: Quantity<InfraNumber>): InfraNumber {
        return quantityToSolver(value, unitSystem.weightUnit, scale.weight)
    }

    private fun quantityToSolver(
        value: Quantity<InfraNumber>,
        targetUnit: PhysicalUnit,
        factor: FltX
    ): InfraNumber {
        val normalized = value.convertTo(targetUnit)
            ?: throw IllegalArgumentException("Incompatible unit: ${value.unit} vs $targetUnit")
        return (normalized.value.toFltX() * factor).toSolverInfraNumber()
    }
}

