/**
 * 域概念
 * Field Concept
 *
 * 定义域的代数结构接口，继承自 CommutativeRing 和MultiplicativeGroup，支持加法、乘法及其逆运算。
 * Defines the algebraic structure interface for field, extending CommutativeRing and MultiplicativeGroup, supporting addition, multiplication and their inverse operations.
*/
package fuookami.ospf.kotlin.math.algebra.concept

/**
 * 域接口
 * Field Interface
 *
 * 继承自 CommutativeRing 和 MultiplicativeGroup，支持加法、乘法及其逆运算。
 * Extends CommutativeRing and MultiplicativeGroup, supporting addition, multiplication, and their inverse operations.
*/
interface Field<Self> : CommutativeRing<Self>, MultiplicativeGroup<Self>
