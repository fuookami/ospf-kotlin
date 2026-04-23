/**
 * Vector（向量）
 * Vector
 *
 * 提供 N 维向量的几何表示及相关计算。
 * Provides geometric representation and calculations for N-dimensional vectors.
 *
 * 主要功能：
 * Main features:
 * - Vector: 泛型向量，支持任意维度 / Generic vector, supporting arbitrary dimensions
 * - Vector2/Vector3: 2D/3D 向量的类型别名 / 2D/3D vector type aliases
 * - 向量运算（加减、点积、叉积）/ Vector operations (addition, subtraction, dot product, cross product)
 * - 向量属性（范数、单位向量）/ Vector properties (norm, unit vector)
 * - 向量投影和角度计算 / Vector projection and angle calculation
 *
 * 应用场景：几何计算、物理模拟、图形渲染、方向表示等。
 * Applications: geometric calculations, physics simulation, graphics rendering, direction representation, etc.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.operator.Minus
import fuookami.ospf.kotlin.math.operator.Plus

/**
 * 计算向量的范数（长度）
 * Calculate the norm (length) of a vector
 *
 * @param vector 向量的分量列表 / List of vector components
 * @return 向量的范数 / The norm of the vector
 */
private fun normOf(vector: List<Flt64>): Flt64 {
    return (vector.sumOf(Flt64) { it.sqr() }).sqrt()
}

/**
 * 计算向量的单位向量
 * Calculate the unit vector of a vector
 *
 * @param vector 向量的分量列表 / List of vector components
 * @return 单位向量的分量列表 / List of unit vector components
 */
private fun unitOf(vector: List<Flt64>): List<Flt64> {
    val norm = normOf(vector)
    return vector.map { it / norm }
}

/**
 * 计算两个向量之间的内积（点积）
 * Calculate the inner product (dot product) between two vectors
 *
 * @param lhs 左向量 / Left vector
 * @param rhs 右向量 / Right vector
 * @return 内积值 / The inner product value
 */
private fun timesBetween(lhs: List<Flt64>, rhs: List<Flt64>): Flt64 {
    assert(lhs.size == rhs.size)
    return lhs.indices.sumOf(Flt64) { lhs[it] * rhs[it] }
}

/**
 * Vector - 泛型向量
 * Vector - Generic vector
 *
 * 表示 N 维空间中的向量，实现内积空间接口。
 * Represents a vector in N-dimensional space, implementing the inner product space interface.
 *
 * @param vector 向量的分量列表 / List of vector components
 * @param dim 维度类型 / Dimension type
 */
open class Vector<D : Dimension>(
    val vector: List<Flt64>,
    val dim: D
) : InnerProductSpace<Vector<D>, Flt64> {
    companion object {
        /**
         * 创建 2D 向量
         * Create a 2D vector
         *
         * @param x x 分量 / x component
         * @param y y 分量 / y component
         * @return 2D 向量 / 2D vector
         */
        operator fun invoke(x: Flt64, y: Flt64): Vector2 {
            return Vector2(listOf(x, y), Dim2)
        }

        /**
         * 创建 3D 向量
         * Create a 3D vector
         *
         * @param x x 分量 / x component
         * @param y y 分量 / y component
         * @param z z 分量 / z component
         * @return 3D 向量 / 3D vector
         */
        operator fun invoke(x: Flt64, y: Flt64, z: Flt64): Vector3 {
            return Vector3(listOf(x, y, z), Dim3)
        }
    }

    /** 向量的维度大小 / The dimension size of the vector */
    val size by vector::size
    /** 向量的索引范围 / The index range of the vector */
    val indices by vector::indices
    /** 向量的范数（长度）/ The norm (length) of the vector */
    override val norm: Flt64 by lazy { normOf(vector) }
    /** 向量的单位向量 / The unit vector of the vector */
    override val unit: Vector<D> by lazy { Vector(unitOf(vector), dim) }

    init {
        assert(vector.size == dim.size)
    }

    /**
     * 获取向量的指定分量
     * Get the specified component of the vector
     *
     * @param i 分量索引 / Component index
     * @return 分量值 / Component value
     * @throws ArrayIndexOutOfBoundsException 索引越界时抛出 / Thrown when index is out of bounds
     */
    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): Flt64 {
        return vector[i]
    }

    /**
     * 向量加法
     * Vector addition
     *
     * @param rhs 另一个向量 / Another vector
     * @return 和向量 / Sum vector
     */
    override operator fun plus(rhs: Vector<D>) = Vector(indices.map { this[it] + rhs[it] }, dim)

    /**
     * 向量减法
     * Vector subtraction
     *
     * @param rhs 另一个向量 / Another vector
     * @return 差向量 / Difference vector
     */
    override operator fun minus(rhs: Vector<D>) = Vector(indices.map { this[it] - rhs[it] }, dim)

    /**
     * 向量缩放
     * Vector scaling
     *
     * @param rhs 缩放因子 / Scaling factor
     * @return 缩放后的向量 / Scaled vector
     */
    override fun scale(rhs: Flt64) = Vector(indices.map { this[it] * rhs }, dim)

    /**
     * 向量点积（内积）
     * Vector dot product (inner product)
     *
     * @param rhs 另一个向量 / Another vector
     * @return 点积值 / Dot product value
     */
    override infix fun dot(rhs: Vector<D>) = timesBetween(vector, rhs.vector)

    /**
     * 向量点积（操作符形式）
     * Vector dot product (operator form)
     *
     * @param rhs 另一个向量 / Another vector
     * @return 点积值 / Dot product value
     */
    operator fun times(rhs: Vector<D>) = dot(rhs)

    /**
     * 向量缩放（操作符形式）
     * Vector scaling (operator form)
     *
     * @param rhs 缩放因子 / Scaling factor
     * @return 缩放后的向量 / Scaled vector
     */
    operator fun times(rhs: Flt64) = scale(rhs)

    /**
     * 向量与点相加
     * Add vector to point
     *
     * @param rhs 点 / Point
     * @return 新的点 / New point
     */
    operator fun plus(rhs: Point<D>) = Point(indices.map { this[it] + rhs[it] }, dim)

    /**
     * 计算两个向量之间的夹角
     * Calculate the angle between two vectors
     *
     * 返回弧度值，若任一向量为零向量则返回 null。
     * Returns angle in radians, returns null if either vector is zero.
     *
     * @param rhs 另一个向量 / Another vector
     * @return 夹角（弧度），或 null / Angle (radians), or null
     */
    override fun angle(rhs: Vector<D>): Flt64? {
        return super<InnerProductSpace>.angle(rhs) as? Flt64
    }

    /**
     * 计算向量在另一向量上的投影
     * Calculate the projection of this vector onto another vector
     *
     * 若目标向量为零向量则返回 null。
     * Returns null if the target vector is zero.
     *
     * @param rhs 目标向量 / Target vector
     * @return 投影向量，或 null / Projection vector, or null
     */
    fun projectionOn(rhs: Vector<D>): Vector<D>? {
        return project(rhs)
    }

    fun orthogonalComponentTo(rhs: Vector<D>): Vector<D>? {
        return orthogonalComponent(rhs)
    }

    override fun toString() = vector.joinToString(",", "[", "]")
}

/** Vector2 类型别名，表示 2D 向量 / Vector2 type alias, representing 2D vector */
typealias Vector2 = Vector<Dim2>

/** 获取 Vector2 的 x 分量 / Get the x component of Vector2 */
@get:JvmName("Vector2X")
val Vector2.x get() = this[0]

/** 获取 Vector2 的 y 分量 / Get the y component of Vector2 */
@get:JvmName("Vector2Y")
val Vector2.y get() = this[1]

/**
 * 创建 2D 向量
 * Create a 2D vector
 *
 * @param x x 分量，默认为 0 / x component, defaults to 0
 * @param y y 分量，默认为 0 / y component, defaults to 0
 * @return 2D 向量 / 2D vector
 */
fun vector2(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero): Vector2 {
    return Vector2(x, y)
}

/** Vector3 类型别名，表示 3D 向量 / Vector3 type alias, representing 3D vector */
typealias Vector3 = Vector<Dim3>

/** 获取 Vector3 的 x 分量 / Get the x component of Vector3 */
@get:JvmName("Vector3X")
val Vector3.x get() = this[0]

/** 获取 Vector3 的 y 分量 / Get the y component of Vector3 */
@get:JvmName("Vector3Y")
val Vector3.y get() = this[1]

/** 获取 Vector3 的 z 分量 / Get the z component of Vector3 */
@get:JvmName("Vector3Z")
val Vector3.z get() = this[2]

/**
 * 创建 3D 向量
 * Create a 3D vector
 *
 * @param x x 分量，默认为 0 / x component, defaults to 0
 * @param y y 分量，默认为 0 / y component, defaults to 0
 * @param z z 分量，默认为 0 / z component, defaults to 0
 * @return 3D 向量 / 3D vector
 */
fun vector3(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero, z: Flt64 = Flt64.zero): Vector3 {
    return Vector3(x, y, z)
}

/**
 * 数值与向量的乘法（反向操作符）
 * Multiplication of number and vector (reverse operator)
 *
 * @param rhs 向量 / Vector
 * @return 缩放后的向量 / Scaled vector
 */
operator fun <D : Dimension> Flt64.times(rhs: Vector<D>): Vector<D> {
    return rhs * this
}

/**
 * 2D 向量叉积（返回标量）
 * 2D vector cross product (returns scalar)
 *
 * 2D 向量叉积的结果是一个标量，表示平行四边形的有向面积。
 * The result of 2D vector cross product is a scalar, representing the signed area of the parallelogram.
 *
 * @param rhs 另一个向量 / Another vector
 * @return 叉积值（标量）/ Cross product value (scalar)
 */
infix fun Vector2.cross(rhs: Vector2): Flt64 {
    return this.x * rhs.y - this.y * rhs.x
}

/**
 * 3D 向量叉积（返回向量）
 * 3D vector cross product (returns vector)
 *
 * 3D 向量叉积的结果是一个垂直于两个输入向量的新向量。
 * The result of 3D vector cross product is a new vector perpendicular to both input vectors.
 *
 * @param rhs 另一个向量 / Another vector
 * @return 叉积向量 / Cross product vector
 */
infix fun Vector3.cross(rhs: Vector3): Vector3 {
    return vector3(
        x = this.y * rhs.z - this.z * rhs.y,
        y = this.z * rhs.x - this.x * rhs.z,
        z = this.x * rhs.y - this.y * rhs.x
    )
}







