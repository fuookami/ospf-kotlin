/**
 * 方向基础设施。
 * Orientation infrastructure.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 方向类别枚举。
 * Orientation category enum.
*/
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
 *
 * @property label 方向标签
 * @property rotation 对应的旋转方向
 * @property rotated 是否为旋转变体
 * @property category 方向类别
*/
@Serializable(with = OrientationSerializer::class)
sealed class Orientation {

    /**
     * 竖直方向（默认方向）。
     * Upright orientation (default orientation).
    */
    object Upright : Orientation() {
        override val label = "Upright"
        override val rank = 0
        override val rotation get() = UprightRotated
        override val category = OrientationCategory.Upright
    }

    /**
     * 竖直旋转方向，深度与宽度互换。
     * Upright rotated orientation, depth and width swapped.
    */
    object UprightRotated : Orientation() {
        override val label = "UprightRotated"
        override val rank = 1
        override fun <V : FloatingNumber<V>> depth(unit: AbstractCuboid<V>) = unit.width
        override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>) = unit.depth
        override val rotation = Upright
        override val rotated = true
        override val category = OrientationCategory.Upright
    }

    /**
     * 侧向方向，高度与宽度互换。
     * Side orientation, height and width swapped.
    */
    object Side : Orientation() {
        override val label = "Side"
        override val rank = 2
        override val rotation get() = SideRotated
        override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>) = unit.width
        override fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>) = unit.height
        override val category = OrientationCategory.Side
    }

    /**
     * 侧向旋转方向，深度与高度互换，宽度不变。
     * Side rotated orientation, depth and height swapped, width unchanged.
    */
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

    /**
     * 平躺方向，高度与深度互换。
     * Lie orientation, height and depth swapped.
    */
    object Lie : Orientation() {
        override val label = "Lie"
        override val rank = 4
        override fun <V : FloatingNumber<V>> depth(unit: AbstractCuboid<V>) = unit.height
        override fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>) = unit.depth
        override val rotation get() = LieRotated
        override val category = OrientationCategory.Lie
    }

    /**
     * 平躺旋转方向，宽度与高度互换，深度不变。
     * Lie rotated orientation, width and height swapped, depth unchanged.
    */
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

    /**
     * 方向的深度尺寸。
     * Depth dimension of the orientation.
    */
    open fun <V : FloatingNumber<V>> depth(unit: AbstractCuboid<V>): Quantity<V> = unit.depth

    /**
     * 方向的宽度尺寸。
     * Width dimension of the orientation.
    */
    open fun <V : FloatingNumber<V>> width(unit: AbstractCuboid<V>): Quantity<V> = unit.width

    /**
     * 方向的高度尺寸。
     * Height dimension of the orientation.
    */
    open fun <V : FloatingNumber<V>> height(unit: AbstractCuboid<V>): Quantity<V> = unit.height

    abstract val rotation: Orientation
    open val rotated: Boolean = false
    abstract val category: OrientationCategory

    /**
     * 获取方向的排序值。
     * Get the ordering value of the orientation.
     * @return 排序值
    */
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

        /**
         * 根据字符串名称查找方向。
         * Find an orientation by string name.
         * @param str 方向名称
         * @return 找到的方向，若未找到则返回 null
        */
        operator fun invoke(str: String): Orientation? {
            return entries.find { it.label == str }
        }

        /**
         * 根据字符串名称获取方向，若不存在则返回错误。
         * Resolve an orientation by string name, returning an error if not found.
         * @param str 方向名称
         * @return 包含方向的结果或错误
        */
        fun require(str: String): Ret<Orientation> {
            val orientation = invoke(str)
            return if (orientation != null) {
                Ok(orientation)
            } else {
                Failed(ErrorCode.IllegalArgument, "Unsupported orientation: $str")
            }
        }

        /**
         * 合并方向列表，去除尺寸重复的方向。
         * Merge orientation list, removing orientations with duplicate dimensions.
         * @param unit 参考容器
         * @param orientations 待合并的方向列表
         * @return 合并后的方向列表
        */
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

/**
 * 方向类型的序列化器。
 * Serializer for the Orientation type.
*/
object OrientationSerializer : KSerializer<Orientation> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Orientation", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Orientation) {
        encoder.encodeString(value.label)
    }

    override fun deserialize(decoder: Decoder): Orientation {
        return when (val result = Orientation.require(decoder.decodeString())) {
            is Ok -> result.value
            is Failed -> throw SerializationException(result.error.message)
            is Fatal -> throw SerializationException(result.errors.joinToString { it.message })
        }
    }
}

/**
 * 根据自定义顺序比较两个方向的大小关系。
 * Compare two orientations according to a custom order.
 * @param rhs 右侧方向
 * @return 比较结果
*/
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

/**
 * 根据列表顺序比较两个方向的大小关系。
 * Compare two orientations according to list order.
 * @param lhs 左侧方向
 * @param rhs 右侧方向
 * @return 比较结果
*/
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
