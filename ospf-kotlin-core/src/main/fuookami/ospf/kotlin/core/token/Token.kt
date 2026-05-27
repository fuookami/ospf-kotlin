/**
 * Token 数据结构，持有变量求解结果的双视图访问。
 * Token data structure holding dual-view access to variable solve results.
 *
 * 内部存储为 Flt64?（求解器后端始终产出 Flt64），公开 API 通过 IntoValue<V> 转换器
 * 提供类型安全的 V? 视图，同时保留 resultFlt64 供求解器内部使用。
 * Internally stores Flt64? (solver backends always produce Flt64); the public API
 * provides a type-safe V? view via IntoValue<V> converter while retaining resultFlt64
 * for solver-internal use.
 */
package fuookami.ospf.kotlin.core.token

import kotlin.random.Random
import kotlin.random.nextULong
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableCombination
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * Generic Token<V> - V is a real type parameter with dual-view access.
 *
 * Internal storage is Flt64? (solver backends always produce Flt64).
 * The primary `result` accessor returns V? by converting via the stored converter.
 * When V = Flt64, the conversion is identity (unchecked cast, zero overhead).
 * The `resultFlt64` accessor provides the raw Flt64? view for solver-internal use.
 *
 * Dual-view pattern:
 *   - Flt64 view: `resultFlt64` (solver-boundary, internal)
 *   - V-typed view: `result` (type-safe, public API)
 */
data class Token<V : RealNumber<V>>(
    val variable: AbstractVariableItem<*, *>,
    val solverIndex: Int,
    internal val refreshCallbacks: MutableMap<AbstractTokenList<V>, (Boolean) -> Unit>,
    private val converter: IntoValue<V>? = null
) {
    val key by variable::key
    internal var __result: Flt64? = null
    internal var _result: Flt64?
        get() = __result
        set(value) {
            __result = value
            refreshCallbacks.values.forEach { it(value != null) }
        }

    /** Flt64 view of result (solver-boundary, internal). */
    val resultFlt64: Flt64? by ::_result
    val doubleResult get() = _result?.toSolverDouble("token.result")

    /**
     * V-typed view of result (primary public API).
     * When converter is set, converts Flt64 -> V via IntoValue<V>.
     * When converter is null (V = Flt64), returns _result via unchecked cast.
     */
    @Suppress("UNCHECKED_CAST")
    val result: V?
        get() = if (converter != null) {
            _result?.let { converter.intoValue(it) }
        } else {
            _result as V?
        }

    /** 设置泛型结果值。 / Set result from the generic value. */
    fun setResult(value: V) {
        _result = value.toFlt64()
    }

    /** 通过给定转换器显式获取类型化结果。 / Explicit typed result conversion through the supplied converter. */
    fun result(converter: IntoValue<V>): V? = _result?.let { converter.intoValue(it) }

    val name by variable::name
    val type by variable::type

    /** Flt64 view of range (solver-boundary). */
    val range: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>?
        get() = if (lowerBound != null && upperBound != null) {
            ValueRange(
                lowerBound = lowerBound!!,
                upperBound = upperBound!!,
                constants = Flt64
            )
        } else {
            null
        }

    /**
     * Check if a V-typed value is within this token's bounds.
     * Converts V to Flt64 internally for range checking.
     * Returns true if no bounds are set.
     */
    fun containsInBounds(value: V, converter: IntoValue<V>): Boolean {
        val r = range ?: return true
        return r.contains(converter.fromValue(value))
    }

    /** Flt64 view of bounds (solver-boundary). */
    val lowerBound by variable::lowerBound
    val upperBound by variable::upperBound

    /** 下界的类型化视图。 / Typed view of lower bound. */
    fun lowerBound(converter: IntoValue<V>): V? = lowerBound?.value?.toFlt64()?.let { converter.intoValue(it) }

    /** 上界的类型化视图。 / Typed view of upper bound. */
    fun upperBound(converter: IntoValue<V>): V? = upperBound?.value?.toFlt64()?.let { converter.intoValue(it) }

    infix fun belongsTo(item: AbstractVariableItem<*, *>): Boolean {
        return variable.belongsTo(item)
    }

    infix fun belongsTo(combination: VariableCombination<*, *, *>): Boolean {
        return variable.belongsTo(combination)
    }

    fun random(rng: Random): Flt64 {
        return if (variable.type.isUnsignedIntegerType) {
            val lower = lowerBound!!.value.unwrap().round().toUInt64().toULong()
            val upper = upperBound!!.value.unwrap().round().toUInt64().toULong()
            UInt64(rng.nextULong(lower, upper)).toFlt64()
        } else if (variable.type.isIntegerType) {
            val lower = lowerBound!!.value.unwrap().round().toInt64().toLong()
            val upper = upperBound!!.value.unwrap().round().toInt64().toLong()
            Int64(rng.nextLong(lower, upper)).toFlt64()
        } else {
            Flt64(
                rng.nextDouble(
                    lowerBound!!.value.unwrap().toSolverDouble("token.random.lowerBound"),
                    upperBound!!.value.unwrap().toSolverDouble("token.random.upperBound")
                )
            )
        }
    }

    override fun hashCode(): Int = key.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Token<*>
        return key == other.key
    }

    override fun toString() = "$name: ${result ?: "?"}"
}
