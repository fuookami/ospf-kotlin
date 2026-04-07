/**
 * 幺半群概念
 * Monoid Concept
 *
 * 定义幺半群的代数结构接口，继承自 Semigroup 和 Inc，具有结合律和单位元。
 * Defines the algebraic structure interface for monoid, extending Semigroup and Inc with associativity and identity element.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.Inc

/**
 * 幺半群接口
 * Monoid Interface
 *
 * 幺半群是一个集合配备一个满足结合律的二元运算和一个单位元。
 * A monoid is a set equipped with an associative binary operation and an identity element.
 */
interface Monoid<Self> : Semigroup<Self>, Inc<Self>

