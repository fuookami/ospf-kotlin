package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import kotlin.random.Random
import kotlin.random.nextULong

/**
 * Generic Token - phantom type parameter T for API signature.
 * Internal implementation uses Flt64 for solver compatibility.
 */
data class TokenOf<T : RealNumber<T>>(
    val variable: AbstractVariableItem<*, *>,
    val solverIndex: Int,
    internal val refreshCallbacks: MutableMap<AbstractTokenListOf<T>, (Boolean) -> Unit>
) {
    val key by variable::key
    internal var __result: Flt64? = null
    internal var _result: Flt64?
        get() = __result
        set(value) {
            __result = value
            refreshCallbacks.values.forEach { it(value != null) }
        }
    val result by ::_result
    val doubleResult get() = _result?.toDouble()

    val name by variable::name
    val type by variable::type
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
    val lowerBound by variable::lowerBound
    val upperBound by variable::upperBound

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

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TokenOf<*>

        return key == other.key
    }

    override fun toString() = "$name: ${result ?: "?"}"
}

/**
 * Legacy typealias for Flt64-specific Token.
 */
typealias Token = TokenOf<Flt64>