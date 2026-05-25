/**
 * 符号量纲注册表
 * Symbol Dimension Registry
 *
 * 维护符号到量纲的映射，用于表达式构造前/后的量纲校验。
 * Maintains symbol-to-dimension mapping for dimension validation before/after expression construction.
 *
 * 主要用途 / Main use cases:
 * - 在构建符号表达式前验证量纲语义 / Validate dimension semantics before building symbol expressions
 * - 推导符号运算结果的量纲 / Infer result dimension from symbol operations
 * - 确保符号表达式具备物理意义 / Ensure symbol expressions have physical meaning
 *
 * 示例 / Example:
 * ```kotlin
 * val registry = SymbolDimensionRegistry()
 * val x = DimensionedSymbol("x", "distance", DerivedQuantity.Length, Meter)
 * val y = DimensionedSymbol("y", "distance", DerivedQuantity.Length, Meter)
 * registry.register(x)
 * registry.register(y)
 * registry.validateAddSubDimension(listOf(x, y))  // OK: same dimension
 * registry.inferDimension(x, y, Operation.Multiply)  // Returns Length^2
 * ```
 */
package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.quantities.quantity.DimensionMismatchException
import fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity
import fuookami.ospf.kotlin.quantities.dimension.div
import fuookami.ospf.kotlin.quantities.dimension.times
import java.util.concurrent.ConcurrentHashMap

/**
 * 运算类型
 * Operation type
 *
 * 用于量纲推导时指定运算类型。
 * Used to specify operation type for dimension inference.
 */
enum class Operation {
    /**
     * 加法 / Addition
     */
    Add,

    /**
     * 减法 / Subtraction
     */
    Subtract,

    /**
     * 乘法 / Multiplication
     */
    Multiply,

    /**
     * 除法 / Division
     */
    Divide
}

/**
 * 符号量纲注册表
 * Symbol dimension registry
 *
 * 维护符号到量纲的映射，用于表达式构造前/后的量纲校验。
 * Maintains symbol-to-dimension mapping for dimension validation before/after expression construction.
 *
 * 使用 ConcurrentHashMap 保证线程安全。
 * Uses ConcurrentHashMap for thread safety.
 */
class SymbolDimensionRegistry {
    private val symbolDimensions = ConcurrentHashMap<Symbol, DimensionedSymbol>()

    /**
     * 注册符号及其量纲
     * Register symbol with its dimension
     *
     * 示例 / Example:
     * ```kotlin
     * val x = DimensionedSymbol("x", "distance", DerivedQuantity.Length, Meter)
     * registry.register(x)
     * ```
     *
     * @param symbol 带量纲的符号 / Dimensioned symbol
     */
    fun register(symbol: DimensionedSymbol) {
        symbolDimensions[symbol] = symbol
    }

    /**
     * 获取符号的量纲信息
     * Get dimension info for a symbol
     *
     * 示例 / Example:
     * ```kotlin
     * val x = Symbol("x")
     * val dimSymbol = registry.getDimension(x)
     * ```
     *
     * @param symbol 符号 / Symbol
     * @return 带量纲的符号，或 null 如果未注册 / Dimensioned symbol, or null if not registered
     */
    fun getDimension(symbol: Symbol): DimensionedSymbol? {
        return symbolDimensions[symbol]
    }

    /**
     * 校验加减运算的量纲一致性
     * Validate dimension consistency for add/sub operations
     *
     * 确保所有符号具有相同的量纲，否则抛出异常。
     * Ensures all symbols have the same dimension, otherwise throws exception.
 *
     * 示例 / Example:
     * ```kotlin
     * val x = DimensionedSymbol("x", null, DerivedQuantity.Length, Meter)
     * val y = DimensionedSymbol("y", null, DerivedQuantity.Length, Meter)
     * registry.register(x)
     * registry.register(y)
     * registry.validateAddSubDimension(listOf(x, y))  // OK
     *
     * val z = DimensionedSymbol("z", null, DerivedQuantity.Time, Second)
     * registry.register(z)
     * registry.validateAddSubDimension(listOf(x, z))  // throws DimensionMismatchException
     * ```
     *
     * @param symbols 待校验的符号列表 / Symbols to validate
     * @throws IllegalArgumentException 如果符号未注册 / If symbol is not registered
     * @throws DimensionMismatchException 如果量纲不匹配 / If dimensions don't match
     */
    fun validateAddSubDimension(symbols: List<Symbol>) {
        if (symbols.isEmpty()) return

        val firstDimension = symbolDimensions[symbols.first()]?.quantity
            ?: throw IllegalArgumentException("Symbol ${symbols.first().name} not registered")

        for (symbol in symbols.drop(1)) {
            val dimension = symbolDimensions[symbol]?.quantity
                ?: throw IllegalArgumentException("Symbol ${symbol.name} not registered")

            if (dimension != firstDimension) {
                throw DimensionMismatchException(
                    expected = firstDimension.dimensionSymbol(),
                    actual = dimension.dimensionSymbol(),
                    operation = "addition/subtraction"
                )
            }
        }
    }

    /**
     * 推导运算结果的量纲
     * Infer result dimension from operation
     *
     * 根据运算类型推导两个符号运算结果的量纲。
     * Infers the dimension of operation result based on operation type.
     *
     * 规则 / Rules:
     * - 加减: 结果量纲与操作数相同 / Add/Subtract: result dimension same as operands
     * - 乘法: 结果量纲为操作数量纲之积 / Multiply: result dimension is product of operands' dimensions
     * - 除法: 结果量纲为操作数量纲之商 / Divide: result dimension is quotient of operands' dimensions
     *
     * 示例 / Example:
     * ```kotlin
     * val length = DerivedQuantity.Length
     * val time = DerivedQuantity.Time
     * registry.inferDimension(x, y, Operation.Multiply)  // Length * Length = Length^2
     * registry.inferDimension(x, z, Operation.Divide)  // Length / Time = Speed
     * ```
     *
     * @param symbol1 第一个符号 / First symbol
     * @param symbol2 第二个符号 / Second symbol
     * @param operation 运算类型 / Operation type
     * @return 结果量纲 / Result dimension
     * @throws IllegalArgumentException 如果符号未注册 / If symbol is not registered
     * @throws DimensionMismatchException 如果加减运算量纲不匹配 / If dimensions don't match for add/subtract
     */
    fun inferDimension(
        symbol1: Symbol,
        symbol2: Symbol,
        operation: Operation
    ): DerivedQuantity {
        val dim1 = symbolDimensions[symbol1]?.quantity
            ?: throw IllegalArgumentException("Symbol ${symbol1.name} not registered")
        val dim2 = symbolDimensions[symbol2]?.quantity
            ?: throw IllegalArgumentException("Symbol ${symbol2.name} not registered")

        return when (operation) {
            Operation.Add, Operation.Subtract -> {
                if (dim1 != dim2) {
                    throw DimensionMismatchException(
                        expected = dim1.dimensionSymbol(),
                        actual = dim2.dimensionSymbol(),
                        operation = operation.name.lowercase()
                    )
                }
                dim1
            }
            Operation.Multiply -> dim1 * dim2
            Operation.Divide -> dim1 / dim2
        }
    }

    /**
     * 检查符号是否已注册
     * Check if symbol is registered
     *
     * 示例 / Example:
     * ```kotlin
     * val isRegistered = registry.isRegistered(Symbol("x"))
     * ```
     *
     * @param symbol 符号 / Symbol
     * @return 是否已注册 / Whether registered
     */
    fun isRegistered(symbol: Symbol): Boolean {
        return symbolDimensions.containsKey(symbol)
    }

    /**
     * 移除符号注册
     * Remove symbol registration
     *
     * 示例 / Example:
     * ```kotlin
     * val removed = registry.unregister(Symbol("x"))
     * ```
     *
     * @param symbol 符号 / Symbol
     * @return 是否成功移除 / Whether successfully removed
     */
    fun unregister(symbol: Symbol): Boolean {
        return symbolDimensions.remove(symbol) != null
    }

    /**
     * 清空所有注册
     * Clear all registrations
     *
     * 示例 / Example:
     * ```kotlin
     * registry.clear()
     * ```
     */
    fun clear() {
        symbolDimensions.clear()
    }
}
