/**
 * 值提供者适配噌 * Value Provider Adapter
 *
 * 提供符号值查找的适配器接口，用于在表达式求值时获取符号对应的数值。 * 支持不同的值来源，妌Map、函数调用或自定义实现。 * Provides adapter interfaces for symbol value lookup,
 * used to retrieve numeric values for symbols during expression evaluation.
 * Supports different value sources such as Map, function calls, or custom implementations.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.Symbol

/**
 * 值提供者接双 * Value Provider Interface
 *
 * 函数式接口，用于根据符号查找对应的浮点数值。 * 在表达式求值时，通过此接口获取符号绑定的实际数值。 * Functional interface for looking up floating-point values by symbol.
 * During expression evaluation, retrieves the actual numeric values bound to symbols.
 *
 * @see MapValueProvider 基于 Map 的实玌/ Map-based implementation
 */
fun interface ValueProvider {
    /**
     * 获取符号对应的倌     * Gets the value for a symbol
     *
     * @param symbol 要查找值的符号 / The symbol to look up the value for
     * @return 符号对应的浮点数值，如果不存在则返回null / The floating-point value for the symbol, or null if not found
     */
    operator fun get(symbol: Symbol): Flt64?
}

/**
 * Map 值提供而 * Map Value Provider
 *
 * 基于 Map 的值提供者实现，使用 Map 存储符号到数值的映射关系。 * Map-based value provider implementation, storing symbol-to-value mappings in a Map.
 *
 * @property values 符号到数值的映射 / Mapping from symbols to values
 */
class MapValueProvider(
    private val values: Map<Symbol, Flt64>
) : ValueProvider {
    /**
     * 从内郌Map 中获取符号对应的倌     * Gets the value for a symbol from the internal Map
     *
     * @param symbol 要查找值的符号 / The symbol to look up the value for
     * @return 符号对应的浮点数值，如果不存在则返回null / The floating-point value for the symbol, or null if not found
     */
    override fun get(symbol: Symbol): Flt64? {
        return values[symbol]
    }
}

/**
 * 缺失值处理策界 * Missing Value Policy
 *
 * 定义当值提供者找不到符号对应值时的处理策略。 * Defines the handling strategy when a value provider cannot find a value for a symbol.
 *
 * - [ReturnNull]: 返回 null，表示无法获取倌/ Returns null to indicate the value cannot be retrieved
 * - [AsZero]: 将缺失值视为零 / Treats missing values as zero
 * - [Fail]: 抛出异常，表示这是错误情册/ Throws an exception to indicate an error condition
 */
enum class MissingValuePolicy {
    /**
     * 返回 null，表示无法获取倌     * Returns null to indicate the value cannot be retrieved
     */
    ReturnNull,

    /**
     * 将缺失值视为零
     * Treats missing values as zero
     */
    AsZero,

    /**
     * 抛出异常，表示这是错误情册     * Throws an exception to indicate an error condition
     */
    Fail
}
