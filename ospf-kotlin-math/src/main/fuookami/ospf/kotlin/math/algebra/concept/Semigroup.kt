/**
 * 半群概念
 * Semigroup Concept
 *
 * 定义半群的代数结构接口，支持加法运算。
 * Defines the algebraic structure interface for semigroup, supporting addition operation.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.Plus

/**
 * 半群接口
 * Semigroup Interface
 *
 * 半群是一个集合配备一个满足结合律的加法运算。
 * A semigroup is a set equipped with an associative addition operation.
 */
interface Semigroup<Self> : Plus<Self, Self>

