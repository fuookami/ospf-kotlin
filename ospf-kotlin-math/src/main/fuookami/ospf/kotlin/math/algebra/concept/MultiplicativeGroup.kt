/**
 * 乘法群概念
 * Multiplicative Group Concept
 *
 * 定义乘法群的代数结构接口，继承自 MultiplicativeMonoid，支持乘法逆元、除法、整除和取余运算。
 * Defines the algebraic structure interface for multiplicative group, extending MultiplicativeMonoid with multiplicative inverse, division, integer division, and remainder operations.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.Div
import fuookami.ospf.kotlin.math.operator.IntDiv
import fuookami.ospf.kotlin.math.operator.Reciprocal
import fuookami.ospf.kotlin.math.operator.Rem

/**
 * 乘法群接口
 * Multiplicative Group Interface
 *
 * 乘法群是一个集合配备乘法运算、单位元和逆元，满足群公理。
 * A multiplicative group is a set equipped with multiplication operation, identity element, and inverse elements satisfying group axioms.
 */
interface MultiplicativeGroup<Self> : MultiplicativeMonoid<Self>,
    Reciprocal<Self>,
    Div<Self, Self>,
    IntDiv<Self, Self>,
    Rem<Self, Self>

