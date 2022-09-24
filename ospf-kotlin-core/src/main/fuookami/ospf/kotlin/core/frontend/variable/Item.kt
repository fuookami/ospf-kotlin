package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*

data class ItemKey(
    val identifier: UInt64,
    val index: Int
) {
    companion object {
        private fun reverseBit(v: UInt64): Int {
            var value = v.toInt32().value
            value = value and -0x55555556 shr 1 or (value and 0x55555555 shl 1)
            value = value and -0x33333334 shr 2 or (value and 0x33333333 shl 2)
            value = value and -0xf0f0f10 shr 4 or (value and 0x0F0F0F0F shl 4)
            value = value and -0xff0100 shr 8 or (value and 0x00FF00FF shl 8)
            value = value and -0x10000 shr 16 or (value and 0x0000FFFF shl 16)
            return value
        }
    }

    override fun hashCode(): Int = reverseBit(identifier) or index
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemKey) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false

        return true
    }
}

abstract class Item<T, Type : VariableType<T>>(
    val type: Type,
    var name: String = "",
    val constants: RealNumberConstants<T>
) where T : RealNumber<T>, T : NumberField<T> {

    abstract val dimension: Int
    abstract val identifier: UInt64
    abstract val index: Int
    abstract val vectorView: IntArray

    val range: Range<Type, T> = Range(type, constants)
    val lowerBound: Flt64
        get() = when (val value = range.lowerBound) {
            is ValueWrapper<*> -> value.toFlt64()
            null -> Flt64.nan
        }
    val upperBound: Flt64
        get() = when (val value = range.upperBound) {
            is ValueWrapper<*> -> value.toFlt64()
            null -> Flt64.nan
        }

    val key: ItemKey get() = ItemKey(identifier, index)

    override fun hashCode() = key.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Item<*, *>) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false

        return true
    }
}

internal object IdentifierGenerator {
    var next: UInt64 = UInt64.zero

    fun flush() {
        next = UInt64.zero
    }

    fun gen(): UInt64 {
        val thisValue = next;
        ++next;
        return thisValue;
    }
}
