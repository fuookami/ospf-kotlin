/**
 * 乘法幺半群
 * Multiplicative Monoid
 *
 * 定义乘法幺半群的代数结构接口，继承自 MultiplicativeSemigroup，具有乘法单位元。
 * Defines the algebraic structure interface for multiplicative monoid, extending MultiplicativeSemigroup with multiplicative identity element.
*/
package fuookami.ospf.kotlin.math.algebra.concept

/**
 * 乘法幺半群接口
 * Multiplicative Monoid Interface
 *
 * 继承自 MultiplicativeSemigroup，具有乘法单位元（一）。
 * Extends MultiplicativeSemigroup, possessing a multiplicative identity element (one).
*/
interface MultiplicativeMonoid<Self> : MultiplicativeSemigroup<Self>
