package fuookami.ospf.kotlin.core.frontend.model.callback

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.variable.*

data class Variable(
    val type: VariableType<*>,
    val identifier: UInt64,
    val vector: IntArray,
    val range: ValueRange<*>,
    val name: String
) {
    constructor(item: Item<*, *>): this(
        type = item.type,
        identifier = item.identifier,
        vector = item.vectorView,
        range = item.range.range,
        name = item.name
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Variable

        if (type != other.type) return false
        if (identifier != other.identifier) return false
        if (!vector.contentEquals(other.vector)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + vector.contentHashCode()
        return result
    }
}
