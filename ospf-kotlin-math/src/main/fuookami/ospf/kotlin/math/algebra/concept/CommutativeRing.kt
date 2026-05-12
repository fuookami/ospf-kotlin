/**
 * 交换环概念
 * Commutative Ring Concept
 *
 * 定义交换环的代数结构接口，继承自 Ring，要求乘法运算满足交换律。
 * Defines the algebraic structure interface for commutative ring, extending Ring and requiring multiplication operation to satisfy commutativity.
 */
package fuookami.ospf.kotlin.math.algebra.concept

interface CommutativeRing<Self> : Ring<Self>
