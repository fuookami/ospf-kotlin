package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

enum class OrientationCategory {
    Upright,
    Side,
    Lie
}

@Serializable
enum class Orientation {
    Upright {
        override val rotation get() = UprightRotated
        override val category = OrientationCategory.Upright
    },

    UprightRotated {
        override fun depth(unit: AbstractCuboid) = unit.width
        override fun width(unit: AbstractCuboid) = unit.depth
        override val rotation = Upright
        override val rotated = true
        override val category = OrientationCategory.Upright
    },

    Side {
        override val rotation get() = SideRotated
        override fun height(unit: AbstractCuboid) = unit.width
        override fun width(unit: AbstractCuboid) = unit.height
        override val category = OrientationCategory.Side
    },

    SideRotated {
        override fun depth(unit: AbstractCuboid) = unit.height
        override fun height(unit: AbstractCuboid) = unit.width
        override fun width(unit: AbstractCuboid) = unit.depth
        override val rotation = Side
        override val rotated = true
        override val category = OrientationCategory.Side
    },

    Lie {
        override fun depth(unit: AbstractCuboid) = unit.height
        override fun height(unit: AbstractCuboid) = unit.depth
        override val rotation get() = LieRotated
        override val category = OrientationCategory.Lie
    },

    LieRotated {
        override fun depth(unit: AbstractCuboid) = unit.width
        override fun height(unit: AbstractCuboid) = unit.depth
        override fun width(unit: AbstractCuboid) = unit.height
        override val rotation = Lie
        override val rotated = true
        override val category = OrientationCategory.Lie
    };

    open fun depth(unit: AbstractCuboid): Flt64 = unit.depth
    open fun width(unit: AbstractCuboid): Flt64 = unit.width
    open fun height(unit: AbstractCuboid): Flt64 = unit.height

    abstract val rotation: Orientation
    open val rotated: Boolean = false
    abstract val category: OrientationCategory

    override fun toString() = this.name

    companion object {
        operator fun invoke(str: String): Orientation? {
            return entries.find { it.name == str }
        }

        fun merge(unit: AbstractCuboid, orientations: List<Orientation>): List<Orientation> {
            return if (orientations.isEmpty()) {
                merge(unit, Orientation.entries.toList())
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

infix fun Orientation.ord(rhs: Orientation): Order {
    return when (val value = this.category ord rhs.category) {
        Order.Equal -> {
            orderBetween(this, rhs)
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
