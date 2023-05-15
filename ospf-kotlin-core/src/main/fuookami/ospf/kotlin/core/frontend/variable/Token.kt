package  fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*

class Token(
    val variable: Item<*, *>,
    val solverIndex: Int
) {
    val key: ItemKey by variable::key
    var result: Flt64? = null

    val name: String by variable::name
    val type: VariableType<*> by variable::type
    val range: ValueRange<Flt64> get() = variable.range.valueRange
    val identifier: UInt64 by variable::identifier
    val index: Int by variable::index
    val vector: IntArray by variable::vectorView
    val lowerBound: Flt64 by variable::lowerBound
    val upperBound: Flt64 by variable::upperBound

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
