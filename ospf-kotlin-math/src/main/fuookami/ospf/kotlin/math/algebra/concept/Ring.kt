/**
 * 环概念
 * Ring Concept
 *
 * 定义环的代数结构接口，继承自 AbelianGroup 和MultiplicativeSemigroup，具有加法群结构和乘法半群结构。
 * Defines the algebraic structure interface for ring, extending AbelianGroup and MultiplicativeSemigroup, possessing additive group structure and multiplicative semigroup structure.
*/
package fuookami.ospf.kotlin.math.algebra.concept

/**
 * 环接口
 * Ring Interface
 *
 * 继承自 AbelianGroup 和 MultiplicativeSemigroup，具有加法群结构和乘法半群结构。
 * Extends AbelianGroup and MultiplicativeSemigroup, possessing additive group structure and multiplicative semigroup structure.
*/
interface Ring<Self> : AbelianGroup<Self>, MultiplicativeSemigroup<Self>
