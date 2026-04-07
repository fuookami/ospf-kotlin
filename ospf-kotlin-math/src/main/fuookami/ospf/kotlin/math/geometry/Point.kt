/**
 * Point（点）
 * Point
 *
 * 提供 N 维空间中点的几何表示及相关计算。
 * Provides geometric representation and calculations for points in N-dimensional space.
 *
 * 主要功能：
 * Main features:
 * - Point: 泛型点，支持任意维度 / Generic point, supporting arbitrary dimensions
 * - Point2/Point3: 2D/3D 点的类型别名 / 2D/3D point type aliases
 * - 点运算（加减向量）/ Point operations (add/subtract vectors)
 * - 距离计算（支持多种距离度量）/ Distance calculation (supporting various distance metrics)
 * - 近似相等判断（approxEq）/ Approximate equality check (approxEq)
 * - 中点计算（midpoint）/ Midpoint calculation
 *
 * 应用场景：几何计算、空间定位、路径规划、碰撞检测等。
 * Applications: geometric calculations, spatial positioning, path planning, collision detection, etc.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Plus

/**
 * Point - 泛型点
 * Point - Generic point
 *
 * 表示 N 维空间中的点，支持与向量的加减运算。
 * Represents a point in N-dimensional space, supporting addition/subtraction with vectors.
 *
 * @param position 点的坐标列表 / List of point coordinates
 * @param dim 维度类型 / Dimension type
 */
data class Point<D : Dimension>(
    val position: List<Flt64>,
    val dim: D
) : Plus<Point<D>, Point<D>>, Minus<Point<D>, Point<D>>, Eq<Point<D>> {
    companion object {
        /**
         * 创建 2D 点
         * Create a 2D point
         *
         * @param x x 坐标 / x coordinate
         * @param y y 坐标 / y coordinate
         * @return 2D 点 / 2D point
         */
        operator fun invoke(x: Flt64, y: Flt64): Point2 {
            return Point(listOf(x, y), Dim2)
        }

        /**
         * 创建 3D 点
         * Create a 3D point
         *
         * @param x x 坐标 / x coordinate
         * @param y y 坐标 / y coordinate
         * @param z z 坐标 / z coordinate
         * @return 3D 点 / 3D point
         */
        operator fun invoke(x: Flt64, y: Flt64, z: Flt64): Point3 {
            return Point(listOf(x, y, z), Dim3)
        }

        /**
         * 从向量创建点
         * Create a point from a vector
         *
         * @param vector 向量 / Vector
         * @return 点 / Point
         */
        operator fun <D : Dimension> invoke(vector: Vector<D>): Point<D> {
            return Point(vector.vector, vector.dim)
        }
    }

    init {
        assert(position.size == dim.size)
    }

    /** 点的维度大小 / The dimension size of the point */
    val size by dim::size
    /** 点的索引范围 / The index range of the point */
    val indices by dim::indices

    /**
     * 获取点的指定坐标
     * Get the specified coordinate of the point
     *
     * @param i 坐标索引 / Coordinate index
     * @return 坐标值 / Coordinate value
     * @throws ArrayIndexOutOfBoundsException 索引越界时抛出 / Thrown when index is out of bounds
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): Flt64 {
        return position[i]
    }

    /**
     * 计算两点之间的欧几里得距离
     * Calculate the Euclidean distance between two points
     *
     * @param rhs 另一个点 / Another point
     * @return 欧几里得距离 / Euclidean distance
     */
    infix fun distance(rhs: Point<D>): Flt64 = Distance.Euclidean(this, rhs)

    /**
     * 计算两点之间的距离（指定距离度量）
     * Calculate the distance between two points (with specified distance metric)
     *
     * @param rhs 另一个点 / Another point
     * @param type 距离度量类型，默认为欧几里得距离 / Distance metric type, defaults to Euclidean
     * @return 距离 / Distance
     */
    fun distanceBetween(rhs: Point<D>, type: Distance = Distance.Euclidean): Flt64 = type(this, rhs)

    /**
     * 点加法
     * Point addition
     *
     * @param rhs 另一个点 / Another point
     * @return 和点 / Sum point
     */
    override fun plus(rhs: Point<D>) = Point(indices.map { this[it] + rhs[it] }, dim)

    /**
     * 点减法
     * Point subtraction
     *
     * @param rhs 另一个点 / Another point
     * @return 差点 / Difference point
     */
    override fun minus(rhs: Point<D>) = Point(indices.map { this[it] - rhs[it] }, dim)

    /**
     * 点加向量
     * Add vector to point
     *
     * @param rhs 向量 / Vector
     * @return 新的点 / New point
     */
    operator fun plus(rhs: Vector<D>) = Point(indices.map { this[it] + rhs[it] }, dim)

    /**
     * 点减向量
     * Subtract vector from point
     *
     * @param rhs 向量 / Vector
     * @return 新的点 / New point
     */
    operator fun minus(rhs: Vector<D>) = Point(indices.map { this[it] - rhs[it] }, dim)

    /**
     * 判断两个点是否相等（部分相等）
     * Check if two points are equal (partial equality)
     *
     * @param rhs 另一个点 / Another point
     * @return 是否相等 / Whether equal
     */
    override fun partialEq(rhs: Point<D>): Boolean {
        if (dim != rhs.dim) {
            return false
        }

        for (i in indices) {
            if (this[i] neq rhs[i]) {
                return false
            }
        }

        return true
    }

    /**
     * 使用容差判断两个点是否近似相等
     * Check if two points are approximately equal using tolerance
     *
     * @param rhs 另一个点 / The other point
     * @param epsilon 容差，默认使用 decimalPrecision / Tolerance, defaults to decimalPrecision
     * @return 是否近似相等 / Whether approximately equal
     */
    infix fun approxEq(rhs: Point<D>): Boolean {
        if (dim != rhs.dim) {
            return false
        }
        for (i in indices) {
            if ((this[i] - rhs[i]).abs() gr Flt64.decimalPrecision) {
                return false
            }
        }
        return true
    }

    /**
     * 使用指定容差判断两个点是否近似相等
     * Check if two points are approximately equal using specified tolerance
     *
     * @param rhs 另一个点 / The other point
     * @param epsilon 容差 / Tolerance
     * @return 是否近似相等 / Whether approximately equal
     */
    fun approxEq(rhs: Point<D>, epsilon: Flt64): Boolean {
        if (dim != rhs.dim) {
            return false
        }
        for (i in indices) {
            if ((this[i] - rhs[i]).abs() gr epsilon) {
                return false
            }
        }
        return true
    }

    /**
     * 计算两点的中点
     * Calculate the midpoint between two points
     *
     * @param rhs 另一个点 / The other point
     * @return 中点 / The midpoint
     */
    infix fun midpoint(rhs: Point<D>): Point<D> {
        return Point(indices.map { (this[it] + rhs[it]) / Flt64(2.0) }, dim)
    }

    override fun toString() = position.joinToString(",", "[", "]")
}

/** Point2 类型别名，表示 2D 点 / Point2 type alias, representing 2D point */
typealias Point2 = Point<Dim2>

/** 获取 Point2 的 x 坐标 / Get the x coordinate of Point2 */
@get:JvmName("Point2X")
val Point2.x get() = this[0]

/** 获取 Point2 的 y 坐标 / Get the y coordinate of Point2 */
@get:JvmName("Point2Y")
val Point2.y get() = this[1]

/** 获取 Point2 的坐标对 / Get the coordinate pair of Point2 */
val Point2.pair get() = Pair(x, y)

/** 2D 原点 / 2D origin point */
val originPoint2 = point2()

/**
 * 创建 2D 点
 * Create a 2D point
 *
 * @param x x 坐标，默认为 0 / x coordinate, defaults to 0
 * @param y y 坐标，默认为 0 / y coordinate, defaults to 0
 * @return 2D 点 / 2D point
 */
fun point2(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero): Point2 {
    return Point2(x, y)
}

/** Point3 类型别名，表示 3D 点 / Point3 type alias, representing 3D point */
typealias Point3 = Point<Dim3>

/** 获取 Point3 的 x 坐标 / Get the x coordinate of Point3 */
@get:JvmName("Point3X")
val Point3.x get() = this[0]

/** 获取 Point3 的 y 坐标 / Get the y coordinate of Point3 */
@get:JvmName("Point3Y")
val Point3.y get() = this[1]

/** 获取 Point3 的 z 坐标 / Get the z coordinate of Point3 */
@get:JvmName("Point3Z")
val Point3.z get() = this[2]

/** 获取 Point3 的坐标三元组 / Get the coordinate triple of Point3 */
val Point3.triple get() = Triple(x, y, z)

/** 3D 原点 / 3D origin point */
val originPoint3 = point3()

/**
 * 创建 3D 点
 * Create a 3D point
 *
 * @param x x 坐标，默认为 0 / x coordinate, defaults to 0
 * @param y y 坐标，默认为 0 / y coordinate, defaults to 0
 * @param z z 坐标，默认为 0 / z coordinate, defaults to 0
 * @return 3D 点 / 3D point
 */
fun point3(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero, z: Flt64 = Flt64.zero): Point3 {
    return Point3(x, y, z)
}







