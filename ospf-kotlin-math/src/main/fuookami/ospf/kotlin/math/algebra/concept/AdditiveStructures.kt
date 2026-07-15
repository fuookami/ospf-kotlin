/**
 * 加法结构
 * Additive Structures
 *
 * 定义加法相关的组合代数结构接口，包括加法半群和加法群的组合类型。
 * Defines composite algebraic structure interfaces related to addition, including additive semigroup and additive group composite types.
*/
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.math.operator.*

/**
 * 加法半群结构接口
 * Additive Semigroup Structure Interface
 *
 * 组合半群、加法运算符和自增运算符的接口。
 * Composite interface combining semigroup, plus operator, and increment operator.
*/
interface PlusSemiGroup<Self> : Semigroup<Self>, Plus<Self, Self>, Inc<Self>

/**
 * 加法群结构接口
 * Additive Group Structure Interface
 *
 * 组合加法半群、阿贝尔群、取负运算符、减法运算符和自减运算符的接口。
 * Composite interface combining additive semigroup, abelian group, negation operator, minus operator, and decrement operator.
*/
interface PlusGroup<Self> : AbelianGroup<Self>, PlusSemiGroup<Self>,
    Neg<Self>, Minus<Self, Self>, Dec<Self>
