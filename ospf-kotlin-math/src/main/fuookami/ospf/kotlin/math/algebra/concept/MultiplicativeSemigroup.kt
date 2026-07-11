/**
 * 乘法半群概念
 * Multiplicative Semigroup Concept
 *
 * 定义乘法半群的代数结构接口，支持乘法运算。
 * Defines the algebraic structure interface for multiplicative semigroup, supporting multiplication operation.
*/
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.Times

/**
 * 乘法半群接口
 * Multiplicative Semigroup Interface
 *
 * 乘法半群是一个集合配备一个满足结合律的乘法运算。
 * A multiplicative semigroup is a set equipped with an associative multiplication operation.
*/
interface MultiplicativeSemigroup<Self> : Times<Self, Self>
