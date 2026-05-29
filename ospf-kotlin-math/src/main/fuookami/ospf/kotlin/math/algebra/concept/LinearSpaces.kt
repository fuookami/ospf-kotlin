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
    /**
     * 获取自身（CRTP 模式的类型安全转换）
     * Get self (type-safe cast for CRTP pattern)
     *
     * @return 当前实例的 Self 类型引用
     * @return The Self-typed reference to this instance
     */
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

    /**
     * 计算范数的平方
     * Compute the squared norm
     *
     * @return 范数的平方值
     * @return The squared norm value
     */
    fun normSquared(): Scalar {
        return norm * norm
    }

    /**
     * 归一化向量，返回单位向量
     * Normalize the vector to return a unit vector
     *
     * @return 归一化后的单位向量，若范数为零则返回 null
     * @return The normalized unit vector, or null if the norm is zero
     */
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

    /**
     * 计算两个向量之间的夹角（弧度）
     * Compute the angle (in radians) between two vectors
     *
     * @param rhs 另一个向量
     * @param rhs The other vector
     * @return 夹角（弧度），若任一范数为零则返回 null
     * @return The angle in radians, or null if either vector has zero norm
     */
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

    /**
     * 判断两个向量是否正交
     * Check whether two vectors are orthogonal
     *
     * @param rhs 另一个向量
     * @param rhs The other vector
     * @param epsilon 容差值
     * @param epsilon The tolerance value
     * @return 是否正交（内积的绝对值小于等于 epsilon）
     * @return Whether the vectors are orthogonal (absolute dot product <= epsilon)
     */
    fun isOrthogonal(rhs: Self, epsilon: Scalar): Boolean {
        return (this dot rhs).abs() <= epsilon
    }

    /**
     * 计算两个向量的余弦相似度
     * Compute the cosine similarity between two vectors
     *
     * @param rhs 另一个向量
     * @param rhs The other vector
     * @return 余弦相似度值，若任一范数为零则返回 null
     * @return The cosine similarity value, or null if either vector has zero norm
     */
    fun cosineSimilarity(rhs: Self): Scalar? {
        val denominator = norm * rhs.norm
        return if (denominator eq denominator.constants.zero) {
            null
        } else {
            (this dot rhs) / denominator
        }
    }

    /**
     * 计算当前向量在另一个向量上的投影
     * Compute the projection of this vector onto another vector
     *
     * @param rhs 目标向量
     * @param rhs The target vector
     * @return 投影向量，若目标向量范数为零则返回 null
     * @return The projection vector, or null if the target vector has zero norm
     */
    fun project(rhs: Self): Self? {
        val denominator = rhs dot rhs
        return if (denominator eq denominator.constants.zero) {
            null
        } else {
            rhs.scale((this dot rhs) / denominator)
        }
    }

    /**
     * 计算当前向量相对于另一个向量的正交分量
     * Compute the orthogonal component of this vector with respect to another vector
     *
     * @param rhs 参考向量
     * @param rhs The reference vector
     * @return 正交分量，若投影不存在则返回 null
     * @return The orthogonal component, or null if projection is not possible
     */
    fun orthogonalComponent(rhs: Self): Self? {
        val projection = project(rhs) ?: return null
        return this - projection
    }
}
