/**
 * 模型基础层
 * Model base layer
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 模型层级结构的基础层：变量 + 约束 + 符号 + 缓存（无目标函数）。
 * Base layer of the model hierarchy: variables + constraints + symbols + caches (no objective).
 *
 * This corresponds to `BasicModel<V>` in the Rust implementation.
 * `MetaModel` extends this with objective functions and higher-level semantics.
 *
 * BasicModel delegates token storage to a [AbstractMutableTokenTable<V>] provided
 * at construction time -- the same mechanism used by MetaModel.
 */
open class BasicModel<V>(
    open val name: String,
    open val tokens: AbstractMutableTokenTable<V>
) : AutoCloseable where V : RealNumber<V>, V : NumberField<V> {

    /** Symbol table. */
    val symbols: MutableList<IntermediateSymbol<*>> = mutableListOf()

    /** Symbol dependency graph, delegated to the token table where available. */
    open val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>>
        get() = tokens.symbolDependencies

    // ── Variable management ──────────────────────────────────────────────

    open fun add(item: AbstractVariableItem<*, *>): Try = tokens.add(item)

    open fun add(items: Iterable<AbstractVariableItem<*, *>>): Try = tokens.add(items)

    // ── Symbol management ────────────────────────────────────────────────

    fun addSymbol(symbol: IntermediateSymbol<*>): Try {
        symbols.add(symbol)
        return tokens.add(symbol)
    }

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
        // Propagate dependency edges to the concrete token table if supported.
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

    fun removeSymbol(symbol: IntermediateSymbol<*>) {
        symbols.remove(symbol)
        tokens.remove(symbol)
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

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
