package  fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*

class Token(
    val variable: Item<*, *>,
    val solverIndex: Int
) {
    val key: ItemKey get() = variable.key
    var result: Flt64? = null

    val name: String get() = variable.name
    val type: VariableType<*> get() = variable.type
    val range: ValueRange<Flt64> get() = variable.range.valueRange
    val lowerBound: Flt64 get() = variable.lowerBound
    val upperBound: Flt64 get() = variable.upperBound

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (key != other.key) return false

        return true
    }

    override fun toString() = "$name: ${result?.toString() ?: "?"}"
}
