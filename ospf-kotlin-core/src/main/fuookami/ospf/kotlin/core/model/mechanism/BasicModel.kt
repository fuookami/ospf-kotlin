/**
 * 模型基础层
 * Model base layer
*/
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 模型层级结构的基础层：变量 + 约束 + 符号 + 缓存（无目标函数）。
 * Base layer of the model hierarchy: variables + constraints + symbols + caches (no objective).
 *
 * 对应 Rust 实现中的 `BasicModel<V>`。
 * `MetaModel` 在此基础上扩展了目标函数和更高层语义。
 *
 * BasicModel delegates token storage to a [AbstractMutableTokenTable<V>] provided
 * at construction time -- the same mechanism used by MetaModel.
 *
 * This corresponds to `BasicModel<V>` in the Rust implementation.
 * `MetaModel` extends this with objective functions and higher-level semantics.
 *
 * @property name 模型名称 / Model name
 * @property tokens 可变符号表 / Mutable token table
*/
open class BasicModel<V>(
    open val name: String,
    open val tokens: AbstractMutableTokenTable<V>
) : AutoCloseable where V : RealNumber<V>, V : NumberField<V> {

    /** 符号表。 / Symbol table. */
    val symbols: MutableList<IntermediateSymbol<*>> = mutableListOf()

    /** 符号依赖图，委托给 token 表实现。 / Symbol dependency graph, delegated to the token table where available. */
    open val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>>
        get() = tokens.symbolDependencies

    // ── Variable management ──────────────────────────────────────────────

    /**
     * 添加变量项。
     * Add a single variable item.
     *
     * @param item 待添加的变量项 / The variable item to add
     * @return 操作结果 / The operation result
    */
    open fun add(item: AbstractVariableItem<*, *>): Try = tokens.add(item)

    /**
     * 批量添加变量项。
     * Add variable items in batch.
     *
     * @param items 待添加的变量项集合 / The collection of variable items to add
     * @return 操作结果 / The operation result
    */
    open fun add(items: Iterable<AbstractVariableItem<*, *>>): Try = tokens.add(items)

    // ── Symbol management ────────────────────────────────────────────────

    /**
     * 添加符号到模型。
     * Add a symbol to the model.
     *
     * @param symbol 待添加的符号 / The symbol to add
     * @return 操作结果 / The operation result
    */
    fun addSymbol(symbol: IntermediateSymbol<*>): Try {
        symbols.add(symbol)
        return tokens.add(symbol)
    }

    /**
     * 添加符号及其依赖项到模型。
     * Add a symbol and its dependencies to the model.
     *
     * @param symbol       要添加的符号 / The symbol to add
     * @param dependencies 该符号所依赖的符号集合 / The set of symbols this symbol depends on
     * @return 操作结果 / The operation result
    */
    fun addSymbolWithDependencies(
        symbol: IntermediateSymbol<*>,
        dependencies: Set<IntermediateSymbol<*>>
    ): Try {
        if (!symbols.contains(symbol)) {
            symbols.add(symbol)
        }
        for (dep in dependencies) {
            if (!symbols.contains(dep)) {
                symbols.add(dep)
            }
        }
        // Propagate dependency edges to the concrete token table if supported. / 如果支持，将依赖边传播到具体的符号表。
        val t = tokens
        when (t) {
            is MutableTokenTable -> {
                t.addSymbolWithDependencies(symbol, dependencies)
            }
            is ConcurrentMutableTokenTable -> {
                t.addSymbolWithDependencies(symbol, dependencies)
            }
        }
        return tokens.add(symbol)
    }

    /**
     * 从模型中移除符号。
     * Remove a symbol from the model.
     *
     * @param symbol 待移除的符号 / The symbol to remove
    */
    fun removeSymbol(symbol: IntermediateSymbol<*>) {
        symbols.remove(symbol)
        tokens.remove(symbol)
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    /**
     * 刷新模型状态；当 [force] 为 `true` 时同时清除已缓存的求解结果。
     * Flush model state; when [force] is `true`, also clear cached solution data.
     *
     * @param force 是否强制清除已缓存的求解结果 / Whether to force clear cached solution data
    */
    open fun flush(force: Boolean) {
        if (force) {
            tokens.clearSolution()
        }
        tokens.flush()
        for (symbol in tokens.symbols) {
            symbol.flush(force)
        }
    }

    override fun close() {
        tokens.flush()
    }
}
