package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuantityIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTableF64
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTableF64
import fuookami.ospf.kotlin.core.token.ConcurrentManualAddTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentAutoTokenTable
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticInequalityConstraint
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticFlattenSubObject
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.LinearModel
import fuookami.ospf.kotlin.core.model.basic.Model
import fuookami.ospf.kotlin.core.model.basic.QuadraticModel
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticInequality
import fuookami.ospf.kotlin.core.token.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.token.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataF64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

/**
 * Factory function to create the appropriate [AbstractMutableTokenTable<Flt64>]
 * based on configuration. Used by [AbstractMetaModel] to construct the token
 * table before passing it to the [BasicModel] superclass constructor.
 */
private fun createTokenTable(
    category: Category,
    concurrent: Boolean,
    manualTokenAddition: Boolean,
    checkTokenExists: Boolean
): AbstractMutableTokenTable<Flt64> {
    return if (concurrent) {
        if (manualTokenAddition) {
            ConcurrentManualAddTokenTable(category, checkTokenExists)
        } else {
            ConcurrentAutoTokenTable(category, checkTokenExists)
        }
    } else {
        if (manualTokenAddition) {
            ManualTokenTable(category, checkTokenExists)
        } else {
            AutoTokenTable(category, checkTokenExists)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V> createTokenTableAs(
    category: Category,
    concurrent: Boolean,
    manualTokenAddition: Boolean,
    checkTokenExists: Boolean
): AbstractMutableTokenTable<V> where V : RealNumber<V>, V : NumberField<V> {
    return createTokenTable(category, concurrent, manualTokenAddition, checkTokenExists) as AbstractMutableTokenTable<V>
}

private fun UtilsLinearPolynomial<Flt64>.toRawString(unfold: UInt64 = UInt64.zero): String {
    return if (monomials.isEmpty()) {
        "$constant"
    } else if (constant neq Flt64.zero) {
        "${monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }} + $constant"
    } else {
        monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }
    }
}

private fun UtilsQuadraticPolynomial<Flt64>.toRawString(unfold: UInt64 = UInt64.zero): String {
    return if (monomials.isEmpty()) {
        "$constant"
    } else if (constant neq Flt64.zero) {
        "${monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }} + $constant"
    } else {
        monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }
    }
}

sealed interface MetaModel<V> : Model, AutoCloseable where V : RealNumber<V>, V : NumberField<V> {
    class SubObject<V>(
        val parent: MetaModel<V>,
        val category: ObjectCategory,
        val name: String,
        val displayName: String? = null,
        // Flt64-internal by design: polynomial arithmetic requires Ring<V> bound not yet available here.
        val polynomial: UtilsLinearPolynomial<Flt64>
    ) where V : RealNumber<V>, V : NumberField<V> {
        /** Flt64 view of evaluation (solver-compatible, internal). */
        fun evaluate(zeroIfNone: Boolean = false): Flt64? {
            return evaluate(
                tokenTable = parent.tokens,
                zeroIfNone = zeroIfNone
            )
        }

        fun evaluate(solution: List<Flt64>, zeroIfNone: Boolean = false): Flt64? {
            var result = polynomial.constant
            for (m in polynomial.monomials) {
                val variable = m.symbol as? AbstractVariableItem<*, *> ?: return if (zeroIfNone) Flt64.zero else null
                val idx = parent.tokens.indexOf(variable) ?: return if (zeroIfNone) Flt64.zero else null
                result += m.coefficient * solution[idx]
            }
            return result
        }

        fun evaluate(tokenTable: AbstractTokenTable<V>, zeroIfNone: Boolean = false): Flt64? {
            var result = polynomial.constant
            for (m in polynomial.monomials) {
                val variable = m.symbol as? AbstractVariableItem<*, *> ?: return if (zeroIfNone) Flt64.zero else null
                val tokenResult = tokenTable.find(variable)?.resultF64 ?: return if (zeroIfNone) Flt64.zero else null
                result += m.coefficient * tokenResult
            }
            return result
        }

        fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, zeroIfNone: Boolean = false): Flt64? {
            var result = polynomial.constant
            for (m in polynomial.monomials) {
                val variable = m.symbol as? AbstractVariableItem<*, *> ?: return if (zeroIfNone) Flt64.zero else null
                val idx = tokenTable.indexOf(variable) ?: return if (zeroIfNone) Flt64.zero else null
                result += m.coefficient * results[idx]
            }
            return result
        }

        /** V-typed evaluation via IntoValue<V> conversion. */
        fun evaluateAsV(converter: fuookami.ospf.kotlin.core.solver.value.IntoValue<V>, zeroIfNone: Boolean = false): V? =
            evaluate(zeroIfNone)?.let { converter.intoValue(it) }

        fun evaluateAsV(solution: List<Flt64>, converter: fuookami.ospf.kotlin.core.solver.value.IntoValue<V>, zeroIfNone: Boolean = false): V? =
            evaluate(solution, zeroIfNone)?.let { converter.intoValue(it) }

        fun evaluateAsV(tokenTable: AbstractTokenTable<V>, converter: fuookami.ospf.kotlin.core.solver.value.IntoValue<V>, zeroIfNone: Boolean = false): V? =
            evaluate(tokenTable, zeroIfNone)?.let { converter.intoValue(it) }

        fun evaluateAsV(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: fuookami.ospf.kotlin.core.solver.value.IntoValue<V>, zeroIfNone: Boolean = false): V? =
            evaluate(results, tokenTable, zeroIfNone)?.let { converter.intoValue(it) }

        fun flush(force: Boolean = false) {
            // Math polynomials don't have caching
        }
    }

    val name: String
    val constraints: List<MathConstraint>
    override val objectCategory: ObjectCategory
    val subObjects: List<SubObject<V>>
    val tokens: AbstractMutableTokenTable<V>
    val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>> get() = tokens.symbolDependencies

    override fun add(item: AbstractVariableItem<*, *>): Try {
        return tokens.add(item)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVars")
    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        return tokens.add(items)
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        tokens.remove(item)
    }

    fun add(symbol: IntermediateSymbol<*>): Try {
        return tokens.add(symbol)
    }

    fun add(symbol: QuantityIntermediateSymbol): Try {
        return tokens.add(symbol.value)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addSymbols")
    fun add(symbols: Iterable<IntermediateSymbol<*>>): Try {
        return tokens.add(symbols)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapSymbols")
    fun <K> add(symbols: Map<K, IntermediateSymbol<*>>): Try {
        return tokens.add(symbols.values)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapSymbolLists")
    fun <K> add(symbols: Map<K, Iterable<IntermediateSymbol<*>>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2Symbols")
    fun <K1, K2> add(symbols: MultiMap2<K1, K2, IntermediateSymbol<*>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2SymbolLists")
    fun <K1, K2> add(symbols: MultiMap2<K1, K2, Iterable<IntermediateSymbol<*>>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3Symbols")
    fun <K1, K2, K3> add(symbols: MultiMap3<K1, K2, K3, IntermediateSymbol<*>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3SymbolLists")
    fun <K1, K2, K3> add(symbols: MultiMap3<K1, K2, K3, Iterable<IntermediateSymbol<*>>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4Symbols")
    fun <K1, K2, K3, K4> add(symbols: MultiMap4<K1, K2, K3, K4, IntermediateSymbol<*>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4SymbolLists")
    fun <K1, K2, K3, K4> add(symbols: MultiMap4<K1, K2, K3, K4, Iterable<IntermediateSymbol<*>>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantitySymbols")
    fun add(symbols: Iterable<QuantityIntermediateSymbol>): Try {
        return tokens.add(symbols.map { it.value })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapQuantitySymbols")
    fun <K> add(symbols: Map<K, QuantityIntermediateSymbol>): Try {
        return tokens.add(symbols.values.map { it.value })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapQuantitySymbolLists")
    fun <K> add(symbols: Map<K, Iterable<QuantityIntermediateSymbol>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2QuantitySymbols")
    fun <K1, K2> add(symbols: MultiMap2<K1, K2, QuantityIntermediateSymbol>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2QuantitySymbolLists")
    fun <K1, K2> add(symbols: MultiMap2<K1, K2, Iterable<QuantityIntermediateSymbol>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3QuantitySymbols")
    fun <K1, K2, K3> add(symbols: MultiMap3<K1, K2, K3, QuantityIntermediateSymbol>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3QuantitySymbolLists")
    fun <K1, K2, K3> add(symbols: MultiMap3<K1, K2, K3, Iterable<QuantityIntermediateSymbol>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4QuantitySymbols")
    fun <K1, K2, K3, K4> add(symbols: MultiMap4<K1, K2, K3, K4, QuantityIntermediateSymbol>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4QuantitySymbolLists")
    fun <K1, K2, K3, K4> add(symbols: MultiMap4<K1, K2, K3, K4, Iterable<QuantityIntermediateSymbol>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    fun remove(symbol: IntermediateSymbol<*>) {
        tokens.remove(symbol)
    }

    fun registerConstraintGroup(group: MetaConstraintGroup)
    fun indicesOfConstraintGroup(group: MetaConstraintGroup): IntRange?

    fun constraintsOfGroup(group: MetaConstraintGroup): List<MathConstraint> {
        return indicesOfConstraintGroup(group)?.let { indices ->
            indices.map { constraints[it] }
        } ?: constraints.filter { it.group == group }
    }

    override fun setSolution(solution: Solution) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        tokens.setSolution(solution)
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }

    fun flush(force: Boolean = false) {
        if (force) {
            tokens.clearSolution()
        }
        tokens.flush()
        for (symbol in tokens.symbols) {
            symbol.flush(force)
        }
        for (constraint in constraints) {
            // Math inequality types don't have flush - they reference tokens via polynomial
        }
        for (objective in subObjects) {
            // Math polynomials don't have caching - no-op
        }
    }

    suspend fun export(): Try {
        return export("$name.opm")
    }

    suspend fun export(name: String): Try {
        return export(Path(".").resolve(name))
    }

    suspend fun export(path: String, unfold: Boolean): Try {
        return export(
            Path(".").resolve(name), if (unfold) {
                UInt64.zero
            } else {
                UInt64.maximum
            }
        )
    }

    suspend fun export(path: String, unfold: UInt64): Try {
        return export(Path(".").resolve(name), unfold)
    }

    suspend fun export(path: Path, unfold: UInt64 = UInt64.zero): Try {
        val file = if (path.isDirectory()) {
            path.resolve("$name.opm").toFile()
        } else {
            path.toFile()
        }
        if (!file.exists()) {
            withContext(Dispatchers.IO) {
                file.createNewFile()
            }
        }
        val writer = withContext(Dispatchers.IO) {
            FileWriter(file)
        }
        val result = when (file.extension) {
            "opm" -> {
                exportOpm(writer, unfold)
            }

            else -> {
                ok
            }
        }
        withContext(Dispatchers.IO) {
            writer.flush()
            writer.close()
        }
        return result
    }

    private suspend fun exportOpm(writer: FileWriter, unfold: UInt64): Try {
        val temp = when (tokens) {
            is MutableTokenTable<*> -> tokens.copy() as MutableTokenTableF64
            is ConcurrentMutableTokenTable<*> -> tokens.copy() as ConcurrentMutableTokenTableF64
            else -> throw IllegalStateException("Unknown token table type: ${tokens::class}")
        }

        for (symbol in tokens.symbols) {
            if (symbol is MathFunctionSymbol<*>) {
                when (val result = symbol.registerAuxiliaryTokens(temp)) {
                    is Ok -> {}
                    is Failed -> {
                        return Failed(result.error)
                    }
                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return withContext(Dispatchers.IO) {
            writer.append("Model Name: $name\n")
            writer.append("\n")

            writer.append("Variables:\n")
            for (token in tokens.tokens.toList().sortedBy { it.solverIndex }) {
                val range = token.range
                writer.append("${token.name}, ${token.type}, ")
                if (range == null) {
                    writer.append("empty\n")
                } else {
                    writer.append("${range}\n")
                }
            }
            writer.append("\n")

            writer.append("Symbols:\n")
            for (symbol in tokens.symbols.toList().sortedBy { it.name }) {
                val range = symbol.range
                writer.append("$symbol = ${symbol.toRawString(UInt64.one)}, ")
                if (range.empty) {
                    writer.append("empty")
                } else {
                    writer.append("${range}\n")
                }
            }
            writer.append("\n")

            writer.append("Objectives:\n")
            for (obj in subObjects) {
                writer.append("${obj.category} ${obj.name}: ${obj.polynomial.toRawString(unfold)} \n")
            }
            writer.append("\n")

            writer.append("Subject to:\n")
            for (constraint in constraints) {
                writer.append("$constraint\n")
            }
            writer.append("\n")

            ok
        }
    }

    override fun close() {
        tokens.close()
    }
}

// Backward compatibility: typealias aliases
typealias MetaModelF64 = MetaModel<Flt64>

interface AbstractLinearMetaModel<V> : MetaModel<V>, LinearModel where V : RealNumber<V>, V : NumberField<V> {
    fun addConstraint(
        constraint: AbstractVariableItem<*, *>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            relation =constraint eq true,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: UtilsLinearPolynomial<Flt64>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            relation = constraint eq Flt64.one,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: LinearIntermediateSymbol<*>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            relation =constraint eq true,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    /**
     * Add constraint using math LinearInequality
     */
    fun addConstraint(
        relation: MathLinearInequality,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        priority: Int? = null,
        withRangeSet: Boolean? = false
    ): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(
                monomials = variables.map { fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(fuookami.ospf.kotlin.math.algebra.number.Flt64.one, it) }.toList(),
                constant = fuookami.ospf.kotlin.math.algebra.number.Flt64.zero
            ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun partition(
        symbols: Iterable<LinearIntermediateSymbol<*>>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(
                monomials = symbols.map { fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(fuookami.ospf.kotlin.math.algebra.number.Flt64.one, it) }.toList(),
                constant = fuookami.ospf.kotlin.math.algebra.number.Flt64.zero
            ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun partition(
        polynomial: UtilsLinearPolynomial<Flt64>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(
            relation = polynomial eq Flt64.one,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }
}

// Backward compatibility: typealias aliases
typealias AbstractLinearMetaModelF64 = AbstractLinearMetaModel<Flt64>

interface AbstractQuadraticMetaModel<V> : MetaModel<V>, QuadraticModel where V : RealNumber<V>, V : NumberField<V> {
    fun addConstraint(
        constraint: UtilsQuadraticPolynomial<Flt64>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            relation = constraint eq Flt64.one,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: QuadraticIntermediateSymbol<*>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            relation =constraint eq true,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    /**
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: MathQuadraticInequality,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        priority: Int? = null,
        withRangeSet: Boolean? = null
    ): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun partition(
        symbols: Iterable<QuadraticIntermediateSymbol<*>>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = UtilsQuadraticPolynomial(
                monomials = symbols.map { it.toMathQuadraticPolynomial() }.flatMap { it.monomials }.toList(),
                constant = fuookami.ospf.kotlin.math.algebra.number.Flt64.zero
            ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun partition(
        polynomial: UtilsQuadraticPolynomial<Flt64>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(
            relation = polynomial eq Flt64.one,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }
}

// Backward compatibility: typealias aliases
typealias AbstractQuadraticMetaModelF64 = AbstractQuadraticMetaModel<Flt64>

data class MetaModelConfiguration(
    internal val manualTokenAddition: Boolean = true,
    internal val concurrent: Boolean = true,
    internal val dumpBlocking: Boolean = false,
    internal val withRangeSet: Boolean = false,
    internal val checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
)

abstract class AbstractMetaModel<V>(
    val category: Category,
    internal val configuration: MetaModelConfiguration
) : BasicModel<V>(
    name = "",
    tokens = createTokenTableAs(
        category,
        configuration.concurrent,
        configuration.manualTokenAddition,
        configuration.checkTokenExists
    )
), MetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // BasicModel provides: name, tokens, symbols, constraints (MetaConstraint), symbolDependencies,
    // add(variable), addSymbol, addSymbolWithDependencies, removeSymbol, addConstraint, flush, close.
    // The MetaModel<V> sealed interface is also implemented; its abstract members
    // (constraints, objectCategory, subObjects, etc.) are provided by concrete subclasses.

    // Resolve diamond inheritance: both BasicModel and MetaModel<V> provide symbolDependencies.
    override val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>>
        get() = tokens.symbolDependencies

    // Resolve diamond inheritance: both BasicModel and MetaModel<V> provide add(item).
    // Both delegate to tokens.add(), so the behavior is identical.
    override fun add(item: AbstractVariableItem<*, *>): Try = tokens.add(item)
    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try = tokens.add(items)

    // Resolve default parameter conflict: both BasicModel.flush and MetaModel.flush
    // declare force=false. Kotlin requires an explicit override without a new default.
    override fun flush(force: Boolean) {
        super<BasicModel>.flush(force)
        // Constraints and sub-objects have no caching in the math-inequality world.
    }

    override fun close() {
        super<BasicModel>.close()
        tokens.close()
    }

    private var currentConstraintGroup: MetaConstraintGroup? = null
    private var currentConstraintGroupIndexLowerBound: Int? = null
    private val constraintGroupIndexMap = HashMap<MetaConstraintGroup, IntRange>()

    override fun registerConstraintGroup(group: MetaConstraintGroup) {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!!..<constraints.size
        }
        currentConstraintGroup = group
        currentConstraintGroupIndexLowerBound = constraints.size
    }

    override fun indicesOfConstraintGroup(group: MetaConstraintGroup): IntRange? {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!!..<constraints.size
            currentConstraintGroup = null
            currentConstraintGroupIndexLowerBound = null
        }
        return constraintGroupIndexMap[group]
    }
}

// Backward compatibility: typealias aliases
typealias AbstractMetaModelF64 = AbstractMetaModel<Flt64>

class LinearMetaModel<V>(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration()
) : AbstractMetaModel<V>(Linear, configuration), AbstractLinearMetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // Math inequality-based constraints storage
    internal val _relationConstraints: MutableList<LinearInequalityConstraint> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    val relationConstraints: List<LinearInequalityConstraint> by ::_relationConstraints

    internal val _subObjects: MutableList<MetaModel.SubObject<V>> = ArrayList()
    @Suppress("UNCHECKED_CAST")
    override val subObjects: List<MetaModel.SubObject<V>> by ::_subObjects

    // NEW: FlattenData-based sub-objects storage
    internal val _flattenSubObjects: MutableList<LinearSubObject<Flt64>> = ArrayList()
    val flattenSubObjects: List<LinearSubObject<Flt64>> by ::_flattenSubObjects

    fun addObject(
        category: ObjectCategory,
        polynomial: UtilsLinearPolynomial<Flt64>,
        name: String,
        displayName: String?
    ): Try {
        _subObjects.add(
            MetaModel.SubObject<V>(
                parent = this,
                category = category,
                name = name,
                displayName = displayName,
                polynomial = polynomial
            )
        )
        return ok
    }

    /**
     * Add objective using LinearFlattenDataF64 (new API)
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenDataF64,
        name: String,
        displayName: String?
    ): Try {
        val subObject = LinearSubObject.invoke(
            category = category,
            flattenData = flattenData,
            tokens = tokens,
            name = name
        )
        _flattenSubObjects.add(subObject)
        return ok
    }

    /**
     * Add constraint using math LinearInequality (LinearModel interface)
     */
    override fun addConstraint(
        relation: MathLinearInequality,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        return addConstraint(
            relation = relation,
            group = null,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = null,
            priority = null,
            withRangeSet = withRangeSet
        )
    }

    override fun toString(): String {
        return name
    }

    /**
     * Add constraint using math LinearInequality (new API)
     */
    override fun addConstraint(
        relation: MathLinearInequality,
        group: MetaConstraintGroup?,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        args: Any?,
        priority: Int?,
        withRangeSet: Boolean?
    ): Try {
        _relationConstraints.add(
            LinearInequalityConstraint(
                inequality = relation,
                group = group,
                lazy = lazy,
                args = args,
                priority = priority
            )
        )
        return ok
    }
}

// Backward compatibility: typealias aliases
typealias LinearMetaModelF64 = LinearMetaModel<Flt64>

class QuadraticMetaModel<V>(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration()
) : AbstractMetaModel<V>(Quadratic, configuration), AbstractLinearMetaModel<V>, AbstractQuadraticMetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // Math inequality-based constraints storage
    internal val _relationConstraints: MutableList<QuadraticInequalityConstraint> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    val relationConstraints: List<QuadraticInequalityConstraint> by ::_relationConstraints

    internal val _subObjects: MutableList<MetaModel.SubObject<V>> = ArrayList()
    @Suppress("UNCHECKED_CAST")
    override val subObjects: List<MetaModel.SubObject<V>> by ::_subObjects

    // NEW: FlattenData-based sub-objects storage
    internal val _flattenSubObjects: MutableList<QuadraticFlattenSubObject> = ArrayList()
    val flattenSubObjects: List<QuadraticFlattenSubObject> by ::_flattenSubObjects

    /**
     * Add math LinearInequality constraint - converts to QuadraticInequality internally
     */
    override fun addConstraint(
        relation: MathLinearInequality,
        group: MetaConstraintGroup?,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        args: Any?,
        priority: Int?,
        withRangeSet: Boolean?
    ): Try {
        // Convert to math QuadraticInequality using unified extension function
        // 使用统一扩展函数转换为 math QuadraticInequality
        return addConstraint(
            relation = relation.toQuadraticInequality(),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            priority = priority,
            withRangeSet = withRangeSet
        )
    }

    /**
     * Add constraint using math LinearInequality (LinearModel interface)
     */
    override fun addConstraint(
        relation: MathLinearInequality,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        // Convert to math QuadraticInequality using unified extension function
        // 使用统一扩展函数转换为 math QuadraticInequality
        return addConstraint(
            relation = relation.toQuadraticInequality(),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    /**
     * Add constraint using math QuadraticInequality (QuadraticModel interface)
     */
    override fun addConstraint(
        relation: MathQuadraticInequality,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        return addConstraint(
            relation = relation,
            group = null,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = null,
            priority = null,
            withRangeSet = withRangeSet
        )
    }

    /**
     * Add objective using LinearFlattenDataF64 (new API - LinearModel interface)
     * Converts to QuadraticFlattenDataF64 internally.
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenDataF64,
        name: String,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = flattenData.toQuadraticFlattenData(),
            name = name,
            displayName = displayName
        )
    }

    /**
     * Add constraint using math QuadraticInequality (new API)
     */
    override fun addConstraint(
        relation: MathQuadraticInequality,
        group: MetaConstraintGroup?,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        args: Any?,
        priority: Int?,
        withRangeSet: Boolean?
    ): Try {
        _relationConstraints.add(
            QuadraticInequalityConstraint(
                inequality = relation,
                group = group,
                lazy = lazy,
                args = args,
                priority = priority
            )
        )
        return ok
    }

    fun addObject(
        category: ObjectCategory,
        polynomial: UtilsQuadraticPolynomial<Flt64>,
        name: String,
        displayName: String?
    ): Try {
        // Convert to QuadraticFlattenDataF64 for the new API
        val flattenData = QuadraticFlattenDataF64(
            monomials = polynomial.monomials,
            constant = polynomial.constant
        )
        _flattenSubObjects.add(
            QuadraticFlattenSubObject(
                category = category,
                flattenData = flattenData,
                name = name,
                displayName = displayName
            )
        )
        // Also add a linear approximation to subObjects for compatibility
        val linearPoly = UtilsLinearPolynomial(
            monomials = polynomial.monomials.map { UtilsLinearMonomial(it.coefficient, it.symbol1) },
            constant = polynomial.constant
        )
        _subObjects.add(
            MetaModel.SubObject<V>(
                parent = this,
                category = category,
                name = name,
                displayName = displayName,
                polynomial = linearPoly
            )
        )
        return ok
    }

    /**
     * Add objective using QuadraticFlattenDataF64 (new API)
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenDataF64,
        name: String,
        displayName: String?
    ): Try {
        _flattenSubObjects.add(
            QuadraticFlattenSubObject(
                category = category,
                flattenData = flattenData,
                name = name,
                displayName = displayName
            )
        )
        return ok
    }
}

// Backward compatibility: typealias aliases
typealias QuadraticMetaModelF64 = QuadraticMetaModel<Flt64>




