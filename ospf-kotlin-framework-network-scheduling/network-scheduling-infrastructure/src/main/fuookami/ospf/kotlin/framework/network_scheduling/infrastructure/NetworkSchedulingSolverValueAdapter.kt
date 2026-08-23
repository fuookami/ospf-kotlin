package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 网络调度领域值与 solver `Flt64` 的集中转换边界。 / Central conversion boundary between network values and solver `Flt64`.
 *
 * @param V 领域数值类型 / Domain numeric type
 */
interface NetworkSchedulingSolverValueAdapter<V : RealNumber<V>> {
    /** 将领域值转换为 solver 值。 / Convert a domain value to a solver value. */
    fun toSolverValue(value: V): Ret<Flt64>

    /** 将 solver 值转换为领域值。 / Convert a solver value to a domain value. */
    fun fromSolverValue(value: Flt64): Ret<V>

    /**
     * 将物理量归一化到目标单位并转换为 solver 值。 / Normalize a quantity to a target unit and convert it to a solver value.
     *
     * @param quantity 原始物理量 / Source quantity
     * @param targetUnit 目标单位 / Target unit
     * @return 归一化 solver 值或转换失败 / Normalized solver value or conversion failure
     */
    fun normalize(quantity: Quantity<V>, targetUnit: PhysicalUnit): Ret<Flt64> {
        if (!quantity.unit.sameDimension(targetUnit)) {
            return networkSchedulingFailure(
                "物理量转换失败：${quantity.unit.symbol} 与 ${targetUnit.symbol} 量纲不兼容 / " +
                        "Quantity conversion failed: ${quantity.unit.symbol} and ${targetUnit.symbol} have incompatible dimensions"
            )
        }
        val converted = Quantity(quantity.value.toFlt64(), quantity.unit).convertTo(targetUnit)
            ?: return networkSchedulingFailure(
                "物理量转换失败：无法转换到 ${targetUnit.symbol} / Quantity conversion failed: cannot convert to ${targetUnit.symbol}"
            )
        return finite(converted.value)
    }

    /** 校验 solver 值有限。 / Validate that a solver value is finite. */
    fun finite(value: Flt64): Ret<Flt64> {
        return if (value.isFinite()) {
            ok(value)
        } else {
            networkSchedulingFailure(
                "数值转换失败：solver 值必须有限 / Numeric conversion failed: solver value must be finite"
            )
        }
    }
}

/** Flt64 solver 数值适配器。 / Flt64 solver value adapter. */
object Flt64NetworkSchedulingSolverValueAdapter : NetworkSchedulingSolverValueAdapter<Flt64> {
    override fun toSolverValue(value: Flt64): Ret<Flt64> = finite(value)

    override fun fromSolverValue(value: Flt64): Ret<Flt64> = finite(value)
}

/** FltX solver 数值适配器。 / FltX solver value adapter. */
object FltXNetworkSchedulingSolverValueAdapter : NetworkSchedulingSolverValueAdapter<FltX> {
    override fun toSolverValue(value: FltX): Ret<Flt64> = finite(value.toFlt64())

    override fun fromSolverValue(value: Flt64): Ret<FltX> {
        return finite(value).map { it.toFltX() }
    }
}
