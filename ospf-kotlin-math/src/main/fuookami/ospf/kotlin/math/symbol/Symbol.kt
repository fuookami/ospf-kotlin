/**
 * 符号定义
 * Symbol Definition
 *
 * 定义符号计算中符号的基本接口。符号是符号表达式中变量的抽象表示，
 * 每个符号具有唯一的名称和可选的显示名称。
 * Defines the basic interface for symbols in symbolic computation.
 * A symbol is an abstract representation of variables in symbolic expressions,
 * each with a unique name and optional display name.
 */
package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/**
 * 符号接口
 * Symbol Interface
 *
 * 表示符号表达式中的变量符号。
 * Represents a variable symbol in symbolic expressions.
 *
 * @property name 符号的唯一标识名称 / Unique identifier name of the symbol
 * @property displayName 符号的显示名称（可选），用于输出和展示 / Display name (optional) for output and visualization
 */
interface Symbol {
    val name: String
    val displayName: String?
}




