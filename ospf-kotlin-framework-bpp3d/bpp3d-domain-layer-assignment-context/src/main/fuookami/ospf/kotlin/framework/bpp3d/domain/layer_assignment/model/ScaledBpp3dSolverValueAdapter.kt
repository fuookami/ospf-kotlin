/**
 * 带缩放的 BPP3D 求解器值适配器。
 * Scaled BPP3D solver value adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

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
    private fun FltX.toSolverNumber(): FltX = FltX(this.toDouble())

    override fun amountToSolver(value: UInt64): FltX {
        return (value.toFltX() * scale.amount).toSolverNumber()
    }

    override fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<FltX> {
        val lower = amountToSolver(value.lowerBound.value.unwrap())
        val upper = amountToSolver(value.upperBound.value.unwrap())
        return ValueRange(
            lower,
            upper,
            value.lowerBound.interval,
            value.upperBound.interval,
            FltX
        ).value!!
    }

    override fun lengthToSolver(value: Quantity<FltX>): FltX {
        return quantityToSolver(value, unitSystem.lengthUnit, scale.length).value!!
    }

    override fun areaToSolver(value: Quantity<FltX>): FltX {
        return quantityToSolver(value, unitSystem.areaUnit, scale.area).value!!
    }

    override fun volumeToSolver(value: Quantity<FltX>): FltX {
        return quantityToSolver(value, unitSystem.volumeUnit, scale.volume).value!!
    }

    override fun depthToSolver(value: Quantity<FltX>): FltX {
        return quantityToSolver(value, unitSystem.lengthUnit, scale.depth).value!!
    }

    override fun weightToSolver(value: Quantity<FltX>): FltX {
        return quantityToSolver(value, unitSystem.weightUnit, scale.weight).value!!
    }

    private fun quantityToSolver(
        value: Quantity<FltX>,
        targetUnit: PhysicalUnit,
        factor: FltX
    ): Ret<FltX> {
        val normalized = value.convertTo(targetUnit)
            ?: return Failed(ErrorCode.IllegalArgument, "Incompatible unit: ${value.unit} vs $targetUnit")
        return ok((normalized.value.toFltX() * factor).toSolverNumber())
    }
}
