/**
 * 域概念
 * Field Concept
 *
 * 定义域的代数结构接口，继承自 CommutativeRing 和MultiplicativeGroup，支持加法、乘法及其逆运算。
 * Defines the algebraic structure interface for field, extending CommutativeRing and MultiplicativeGroup, supporting addition, multiplication and their inverse operations.
 */
package fuookami.ospf.kotlin.math.algebra.concept

interface Field<Self> : CommutativeRing<Self>, MultiplicativeGroup<Self>
