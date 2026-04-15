@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuantityIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.FunctionSymbol
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.intermediate_model.LinearInequalityConstraint
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticInequalityConstraint
import fuookami.ospf.kotlin.core.intermediate_model.monomial.Monomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.MonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenSubObject
import fuookami.ospf.kotlin.core.model.LinearModel
import fuookami.ospf.kotlin.core.model.Model
import fuookami.ospf.kotlin.core.model.QuadraticModel
import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticPolynomial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

sealed interface MetaModel : Model, AutoCloseable {
    class SubObject<Poly : Polynomial<Poly, M, Cell>, M : Monomial<M, Cell>, Cell : MonomialCell<Cell>>(
        val parent: MetaModel,
        val category: ObjectCategory,
        val polynomial: Poly,
        val name: String = polynomial.name
    ) {
        fun evaluate(zeroIfNone: Boolean = false): Flt64? {
            return evaluate(
                tokenTable = parent.tokens,
                zeroIfNone = zeroIfNone
            )
        }

        fun evaluate(results: List<Flt64>, zeroIfNone: Boolean = false): Flt64? {
            return evaluate(
                results = results,
                tokenTable = parent.tokens,
                zeroIfNone = zeroIfNone
            )
        }

        fun evaluate(tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
            return polynomial.evaluate(
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        }

        fun evaluate(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
            return polynomial.evaluate(
                results = results,
                tokenTable = tokenTable,
                zeroIfNone = zeroIfNone
            )
        }
    }

    val name: String
    val constraints: List<MathConstraint>
    override val objectCategory: ObjectCategory
    val subObjects: List<SubObject<*, *, *>>
    val tokens: AbstractMutableTokenTable

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

    fun add(symbol: IntermediateSymbol): Try {
        return tokens.add(symbol)
    }

    fun add(symbol: QuantityIntermediateSymbol): Try {
        return tokens.add(symbol.value)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addSymbols")
    fun add(symbols: Iterable<IntermediateSymbol>): Try {
        return tokens.add(symbols)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapSymbols")
    fun <K> add(symbols: Map<K, IntermediateSymbol>): Try {
        return tokens.add(symbols.values)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapSymbolLists")
    fun <K> add(symbols: Map<K, Iterable<IntermediateSymbol>>): Try {
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
    fun <K1, K2> add(symbols: MultiMap2<K1, K2, IntermediateSymbol>): Try {
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
    fun <K1, K2> add(symbols: MultiMap2<K1, K2, Iterable<IntermediateSymbol>>): Try {
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
    fun <K1, K2, K3> add(symbols: MultiMap3<K1, K2, K3, IntermediateSymbol>): Try {
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
    fun <K1, K2, K3> add(symbols: MultiMap3<K1, K2, K3, Iterable<IntermediateSymbol>>): Try {
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
    fun <K1, K2, K3, K4> add(symbols: MultiMap4<K1, K2, K3, K4, IntermediateSymbol>): Try {
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
    fun <K1, K2, K3, K4> add(symbols: MultiMap4<K1, K2, K3, K4, Iterable<IntermediateSymbol>>): Try {
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

    fun remove(symbol: IntermediateSymbol) {
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
            objective.polynomial.flush(force)
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
            is MutableTokenTable -> tokens.copy() as MutableTokenTable
            is ConcurrentMutableTokenTable -> tokens.copy() as ConcurrentMutableTokenTable
            else -> throw IllegalStateException("Unknown token table type: ${tokens::class}")
        }

        for (symbol in tokens.symbols) {
            if (symbol is FunctionSymbol) {
                when (val result = symbol.register(temp)) {
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

interface AbstractLinearMetaModel : MetaModel, LinearModel {
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
        constraint: LinearMonomial,
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
        constraint: AbstractLinearPolynomial<*>,
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
        constraint: LinearIntermediateSymbol,
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
            polynomial = sum(variables),
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
        symbols: Iterable<LinearIntermediateSymbol>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = sum(symbols),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearMonomials")
    fun partition(
        monomials: Iterable<LinearMonomial>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = sum(monomials),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun partition(
        polynomial: AbstractLinearPolynomial<*>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(
            relation =polynomial eq true,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }
}

interface AbstractQuadraticMetaModel : MetaModel, QuadraticModel {
    fun addConstraint(
        constraint: QuadraticMonomial,
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

    fun addConstraint(
        constraint: AbstractQuadraticPolynomial<*>,
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

    fun addConstraint(
        constraint: QuadraticIntermediateSymbol,
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
    @JvmName("partitionQuadraticMonomials")
    fun partition(
        monomials: Iterable<QuadraticMonomial>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = qsum(monomials),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun partition(
        symbols: Iterable<QuadraticIntermediateSymbol>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = qsum(symbols),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun partition(
        polynomial: AbstractQuadraticPolynomial<*>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(
            relation =polynomial eq Flt64.one,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }
}

data class MetaModelConfiguration(
    internal val manualTokenAddition: Boolean = true,
    internal val concurrent: Boolean = true,
    internal val dumpBlocking: Boolean = false,
    internal val withRangeSet: Boolean = false,
    internal val checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
)

abstract class AbstractMetaModel(
    val category: Category,
    internal val configuration: MetaModelConfiguration
) : MetaModel {
    override val tokens: AbstractMutableTokenTable = if (configuration.concurrent) {
        if (configuration.manualTokenAddition) {
            ConcurrentManualAddTokenTable(category, configuration.checkTokenExists)
        } else {
            ConcurrentAutoTokenTable(category, configuration.checkTokenExists)
        }
    } else {
        if (configuration.manualTokenAddition) {
            ManualTokenTable(category, configuration.checkTokenExists)
        } else {
            AutoTokenTable(category, configuration.checkTokenExists)
        }
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

class LinearMetaModel(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration()
) : AbstractMetaModel(Linear, configuration), AbstractLinearMetaModel {
    // Math inequality-based constraints storage
    internal val _relationConstraints: MutableList<LinearInequalityConstraint> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    val relationConstraints: List<LinearInequalityConstraint> by ::_relationConstraints

    internal val _subObjects: MutableList<MetaModel.SubObject<LinearPolynomial, LinearMonomial, LinearMonomialCell>> = ArrayList()
    override val subObjects: List<MetaModel.SubObject<*, *, *>> by ::_subObjects

    // NEW: FlattenData-based sub-objects storage
    internal val _flattenSubObjects: MutableList<LinearSubObject> = ArrayList()
    val flattenSubObjects: List<LinearSubObject> by ::_flattenSubObjects

    override fun addObject(
        category: ObjectCategory,
        polynomial: AbstractLinearPolynomial<*>,
        name: String?,
        displayName: String?
    ): Try {
        val obj = LinearPolynomial(polynomial)
        name?.let { obj.name = it }
        displayName?.let { obj.displayName = it }
        _subObjects.add(
            MetaModel.SubObject(
                parent = this,
                category = category,
                polynomial = obj
            )
        )
        return ok
    }

    /**
     * Add objective using LinearFlattenData (new API)
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenData,
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

class QuadraticMetaModel(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration()
) : AbstractMetaModel(Quadratic, configuration), AbstractLinearMetaModel, AbstractQuadraticMetaModel {
    // Math inequality-based constraints storage
    internal val _relationConstraints: MutableList<QuadraticInequalityConstraint> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    val relationConstraints: List<QuadraticInequalityConstraint> by ::_relationConstraints

    internal val _subObjects: MutableList<MetaModel.SubObject<QuadraticPolynomial, QuadraticMonomial, QuadraticMonomialCell>> = ArrayList()
    override val subObjects: List<MetaModel.SubObject<*, *, *>> by ::_subObjects

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
        // Convert to math QuadraticInequality
        val quadraticInequality = MathQuadraticInequality(
            relation.lhs.toQuadraticPolynomial(),
            relation.rhs.toQuadraticPolynomial(),
            relation.comparison
        )
        return addConstraint(
            relation = quadraticInequality,
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
        // Convert to math QuadraticInequality
        val quadraticInequality = MathQuadraticInequality(
            relation.lhs.toQuadraticPolynomial(),
            relation.rhs.toQuadraticPolynomial(),
            relation.comparison
        )
        return addConstraint(
            relation = quadraticInequality,
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
     * Add objective using LinearFlattenData (new API - LinearModel interface)
     * Converts to QuadraticFlattenData internally.
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenData,
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

    override fun addObject(
        category: ObjectCategory,
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String?,
        displayName: String?
    ): Try {
        val obj = QuadraticPolynomial(polynomial)
        name?.let { obj.name = it }
        displayName?.let { obj.displayName = it }
        _subObjects.add(
            MetaModel.SubObject(
                parent = this,
                category = category,
                polynomial = obj
            )
        )
        return ok
    }

    /**
     * Add objective using QuadraticFlattenData (new API)
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenData,
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




