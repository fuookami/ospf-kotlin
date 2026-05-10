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
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

// Solver-boundary bridge: registerConstraints on star-projected function symbols
// Delegates to SolverBoundaryCasts (single UNCHECKED_CAST location)
private fun MathFunctionSymbolBase<*>.registerConstraintsUnchecked(model: AbstractLinearMechanismModel<*>): Try {
    return SolverBoundaryCasts.registerConstraintsLinearStar(this, model)
}

private fun QuadraticMathFunctionSymbolBase<*>.registerConstraintsUnchecked(model: AbstractQuadraticMechanismModel<*>): Try {
    return SolverBoundaryCasts.registerConstraintsQuadraticStar(this, model)
}

@Suppress("UNCHECKED_CAST")
private fun MechanismModel<*>.asLinearMechanismModelFlt64OrNull(): LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
    return this as? LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
}

@Suppress("UNCHECKED_CAST")
private fun MechanismModel<*>.asQuadraticMechanismModelFlt64OrNull(): QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
    return this as? QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
}

@Suppress("UNCHECKED_CAST")
private fun <V> copyMutableTokenTableAsFlt64(tokens: MutableTokenTable<V>): MutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return tokens.copy() as MutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
}

@Suppress("UNCHECKED_CAST")
private fun <V> copyConcurrentMutableTokenTableAsFlt64(tokens: ConcurrentMutableTokenTable<V>): ConcurrentMutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return tokens.copy() as ConcurrentMutableTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
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
        is LinearMechanismModel<*> -> model.asLinearMechanismModelFlt64OrNull()?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.IllegalArgument, "Cannot convert MechanismModel<V> to Flt64: linear cast failed"))
        is QuadraticMechanismModel<*> -> model.asQuadraticMechanismModelFlt64OrNull()?.let { Ok(it) }
            ?: Failed(Err(ErrorCode.IllegalArgument, "Cannot convert MechanismModel<V> to Flt64: quadratic cast failed"))
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
    dualById: Map<String, Flt64>,
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

    @Suppress("UNCHECKED_CAST")
    private fun linearConstraintsAsFlt64(): List<LinearConstraintImpl<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        return linearConstraints as List<LinearConstraintImpl<fuookami.ospf.kotlin.math.algebra.number.Flt64>>
    }

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
                            converter = metaModel.converter
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        LinearSubObject(
                            category = it.category,
                            flattenData = LinearFlattenData<V>(
                                it.polynomial.monomials.map { m -> LinearMonomial(m.coefficient, m.symbol) },
                                it.polynomial.constant
                            ),
                            tokens = tokens,
                            name = it.name,
                            converter = metaModel.converter
                        )
                    }),
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
            val (constraints, subObjects) = dumpMechanismPartsAsync(
                metaModel = metaModel,
                relationConstraints = metaModel._relationConstraints,
                subObjects = metaModel._subObjects,
                scope = scope,
                callBack = callBack,
                memoryCheckInSingleTask = true,
                createConstraint = {
                    LinearConstraintImpl(
                        relation = LinearRelationImpl(it.flattenData, it.sign),
                        tokens = tokens,
                        converter = metaModel.converter
                    )
                },
                createSubObject = {
                    LinearSubObject(
                        category = it.category,
                        flattenData = LinearFlattenData<V>(
                            it.polynomial.monomials.map { m -> LinearMonomial(m.coefficient, m.symbol) },
                            it.polynomial.constant
                        ),
                        tokens = tokens,
                        name = it.name,
                        converter = metaModel.converter
                    )
                }
            )

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

    @Suppress("DEPRECATION")
    internal fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        // Adapter boundary: Benders cut operates on Flt64; safe when V=Flt64
        val fixedValues = toFlt64FixedVariables(fixedVariables, parent.converter::fromValue)
        val constraints = linearConstraintsAsFlt64()
        val constants = constraints.foldIndexed(Flt64.zero) { _, acc, constraint ->
            acc + (dualSolution[constraint] ?: Flt64.zero) * constraint.rhs
        }
        val polynomials = HashMap<AbstractVariableItem<*, *>, Flt64>()
        for (constraint in constraints) {
            val dual = dualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedValues) {
                    val coefficient = (dualSolution[constraint] ?: Flt64.zero) * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        polynomials[variable] = (polynomials[variable] ?: Flt64.zero) - coefficient
                    }
                }
            }
        }
        val rhs = LinearPolynomial(
            monomials = polynomials.map { LinearMonomial(it.value, it.key) },
            constant = constants
        )
        return when (this.objectFunction.category) {
            ObjectCategory.Maximum -> {
                listOf((LinearPolynomial(listOf(LinearMonomial(Flt64.one, objectVariable)), Flt64.zero) le rhs).normalize())
            }

            ObjectCategory.Minimum -> {
                listOf((LinearPolynomial(listOf(LinearMonomial(Flt64.one, objectVariable)), Flt64.zero) ge rhs).normalize())
            }
        }
    }

    @Suppress("DEPRECATION")
    internal fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        // Adapter boundary: Benders cut operates on Flt64; safe when V=Flt64
        val fixedValues = toFlt64FixedVariables(fixedVariables, parent.converter::fromValue)
        val constraints = linearConstraintsAsFlt64()
        var value = Flt64.zero
        var constants = Flt64.zero
        val polynomials = HashMap<AbstractVariableItem<*, *>, Flt64>()
        for (constraint in constraints) {
            val dual = farkasDualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            value += dual * constraint.rhs
            constants += dual * constraint.rhs
            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedValues) {
                    val coefficient = dual * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        polynomials[variable] = (polynomials[variable] ?: Flt64.zero) - coefficient
                    }
                    value -= dual * cell.coefficient * fixedValues[variable]!!
                }
            }
        }
        if (value ls Flt64.zero) {
            logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
            constants *= -Flt64.one
            polynomials.replaceAll { _, v -> -v }
        }
        val lhs = LinearPolynomial(
            monomials = polynomials.map { LinearMonomial(it.value, it.key) },
            constant = constants
        )
        return listOf((lhs le Flt64.zero).normalize())
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
        // Adapter boundary: Benders cut operates on Flt64; safe when V=Flt64
        val constraints = linearConstraintsAsFlt64()
        validateDualById(constraints, dualSolutionById, logger)
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64> = buildMap {
            for (c in constraints) {
                val v = dualSolutionById[c.name]
                if (v != null && v neq Flt64.zero) put(c, v)
            }
        }
        return generateOptimalCut(objectVariable, fixedVariables, dualSolution)
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
        // Adapter boundary: Benders cut operates on Flt64; safe when V=Flt64
        val constraints = linearConstraintsAsFlt64()
        validateDualById(constraints, farkasDualSolutionById, logger)
        val farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64> = buildMap {
            for (c in constraints) {
                val v = farkasDualSolutionById[c.name]
                if (v != null && v neq Flt64.zero) put(c, v)
            }
        }
        return generateFeasibleCut(fixedVariables, farkasDualSolution)
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

    @Suppress("UNCHECKED_CAST")
    private fun quadraticConstraintsAsFlt64(): List<QuadraticConstraintImpl<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        return quadraticConstraints as List<QuadraticConstraintImpl<fuookami.ospf.kotlin.math.algebra.number.Flt64>>
    }

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
                            converter = metaModel.converter
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        QuadraticSubObject(
                            category = it.category,
                            flattenData = LinearFlattenData<V>(
                                it.polynomial.monomials.map { m -> LinearMonomial(m.coefficient, m.symbol) },
                                it.polynomial.constant
                            ).toQuadraticFlattenData(),
                            tokens = tokens,
                            name = it.name,
                            converter = metaModel.converter
                        )
                    }),
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
            val (constraints, subObjects) = dumpMechanismPartsAsync(
                metaModel = metaModel,
                relationConstraints = metaModel._relationConstraints,
                subObjects = metaModel._subObjects,
                scope = scope,
                callBack = callBack,
                memoryCheckInSingleTask = false,
                createConstraint = {
                    QuadraticConstraintImpl(
                        relation = QuadraticRelationImpl(it.flattenData, it.sign),
                        tokens = tokens,
                        converter = metaModel.converter
                    )
                },
                createSubObject = {
                    QuadraticSubObject(
                        category = it.category,
                        flattenData = LinearFlattenData<V>(
                            it.polynomial.monomials.map { m -> LinearMonomial(m.coefficient, m.symbol) },
                            it.polynomial.constant
                        ).toQuadraticFlattenData(),
                        tokens = tokens,
                        name = it.name,
                        converter = metaModel.converter
                    )
                }
            )

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

    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    internal fun generateOptimalCut(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
    ): Ret<List<Any>> {
        // Adapter boundary: Benders cut operates on Flt64; safe when V=Flt64
        val fixedValues = toFlt64FixedVariables(fixedVariables, parent.converter::fromValue)
        val constraints = quadraticConstraintsAsFlt64()
        val constants = constraints.fold(Flt64.zero) { acc, constraint ->
            acc + (dualSolution[constraint] ?: Flt64.zero) * constraint.rhs
        }
        val linearPolynomial = HashMap<AbstractVariableItem<*, *>, Flt64>()
        val quadraticPolynomial = HashMap<OrderedVariablePair, Flt64>()
        for (constraint in constraints) {
            val dual = dualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            for (cell in constraint.lhs) {
                val variable1 = cell.token1.variable
                val variable2 = cell.token2?.variable
                if (variable2 == null) {
                    if (variable1 in fixedValues) {
                        val projected = -dual * cell.coefficient
                        if (projected neq Flt64.zero) {
                            linearPolynomial[variable1] = (linearPolynomial[variable1] ?: Flt64.zero) + projected
                        }
                    }
                } else if (variable1 in fixedValues && variable2 in fixedValues) {
                    val projected = -dual * cell.coefficient
                    if (projected neq Flt64.zero) {
                        val key = OrderedVariablePair.of(variable1, variable2)
                        quadraticPolynomial[key] = (quadraticPolynomial[key] ?: Flt64.zero) + projected
                    }
                }
            }
        }

        val hasQuadratic = quadraticPolynomial.any { (_, coefficient) -> coefficient neq Flt64.zero }
        if (!hasQuadratic) {
            val rhs = LinearPolynomial(
                monomials = linearPolynomial
                    .filterValues { it neq Flt64.zero }
                    .map { LinearMonomial(it.value, it.key) },
                constant = constants
            )
            val lhs = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, objectVariable)),
                constant = Flt64.zero
            )
            val cut = when (this.objectFunction.category) {
                ObjectCategory.Maximum -> {
                    (lhs le rhs).normalize()
                }

                ObjectCategory.Minimum -> {
                    (lhs ge rhs).normalize()
                }
            }
            return Ok(listOf(cut))
        }

        val rhs = QuadraticPolynomial(
            monomials = linearPolynomial
                .filterValues { it neq Flt64.zero }
                .map { QuadraticMonomial.linear(it.value, it.key) } +
                quadraticPolynomial
                    .filterValues { it neq Flt64.zero }
                    .map { QuadraticMonomial.quadratic(it.value, it.key.first, it.key.second) },
            constant = constants
        )
        val lhs = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.linear(Flt64.one, objectVariable)),
            constant = Flt64.zero
        )
        val cut = when (this.objectFunction.category) {
            ObjectCategory.Maximum -> {
                (lhs le rhs).normalize()
            }

            ObjectCategory.Minimum -> {
                (lhs ge rhs).normalize()
            }
        }
        return Ok(listOf(cut))
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
    ): Ret<List<Any>> {
        // Adapter boundary: Benders cut operates on Flt64; safe when V=Flt64
        val fixedValues = toFlt64FixedVariables(fixedVariables, parent.converter::fromValue)
        val constraints = quadraticConstraintsAsFlt64()
        var value = Flt64.zero
        var constants = Flt64.zero
        val linearPolynomial = HashMap<AbstractVariableItem<*, *>, Flt64>()
        val quadraticPolynomial = HashMap<OrderedVariablePair, Flt64>()
        for (constraint in constraints) {
            val dual = farkasDualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            value += dual * constraint.rhs
            constants += dual * constraint.rhs
            for (cell in constraint.lhs) {
                val variable1 = cell.token1.variable
                val variable2 = cell.token2?.variable
                if (variable2 == null) {
                    if (variable1 in fixedValues) {
                        val projected = -dual * cell.coefficient
                        if (projected neq Flt64.zero) {
                            linearPolynomial[variable1] = (linearPolynomial[variable1] ?: Flt64.zero) + projected
                        }
                        value -= dual * cell.coefficient * fixedValues[variable1]!!
                    }
                } else if (variable1 in fixedValues && variable2 in fixedValues) {
                    val projected = -dual * cell.coefficient
                    if (projected neq Flt64.zero) {
                        val key = OrderedVariablePair.of(variable1, variable2)
                        quadraticPolynomial[key] = (quadraticPolynomial[key] ?: Flt64.zero) + projected
                    }
                    value -= dual * cell.coefficient * fixedValues[variable1]!! * fixedValues[variable2]!!
                }
            }
        }
        if (value ls Flt64.zero) {
            logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
            constants *= -Flt64.one
            linearPolynomial.replaceAll { _, coefficient -> -coefficient }
            quadraticPolynomial.replaceAll { _, coefficient -> -coefficient }
        }

        val hasQuadratic = quadraticPolynomial.any { (_, coefficient) -> coefficient neq Flt64.zero }
        if (!hasQuadratic) {
            val lhs = LinearPolynomial(
                monomials = linearPolynomial
                    .filterValues { it neq Flt64.zero }
                    .map { LinearMonomial(it.value, it.key) },
                constant = constants
            )
            return Ok(listOf((lhs le Flt64.zero).normalize()))
        }

        val lhs = QuadraticPolynomial(
            monomials = linearPolynomial
                .filterValues { it neq Flt64.zero }
                .map { QuadraticMonomial.linear(it.value, it.key) } +
                quadraticPolynomial
                    .filterValues { it neq Flt64.zero }
                    .map { QuadraticMonomial.quadratic(it.value, it.key.first, it.key.second) },
            constant = constants
        )
        val rhs = QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>(emptyList(), Flt64.zero)
        return Ok(listOf((lhs le rhs).normalize()))
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
        // Adapter boundary: Benders cut operates on Flt64; safe when V=Flt64
        val constraints = quadraticConstraintsAsFlt64()
        validateDualById(constraints, dualSolutionById, logger)
        val dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64> = buildMap {
            for (c in constraints) {
                val v = dualSolutionById[c.name]
                if (v != null && v neq Flt64.zero) put(c, v)
            }
        }
        return generateOptimalCut(objective, objectVariable, fixedVariables, dualSolution)
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
        // Adapter boundary: Benders cut operates on Flt64; safe when V=Flt64
        val constraints = quadraticConstraintsAsFlt64()
        validateDualById(constraints, farkasDualSolutionById, logger)
        val farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64> = buildMap {
            for (c in constraints) {
                val v = farkasDualSolutionById[c.name]
                if (v != null && v neq Flt64.zero) put(c, v)
            }
        }
        return generateFeasibleCut(fixedVariables, farkasDualSolution)
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


