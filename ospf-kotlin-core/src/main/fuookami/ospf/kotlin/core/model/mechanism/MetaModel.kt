package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolFlt64
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbolFlt64
import fuookami.ospf.kotlin.core.intermediate_symbol.QuantityIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTableFlt64
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTableFlt64
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTableFlt64
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTableFlt64
import fuookami.ospf.kotlin.core.token.ConcurrentManualAddTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentAutoTokenTable
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
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
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

/**
 * Factory function to create the appropriate [AbstractMutableTokenTableFlt64]
 * based on configuration. Used by [AbstractMetaModel] to construct the token
 * table before passing it to the [BasicModel] superclass constructor.
 */
private fun createTokenTable(
    category: Category,
    concurrent: Boolean,
    manualTokenAddition: Boolean,
    checkTokenExists: Boolean
): AbstractMutableTokenTableFlt64 {
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


private fun LinearPolynomial<Flt64>.toRawString(unfold: UInt64 = UInt64.zero): String {
    return if (monomials.isEmpty()) {
        "$constant"
    } else if (constant neq Flt64.zero) {
        "${monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }} + $constant"
    } else {
        monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }
    }
}

private fun QuadraticPolynomial<Flt64>.toRawString(unfold: UInt64 = UInt64.zero): String {
    return if (monomials.isEmpty()) {
        "$constant"
    } else if (constant neq Flt64.zero) {
        "${monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }} + $constant"
    } else {
        monomials.filter { it.coefficient neq Flt64.zero }.joinToString(" + ") { it.toString() }
    }
}

// ========== V → Flt64 conversion via IntoValue converter ==========

private fun <V> LinearPolynomial<V>.toFlt64Poly(converter: IntoValue<V>): LinearPolynomial<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) },
        converter.fromValue(constant)
    )
}

private fun <V> QuadraticPolynomial<V>.toFlt64QuadraticPoly(converter: IntoValue<V>): QuadraticPolynomial<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
        converter.fromValue(constant)
    )
}

sealed interface MetaModel<V> : Model<V>, AutoCloseable where V : RealNumber<V>, V : NumberField<V> {
    val converter: IntoValue<V>
    class SubObject<V>(
        val parent: MetaModel<V>,
        val category: ObjectCategory,
        val name: String,
        val displayName: String? = null,
        val polynomial: LinearPolynomial<V>
    ) where V : RealNumber<V>, V : NumberField<V> {
        /** Primary V-typed evaluation. */
        fun evaluate(zeroIfNone: Boolean = false): V? {
            return evaluate(
                tokenTable = parent.tokens,
                zeroIfNone = zeroIfNone
            )
        }

        fun evaluate(tokenTable: AbstractTokenTable<V>, zeroIfNone: Boolean = false): V? {
            val vZero = polynomial.constant - polynomial.constant
            var result: V? = null
            for (m in polynomial.monomials) {
                val variable = m.symbol as? AbstractVariableItem<*, *> ?: return if (zeroIfNone) vZero else null
                val token = tokenTable.find(variable) ?: return if (zeroIfNone) vZero else null
                val tokenValue = token.result ?: return if (zeroIfNone) vZero else null
                val term = m.coefficient * tokenValue
                result = if (result == null) term else result + term
            }
            return result ?: polynomial.constant
        }

        fun evaluate(results: List<V>, zeroIfNone: Boolean = false): V? {
            return evaluate(
                results = results,
                tokenTable = parent.tokens,
                zeroIfNone = zeroIfNone
            )
        }

        fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, zeroIfNone: Boolean = false): V? {
            val vZero = polynomial.constant - polynomial.constant
            var result: V? = null
            for (m in polynomial.monomials) {
                val variable = m.symbol as? AbstractVariableItem<*, *> ?: return if (zeroIfNone) vZero else null
                val idx = tokenTable.indexOf(variable) ?: return if (zeroIfNone) vZero else null
                val tokenValue = results[idx]
                val term = m.coefficient * tokenValue
                result = if (result == null) term else result + term
            }
            return result ?: polynomial.constant
        }

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

    override fun setSolution(solution: List<V>) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>) {
        tokens.setSolution(solution)
    }

    fun setSolverSolution(solution: Solution<Flt64>) {
        tokens.setSolverSolution(solution)
    }

    fun setSolverSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        tokens.setSolverSolution(solution)
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
            is MutableTokenTable<*> -> tokens.copy() as MutableTokenTableFlt64
            is ConcurrentMutableTokenTable<*> -> tokens.copy() as ConcurrentMutableTokenTableFlt64
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
                writer.append("${obj.category} ${obj.name}: ${obj.polynomial.toFlt64Poly(converter).toRawString(unfold)} \n")
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
typealias MetaModelFlt64 = MetaModel<Flt64>

interface AbstractLinearMetaModel<V> : MetaModel<V>, LinearModel<V> where V : RealNumber<V>, V : NumberField<V> {
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
        constraint: LinearPolynomial<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        val fltPoly = constraint.toFlt64Poly(converter)
        val onePoly = LinearPolynomial<Flt64>(emptyList(), Flt64.one)
        return addConstraint(
            relation = Flt64LinearInequality(fltPoly, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: LinearIntermediateSymbol<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            relation = constraint eq true,
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
        relation: Flt64LinearInequality,
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
            polynomial = LinearPolynomial(
                monomials = variables.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
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
        symbols: Iterable<LinearIntermediateSymbol<V>>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = LinearPolynomial(
                monomials = symbols.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
            ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun partition(
        polynomial: LinearPolynomial<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        val fltPoly = polynomial.toFlt64Poly(converter)
        val onePoly = LinearPolynomial<Flt64>(emptyList(), Flt64.one)
        return addConstraint(
            relation = Flt64LinearInequality(fltPoly, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }
}

// Backward compatibility: typealias aliases
typealias AbstractLinearMetaModelFlt64 = AbstractLinearMetaModel<Flt64>

interface AbstractQuadraticMetaModel<V> : MetaModel<V>, QuadraticModel<V> where V : RealNumber<V>, V : NumberField<V> {
    fun addConstraint(
        constraint: QuadraticPolynomial<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        val fltPoly = constraint.toFlt64QuadraticPoly(converter)
        val onePoly = QuadraticPolynomial<Flt64>(emptyList(), Flt64.one)
        return addConstraint(
            relation = QuadraticInequality(fltPoly, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: QuadraticIntermediateSymbol<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        val fltPoly = constraint.toQuadraticPolynomial().toFlt64QuadraticPoly(converter)
        val onePoly = QuadraticPolynomial<Flt64>(emptyList(), Flt64.one)
        return addConstraint(
            relation = QuadraticInequality(fltPoly, onePoly, Comparison.EQ),
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
        relation: QuadraticInequality,
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
        symbols: Iterable<QuadraticIntermediateSymbol<V>>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = QuadraticPolynomial(
                monomials = symbols.map { it.toQuadraticPolynomial() }.flatMap { it.monomials }.toList(),
                constant = converter.zero
            ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun partition(
        polynomial: QuadraticPolynomial<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        val fltPoly = polynomial.toFlt64QuadraticPoly(converter)
        val onePoly = QuadraticPolynomial<Flt64>(emptyList(), Flt64.one)
        return addConstraint(
            relation = QuadraticInequality(fltPoly, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }
}

// Backward compatibility: typealias aliases
typealias AbstractQuadraticMetaModelFlt64 = AbstractQuadraticMetaModel<Flt64>

data class MetaModelConfiguration(
    internal val manualTokenAddition: Boolean = true,
    internal val concurrent: Boolean = true,
    internal val dumpBlocking: Boolean = false,
    internal val withRangeSet: Boolean = false,
    internal val checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
)

@Suppress("UNCHECKED_CAST")
abstract class AbstractMetaModel<V>(
    val category: Category,
    internal val configuration: MetaModelConfiguration,
    // Default: safe when V=Flt64; non-Flt64 callers must supply an explicit converter.
    override val converter: IntoValue<V> = IntoValue.Flt64 as IntoValue<V>
) : BasicModel<V>(
    name = "",
    tokens = createTokenTable(category, configuration.concurrent, configuration.manualTokenAddition, configuration.checkTokenExists) as AbstractMutableTokenTable<V>
), MetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // Safe cast: internal token storage is always Flt64-based.
    // This reference is used for adapter-boundary operations (e.g., LinearSubObject.invoke).
    @Suppress("UNCHECKED_CAST")
    internal val flt64Tokens: AbstractMutableTokenTableFlt64 get() = tokens as AbstractMutableTokenTableFlt64
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
typealias AbstractMetaModelFlt64 = AbstractMetaModel<Flt64>

class LinearMetaModel<V>(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration(),
    @Suppress("UNCHECKED_CAST")
    converter: IntoValue<V> = IntoValue.Flt64 as IntoValue<V>
) : AbstractMetaModel<V>(Linear, configuration, converter), AbstractLinearMetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // Math inequality-based constraints storage
    internal val _relationConstraints: MutableList<LinearInequalityConstraint<V>> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    val relationConstraints: List<LinearInequalityConstraint<V>> by ::_relationConstraints

    internal val _subObjects: MutableList<MetaModel.SubObject<V>> = ArrayList()
    @Suppress("UNCHECKED_CAST")
    override val subObjects: List<MetaModel.SubObject<V>> by ::_subObjects

    // NEW: FlattenData-based sub-objects storage
    internal val _flattenSubObjects: MutableList<LinearSubObjectFlt64> = ArrayList()
    val flattenSubObjects: List<LinearSubObjectFlt64> by ::_flattenSubObjects

    fun addObject(
        category: ObjectCategory,
        polynomial: LinearPolynomial<V>,
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
     * Add objective using LinearFlattenDataFlt64 (new API)
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenDataFlt64,
        name: String,
        displayName: String?
    ): Try {
        val subObject = LinearSubObject.invoke(
            category = category,
            flattenData = flattenData,
            tokens = flt64Tokens,
            name = name
        )
        _flattenSubObjects.add(subObject)
        return ok
    }

    /**
     * Add constraint using math LinearInequality (LinearModel interface)
     */
    override fun addConstraint(
        relation: Flt64LinearInequality,
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
        relation: Flt64LinearInequality,
        group: MetaConstraintGroup?,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        args: Any?,
        priority: Int?,
        withRangeSet: Boolean?
    ): Try {
        // Adapter boundary: Flt64LinearInequality → LinearInequality<V> safe only when V=Flt64.
        // Non-Flt64 callers should use the V-typed addConstraint overload.
        @Suppress("UNCHECKED_CAST")
        _relationConstraints.add(
            LinearInequalityConstraint<V>(
                inequality = relation as LinearInequality<V>,
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
typealias LinearMetaModelFlt64 = LinearMetaModel<Flt64>

class QuadraticMetaModel<V>(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration(),
    @Suppress("UNCHECKED_CAST")
    converter: IntoValue<V> = IntoValue.Flt64 as IntoValue<V>
) : AbstractMetaModel<V>(Quadratic, configuration, converter), AbstractLinearMetaModel<V>, AbstractQuadraticMetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // Math inequality-based constraints storage
    internal val _relationConstraints: MutableList<QuadraticInequalityConstraint<V>> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    val relationConstraints: List<QuadraticInequalityConstraint<V>> by ::_relationConstraints

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
        relation: Flt64LinearInequality,
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
        relation: Flt64LinearInequality,
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
        relation: QuadraticInequality,
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
     * Add objective using LinearFlattenDataFlt64 (new API - LinearModel interface)
     * Converts to QuadraticFlattenDataFlt64 internally.
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenDataFlt64,
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
        relation: QuadraticInequality,
        group: MetaConstraintGroup?,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        args: Any?,
        priority: Int?,
        withRangeSet: Boolean?
    ): Try {
        // Adapter boundary: QuadraticInequality → QuadraticInequalityOf<V> safe only when V=Flt64.
        @Suppress("UNCHECKED_CAST")
        _relationConstraints.add(
            QuadraticInequalityConstraint<V>(
                inequality = relation as QuadraticInequalityOf<V>,
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
        polynomial: QuadraticPolynomial<V>,
        name: String,
        displayName: String?
    ): Try {
        val flt64Poly = polynomial.toFlt64QuadraticPoly(converter)
        // Convert to QuadraticFlattenDataFlt64 for the new API
        val flattenData = QuadraticFlattenDataFlt64(
            monomials = flt64Poly.monomials,
            constant = flt64Poly.constant
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
        val linearPoly = LinearPolynomial(
            monomials = polynomial.monomials.map { LinearMonomial(it.coefficient, it.symbol1) },
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
     * Add objective using QuadraticFlattenDataFlt64 (new API)
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenDataFlt64,
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
typealias QuadraticMetaModelFlt64 = QuadraticMetaModel<Flt64>

