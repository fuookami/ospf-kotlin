/**
 * 交换环概念
 * Commutative Ring Concept
 *
 * 定义交换环的代数结构接口，继承自 Ring，要求乘法运算满足交换律。
 * Defines the algebraic structure interface for commutative ring, extending Ring and requiring multiplication operation to satisfy commutativity.
 */
package fuookami.ospf.kotlin.math.algebra.concept

/**
 * 交换环接口
 * Commutative Ring Interface
 *
 * 继承自 Ring，要求乘法运算满足交换律，即 a * b == b * a。
 * Extends Ring, requiring multiplication to satisfy commutativity, i.e., a * b == b * a.
 */
interface CommutativeRing<Self> : Ring<Self>
