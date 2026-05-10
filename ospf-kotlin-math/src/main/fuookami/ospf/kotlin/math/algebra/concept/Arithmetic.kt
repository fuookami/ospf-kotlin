/**
 * 算术概念
 * Arithmetic Concept
 *
 * 定义算术类型的核心接口，支持复制、相等比较和常量访问。
 * Defines core interfaces for arithmetic types, supporting copying, equality comparison, and constant access.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.PartialEq

/**
 * 算术接口
 * Arithmetic Interface
 *
 * 算术类型的基本接口，提供常量访问和等价判断功能。
 * Basic interface for arithmetic types, providing constant access and equivalence checking.
 *
 * @param Self 算术类型
 * @param Self The arithmetic type
 */
interface Arithmetic<Self> : Copyable<Self>, PartialEq<Self> {
    /**
     * 算术常量
     * Arithmetic constants
     */
    val constants: ArithmeticConstants<Self>

    /**
     * 等价判断
     * Equivalence checking
     *
     * @param rhs 另一个值
     * @param rhs The other value
     * @return 是否等价
     * @return Whether the values are equivalent
     */
    infix fun equiv(rhs: Self): Boolean
}

/**
 * 算术常量接口
 * Arithmetic Constants Interface
 *
 * 提供算术类型的基本常量。
 * Provides basic constants for arithmetic types.
 *
 * @param Self 算术类型
 * @param Self The arithmetic type
 */
interface ArithmeticConstants<Self> : ArithmeticConst<Self>
