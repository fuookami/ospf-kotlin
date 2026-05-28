/**
 * 群概念
 * Group Concept
 *
 * 定义群的代数结构接口，继承自 Monoid，支持逆元、减法和自减运算。
 * Defines the algebraic structure interface for group, extending Monoid with inverse, subtraction, and decrement operations.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.*

/**
 * 群接口
 * Group Interface
 *
 * 群是一个集合配备一个满足结合律的二元运算、单位元和逆元。
 * A group is a set equipped with an associative binary operation, identity element, and inverse elements.
 */
interface Group<Self> : Monoid<Self>, Neg<Self>, Minus<Self, Self>, Dec<Self>
