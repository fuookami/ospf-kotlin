package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

data class ProjectionShape(
    val length: Flt64,
    val width: Flt64
) {
    companion object {
        operator fun invoke(length: Flt64, width: Flt64): ProjectionShape {
            return ProjectionShape(maxOf(length, width), min(length, width))
        }
    }

    val area: Flt64 = length * width

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProjectionShape

        if (length != other.length) return false
        if (width != other.width) return false

        return true
    }

    override fun hashCode(): Int {
        return length.hashCode() or width.hashCode()
    }
}

sealed class ProjectivePlane {
    abstract fun length(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): Flt64
    abstract fun width(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): Flt64
    abstract fun height(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): Flt64

    abstract fun length(space: AbstractContainer3Shape): Flt64
    abstract fun width(space: AbstractContainer3Shape): Flt64
    abstract fun height(space: AbstractContainer3Shape): Flt64

    fun shape(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright) = ProjectionShape.invoke(
        length = this.length(unit, orientation),
        width = this.width(unit, orientation)
    )

    fun shape(space: AbstractContainer3Shape) = ProjectionShape.invoke(
        length = this.length(space),
        width = this.width(space)
    )

    abstract fun distance(point3: Point3): Flt64
    abstract fun point2(point3: Point3): Point2
    abstract fun point3(point2: Point2, distance: Flt64 = Flt64.zero): Point3
    abstract fun vector(distance: Flt64 = Flt64.one): Vector3
}

typealias Direction = ProjectivePlane

/**
 * ZOX
 */
object Bottom : ProjectivePlane() {
    override fun length(unit: AbstractCuboid, orientation: Orientation) = orientation.depth(unit)
    override fun width(unit: AbstractCuboid, orientation: Orientation) = orientation.width(unit)
    override fun height(unit: AbstractCuboid, orientation: Orientation) = orientation.height(unit)

    override fun length(space: AbstractContainer3Shape) = space.depth
    override fun width(space: AbstractContainer3Shape) = space.width
    override fun height(space: AbstractContainer3Shape) = space.height

    override fun distance(point3: Point3) = point3.y
    override fun point2(point3: Point3) = point2(x = point3.z, y = point3.x)
    override fun point3(point2: Point2, distance: Flt64) = point3(x = point2.y, y = distance, z = point2.x)
    override fun vector(distance: Flt64) = vector3(y = distance)

    override fun toString(): String {
        return "Bottom-ZOX"
    }
}

typealias ZOX = Bottom

/**
 * XOY
 */
object Side : ProjectivePlane() {
    override fun length(unit: AbstractCuboid, orientation: Orientation) = orientation.width(unit)
    override fun width(unit: AbstractCuboid, orientation: Orientation) = orientation.height(unit)
    override fun height(unit: AbstractCuboid, orientation: Orientation) = orientation.depth(unit)

    override fun length(space: AbstractContainer3Shape) = space.width
    override fun width(space: AbstractContainer3Shape) = space.height
    override fun height(space: AbstractContainer3Shape) = space.depth

    override fun distance(point3: Point3) = point3.z
    override fun point2(point3: Point3) = point2(x = point3.x, y = point3.y)
    override fun point3(point2: Point2, distance: Flt64) = point3(x = point2.x, y = point2.y, z = distance)
    override fun vector(distance: Flt64) = vector3(z = distance)

    override fun toString(): String {
        return "Side-XOY"
    }
}

typealias XOY = Side

/**
 * ZOY
 */
object Front : ProjectivePlane() {
    override fun length(unit: AbstractCuboid, orientation: Orientation) = orientation.depth(unit)
    override fun width(unit: AbstractCuboid, orientation: Orientation) = orientation.height(unit)
    override fun height(unit: AbstractCuboid, orientation: Orientation) = orientation.width(unit)

    override fun length(space: AbstractContainer3Shape) = space.depth
    override fun width(space: AbstractContainer3Shape) = space.height
    override fun height(space: AbstractContainer3Shape) = space.width

    override fun distance(point3: Point3) = point3.x
    override fun point2(point3: Point3) = point2(x = point3.y, y = point3.z)
    override fun point3(point2: Point2, distance: Flt64) = point3(x = distance, y = point2.x, z = point2.y)
    override fun vector(distance: Flt64) = vector3(x = distance)

    override fun toString(): String {
        return "Front-ZOY"
    }
}

typealias ZOY = Front

sealed interface Projection<
    T : Cuboid<T>,
    P : ProjectivePlane
>: Copyable<Projection<T, P>> {
    val view: CuboidView<T>
    val plane: P
    val unit: T get() = view.unit
    val orientation: Orientation get() = view.orientation
    val length: Flt64 get() = plane.length(view)
    val width: Flt64 get() = plane.width(view)
    val height: Flt64 get() = plane.height(view)
    val area: Flt64 get() = length * width
    val weight: Flt64 get() = unit.weight

    fun amount(unit: AbstractCuboid): UInt64
}

data class PlaneProjection<
    T : Cuboid<T>,
    P : ProjectivePlane
>(
    override val view: CuboidView<T>,
    override val plane: P
) : Projection<T, P> {
    override fun amount(unit: AbstractCuboid): UInt64 {
        return if (unit == this.unit) {
            UInt64.one
        } else {
            UInt64.zero
        }
    }

    override fun copy() = PlaneProjection(view.copy(), plane)
}

data class PileProjection<
    T : Cuboid<T>,
    P : ProjectivePlane
>(
    override val view: CuboidView<T>,
    override val plane: P,
    val layer: UInt64,
) : Projection<T, P> {
    override val height = plane.height(view) * layer.toFlt64()
    override val weight = unit.weight * layer.toFlt64()

    constructor(plane: PlaneProjection<T, P>, layer: UInt64 = UInt64.one) : this(plane.view, plane.plane, layer)

    override fun amount(unit: AbstractCuboid): UInt64 {
        return if (unit == this.unit) {
            layer
        } else {
            UInt64.zero
        }
    }

    override fun copy() = PileProjection(view.copy(), plane, layer)
}

data class MultiPileProjection<
    T : Cuboid<T>,
    P : ProjectivePlane
>(
    val views: List<CuboidView<T>>,
    override val plane: P,
) : Projection<T, P> {
    override val view = views.first()

    override val length = views.maxOf { plane.length(it) }
    override val width = views.maxOf { plane.width(it) }
    override val height = views.sumOf { plane.height(it) }
    override val weight = views.sumOf { it.weight }

    override fun amount(unit: AbstractCuboid) = UInt64(views.count { it.unit == unit })

    override fun copy() = MultiPileProjection(views.map { it.copy() }, plane)
}
