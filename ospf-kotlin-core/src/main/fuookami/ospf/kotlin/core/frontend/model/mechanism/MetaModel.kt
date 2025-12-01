package fuookami.ospf.kotlin.core.frontend.model.mechanism

import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.model.*

sealed interface MetaModel : Model {
    class SubObject<Poly : Polynomial<Poly, M, Cell>, M : Monomial<M, Cell>, Cell : MonomialCell<Cell>>(
        val parent: MetaModel,
        val category: ObjectCategory,
        val polynomial: Poly,
        val name: String = polynomial.name
    ) {
        fun value(zeroIfNone: Boolean = false): Flt64? {
            return value(parent.tokens, zeroIfNone)
        }

        fun value(results: List<Flt64>, zeroIfNone: Boolean = false): Flt64? {
            return value(results, parent.tokens, zeroIfNone)
        }

        fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
            return polynomial.evaluate(tokenTable, zeroIfNone)
        }

        fun value(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
            return polynomial.evaluate(results, tokenTable, zeroIfNone)
        }
    }

    val name: String
    val constraints: List<MetaConstraint<*>>
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
            }
        }
        return ok
    }

    fun remove(symbol: IntermediateSymbol) {
        tokens.remove(symbol)
    }

    fun registerConstraintGroup(group: MetaConstraintGroup)
    fun indicesOfConstraintGroup(group: MetaConstraintGroup): IntRange?

    fun constraintsOfGroup(group: MetaConstraintGroup): List<MetaConstraint<*>> {
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
            constraint.constraint.flush(force)
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
        return export(Path(".").resolve(name), if (unfold) { UInt64.zero } else { UInt64.maximum })
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
        when (val result = when (tokens) {
            is MutableTokenTable -> {
                val temp = tokens.copy() as MutableTokenTable
                when (val result = tokens.symbols.register(temp)) {
                    is Ok -> {
                        Ok(temp)
                    }

                    is Failed -> {
                        Failed(result.error)
                    }
                }
            }

            is ConcurrentMutableTokenTable -> {
                coroutineScope<Ret<AbstractTokenTable>> {
                    val temp = tokens.copy() as ConcurrentMutableTokenTable
                    when (val result = tokens.symbols.register(temp)) {
                        is Ok -> {
                            Ok(temp)
                        }

                        is Failed -> {
                            Failed(result.error)
                        }
                    }
                }
            }
        }) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
                writer.append("$constraint: ${constraint.constraint.toRawString(unfold)}\n")
            }
            writer.append("\n")

            ok
        }
    }
}

interface AbstractLinearMetaModel : MetaModel, LinearModel {
    fun addConstraint(
        constraint: AbstractVariableItem<*, *>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(constraint eq true, group, name, displayName, args, withRangeSet)
    }

    fun addConstraint(
        constraint: LinearMonomial,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(constraint eq true, group, name, displayName, args, withRangeSet)
    }

    fun addConstraint(
        constraint: AbstractLinearPolynomial<*>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(constraint eq true, group, name, displayName, args, withRangeSet)
    }

    fun addConstraint(
        constraint: LinearIntermediateSymbol,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(constraint eq true, group, name, displayName, args, withRangeSet)
    }

    fun addConstraint(
        constraint: LinearInequality,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(sum(variables), group, name, displayName, args)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun partition(
        symbols: Iterable<LinearIntermediateSymbol>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(sum(symbols), group, name, displayName, args)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearMonomials")
    fun partition(
        monomials: Iterable<LinearMonomial>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(sum(monomials), group, name, displayName, args)
    }

    fun partition(
        polynomial: AbstractLinearPolynomial<*>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(polynomial eq true, group, name, displayName, args)
    }
}

interface AbstractQuadraticMetaModel : MetaModel, QuadraticModel {
    fun addConstraint(
        constraint: QuadraticMonomial,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(constraint eq true, group, name, displayName, args, withRangeSet)
    }

    fun addConstraint(
        constraint: AbstractQuadraticPolynomial<*>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(constraint eq true, group, name, displayName, args, withRangeSet)
    }

    fun addConstraint(
        constraint: QuadraticIntermediateSymbol,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(constraint eq true, group, name, displayName, args, withRangeSet)
    }

    fun addConstraint(
        constraint: QuadraticInequality,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticMonomials")
    fun partition(
        monomials: Iterable<QuadraticMonomial>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(qsum(monomials), group, name, displayName, args)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun partition(
        symbols: Iterable<QuadraticIntermediateSymbol>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(qsum(symbols), group, name, displayName, args)
    }

    fun partition(
        polynomial: AbstractQuadraticPolynomial<*>,
        group: MetaConstraintGroup?,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(polynomial eq Flt64.one, group, name, displayName, args)
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
    internal val _constraints: MutableList<MetaConstraint<LinearInequality>> = ArrayList()
    override val constraints: List<MetaConstraint<*>> by ::_constraints
    internal val _subObjects: MutableList<MetaModel.SubObject<LinearPolynomial, LinearMonomial, LinearMonomialCell>> = ArrayList()
    override val subObjects: List<MetaModel.SubObject<*, *, *>> by ::_subObjects

    override fun addConstraint(
        constraint: LinearInequality,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        name?.let { constraint.name = it }
        displayName?.let { constraint.name = it }
        _constraints.add(MetaConstraint(constraint))
        
        if (withRangeSet ?: this.configuration.withRangeSet
            && constraint.lhs.monomials.size == 1
            && !constraint.lhs.monomials.first().pure
            && constraint.rhs.monomials.isEmpty()
        ) {
            val symbol = constraint.lhs.monomials.first().symbol.exprSymbol!!
            val constant = constraint.rhs.constant - constraint.lhs.constant
            when (constraint.sign) {
                Sign.Less, Sign.LessEqual -> {
                    symbol.range.leq(constant)
                }

                Sign.Greater, Sign.GreaterEqual -> {
                    symbol.range.geq(constant)
                }

                Sign.Equal -> {
                    symbol.range.eq(constant)
                }

                Sign.Unequal -> {}
            }
        }

        return ok
    }

    override fun addObject(
        category: ObjectCategory,
        polynomial: AbstractLinearPolynomial<*>,
        name: String?,
        displayName: String?
    ): Try {
        val obj = LinearPolynomial(polynomial)
        name?.let { obj.name = it }
        displayName?.let { obj.displayName = it }
        _subObjects.add(MetaModel.SubObject(this, category, obj))
        return ok
    }

    override fun toString(): String {
        return name
    }

    override fun addConstraint(
        constraint: LinearInequality,
        group: MetaConstraintGroup?,
        name: String?,
        displayName: String?,
        args: Any?,
        withRangeSet: Boolean?
    ): Try {
        name?.let { constraint.name = it }
        displayName?.let { constraint.name = it }
        _constraints.add(MetaConstraint(constraint, group, args))

        if (withRangeSet ?: this.configuration.withRangeSet
            && constraint.lhs.monomials.size == 1
            && !constraint.lhs.monomials.first().pure
            && constraint.rhs.monomials.isEmpty()
        ) {
            val symbol = constraint.lhs.monomials.first().symbol.exprSymbol!!
            val constant = constraint.rhs.constant - constraint.lhs.constant
            when (constraint.sign) {
                Sign.Less, Sign.LessEqual -> {
                    symbol.range.leq(constant)
                }

                Sign.Greater, Sign.GreaterEqual -> {
                    symbol.range.geq(constant)
                }

                Sign.Equal -> {
                    symbol.range.eq(constant)
                }

                Sign.Unequal -> {}
            }
        }

        return ok
    }
}

class QuadraticMetaModel(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration()
) : AbstractMetaModel(Quadratic, configuration), AbstractLinearMetaModel, AbstractQuadraticMetaModel {
    internal val _constraints: MutableList<MetaConstraint<QuadraticInequality>> = ArrayList()
    override val constraints: List<MetaConstraint<*>> by ::_constraints
    internal val _subObjects: MutableList<MetaModel.SubObject<QuadraticPolynomial, QuadraticMonomial, QuadraticMonomialCell>> = ArrayList()
    override val subObjects: List<MetaModel.SubObject<*, *, *>> by ::_subObjects

    override fun addConstraint(
        constraint: LinearInequality,
        group: MetaConstraintGroup?,
        name: String?,
        displayName: String?,
        args: Any?,
        withRangeSet: Boolean?
    ): Try {
        return addConstraint(QuadraticInequality(constraint), group, name, displayName, args, withRangeSet)
    }

    override fun addConstraint(
        constraint: QuadraticInequality,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        name?.let { constraint.name = it }
        displayName?.let { constraint.name = it }
        _constraints.add(MetaConstraint(constraint))

        if (withRangeSet ?: this.configuration.withRangeSet
            && !constraint.lhs.monomials.first().pure
            && constraint.lhs.monomials.first().symbol.symbol2 == null
            && constraint.rhs.monomials.isEmpty()
        ) {
            val symbol = constraint.lhs.monomials.first().symbol.symbol1.v2
                ?: constraint.lhs.monomials.first().symbol.symbol1.v3!!
            val constant = constraint.rhs.constant - constraint.lhs.constant
            when (constraint.sign) {
                Sign.Less, Sign.LessEqual -> {
                    symbol.range.leq(constant)
                }

                Sign.Greater, Sign.GreaterEqual -> {
                    symbol.range.geq(constant)
                }

                Sign.Equal -> {
                    symbol.range.eq(constant)
                }

                Sign.Unequal -> {}
            }
        }

        return ok
    }

    override fun addConstraint(
        constraint: QuadraticInequality,
        group: MetaConstraintGroup?,
        name: String?,
        displayName: String?,
        args: Any?,
        withRangeSet: Boolean?
    ): Try {
        name?.let { constraint.name = it }
        displayName?.let { constraint.name = it }
        _constraints.add(MetaConstraint(constraint, group, args))

        if (withRangeSet ?: this.configuration.withRangeSet
            && !constraint.lhs.monomials.first().pure
            && constraint.lhs.monomials.first().symbol.symbol2 == null
            && constraint.rhs.monomials.isEmpty()
        ) {
            val symbol = constraint.lhs.monomials.first().symbol.symbol1.v2
                ?: constraint.lhs.monomials.first().symbol.symbol1.v3!!
            val constant = constraint.rhs.constant - constraint.lhs.constant
            when (constraint.sign) {
                Sign.Less, Sign.LessEqual -> {
                    symbol.range.leq(constant)
                }

                Sign.Greater, Sign.GreaterEqual -> {
                    symbol.range.geq(constant)
                }

                Sign.Equal -> {
                    symbol.range.eq(constant)
                }

                Sign.Unequal -> {}
            }
        }

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
        _subObjects.add(MetaModel.SubObject(this, category, obj))
        return ok
    }
}
