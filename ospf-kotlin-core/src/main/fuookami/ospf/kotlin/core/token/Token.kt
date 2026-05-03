package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableCombination
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import kotlin.random.Random
import kotlin.random.nextULong

/**
 * Generic Token<V> - V is a real type parameter with dual-view access.
 *
 * Internal storage is Flt64? (solver backends always produce Flt64).
 * The primary `result` accessor returns V? by converting via the stored converter.
 * When V = Flt64, the conversion is identity (unchecked cast, zero overhead).
 * The `resultFlt64` accessor provides the raw Flt64? view for solver-internal use.
 *
 * Dual-view pattern:
 *   - Flt64 view: `resultFlt64` (solver-compatible, internal)
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

    /** Flt64 view of result (solver-compatible, internal). */
    val resultFlt64: Flt64? by ::_result
    val doubleResult get() = _result?.toDouble()

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

    /** Set result from generic type V. Converts V to Flt64 via RealNumber.toFlt64(). */
    fun setResultFromV(value: V) {
        _result = value.toFlt64()
    }

    /** V-typed view of result via explicit IntoValue<V> conversion. Kept for backward compatibility. */
    fun resultAsV(converter: IntoValue<V>): V? = _result?.let { converter.intoValue(it) }

    val name by variable::name
    val type by variable::type

    /** Flt64 view of range (solver-compatible). */
    val range: ValueRange<Flt64>?
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

    /** Flt64 view of bounds (solver-compatible). */
    val lowerBound by variable::lowerBound
    val upperBound by variable::upperBound

    /** V-typed view of lower bound. */
    fun lowerBoundAsV(converter: IntoValue<V>): V? = lowerBound?.value?.toFlt64()?.let { converter.intoValue(it) }

    /** V-typed view of upper bound. */
    fun upperBoundAsV(converter: IntoValue<V>): V? = upperBound?.value?.toFlt64()?.let { converter.intoValue(it) }

    infix fun belongsTo(item: AbstractVariableItem<*, *>): Boolean {
        return variable.belongsTo(item)
    }

    infix fun belongsTo(combination: VariableCombination<*, *, *>): Boolean {
        return variable.belongsTo(combination)
    }

    fun random(rng: Random): Flt64 {
        return if (variable.type.isUnsignedIntegerType) {
            Flt64(
                rng.nextULong(
                    lowerBound!!.value.unwrap().round().toDouble().toULong(),
                    upperBound!!.value.unwrap().round().toDouble().toULong()
                ).toDouble()
            )
        } else if (variable.type.isIntegerType) {
            Flt64(
                rng.nextLong(
                    lowerBound!!.value.unwrap().round().toDouble().toLong(),
                    upperBound!!.value.unwrap().round().toDouble().toLong()
                ).toDouble()
            )
        } else {
            Flt64(
                rng.nextDouble(
                    lowerBound!!.value.unwrap().toDouble(),
                    upperBound!!.value.unwrap().toDouble()
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

