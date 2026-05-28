/**
 * 线性空间概念
 * Linear Spaces Concept
 *
 * 定义线性空间相关的代数结构接口，包括全序、向量空间、赋范空间和内积空间。
 * Defines algebraic structure interfaces related to linear spaces, including total ordering, vector space, normed space, and inner product space.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.math.operator.*

/**
 * 全序接口
 * Totally Ordered Interface
 *
 * 定义全序关系，继承自 Ord 接口。
 * Defines total ordering relation, extending from Ord interface.
 */
interface TotallyOrdered<Self : Comparable<Self>> : Ord<Self> {
    @Suppress("UNCHECKED_CAST")
    private fun self(): Self {
        // 安全不变量：实现方通过 CRTP 模式声明为 TotallyOrdered<Self>，运行时 this 即 Self。
        // Safety invariant: implementers follow CRTP as TotallyOrdered<Self>, so runtime this is Self.
        return this as Self
    }

    /**
     * 取两个值中的较小倌
     * Get the minimum of two values
     *
     * @param rhs 另一个倌
     * @param rhs The other value
     * @return 较小倌
     * @return The minimum value
     */
    fun minValue(rhs: Self): Self = if (self() <= rhs) self() else rhs

    /**
     * 取两个值中的较大倌
     * Get the maximum of two values
     *
     * @param rhs 另一个倌
     * @param rhs The other value
     * @return 较大倌
     * @return The maximum value
     */
    fun maxValue(rhs: Self): Self = if (self() >= rhs) self() else rhs

    /**
     * 判断值是否在指定范围册
     * Check if the value is within the specified range
     *
     * @param lower 下界
     * @param lower The lower bound
     * @param upper 上界
     * @param upper The upper bound
     * @return 是否在范围内
     * @return Whether the value is within the range
     */
    fun isBetween(lower: Self, upper: Self): Boolean = self() >= lower && self() <= upper

    /**
     * 将值限制在指定范围册
     * Clamp the value to the specified range
     *
     * @param lower 下界
     * @param lower The lower bound
     * @param upper 上界
     * @param upper The upper bound
     * @return 限制后的倌
     * @return The clamped value
     */
    fun clampValue(lower: Self, upper: Self): Self = when {
        self() < lower -> lower
        self() > upper -> upper
        else -> self()
    }
}

/**
 * 向量空间接口
 * Vector Space Interface
 *
 * 向量空间是一个集合，支持向量加法、减法和标量缩放运算。
 * A vector space is a set that supports vector addition, subtraction, and scalar multiplication.
 *
 * @param Self 向量类型
 * @param Self The vector type
 * @param Scalar 标量类型
 * @param Scalar The scalar type
 */
interface VectorSpace<Self : VectorSpace<Self, Scalar>, Scalar> : Plus<Self, Self>, Minus<Self, Self> {
    /**
     * 标量缩放运算
     * Scalar multiplication operation
     *
     * @param rhs 标量倌
     * @param rhs The scalar value
     * @return 缩放后的向量
     * @return The scaled vector
     */
    fun scale(rhs: Scalar): Self
}

/**
 * 赋范空间接口
 * Normed Space Interface
 *
 * 赋范空间是一个向量空间，具有范数和单位向量概念。
 * A normed space is a vector space with norm and unit vector concepts.
 *
 * @param Self 向量类型
 * @param Self The vector type
 * @param Scalar 标量类型
 * @param Scalar The scalar type
 */
interface NormedSpace<Self : VectorSpace<Self, Scalar>, Scalar> : VectorSpace<Self, Scalar>
        where Scalar : RealNumber<Scalar>, Scalar : NumberField<Scalar> {
    /**
     * 向量的范敌
     * The norm of the vector
     */
    val norm: Scalar

    /**
     * 单位向量
     * The unit vector
     */
    val unit: Self

    fun normSquared(): Scalar {
        return norm * norm
    }

    fun normalize(): Self? {
        return if (norm eq norm.constants.zero) {
            null
        } else {
            scale(norm.constants.one / norm)
        }
    }
}

/**
 * 内积空间接口
 * Inner Product Space Interface
 *
 * 内积空间是一个赋范空间，支持内积运算。
 * An inner product space is a normed space that supports dot product operation.
 *
 * @param Self 向量类型
 * @param Self The vector type
 * @param Scalar 标量类型
 * @param Scalar The scalar type
 */
interface InnerProductSpace<Self : InnerProductSpace<Self, Scalar>, Scalar> : NormedSpace<Self, Scalar>
        where Scalar : RealNumber<Scalar>, Scalar : NumberField<Scalar> {
    /**
     * 内积运算
     * Dot product operation
     *
     * @param rhs 另一个向里
     * @param rhs The other vector
     * @return 内积倌
     * @return The dot product value
     */
    infix fun dot(rhs: Self): Scalar

    fun angle(rhs: Self): FloatingNumber<*>? {
        val cosine = cosineSimilarity(rhs) ?: return null
        val one = cosine.constants.one
        val clamped = when {
            cosine > one -> one
            cosine < -one -> -one
            else -> cosine
        }
        return clamped.acos()
    }

    fun isOrthogonal(rhs: Self, epsilon: Scalar): Boolean {
        return (this dot rhs).abs() <= epsilon
    }

    fun cosineSimilarity(rhs: Self): Scalar? {
        val denominator = norm * rhs.norm
        return if (denominator eq denominator.constants.zero) {
            null
        } else {
            (this dot rhs) / denominator
        }
    }

    fun project(rhs: Self): Self? {
        val denominator = rhs dot rhs
        return if (denominator eq denominator.constants.zero) {
            null
        } else {
            rhs.scale((this dot rhs) / denominator)
        }
    }

    fun orthogonalComponent(rhs: Self): Self? {
        val projection = project(rhs) ?: return null
        return this - projection
    }
}
