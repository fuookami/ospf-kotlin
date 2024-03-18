package  fuookami.ospf.kotlin.core.frontend.variable

import kotlin.random.*
import fuookami.ospf.kotlin.utils.math.*

data class Token(
    val variable: AbstractVariableItem<*, *>,
    val solverIndex: Int
) {
    val key by variable::key
    internal var _result: Flt64? = null
    val result by ::_result

    val name by variable::name
    val type by variable::type
    val range: ValueRange<Flt64>
        get() = ValueRange(
            lowerBound.toFlt64(),
            upperBound.toFlt64(),
            variable.range.lowerInterval,
            variable.range.upperInterval
        )
    val lowerBound by variable::lowerBound
    val upperBound by variable::upperBound

    fun belongsTo(item: AbstractVariableItem<*, *>): Boolean {
        return variable.belongsTo(item)
    }

    fun belongsTo(combination: VariableCombination<*, *, *>): Boolean {
        return variable.belongsTo(combination)
    }

    fun random(rng: Random): Flt64 {
        return if (variable.type.isUnsignedIntegerType) {
            Flt64(
                rng.nextULong(
                    lowerBound.round().toDouble().toULong(),
                    upperBound.round().toDouble().toULong()
                ).toDouble()
            )
        } else if (variable.type.isIntegerType) {
            Flt64(
                rng.nextLong(
                    lowerBound.round().toDouble().toLong(),
                    upperBound.round().toDouble().toLong()
                ).toDouble()
            )
        } else {
            Flt64(
                rng.nextDouble(
                    lowerBound.toDouble(),
                    upperBound.toDouble()
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
