/**
 * 方向基础设施。
 * Orientation infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*

enum class OrientationCategory {
    Upright,
    Side,
    Lie
}

/**
 * 方向类型（密封类版本）
 * Orientation type (sealed class version)
 *
 * 从 enum 迁移为 sealed class，同时保持字符串序列化/反序列化与旧枚举名称兼容。
 * Migrated from enum to sealed class while preserving string serialization/deserialization
 * compatibility with previous enum names.
 */
@Serializable(with = OrientationSerializer::class)
sealed class Orientation {
    object Upright : Orientation() {
        override val label = "Upright"
        override val rank = 0
        override val rotation get() = UprightRotated
        override val category = OrientationCategory.Upright
    }

    object UprightRotated : Orientation() {
        override val label = "UprightRotated"
        override val rank = 1
        override fun <V : FloatingNumber<V>> depth(unit: AbstractCuboid<V>) = unit.width
        override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>) = unit.depth
        override val rotation = Upright
        override val rotated = true
        override val category = OrientationCategory.Upright
    }

    object Side : Orientation() {
        override val label = "Side"
        override val rank = 2
        override val rotation get() = SideRotated
        override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>) = unit.width
        override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>) = unit.height
        override val category = OrientationCategory.Side
    }

    object SideRotated : Orientation() {
        override val label = "SideRotated"
        override val rank = 3
        override fun <V : FloatingNumber<V>> depth(unit: AbstractCuboid<V>) = unit.height
        override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>) = unit.width
        override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>) = unit.depth
        override val rotation = Side
        override val rotated = true
        override val category = OrientationCategory.Side
    }

    object Lie : Orientation() {
        override val label = "Lie"
        override val rank = 4
        override fun <V : FloatingNumber<V>> depth(unit: AbstractCuboid<V>) = unit.height
        override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>) = unit.depth
        override val rotation get() = LieRotated
        override val category = OrientationCategory.Lie
    }

    object LieRotated : Orientation() {
        override val label = "LieRotated"
        override val rank = 5
        override fun <V : FloatingNumber<V>> depth(unit: AbstractCuboid<V>) = unit.width
        override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>) = unit.depth
        override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>) = unit.height
        override val rotation = Lie
        override val rotated = true
        override val category = OrientationCategory.Lie
    }

    abstract val label: String
    protected abstract val rank: Int

    open fun <V : FloatingNumber<V>> depth(unit: AbstractCuboid<V>): Quantity<V> = unit.depth
    open fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>): Quantity<V> = unit.width
    open fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>): Quantity<V> = unit.height

    abstract val rotation: Orientation
    open val rotated: Boolean = false
    abstract val category: OrientationCategory

    fun orderValue(): Int = rank

    override fun toString() = label

    companion object {
        val entries: List<Orientation>
            get() = listOf(
                Upright,
                UprightRotated,
                Side,
                SideRotated,
                Lie,
                LieRotated
            )

        operator fun invoke(str: String): Orientation? {
            return entries.find { it.label == str }
        }

        fun require(str: String): Orientation {
            return invoke(str) ?: throw IllegalArgumentException("Unsupported orientation: $str")
        }

        fun <V : FloatingNumber<V>> merge(unit: AbstractCuboid<V>, orientations: List<Orientation>): List<Orientation> {
            return if (orientations.isEmpty()) {
                merge(unit, entries)
            } else if (orientations.size == 1) {
                orientations.toList()
            } else {
                val ret = arrayListOf(orientations.first())
                for (j in 1 until orientations.size) {
                    val thisOrientation = orientations[j]
                    val sameOrientationExist = ret.any {
                        it.width(unit) eq thisOrientation.width(unit)
                                && it.height(unit) eq thisOrientation.height(unit)
                                && it.depth(unit) eq thisOrientation.depth(unit)
                    }
                    if (!sameOrientationExist) {
                        ret.add(thisOrientation)
                    }
                }
                ret
            }
        }
    }
}

object OrientationSerializer : KSerializer<Orientation> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Orientation", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Orientation) {
        encoder.encodeString(value.label)
    }

    override fun deserialize(decoder: Decoder): Orientation {
        return Orientation.require(decoder.decodeString())
    }
}

infix fun Orientation.ord(rhs: Orientation): Order {
    return when (val value = this.category ord rhs.category) {
        Order.Equal -> {
            this.orderValue() ord rhs.orderValue()
        }

        else -> {
            value
        }
    }
}

fun List<Orientation>.ord(lhs: Orientation, rhs: Orientation): Order {
    return if (this.isEmpty()) {
        lhs ord rhs
    } else {
        val lhsValue = this.indexOf(lhs)
        val rhsValue = this.indexOf(rhs)
        if (lhsValue != -1 && rhsValue != -1) {
            lhsValue ord rhsValue
        } else if (lhsValue == -1) {
            Order.Greater()
        } else {
            Order.Less()
        }
    }
}
