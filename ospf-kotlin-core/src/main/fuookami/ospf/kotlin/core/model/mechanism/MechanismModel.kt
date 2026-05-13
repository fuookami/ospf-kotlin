package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.TokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentTokenTable
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatus
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbolBase
import fuookami.ospf.kotlin.core.intermediate_symbol.function.QuadraticMathFunctionSymbolBase
import fuookami.ospf.kotlin.core.intermediate_symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.normalize
import fuookami.ospf.kotlin.math.symbol.inequality.le
import fuookami.ospf.kotlin.math.symbol.inequality.ge
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.toQuadraticInequality
import fuookami.ospf.kotlin.core.token.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.token.register
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.math.operator.pow
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.Quadratic
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.token.TokenList
import fuookami.ospf.kotlin.core.solver.value.IntoValue

// Solver-boundary bridge: registerConstraints on star-projected function symbols
// Delegates to SolverBoundaryCasts (single UNCHECKED_CAST location)
private fun MathFunctionSymbolBase<*>.registerConstraintsUnchecked(model: AbstractLinearMechanismModel<*>): Try {
    return SolverBoundaryCasts.registerConstraintsLinearStar(this, model)
}

private fun QuadraticMathFunctionSymbolBase<*>.registerConstraintsUnchecked(model: AbstractQuadraticMechanismModel<*>): Try {
    return SolverBoundaryCasts.registerConstraintsQuadraticStar(this, model)
}


private fun <V> copyTokenWithFlt64Converter(token: Token<V>): Token<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val copied = Token<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        variable = token.variable,
        solverIndex = token.solverIndex,
        refreshCallbacks = mutableMapOf<AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, (Boolean) -> Unit>(),
        converter = IntoValue.Identity
    )
    copied.__result = token.resultFlt64
    return copied
}

private fun <V> createFlt64TokenTable(tokens: AbstractTokenTable<V>): AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val copiedTokens = tokens.tokens.map { copyTokenWithFlt64Converter(it) }
    val copiedTokenMap = copiedTokens.associateBy { it.key }
    val copiedTokenList = TokenList(copiedTokenMap)
    return TokenTable(
        category = tokens.category,
        tokenList = copiedTokenList,
        symbols = tokens.symbols.toList()
    )
}

@Suppress("UNCHECKED_CAST")
private fun <V> copyMutableTokenTableAsFlt64(tokens: MutableTokenTable<V>): MutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return tokens.copy() as MutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
}

@Suppress("UNCHECKED_CAST")
private fun <V> copyConcurrentMutableTokenTableAsFlt64(tokens: ConcurrentMutableTokenTable<V>): ConcurrentMutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return tokens.copy() as ConcurrentMutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
}

private fun <V> convertLinearSubObjectToFlt64(
    subObject: LinearSubObject<V>,
    tokens: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): LinearSubObject<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    val flattenData = LinearFlattenData(
        monomials = subObject.cells.map { cell ->
            LinearMonomial(
                coefficient = cell.coefficient.toFlt64(),
                symbol = cell.token.variable
            )
        },
        constant = subObject.constant.toFlt64()
    )
    return LinearSubObject.invoke(
        category = subObject.category,
        flattenData = flattenData,
        tokens = tokens,
        name = subObject.name
    )
}

private fun <V> convertQuadraticSubObjectToFlt64(
    subObject: QuadraticSubObject<V>,
    tokens: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): QuadraticSubObject<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    val flattenData = QuadraticFlattenData(
        monomials = subObject.cells.map { cell ->
            if (cell.token2 == null) {
                QuadraticMonomial.linear(
                    coefficient = cell.coefficient.toFlt64(),
                    symbol = cell.token1.variable
                )
            } else {
                QuadraticMonomial.quadratic(
                    coefficient = cell.coefficient.toFlt64(),
                    symbol1 = cell.token1.variable,
                    symbol2 = cell.token2!!.variable
                )
            }
        },
        constant = subObject.constant.toFlt64()
    )
    return QuadraticSubObject.invoke(
        category = subObject.category,
        flattenData = flattenData,
        tokens = tokens,
        name = subObject.name
    )
}

private fun <V> convertLinearConstraintToFlt64(
    constraint: LinearConstraintImpl<V>,
    tokens: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): LinearConstraintImpl<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    val relation = LinearRelationImpl(
        flattenData = LinearFlattenData(
            monomials = constraint.lhs.map { cell ->
                LinearMonomial(
                    coefficient = cell.coefficient.toFlt64(),
                    symbol = cell.token.variable
                )
            },
            constant = -constraint.rhs.toFlt64()
        ),
        sign = constraint.sign.toComparison(),
        name = constraint.name
    )
    return LinearConstraintImpl(
        relation = relation,
        tokens = tokens,
        converter = IntoValue.Identity,
        lazy = constraint.lazy,
        name = constraint.name,
        origin = constraint.origin,
        from = constraint.from
    )
}

private fun <V> convertQuadraticConstraintToFlt64(
    constraint: QuadraticConstraintImpl<V>,
    tokens: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): QuadraticConstraintImpl<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    val relation = QuadraticRelationImpl(
        flattenData = QuadraticFlattenData(
            monomials = constraint.lhs.map { cell ->
                if (cell.token2 == null) {
                    QuadraticMonomial.linear(
                        coefficient = cell.coefficient.toFlt64(),
                        symbol = cell.token1.variable
                    )
                } else {
                    QuadraticMonomial.quadratic(
                        coefficient = cell.coefficient.toFlt64(),
                        symbol1 = cell.token1.variable,
                        symbol2 = cell.token2!!.variable
                    )
                }
            },
            constant = -constraint.rhs.toFlt64()
        ),
        sign = constraint.sign.toComparison(),
        name = constraint.name
    )
    return QuadraticConstraintImpl(
        relation = relation,
        tokens = tokens,
        converter = IntoValue.Identity,
        lazy = constraint.lazy,
        name = constraint.name,
        origin = constraint.origin,
        from = constraint.from
    )
}

private fun <V> convertLinearMechanismModelToFlt64(model: LinearMechanismModel<V>): LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val flt64Tokens = createFlt64TokenTable(model.tokens)
    val flt64Parent = LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        name = model.parent.name,
        objectCategory = model.parent.objectCategory,
        configuration = model.parent.configuration,
        converter = IntoValue.Identity
    )
    val flt64Constraints = model.linearConstraints.map { convertLinearConstraintToFlt64(it, flt64Tokens) }
    val flt64SubObjects = model.objectFunction.subObjects.map { convertLinearSubObjectToFlt64(it, flt64Tokens) }
    return LinearMechanismModel(
        parent = flt64Parent,
        name = model.name,
        constraints = flt64Constraints,
        objectFunction = SingleObject(model.objectFunction.category, flt64SubObjects),
        tokens = flt64Tokens
    )
}

private fun <V> convertQuadraticMechanismModelToFlt64(model: QuadraticMechanismModel<V>): QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    val flt64Tokens = createFlt64TokenTable(model.tokens)
    val flt64Parent = QuadraticMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        name = model.parent.name,
        objectCategory = model.parent.objectCategory,
        configuration = model.parent.configuration,
        converter = IntoValue.Identity
    )
    val flt64Constraints = model.quadraticConstraints.map { convertQuadraticConstraintToFlt64(it, flt64Tokens) }
    val flt64SubObjects = model.objectFunction.subObjects.map { convertQuadraticSubObjectToFlt64(it, flt64Tokens) }
    return QuadraticMechanismModel(
        parent = flt64Parent,
        name = model.name,
        constraints = flt64Constraints,
        objectFunction = SingleObject(model.objectFunction.category, flt64SubObjects),
        tokens = flt64Tokens
    )
}

@Suppress("UNCHECKED_CAST")
private fun <V, T> tokenTableAs(table: AbstractTokenTable<T>): AbstractTokenTable<V>
        where V : RealNumber<V>, V : NumberField<V>, T : RealNumber<T>, T : NumberField<T> {
    return table as AbstractTokenTable<V>
}

private fun <V> toSolverFixedValues(
    fixedVariables: Map<AbstractVariableItem<*, *>, V>?,
    toFlt64: (V) -> Flt64
): Map<Symbol, Flt64>? {
    return fixedVariables
        ?.mapValues { (_, value) -> toFlt64(value) }
        ?.mapKeys { (variable, _) -> variable as Symbol }
}

private fun <V> toFlt64FixedVariables(
    fixedVariables: Map<AbstractVariableItem<*, *>, V>,
    toFlt64: (V) -> Flt64
): Map<AbstractVariableItem<*, *>, Flt64> {
    return fixedVariables.mapValues { (_, value) -> toFlt64(value) }
}

sealed interface MechanismModel<V> : AutoCloseable where V : RealNumber<V>, V : NumberField<V> {
    val name: String
    val constraints: List<Constraint<V, *>>
    val objectFunction: Object
    val tokens: AbstractTokenTable<V>

    override fun close() {
        tokens.close()
    }
}

/**
 * Convert a generic [MechanismModel]<V> to a Flt64-specific [MechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>].
 *
 * Validates that V is compatible with Flt64 by verifying the model is an instance
 * of a concrete MechanismModel subclass. Since all internal numerical data is
 * already Flt64, the conversion is safe when the model is a concrete
 * [LinearMechanismModel] or [QuadraticMechanismModel].
 * Returns [Failed] for unexpected model types.
 *
 * This function is intended to replace inlined V-to-Flt64 conversion logic in
 * invoke() methods.
 */
internal fun <V> convertMechanismModelToFlt64(model: MechanismModel<V>): Ret<MechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>> where V : RealNumber<V>, V : NumberField<V> {
    return when (model) {
        is LinearMechanismModel<*> -> {
            @Suppress("UNCHECKED_CAST")
            Ok(convertLinearMechanismModelToFlt64(model as LinearMechanismModel<V>))
        }

        is QuadraticMechanismModel<*> -> {
            @Suppress("UNCHECKED_CAST")
            Ok(convertQuadraticMechanismModelToFlt64(model as QuadraticMechanismModel<V>))
        }

        else -> {
            Failed(Err(ErrorCode.IllegalArgument, "Cannot convert MechanismModel<V> to Flt64: unexpected model type ${model::class.simpleName}"))
        }
    }
}

// Backward compatibility: typealias aliases

interface AbstractLinearMechanismModel<V> : MechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
    /**
     * Add constraint using math LinearInequality
     */
    fun addConstraint(
        relation: LinearInequality<V>,
        name: String? = null,
        from: Pair<IntermediateSymbol<out V>, Boolean>? = null,
    ): Try

    fun addConstraint(
        relation: LinearInequality<V>,
        name: String? = null,
        from: IntermediateSymbol<out V>?,
    ): Try {
        return addConstraint(
            relation = relation,
            name = name,
            from = from?.let { it to false }
        )
    }
}

interface AbstractQuadraticMechanismModel<V> : AbstractLinearMechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
    /**
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        name: String? = null,
        from: Pair<IntermediateSymbol<out V>, Boolean>? = null
    ): Try

    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        name: String? = null,
        from: IntermediateSymbol<out V>?
    ): Try {
        return addConstraint(
            relation = relation,
            name = name,
            from = from?.let { it to false }
        )
    }
}

// Backward compatibility: typealias aliases

interface SingleObjectMechanismModel<V> : MechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
    override val objectFunction: SingleObject<SubObject<V>>
}


private data class OrderedVariablePair(
    val first: AbstractVariableItem<*, *>,
    val second: AbstractVariableItem<*, *>
) {
    companion object {
        fun of(
            lhs: AbstractVariableItem<*, *>,
            rhs: AbstractVariableItem<*, *>
        ): OrderedVariablePair {
            return if (
                lhs.key.identifier < rhs.key.identifier ||
                (lhs.key.identifier == rhs.key.identifier && lhs.key.index <= rhs.key.index)
            ) {
                OrderedVariablePair(lhs, rhs)
            } else {
                OrderedVariablePair(rhs, lhs)
            }
        }
    }
}

private fun validateDualById(
    constraints: List<Constraint<*, *>>,
    dualById: Map<String, *>,
    log: org.apache.logging.log4j.kotlin.KotlinLogger
) {
    // Detect duplicate constraint names �?multiple constraints sharing the same name
    // would silently reuse the same dual value from the by-id map.
    val nameCounts = HashMap<String, Int>()
    for (c in constraints) {
        nameCounts[c.name] = (nameCounts[c.name] ?: 0) + 1
    }
    for ((name, count) in nameCounts) {
        if (count > 1) {
            log.warn { "Duplicate constraint name '$name' appears $count times in model; by_id lookup will use the same dual value for all" }
        }
    }
    // Detect names in dualById that don't match any constraint �?likely a caller error.
    val constraintNames = nameCounts.keys
    for (name in dualById.keys) {
        if (name !in constraintNames) {
            log.warn { "dualSolutionById contains name '$name' which does not match any constraint in the model; it will be ignored" }
        }
    }
}

private suspend fun <T, R> dumpItemsAsync(
    items: List<T>,
    scope: CoroutineScope,
    memoryCheckInSingleTask: Boolean,
    onProgress: ((UInt64) -> Unit)? = null,
    transform: (T) -> R
): List<R> {
    val factor = Flt64(items.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
    val deferredItems = if (factor >= 1) {
        val completedLock = Any()
        var completed = UInt64.zero
        val segment = pow(UInt64.ten, factor).toInt()
        (0..(items.size / segment)).map { i ->
            scope.async(Dispatchers.Default) {
                val result = items
                    .subList(
                        (i * segment),
                        minOf(items.size, (i + 1) * segment)
                    ).map(transform)
                if (memoryUseOver()) {
                    System.gc()
                }
                if (onProgress != null) {
                    synchronized(completedLock) {
                        completed += result.usize
                        onProgress(completed)
                    }
                }
                result
            }
        }
    } else {
        items.map { item ->
            scope.async(Dispatchers.Default) {
                val result = listOf(transform(item))
                if (memoryCheckInSingleTask && memoryUseOver()) {
                    System.gc()
                }
                result
            }
        }
    }
    return deferredItems.flatMap { it.await() }
}

private suspend fun <V, RC, SO, C, S> dumpMechanismPartsAsync(
    metaModel: MetaModel<V>,
    relationConstraints: List<RC>,
    subObjects: List<SO>,
    scope: CoroutineScope,
    callBack: MechanismModelDumpingStatusCallBack?,
    memoryCheckInSingleTask: Boolean,
    createConstraint: (RC) -> C,
    createSubObject: (SO) -> S
): Pair<List<C>, List<S>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = dumpItemsAsync(
        items = relationConstraints,
        scope = scope,
        memoryCheckInSingleTask = memoryCheckInSingleTask,
        onProgress = callBack?.let { callback ->
            { ready ->
                callback(
                    MechanismModelDumpingStatus.dumpingConstrains(
                        ready = ready,
                        model = metaModel
                    )
                )
            }
        },
        transform = createConstraint
    )
    val dumpedSubObjects = dumpItemsAsync(
        items = subObjects,
        scope = scope,
        memoryCheckInSingleTask = memoryCheckInSingleTask,
        transform = createSubObject
    )

    if (callBack != null) {
        callBack(
            MechanismModelDumpingStatus.dumpingConstrains(
                ready = metaModel.constraints.usize,
                model = metaModel
            )
        )
    }

    return constraints to dumpedSubObjects
}

private fun <V> buildLinearObjectiveSubObjects(
    metaModel: LinearMetaModel<V>,
    tokens: AbstractTokenTable<V>
): List<LinearSubObject<V>> where V : RealNumber<V>, V : NumberField<V> {
    if (metaModel.flattenSubObjects.isNotEmpty()) {
        return metaModel.flattenSubObjects.map { source ->
            LinearSubObject(
                category = source.category,
                cells = ArrayList(source.cells),
                _constant = source.constant,
                name = source.name,
            )
        }
    }
    return metaModel._subObjects.map {
        LinearSubObject(
            category = it.category,
            flattenData = LinearFlattenData(
                it.polynomial.monomials.map { m -> LinearMonomial(m.coefficient, m.symbol) },
                it.polynomial.constant
            ),
            tokens = tokens,
            name = it.name,
            converter = metaModel.converter
        )
    }
}

private fun <V> buildQuadraticObjectiveSubObjects(
    metaModel: QuadraticMetaModel<V>,
    tokens: AbstractTokenTable<V>
): List<QuadraticSubObject<V>> where V : RealNumber<V>, V : NumberField<V> {
    if (metaModel.flattenSubObjects.isNotEmpty()) {
        return metaModel.flattenSubObjects.map { source ->
            QuadraticSubObject(
                category = source.category,
                cells = ArrayList(
                    source.flattenData.monomials.mapNotNull { monomial ->
                        val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
                        val token1 = tokens.find(variable1) ?: return@mapNotNull null
                        val token2 = if (monomial.symbol2 != null) {
                            tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: return@mapNotNull null
                        } else {
                            null
                        }
                        if (monomial.coefficient eq metaModel.converter.zero) {
                            null
                        } else {
                            fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl(
                                tokenTable = tokens,
                                _coefficientFlt64 = metaModel.converter.fromValue(monomial.coefficient),
                                token1 = token1,
                                token2 = token2,
                                converter = metaModel.converter
                            )
                        }
                    }
                ),
                _constant = source.flattenData.constant,
                name = source.name
            )
        }
    }
    return metaModel._subObjects.map {
        QuadraticSubObject(
            category = it.category,
            flattenData = LinearFlattenData(
                it.polynomial.monomials.map { m -> LinearMonomial(m.coefficient, m.symbol) },
                it.polynomial.constant
            ).toQuadraticFlattenData(),
            tokens = tokens,
            name = it.name,
            converter = metaModel.converter
        )
    }
}

class LinearMechanismModel<V>(
    internal val parent: LinearMetaModel<V>,
    override var name: String,
    constraints: List<LinearConstraintImpl<V>>,
    override val objectFunction: SingleObject<LinearSubObject<V>>,
    override val tokens: AbstractTokenTable<V>
) : BasicMechanismModel<V>(name, tokens), AbstractLinearMechanismModel<V>, SingleObjectMechanismModel<V>
        where V : RealNumber<V>, V : NumberField<V> {
    private val logger = logger()

    /**
     * Constraints storage. Inherits query helpers (numVariables) from BasicMechanismModel.
     */
    private val _constraints: MutableList<LinearConstraintImpl<V>> = constraints.toMutableList()
    internal val concurrent by parent.configuration::concurrent
    override val constraints: List<Constraint<V, *>> get() = _constraints
    internal val linearConstraints: List<LinearConstraintImpl<V>> get() = _constraints

    companion object {
        private val logger = logger()

        /**
         * V-typed factory: create LinearMechanismModel<V> from LinearMetaModel<V>.
         * Uses the V-typed SubObject companion overload with IntoValue<V> converter.
         */
        suspend operator fun <V> invoke(
            metaModel: LinearMetaModel<V>,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, V>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<LinearMechanismModel<V>> where V : RealNumber<V>, V : NumberField<V> {
            logger.info { "Creating LinearMechanismModel<V> for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(
                tokens = metaModel.tokens,
                fixedVariables = fixedVariables,
                toFlt64 = metaModel.converter::fromValue,
                callBack = registrationStatusCallBack
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (Runtime.getRuntime().availableProcessors() > 2 && concurrent ?: metaModel.configuration.concurrent) {
                if (blocking ?: metaModel.configuration.dumpBlocking) {
                    runBlocking {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }
                } else {
                    coroutineScope {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }
                }
            } else {
                LinearMechanismModel<V>(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = metaModel._relationConstraints.map {
                        LinearConstraintImpl(
                            relation = LinearRelationImpl(it.flattenData, it.sign),
                            tokens = tokens,
                            converter = metaModel.converter,
                            lazy = it.lazy,
                            name = it.name,
                            origin = it
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, buildLinearObjectiveSubObjects(metaModel, tokens)),
                    tokens = tokens
                )
            }
            System.gc()

            logger.trace { "Registering function symbol constraints for $metaModel" }
            for ((i, symbol) in tokens.symbols.withIndex()) {
                val result = when (symbol) {
                    is MathFunctionSymbolBase<*> -> symbol.registerConstraintsUnchecked(model)
                    else -> ok
                }
                when (result) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                if (dumpingStatusCallBack != null && i % 100 == 0) {
                    dumpingStatusCallBack(
                        MechanismModelDumpingStatus.dumpingSymbols(
                            ready = UInt64(i),
                            model = metaModel
                        )
                    )
                }
            }
            if (dumpingStatusCallBack != null) {
                dumpingStatusCallBack(
                    MechanismModelDumpingStatus.dumpingSymbols(
                        ready = tokens.symbols.usize,
                        model = metaModel
                    )
                )
            }
            logger.trace { "Function symbol constraints registered for $metaModel" }

            logger.info { "LinearMechanismModel<V> created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        private suspend fun <V> dumpAsync(
            metaModel: LinearMetaModel<V>,
            tokens: AbstractTokenTable<V>,
            scope: CoroutineScope,
            callBack: MechanismModelDumpingStatusCallBack? = null
        ): LinearMechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
            val constraints = dumpItemsAsync(
                items = metaModel._relationConstraints,
                scope = scope,
                memoryCheckInSingleTask = true,
                onProgress = callBack?.let { callback ->
                    { ready ->
                        callback(
                            MechanismModelDumpingStatus.dumpingConstrains(
                                ready = ready,
                                model = metaModel
                            )
                        )
                    }
                },
                transform = {
                    LinearConstraintImpl(
                        relation = LinearRelationImpl(it.flattenData, it.sign),
                        tokens = tokens,
                        converter = metaModel.converter,
                        lazy = it.lazy,
                        name = it.name,
                        origin = it
                    )
                }
            )
            val subObjects = buildLinearObjectiveSubObjects(metaModel, tokens)

            if (callBack != null) {
                callBack(
                    MechanismModelDumpingStatus.dumpingConstrains(
                        ready = metaModel.constraints.usize,
                        model = metaModel
                    )
                )
            }

            return LinearMechanismModel<V>(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints.toMutableList(),
                objectFunction = SingleObject(metaModel.objectCategory, subObjects),
                tokens = tokens
            )
        }

        @kotlin.Deprecated("Use invoke(metaModel: LinearMetaModel<V>) instead. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmName("invokeFlt64")
        suspend operator fun invoke(
            metaModel: LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
            return invoke<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                metaModel = metaModel,
                concurrent = concurrent,
                blocking = blocking,
                fixedVariables = fixedVariables,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack
            )
        }

        private suspend fun <V> unfold(
            tokens: AbstractMutableTokenTable<V>,
            fixedVariables: Map<AbstractVariableItem<*, *>, V>? = null,
            toFlt64: (V) -> Flt64,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable<V>> where V : RealNumber<V>, V : NumberField<V> {
            return when (tokens) {
                is MutableTokenTable<V> -> {
                    val temp = copyMutableTokenTableAsFlt64(tokens)
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = toSolverFixedValues(fixedVariables, toFlt64),
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(tokenTableAs<V, Flt64>(TokenTable(temp)))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                is ConcurrentMutableTokenTable<V> -> {
                    val temp = copyConcurrentMutableTokenTableAsFlt64(tokens)
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = toSolverFixedValues(fixedVariables, toFlt64),
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(tokenTableAs<V, Flt64>(ConcurrentTokenTable(temp)))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                else -> {
                    Failed(Err(ErrorCode.ApplicationError, "Unsupported token table type: ${tokens::class.simpleName}"))
                }
            }
        }
    }

    override fun addConstraint(
        relation: LinearInequality<V>,
        name: String?,
        from: Pair<IntermediateSymbol<out V>, Boolean>?
    ): Try {
        val flattenData = relation.toLinearFlattenData()
        _constraints.add(
            LinearConstraintImpl(
                relation = LinearRelationImpl(flattenData, relation.comparison),
                tokens = tokens,
                converter = parent.converter,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )
        )
        return ok
    }

    private fun generateOptimalCutV(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<V, Linear>, V>
    ): List<LinearInequality<V>> {
        val zero = parent.converter.zero
        val one = parent.converter.one
        val constants = linearConstraints.fold(zero) { acc, constraint ->
            acc + (dualSolution[constraint] ?: zero) * constraint.rhs
        }
        val polynomials = HashMap<AbstractVariableItem<*, *>, V>()
        for (constraint in linearConstraints) {
            val dual = dualSolution[constraint] ?: continue
            if (dual eq zero) {
                continue
            }

            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedVariables) {
                    val coefficient = dual * cell.coefficient
                    if (coefficient neq zero) {
                        polynomials[variable] = (polynomials[variable] ?: zero) - coefficient
                    }
                }
            }
        }
        val rhs = LinearPolynomial(
            monomials = polynomials.map { LinearMonomial(it.value, it.key) },
            constant = constants
        )
        val lhs = LinearPolynomial(listOf(LinearMonomial(one, objectVariable)), zero)
        return when (this.objectFunction.category) {
            ObjectCategory.Maximum -> {
                listOf(lhs le rhs)
            }

            ObjectCategory.Minimum -> {
                listOf(lhs ge rhs)
            }
        }
    }

    private fun generateFeasibleCutV(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<V, Linear>, V>
    ): List<LinearInequality<V>> {
        val zero = parent.converter.zero
        val one = parent.converter.one
        var value = zero
        var constants = zero
        val polynomials = HashMap<AbstractVariableItem<*, *>, V>()
        for (constraint in linearConstraints) {
            val dual = farkasDualSolution[constraint] ?: continue
            if (dual eq zero) {
                continue
            }

            value += dual * constraint.rhs
            constants += dual * constraint.rhs
            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedVariables) {
                    val coefficient = dual * cell.coefficient
                    if (coefficient neq zero) {
                        polynomials[variable] = (polynomials[variable] ?: zero) - coefficient
                    }
                    value -= dual * cell.coefficient * fixedVariables[variable]!!
                }
            }
        }
        if (value ls zero) {
            logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
            constants *= -one
            polynomials.replaceAll { _, v -> -v }
        }
        val lhs = LinearPolynomial(
            monomials = polynomials.map { LinearMonomial(it.value, it.key) },
            constant = constants
        )
        return listOf(lhs le zero)
    }

    private fun toFlt64LinearCut(cut: LinearInequality<V>): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        return LinearInequality(
            lhs = LinearPolynomial(
                monomials = cut.lhs.monomials.map { LinearMonomial(parent.converter.fromValue(it.coefficient), it.symbol) },
                constant = parent.converter.fromValue(cut.lhs.constant)
            ),
            rhs = LinearPolynomial(
                monomials = cut.rhs.monomials.map { LinearMonomial(parent.converter.fromValue(it.coefficient), it.symbol) },
                constant = parent.converter.fromValue(cut.rhs.constant)
            ),
            comparison = cut.comparison,
            name = cut.name,
            displayName = cut.displayName
        ).normalize()
    }

    internal fun generateOptimalCutByIdV(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolutionById: Map<String, V>
    ): List<LinearInequality<V>> {
        val zero = parent.converter.zero
        validateDualById(linearConstraints, dualSolutionById, logger)
        val dualSolution: kotlin.collections.Map<Constraint<V, Linear>, V> = buildMap {
            for (constraint in linearConstraints) {
                val dual = dualSolutionById[constraint.name]
                if (dual != null && dual neq zero) {
                    put(constraint, dual)
                }
            }
        }
        return generateOptimalCutV(objectVariable, fixedVariables, dualSolution)
    }

    internal fun generateFeasibleCutByIdV(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolutionById: Map<String, V>
    ): List<LinearInequality<V>> {
        val zero = parent.converter.zero
        validateDualById(linearConstraints, farkasDualSolutionById, logger)
        val dualSolution: kotlin.collections.Map<Constraint<V, Linear>, V> = buildMap {
            for (constraint in linearConstraints) {
                val dual = farkasDualSolutionById[constraint.name]
                if (dual != null && dual neq zero) {
                    put(constraint, dual)
                }
            }
        }
        return generateFeasibleCutV(fixedVariables, dualSolution)
    }

    internal fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        // 适配器边界：对偶解来自求解器原生类型 / Adapter boundary: dual solution from solver raw type
        val dualAsV: MutableMap<Constraint<V, Linear>, V> = LinkedHashMap()
        for (constraint in linearConstraints) {
            val typed = SolverBoundaryCasts.linearConstraintAsFlt64(constraint)
            val dual = dualSolution[typed] ?: continue
            if (dual neq Flt64.zero) {
                dualAsV[constraint] = parent.converter.intoValue(dual)
            }
        }

        return generateOptimalCutV(
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualAsV
        ).map { toFlt64LinearCut(it) }
    }

    internal fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        // 适配器边界：Farkas 对偶解来自求解器原生类型 / Adapter boundary: Farkas dual solution from solver raw type
        val dualAsV: MutableMap<Constraint<V, Linear>, V> = LinkedHashMap()
        for (constraint in linearConstraints) {
            val typed = SolverBoundaryCasts.linearConstraintAsFlt64(constraint)
            val dual = farkasDualSolution[typed] ?: continue
            if (dual neq Flt64.zero) {
                dualAsV[constraint] = parent.converter.intoValue(dual)
            }
        }

        return generateFeasibleCutV(
            fixedVariables = fixedVariables,
            farkasDualSolution = dualAsV
        ).map { toFlt64LinearCut(it) }
    }

    /**
     * Generate optimal Benders cut using constraint-name-based dual solution lookup.
     *
     * Convenience overload of [generateOptimalCut] that accepts a `Map<String, Flt64>`
     * (constraint name �?dual value) instead of `Map<Constraint<Flt64, Linear>, Flt64>`.
     * Use when the caller has dual values keyed by constraint name (e.g., from
     * serialized results, cross-process communication, or log output) rather than
     * direct Constraint object references.
     *
     * @param objectVariable  the objective variable (theta) to project onto
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param dualSolutionById  constraint name �?dual value mapping
     * @return list of linear cuts
     */
    internal fun generateOptimalCutById(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolutionById: Map<String, Flt64>
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        val dualSolutionByIdV = dualSolutionById.mapValues { (_, value) ->
            parent.converter.intoValue(value)
        }
        return generateOptimalCutByIdV(
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolutionById = dualSolutionByIdV
        ).map { toFlt64LinearCut(it) }
    }

    /**
     * Generate feasible Benders cut using constraint-name-based Farkas dual solution lookup.
     *
     * Convenience overload of [generateFeasibleCut] that accepts a `Map<String, Flt64>`
     * (constraint name �?Farkas dual value) instead of `Map<Constraint<Flt64, Linear>, Flt64>`.
     *
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param farkasDualSolutionById  constraint name �?Farkas dual value mapping
     * @return list of linear cuts
     */
    @kotlin.Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific Benders cut method will be removed in a future version.", level = DeprecationLevel.WARNING)
    fun generateFeasibleCutById(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolutionById: Map<String, Flt64>
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        val dualSolutionByIdV = farkasDualSolutionById.mapValues { (_, value) ->
            parent.converter.intoValue(value)
        }
        return generateFeasibleCutByIdV(
            fixedVariables = fixedVariables,
            farkasDualSolutionById = dualSolutionByIdV
        ).map { toFlt64LinearCut(it) }
    }

    /**
     * Generate optimal Benders cut from raw solver dual output.
     *
     * Convenience overload that accepts raw dual values ([Solution]) and a
     * [LinearTriadModelView] for origin resolution, delegating to
     * [LinearTriadModelView.tidyDualSolution] to map raw values back to
     * Constraint objects, then calling [generateOptimalCut].
     *
     * @param objectVariable  the objective variable (theta) to project onto
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param dualValues      raw dual values from the solver output
     * @param triadModel      the LinearTriadModel containing origin mapping
     * @return list of linear cuts
     *
     * Solver boundary: dualValues and return type are Flt64 because they represent
     * raw solver output. The V-typed model delegates to these for solver integration.
     */
    internal fun generateOptimalCutFromOutput(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualValues: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        triadModel: LinearTriadModelView
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        val dualSolution = triadModel.tidyDualSolution(dualValues)
        return generateOptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    /**
     * Generate feasible Benders cut from raw solver Farkas dual output.
     *
     * Convenience overload that accepts raw Farkas dual values ([Solution]) and a
     * [LinearTriadModelView] for origin resolution, delegating to
     * [LinearTriadModelView.tidyDualSolution] to map raw values back to
     * Constraint objects, then calling [generateFeasibleCut].
     *
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param farkasDualValues raw Farkas dual values from the solver output
     * @param triadModel      the LinearTriadModel containing origin mapping
     * @return list of linear cuts
     *
     * Solver boundary: farkasDualValues and return type are Flt64 because they represent
     * raw solver output. The V-typed model delegates to these for solver integration.
     */
    internal fun generateFeasibleCutFromOutput(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualValues: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        triadModel: LinearTriadModelView
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        val farkasDualSolution = triadModel.tidyDualSolution(farkasDualValues)
        return generateFeasibleCut(fixedVariables, farkasDualSolution)
    }

    val numConstraints: Int get() = _constraints.size

    override fun close() {
        tokens.close()
    }

    override fun toString(): String {
        return name
    }
}

// Backward compatibility: typealias aliases

class QuadraticMechanismModel<V>(
    internal val parent: QuadraticMetaModel<V>,
    override var name: String,
    constraints: List<QuadraticConstraintImpl<V>>,
    override val objectFunction: SingleObject<QuadraticSubObject<V>>,
    override val tokens: AbstractTokenTable<V>
) : BasicMechanismModel<V>(name, tokens), AbstractQuadraticMechanismModel<V>, SingleObjectMechanismModel<V>
        where V : RealNumber<V>, V : NumberField<V> {
    private val logger = logger()

    /**
     * Constraints storage. Inherits query helpers (numVariables) from BasicMechanismModel.
     */
    private val _constraints: MutableList<QuadraticConstraintImpl<V>> = constraints.toMutableList()
    internal val concurrent by parent.configuration::concurrent
    override val constraints: List<Constraint<V, *>> get() = _constraints
    internal val quadraticConstraints: List<QuadraticConstraintImpl<V>> get() = _constraints

    companion object {
        private val logger = logger()

        /**
         * V-typed factory: create QuadraticMechanismModel<V> from QuadraticMetaModel<V>.
         * Uses the V-typed SubObject companion overload with IntoValue<V> converter.
         */
        suspend operator fun <V> invoke(
            metaModel: QuadraticMetaModel<V>,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, V>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<QuadraticMechanismModel<V>> where V : RealNumber<V>, V : NumberField<V> {
            logger.info { "Creating QuadraticMechanismModel<V> for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(
                tokens = metaModel.tokens,
                fixedVariables = fixedVariables,
                toFlt64 = metaModel.converter::fromValue,
                callBack = registrationStatusCallBack
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (Runtime.getRuntime().availableProcessors() > 2 && concurrent ?: metaModel.configuration.concurrent) {
                if (blocking ?: metaModel.configuration.dumpBlocking) {
                    runBlocking {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }
                } else {
                    coroutineScope {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }
                }
            } else {
                QuadraticMechanismModel<V>(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = metaModel._relationConstraints.map {
                        QuadraticConstraintImpl(
                            relation = QuadraticRelationImpl(it.flattenData, it.sign),
                            tokens = tokens,
                            converter = metaModel.converter,
                            lazy = it.lazy,
                            name = it.name,
                            origin = it
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, buildQuadraticObjectiveSubObjects(metaModel, tokens)),
                    tokens = tokens
                )
            }
            System.gc()

            logger.trace { "Registering function symbol constraints for $metaModel" }
            for ((i, symbol) in tokens.symbols.withIndex()) {
                val result = when (symbol) {
                    is QuadraticMathFunctionSymbolBase<*> -> symbol.registerConstraintsUnchecked(model)
                    is MathFunctionSymbolBase<*> -> symbol.registerConstraintsUnchecked(model)
                    else -> ok
                }
                when (result) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                if (dumpingStatusCallBack != null && i % 100 == 0) {
                    dumpingStatusCallBack(
                        MechanismModelDumpingStatus.dumpingSymbols(
                            ready = UInt64(i),
                            model = metaModel
                        )
                    )
                }
            }
            if (dumpingStatusCallBack != null) {
                dumpingStatusCallBack(
                    MechanismModelDumpingStatus.dumpingSymbols(
                        ready = tokens.symbols.usize,
                        model = metaModel
                    )
                )
            }
            logger.trace { "Function symbol constraints registered for $metaModel" }

            logger.info { "QuadraticMechanismModel<V> created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        private suspend fun <V> dumpAsync(
            metaModel: QuadraticMetaModel<V>,
            tokens: AbstractTokenTable<V>,
            scope: CoroutineScope,
            callBack: MechanismModelDumpingStatusCallBack? = null
        ): QuadraticMechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
            val constraints = dumpItemsAsync(
                items = metaModel._relationConstraints,
                scope = scope,
                memoryCheckInSingleTask = false,
                onProgress = callBack?.let { callback ->
                    { ready ->
                        callback(
                            MechanismModelDumpingStatus.dumpingConstrains(
                                ready = ready,
                                model = metaModel
                            )
                        )
                    }
                },
                transform = {
                    QuadraticConstraintImpl(
                        relation = QuadraticRelationImpl(it.flattenData, it.sign),
                        tokens = tokens,
                        converter = metaModel.converter,
                        lazy = it.lazy,
                        name = it.name,
                        origin = it
                    )
                }
            )
            val subObjects = buildQuadraticObjectiveSubObjects(metaModel, tokens)

            if (callBack != null) {
                callBack(
                    MechanismModelDumpingStatus.dumpingConstrains(
                        ready = metaModel.constraints.usize,
                        model = metaModel
                    )
                )
            }

            return QuadraticMechanismModel<V>(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints.toMutableList(),
                objectFunction = SingleObject(metaModel.objectCategory, subObjects),
                tokens = tokens
            )
        }

        @kotlin.Deprecated("Use invoke(metaModel: QuadraticMetaModel<V>) instead. This Flt64-specific overload will be removed in a future version.", level = DeprecationLevel.WARNING)
        @JvmName("invokeFlt64")
        suspend operator fun invoke(
            metaModel: QuadraticMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
            return invoke<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                metaModel = metaModel,
                concurrent = concurrent,
                blocking = blocking,
                fixedVariables = fixedVariables,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack
            )
        }

        private suspend fun <V> unfold(
            tokens: AbstractMutableTokenTable<V>,
            fixedVariables: Map<AbstractVariableItem<*, *>, V>? = null,
            toFlt64: (V) -> Flt64,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable<V>> where V : RealNumber<V>, V : NumberField<V> {
            return when (tokens) {
                is MutableTokenTable<V> -> {
                    val temp = copyMutableTokenTableAsFlt64(tokens)
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = toSolverFixedValues(fixedVariables, toFlt64),
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(tokenTableAs<V, Flt64>(TokenTable(temp)))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                is ConcurrentMutableTokenTable<V> -> {
                    val temp = copyConcurrentMutableTokenTableAsFlt64(tokens)
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = toSolverFixedValues(fixedVariables, toFlt64),
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(tokenTableAs<V, Flt64>(ConcurrentTokenTable(temp)))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                else -> {
                    Failed(Err(ErrorCode.ApplicationError, "Unsupported token table type: ${tokens::class.simpleName}"))
                }
            }
        }
    }

    override fun addConstraint(
        relation: LinearInequality<V>,
        name: String?,
        from: Pair<IntermediateSymbol<out V>, Boolean>?
    ): Try {
        val flattenData = relation.toLinearFlattenData()
        // Promote linear flatten data to quadratic (each linear monomial c*x becomes quadratic c*x*null)
        val qMonomials = flattenData.monomials.map { QuadraticMonomial(it.coefficient, it.symbol, null) }
        val qFlattenData = QuadraticFlattenData<V>(qMonomials, flattenData.constant)
        _constraints.add(
            QuadraticConstraintImpl(
                relation = QuadraticRelationImpl(qFlattenData, relation.comparison),
                tokens = tokens,
                converter = parent.converter,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )
        )
        return ok
    }

    override fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        name: String?,
        from: Pair<IntermediateSymbol<out V>, Boolean>?
    ): Try {
        val flattenData = relation.toQuadraticFlattenData()
        _constraints.add(
            QuadraticConstraintImpl(
                relation = QuadraticRelationImpl(flattenData, relation.comparison),
                tokens = tokens,
                converter = parent.converter,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )
        )
        return ok
    }

    private fun generateOptimalCutV(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<V, Quadratic>, V>
    ): List<Any> {
        val zero = parent.converter.zero
        val one = parent.converter.one
        val constants = quadraticConstraints.fold(zero) { acc, constraint ->
            acc + (dualSolution[constraint] ?: zero) * constraint.rhs
        }
        val linearPolynomial = HashMap<AbstractVariableItem<*, *>, V>()
        val quadraticPolynomial = HashMap<OrderedVariablePair, V>()
        for (constraint in quadraticConstraints) {
            val dual = dualSolution[constraint] ?: continue
            if (dual eq zero) {
                continue
            }

            for (cell in constraint.lhs) {
                val variable1 = cell.token1.variable
                val variable2 = cell.token2?.variable
                if (variable2 == null) {
                    if (variable1 in fixedVariables) {
                        val projected = -dual * cell.coefficient
                        if (projected neq zero) {
                            linearPolynomial[variable1] = (linearPolynomial[variable1] ?: zero) + projected
                        }
                    }
                } else if (variable1 in fixedVariables && variable2 in fixedVariables) {
                    val projected = -dual * cell.coefficient
                    if (projected neq zero) {
                        val key = OrderedVariablePair.of(variable1, variable2)
                        quadraticPolynomial[key] = (quadraticPolynomial[key] ?: zero) + projected
                    }
                }
            }
        }

        val hasQuadratic = quadraticPolynomial.any { (_, coefficient) -> coefficient neq zero }
        if (!hasQuadratic) {
            val rhs = LinearPolynomial(
                monomials = linearPolynomial
                    .filterValues { it neq zero }
                    .map { LinearMonomial(it.value, it.key) },
                constant = constants
            )
            val lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(one, objectVariable)),
                constant = zero
            )
            val cut = when (this.objectFunction.category) {
                ObjectCategory.Maximum -> {
                    lhs le rhs
                }

                ObjectCategory.Minimum -> {
                    lhs ge rhs
                }
            }
            return listOf(cut)
        }

        val rhs = QuadraticPolynomial(
            monomials = linearPolynomial
                .filterValues { it neq zero }
                .map { QuadraticMonomial.linear(it.value, it.key) } +
                quadraticPolynomial
                    .filterValues { it neq zero }
                    .map { QuadraticMonomial.quadratic(it.value, it.key.first, it.key.second) },
            constant = constants
        )
        val lhs = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.linear(one, objectVariable)),
            constant = zero
        )
        val cut = when (this.objectFunction.category) {
            ObjectCategory.Maximum -> {
                lhs le rhs
            }

            ObjectCategory.Minimum -> {
                lhs ge rhs
            }
        }
        return listOf(cut)
    }

    private fun generateFeasibleCutV(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<V, Quadratic>, V>
    ): List<Any> {
        val zero = parent.converter.zero
        val one = parent.converter.one
        var value = zero
        var constants = zero
        val linearPolynomial = HashMap<AbstractVariableItem<*, *>, V>()
        val quadraticPolynomial = HashMap<OrderedVariablePair, V>()
        for (constraint in quadraticConstraints) {
            val dual = farkasDualSolution[constraint] ?: continue
            if (dual eq zero) {
                continue
            }

            value += dual * constraint.rhs
            constants += dual * constraint.rhs
            for (cell in constraint.lhs) {
                val variable1 = cell.token1.variable
                val variable2 = cell.token2?.variable
                if (variable2 == null) {
                    if (variable1 in fixedVariables) {
                        val projected = -dual * cell.coefficient
                        if (projected neq zero) {
                            linearPolynomial[variable1] = (linearPolynomial[variable1] ?: zero) + projected
                        }
                        value -= dual * cell.coefficient * fixedVariables[variable1]!!
                    }
                } else if (variable1 in fixedVariables && variable2 in fixedVariables) {
                    val projected = -dual * cell.coefficient
                    if (projected neq zero) {
                        val key = OrderedVariablePair.of(variable1, variable2)
                        quadraticPolynomial[key] = (quadraticPolynomial[key] ?: zero) + projected
                    }
                    value -= dual * cell.coefficient * fixedVariables[variable1]!! * fixedVariables[variable2]!!
                }
            }
        }
        if (value ls zero) {
            logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
            constants *= -one
            linearPolynomial.replaceAll { _, coefficient -> -coefficient }
            quadraticPolynomial.replaceAll { _, coefficient -> -coefficient }
        }

        val hasQuadratic = quadraticPolynomial.any { (_, coefficient) -> coefficient neq zero }
        if (!hasQuadratic) {
            val lhs = LinearPolynomial(
                monomials = linearPolynomial
                    .filterValues { it neq zero }
                    .map { LinearMonomial(it.value, it.key) },
                constant = constants
            )
            return listOf(lhs le zero)
        }

        val lhs = QuadraticPolynomial(
            monomials = linearPolynomial
                .filterValues { it neq zero }
                .map { QuadraticMonomial.linear(it.value, it.key) } +
                quadraticPolynomial
                    .filterValues { it neq zero }
                    .map { QuadraticMonomial.quadratic(it.value, it.key.first, it.key.second) },
            constant = constants
        )
        val rhs = QuadraticPolynomial(emptyList(), zero)
        return listOf(lhs le rhs)
    }

    private fun toFlt64Cut(cut: Any): Any {
        val linearCut = SolverBoundaryCasts.linearInequalityAsV<V>(cut)
        if (linearCut != null) {
            return (LinearInequality(
                lhs = LinearPolynomial(
                    monomials = linearCut.lhs.monomials.map { LinearMonomial(parent.converter.fromValue(it.coefficient), it.symbol) },
                    constant = parent.converter.fromValue(linearCut.lhs.constant)
                ),
                rhs = LinearPolynomial(
                    monomials = linearCut.rhs.monomials.map { LinearMonomial(parent.converter.fromValue(it.coefficient), it.symbol) },
                    constant = parent.converter.fromValue(linearCut.rhs.constant)
                ),
                comparison = linearCut.comparison,
                name = linearCut.name,
                displayName = linearCut.displayName
            ) as LinearInequality<Flt64>).normalize()
        }

        val quadraticCut = SolverBoundaryCasts.quadraticInequalityAsV<V>(cut)
        if (quadraticCut != null) {
            val flt64Cut = QuadraticInequalityOf(
                lhs = QuadraticPolynomial(
                    monomials = quadraticCut.lhs.monomials.map { QuadraticMonomial(parent.converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
                    constant = parent.converter.fromValue(quadraticCut.lhs.constant)
                ),
                rhs = QuadraticPolynomial(
                    monomials = quadraticCut.rhs.monomials.map { QuadraticMonomial(parent.converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
                    constant = parent.converter.fromValue(quadraticCut.rhs.constant)
                ),
                comparison = quadraticCut.comparison,
                name = quadraticCut.name,
                displayName = quadraticCut.displayName
            )
            return flt64Cut.normalize()
        }

        return cut
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun generateOptimalCutByIdV(
        objective: V,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolutionById: Map<String, V>
    ): List<Any> {
        val zero = parent.converter.zero
        validateDualById(quadraticConstraints, dualSolutionById, logger)
        val dualSolution: kotlin.collections.Map<Constraint<V, Quadratic>, V> = buildMap {
            for (constraint in quadraticConstraints) {
                val dual = dualSolutionById[constraint.name]
                if (dual != null && dual neq zero) {
                    put(constraint, dual)
                }
            }
        }
        return generateOptimalCutV(objectVariable, fixedVariables, dualSolution)
    }

    internal fun generateFeasibleCutByIdV(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolutionById: Map<String, V>
    ): List<Any> {
        val zero = parent.converter.zero
        validateDualById(quadraticConstraints, farkasDualSolutionById, logger)
        val dualSolution: kotlin.collections.Map<Constraint<V, Quadratic>, V> = buildMap {
            for (constraint in quadraticConstraints) {
                val dual = farkasDualSolutionById[constraint.name]
                if (dual != null && dual neq zero) {
                    put(constraint, dual)
                }
            }
        }
        return generateFeasibleCutV(fixedVariables, dualSolution)
    }

    internal fun generateOptimalCut(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
    ): Ret<List<Any>> {
        // 兼容性保留原签名；当前公式中 objective 未参与计算 / Keep signature for compatibility; objective is currently unused in this formulation
        val dualAsV: MutableMap<Constraint<V, Quadratic>, V> = LinkedHashMap()
        for (constraint in quadraticConstraints) {
            val typed = SolverBoundaryCasts.quadraticConstraintAsFlt64(constraint)
            val dual = dualSolution[typed] ?: continue
            if (dual neq Flt64.zero) {
                dualAsV[constraint] = parent.converter.intoValue(dual)
            }
        }
        val cutsV = generateOptimalCutV(
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualAsV
        )
        val cutsFlt64 = cutsV.map { cut -> toFlt64Cut(cut) }
        return Ok(cutsFlt64)
    }

    internal fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
    ): Ret<List<Any>> {
        val dualAsV: MutableMap<Constraint<V, Quadratic>, V> = LinkedHashMap()
        for (constraint in quadraticConstraints) {
            val typed = SolverBoundaryCasts.quadraticConstraintAsFlt64(constraint)
            val dual = farkasDualSolution[typed] ?: continue
            if (dual neq Flt64.zero) {
                dualAsV[constraint] = parent.converter.intoValue(dual)
            }
        }
        val cutsV = generateFeasibleCutV(
            fixedVariables = fixedVariables,
            farkasDualSolution = dualAsV
        )
        val cutsFlt64 = cutsV.map { cut -> toFlt64Cut(cut) }
        return Ok(cutsFlt64)
    }

    /**
     * Generate optimal Benders cut using constraint-name-based dual solution lookup.
     *
     * Convenience overload of [generateOptimalCut] that accepts a `Map<String, Flt64>`
     * (constraint name �?dual value) instead of `Map<Constraint<Flt64, Quadratic>, Flt64>`.
     * Use when the caller has dual values keyed by constraint name (e.g., from
     * serialized results, cross-process communication, or log output) rather than
     * direct Constraint object references.
     *
     * @param objective        the objective value of the sub-problem solution
     * @param objectVariable   the objective variable (theta) to project onto
     * @param fixedVariables   variables fixed in the sub-problem and their values
     * @param dualSolutionById constraint name �?dual value mapping
     * @return list of cuts (linear or quadratic inequalities)
     */
    @kotlin.Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific Benders cut method will be removed in a future version.", level = DeprecationLevel.WARNING)
    fun generateOptimalCutById(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolutionById: Map<String, Flt64>
    ): Ret<List<Any>> {
        val dualSolutionByIdV = dualSolutionById.mapValues { (_, value) ->
            parent.converter.intoValue(value)
        }
        val cutsV = generateOptimalCutByIdV(
            objective = parent.converter.intoValue(objective),
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolutionById = dualSolutionByIdV
        )
        return Ok(cutsV.map { cut -> toFlt64Cut(cut) })
    }

    /**
     * Generate feasible Benders cut using constraint-name-based Farkas dual solution lookup.
     *
     * Convenience overload of [generateFeasibleCut] that accepts a `Map<String, Flt64>`
     * (constraint name �?Farkas dual value) instead of `Map<Constraint<Flt64, Quadratic>, Flt64>`.
     *
     * @param fixedVariables         variables fixed in the sub-problem and their values
     * @param farkasDualSolutionById constraint name �?Farkas dual value mapping
     * @return list of cuts (linear or quadratic inequalities)
     */
    @kotlin.Deprecated("Use solveV(model, converter) for V-typed results. This Flt64-specific Benders cut method will be removed in a future version.", level = DeprecationLevel.WARNING)
    fun generateFeasibleCutById(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolutionById: Map<String, Flt64>
    ): Ret<List<Any>> {
        val dualSolutionByIdV = farkasDualSolutionById.mapValues { (_, value) ->
            parent.converter.intoValue(value)
        }
        val cutsV = generateFeasibleCutByIdV(
            fixedVariables = fixedVariables,
            farkasDualSolutionById = dualSolutionByIdV
        )
        return Ok(cutsV.map { cut -> toFlt64Cut(cut) })
    }

    /**
     * Generate optimal Benders cut from raw solver dual output.
     *
     * Convenience overload that accepts raw dual values ([Solution]) and a
     * [QuadraticTetradModelView] for origin resolution, delegating to
     * [QuadraticTetradModelView.tidyDualSolution] to map raw values back to
     * Constraint objects, then calling [generateOptimalCut].
     *
     * @param objective       the objective value of the sub-problem solution
     * @param objectVariable  the objective variable (theta) to project onto
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param dualValues      raw dual values from the solver output
     * @param tetradModel     the QuadraticTetradModel containing origin mapping
     * @return list of cuts (linear or quadratic inequalities)
     *
     * Solver boundary: dualValues and return type are Flt64 because they represent
     * raw solver output. The V-typed model delegates to these for solver integration.
     */
    internal fun generateOptimalCutFromOutput(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualValues: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        tetradModel: QuadraticTetradModelView
    ): Ret<List<Any>> {
        val dualSolution = tetradModel.tidyDualSolution(dualValues)
        return generateOptimalCut(objective, objectVariable, fixedVariables, dualSolution)
    }

    /**
     * Generate feasible Benders cut from raw solver Farkas dual output.
     *
     * Convenience overload that accepts raw Farkas dual values ([Solution]) and a
     * [QuadraticTetradModelView] for origin resolution, delegating to
     * [QuadraticTetradModelView.tidyDualSolution] to map raw values back to
     * Constraint objects, then calling [generateFeasibleCut].
     *
     * @param fixedVariables    variables fixed in the sub-problem and their values
     * @param farkasDualValues  raw Farkas dual values from the solver output
     * @param tetradModel       the QuadraticTetradModel containing origin mapping
     * @return list of cuts (linear or quadratic inequalities)
     *
     * Solver boundary: farkasDualValues and return type are Flt64 because they represent
     * raw solver output. The V-typed model delegates to these for solver integration.
     */
    internal fun generateFeasibleCutFromOutput(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualValues: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        tetradModel: QuadraticTetradModelView
    ): Ret<List<Any>> {
        val farkasDualSolution = tetradModel.tidyDualSolution(farkasDualValues)
        return generateFeasibleCut(fixedVariables, farkasDualSolution)
    }

    val numConstraints: Int get() = _constraints.size

    override fun close() {
        tokens.close()
    }

    override fun toString(): String {
        return name
    }
}

// Backward compatibility: typealias aliases


