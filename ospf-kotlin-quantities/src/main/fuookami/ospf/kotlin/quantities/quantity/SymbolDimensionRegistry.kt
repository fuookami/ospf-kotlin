package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity
import fuookami.ospf.kotlin.quantities.dimension.div
import fuookami.ospf.kotlin.quantities.dimension.times
import java.util.concurrent.ConcurrentHashMap

/**
 * 运算类型
 * Operation type
 */
enum class Operation {
    Add, Subtract, Multiply, Divide
}

/**
 * 符号量纲注册表
 * Symbol dimension registry
 *
 * 维护符号到量纲的映射，用于表达式构造前/后的量纲校验。
 * Maintains symbol-to-dimension mapping for dimension validation before/after expression construction.
 */
class SymbolDimensionRegistry {
    private val symbolDimensions = ConcurrentHashMap<Symbol, DimensionedSymbol>()

    /**
     * 注册符号及其量纲
     * Register symbol with its dimension
     */
    fun register(symbol: DimensionedSymbol) {
        symbolDimensions[symbol] = symbol
    }

    /**
     * 获取符号的量纲信息
     * Get dimension info for a symbol
     */
    fun getDimension(symbol: Symbol): DimensionedSymbol? {
        return symbolDimensions[symbol]
    }

    /**
     * 校验加减运算的量纲一致性
     * Validate dimension consistency for add/sub operations
     *
     * @throws DimensionMismatchException 如果量纲不匹配
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
     */
    fun isRegistered(symbol: Symbol): Boolean {
        return symbolDimensions.containsKey(symbol)
    }

    /**
     * 移除符号注册
     * Remove symbol registration
     */
    fun unregister(symbol: Symbol): Boolean {
        return symbolDimensions.remove(symbol) != null
    }

    /**
     * 清空所有注册
     * Clear all registrations
     */
    fun clear() {
        symbolDimensions.clear()
    }
}