/**
 * 乘法结构
 * Multiplicative Structures
 *
 * 定义乘法相关的组合代数结构接口，包括乘法半群和乘法群的组合类型。
 * Defines composite algebraic structure interfaces related to multiplication, including multiplicative semigroup and multiplicative group composite types.
*/
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.*

/**
 * 乘法半群结构接口
 * Multiplicative Semigroup Structure Interface
 *
 * 组合乘法半群和乘法运算符的接口。
 * Composite interface combining multiplicative semigroup and times operator.
*/
interface TimesSemiGroup<Self> : MultiplicativeSemigroup<Self>, Times<Self, Self>

/**
 * 乘法群结构接口
 * Multiplicative Group Structure Interface
 *
 * 组合乘法群、乘法半群、倒数、除法、整除和取余运算符的接口。
 * Composite interface combining multiplicative group, times semigroup, reciprocal, division, integer division, and remainder operators.
*/
interface TimesGroup<Self> : MultiplicativeGroup<Self>, TimesSemiGroup<Self>,
    Reciprocal<Self>, Div<Self, Self>, IntDiv<Self, Self>, Rem<Self, Self>
