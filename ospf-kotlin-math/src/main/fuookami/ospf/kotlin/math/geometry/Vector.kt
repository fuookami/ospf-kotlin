/**
 * 向量
 * Vector
 *
 * 定义几何空间中的向量数据结构，支持任意维度和数值类型。
 * 向量是具有方向和大小的几何对象，支持内积空间运算。
 * Defines vector data structure in geometric space, supporting arbitrary dimensions and number types.
 * A vector is a geometric object with direction and magnitude, supporting inner product space operations.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> castVectorValue(value: Any): V {
    // 安全不变量：向量范数/夹角结果与向量分量处于同一 V 数域。
    // Safety invariant: vector norm/angle results stay in the same V numeric domain as vector components.
    return value as V
}

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> castNullableVectorValue(value: Any?): V? {
    // 安全不变量：同上；null 分支保留，非 null 分支为 V 兼容值。
    // Safety invariant: same as above; null remains null and non-null values are V-compatible.
    return value as V?
}

/**
 * 计算向量的范数（模长）
 * Calculates the norm (magnitude) of a vector
 *
 * @param V 浮点数类型 / Floating-point number type
 * @param vector 向量分量列表 / List of vector components
 * @return 向量的范数 / Norm of the vector
 */
private fun <V : FloatingNumber<V>> normOf(vector: List<V>): V {
    val v = vector[0]
    return castVectorValue((vector.indices.sumOf(v.constants) { i -> vector[i].sqr() }).sqrt())
}

/**
 * 计算单位向量
 * Calculates the unit vector
 *
 * @param V 浮点数类型 / Floating-point number type
 * @param vector 向量分量列表 / List of vector components
 * @return 单位向量的分量列表 / List of unit vector components
 */
private fun <V : FloatingNumber<V>> unitOf(vector: List<V>): List<V> {
    val norm = normOf(vector)
    return vector.map { it / norm }
}

/**
 * 计算两个向量的点积
 * Calculates the dot product of two vectors
 *
 * @param V 浮点数类型 / Floating-point number type
 * @param lhs 左向量 / Left vector
 * @param rhs 右向量 / Right vector
 * @return 点积值 / Dot product value
 */
private fun <V : FloatingNumber<V>> timesBetween(lhs: List<V>, rhs: List<V>): V {
    assert(lhs.size == rhs.size)
    val v = lhs[0]
    return lhs.indices.sumOf(v.constants) { lhs[it] * rhs[it] }
}

/**
 * 向量类
 * Vector Class
 *
 * 表示几何空间中的向量，支持任意维度和浮点数类型。
 * 实现内积空间接口，支持向量加减、缩放、点积、叉积等运算。
 * Represents a vector in geometric space, supporting arbitrary dimensions and floating-point types.
 * Implements inner product space interface, supporting vector addition, subtraction, scaling, dot product, cross product, etc.
 *
 * @param D 维度类型 / Dimension type
 * @param V 数值类型 / Number type
 * @property vector 向量分量列表 / List of vector components
 * @property dim 维度信息 / Dimension information
 */
open class Vector<D : Dimension, V : FloatingNumber<V>>(
    val vector: List<V>,
    val dim: D
) : InnerProductSpace<Vector<D, V>, V> {
    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <D : Dimension> dim2AsType(): D {
            // 安全不变量：二维构造函数只在调用方期望 D=Dim2 时使用。
            // Safety invariant: 2D factory is used when caller expects D=Dim2.
            return Dim2 as D
        }

        @Suppress("UNCHECKED_CAST")
        private fun <D : Dimension> dim3AsType(): D {
            // 安全不变量：三维构造函数只在调用方期望 D=Dim3 时使用。
            // Safety invariant: 3D factory is used when caller expects D=Dim3.
            return Dim3 as D
        }

        /** 通过二维坐标创建泛型向量 / Create a generic vector from 2D coordinates */
        operator fun <D : Dimension, V : FloatingNumber<V>> invoke(x: V, y: V): Vector<D, V> {
            return Vector(listOf(x, y), dim2AsType())
        }

        /** 通过三维坐标创建泛型向量 / Create a generic vector from 3D coordinates */
        operator fun <D : Dimension, V : FloatingNumber<V>> invoke(x: V, y: V, z: V): Vector<D, V> {
            return Vector(listOf(x, y, z), dim3AsType())
        }

        /** 通过 Flt64 二维坐标创建向量 / Create a vector from Flt64 2D coordinates */
        operator fun invoke(x: Flt64, y: Flt64): Vector<Dim2, Flt64> {
            return Vector(listOf(x, y), Dim2)
        }

        /** 通过 Flt64 三维坐标创建向量 / Create a vector from Flt64 3D coordinates */
        operator fun invoke(x: Flt64, y: Flt64, z: Flt64): Vector<Dim3, Flt64> {
            return Vector(listOf(x, y, z), Dim3)
        }
    }

    val size by vector::size
    val indices by vector::indices
    override val norm: V by lazy { normOf(vector) }
    override val unit: Vector<D, V> by lazy { Vector(unitOf(vector), dim) }

    init {
        assert(vector.size == dim.size)
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    operator fun get(i: Int): V {
        return vector[i]
    }

    override operator fun plus(rhs: Vector<D, V>) = Vector(indices.map { this[it] + rhs[it] }, dim)

    override operator fun minus(rhs: Vector<D, V>) = Vector(indices.map { this[it] - rhs[it] }, dim)

    override fun scale(rhs: V) = Vector(indices.map { this[it] * rhs }, dim)

    override infix fun dot(rhs: Vector<D, V>) = timesBetween(vector, rhs.vector)

    operator fun times(rhs: Vector<D, V>) = dot(rhs)

    operator fun times(rhs: V) = scale(rhs)

    operator fun plus(rhs: Point<D, V>) = Point(indices.map { this[it] + rhs[it] }, dim)

    override fun angle(rhs: Vector<D, V>): V? {
        return castNullableVectorValue<V>(super<InnerProductSpace>.angle(rhs))
    }

    /** 计算在另一向量上的投影 / Compute projection onto another vector */
    fun projectionOn(rhs: Vector<D, V>): Vector<D, V>? {
        return project(rhs)
    }

    /** 计算相对于另一向量的正交分量 / Compute orthogonal component relative to another vector */
    fun orthogonalComponentTo(rhs: Vector<D, V>): Vector<D, V>? {
        return orthogonalComponent(rhs)
    }

    override fun toString() = vector.joinToString(",", "[", "]")
}

/** 二维向量 X 分量 / 2D vector X component */
@get:JvmName("Vector2X")
val Vector<Dim2, Flt64>.x get() = this[0]

/** 二维向量 Y 分量 / 2D vector Y component */
@get:JvmName("Vector2Y")
val Vector<Dim2, Flt64>.y get() = this[1]

/** 创建二维向量 / Create a 2D vector */
fun vector2(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero): Vector<Dim2, Flt64> {
    return Vector(listOf(x, y), Dim2)
}

/** 三维向量 X 分量 / 3D vector X component */
@get:JvmName("Vector3X")
val Vector<Dim3, Flt64>.x get() = this[0]

/** 三维向量 Y 分量 / 3D vector Y component */
@get:JvmName("Vector3Y")
val Vector<Dim3, Flt64>.y get() = this[1]

/** 三维向量 Z 分量 / 3D vector Z component */
@get:JvmName("Vector3Z")
val Vector<Dim3, Flt64>.z get() = this[2]

/** 创建三维向量 / Create a 3D vector */
fun vector3(x: Flt64 = Flt64.zero, y: Flt64 = Flt64.zero, z: Flt64 = Flt64.zero): Vector<Dim3, Flt64> {
    return Vector(listOf(x, y, z), Dim3)
}

/** 标量乘向量（左乘） / Scalar times vector (left multiplication) */
operator fun <D : Dimension, V : FloatingNumber<V>> V.times(rhs: Vector<D, V>): Vector<D, V> {
    return rhs * this
}

/** 二维向量叉积（返回标量） / 2D vector cross product (returns scalar) */
infix fun Vector<Dim2, Flt64>.cross(rhs: Vector<Dim2, Flt64>): Flt64 {
    return this.x * rhs.y - this.y * rhs.x
}

/** 三维向量叉积（返回向量） / 3D vector cross product (returns vector) */
infix fun Vector<Dim3, Flt64>.cross(rhs: Vector<Dim3, Flt64>): Vector<Dim3, Flt64> {
    return vector3(
        x = this.y * rhs.z - this.z * rhs.y,
        y = this.z * rhs.x - this.x * rhs.z,
        z = this.x * rhs.y - this.y * rhs.x
    )
}
