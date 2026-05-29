/**
 * 阿贝尔群概念
 * Abelian Group Concept
 *
 * 定义阿贝尔群（交换群）的代数结构接口，继承自 Group，要求加法运算满足交换律。
 * Defines the algebraic structure interface for Abelian group (commutative group), extending Group and requiring addition operation to satisfy commutativity.
 */
package fuookami.ospf.kotlin.math.algebra.concept

/**
 * 阿贝尔群（交换群）接口
 * Abelian Group (Commutative Group) Interface
 *
 * 继承自 Group，要求二元运算满足交换律，即 a + b == b + a。
 * Extends Group, requiring the binary operation to satisfy commutativity, i.e., a + b == b + a.
 */
interface AbelianGroup<Self> : Group<Self>
