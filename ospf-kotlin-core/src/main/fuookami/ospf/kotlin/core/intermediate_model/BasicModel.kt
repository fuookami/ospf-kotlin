package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.token.LegacyAbstractTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableCombinationItem
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Base layer of the model hierarchy: variables + constraints + symbols + caches (no objective).
 *
 * This corresponds to `BasicModel<V>` in the Rust implementation.
 * `MetaModel` extends this with objective functions and higher-level semantics.
 *
 * BasicModel delegates token storage to a [LegacyAbstractMutableTokenTable] provided
 * at construction time -- the same mechanism used by MetaModel.
 */
open class BasicModel<V : RealNumber<V>>(
    open val name: String,
    open val tokens: LegacyAbstractMutableTokenTable
) : AutoCloseable {

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