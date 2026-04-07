/**
 * 线性空间概念
 * Linear Spaces Concept
 *
 * 定义线性空间相关的代数结构接口，包括全序、向量空间、赋范空间和内积空间。
 * Defines algebraic structure interfaces related to linear spaces, including total ordering, vector space, normed space, and inner product space.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.math.operator.Plus
import fuookami.ospf.kotlin.math.operator.Minus

/**
 * 全序接口
 * Totally Ordered Interface
 *
 * 定义全序关系，继承自 Ord 接口。
 * Defines total ordering relation, extending from Ord interface.
 */
interface TotallyOrdered<in Self> : Ord<Self>

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
interface VectorSpace<Self, Scalar> : Plus<Self, Self>, Minus<Self, Self> {
    /**
     * 标量缩放运算
     * Scalar multiplication operation
     *
     * @param rhs 标量值
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
interface NormedSpace<Self, Scalar> : VectorSpace<Self, Scalar> {
    /**
     * 向量的范数
     * The norm of the vector
     */
    val norm: Scalar

    /**
     * 单位向量
     * The unit vector
     */
    val unit: Self
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
interface InnerProductSpace<Self, Scalar> : NormedSpace<Self, Scalar> {
    /**
     * 内积运算
     * Dot product operation
     *
     * @param rhs 另一个向量
     * @param rhs The other vector
     * @return 内积值
     * @return The dot product value
     */
    infix fun dot(rhs: Self): Scalar
}
