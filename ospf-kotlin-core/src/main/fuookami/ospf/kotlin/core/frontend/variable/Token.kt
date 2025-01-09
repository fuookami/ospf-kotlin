package  fuookami.ospf.kotlin.core.frontend.variable

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*

data class Token(
    val variable: AbstractVariableItem<*, *>,
    val solverIndex: Int
) {
    val key by variable::key
    internal var _result: Flt64? = null
    val result by ::_result
    val doubleResult get() = _result?.toDouble()

    val name by variable::name
    val type by variable::type
    val range: ValueRange<Flt64>?
        get() = if (lowerBound != null && upperBound != null) {
            ValueRange(lowerBound!!, upperBound!!, Flt64)
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

        other as Token

        return key == other.key
    }

    override fun toString() = "$name: ${result ?: "?"}"
}
