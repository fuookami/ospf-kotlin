/**
 * 值提供者适配器
 * Value Provider Adapter
 *
 * 提供符号值查找的适配器接口，用于在表达式求值时获取符号对应的数值。
 * 支持不同的值来源，如 Map、函数调用或自定义实现。
 * Provides adapter interfaces for symbol value lookup,
 * used to retrieve numeric values for symbols during expression evaluation.
 * Supports different value sources such as Map, function calls, or custom implementations.
 */
package fuookami.ospf.kotlin.math.symbol.adapter

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol

fun interface ValueProvider {
    operator fun get(symbol: Symbol): Flt64?
}

class MapValueProvider(
    private val values: Map<Symbol, Flt64>
) : ValueProvider {
    override fun get(symbol: Symbol): Flt64? {
        return values[symbol]
    }
}

enum class MissingValuePolicy {
    ReturnNull,
    AsZero,
    Fail
}




