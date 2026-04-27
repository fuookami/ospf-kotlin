package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.token.LegacyAbstractTokenTable
import fuookami.ospf.kotlin.core.token.LegacyAbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTableF64
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTableF64
import fuookami.ospf.kotlin.core.token.TokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatus
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.le
import fuookami.ospf.kotlin.math.symbol.inequality.ge
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticInequality
import fuookami.ospf.kotlin.core.token.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.token.register
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.math.operator.pow
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger

sealed interface MechanismModel<V : RealNumber<V>> : AutoCloseable {
    val name: String
    val constraints: List<Constraint<V, *>>
    val objectFunction: Object
    val tokens: LegacyAbstractTokenTable

    override fun close() {
        tokens.close()
    }
}

/**
 * Convert a generic [MechanismModel]<V> to a Flt64-specific [MechanismModelF64].
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
@Suppress("UNCHECKED_CAST")
fun <V : RealNumber<V>> convertMechanismModelToF64(model: MechanismModel<V>): Ret<MechanismModelF64> {
    return when (model) {
        is LinearMechanismModel<*> -> {
            Ok(model as LinearMechanismModelF64)
        }
        is QuadraticMechanismModel<*> -> {
            Ok(model as QuadraticMechanismModelF64)
        }
        else -> {
            Failed(Err(ErrorCode.IllegalArgument, "Cannot convert MechanismModel<V> to F64: unexpected model type ${model::class.simpleName}"))
        }
    }
}

// Backward compatibility: typealias aliases
typealias MechanismModelF64 = MechanismModel<Flt64>

interface AbstractLinearMechanismModel<V : RealNumber<V>> : MechanismModel<V> {
    /**
     * Add constraint using math LinearInequality
     */
    fun addConstraint(
        relation: MathLinearInequality,
        name: String? = null,
        from: Pair<IntermediateSymbol<*>, Boolean>? = null,
    ): Try

    fun addConstraint(
        relation: MathLinearInequality,
        name: String? = null,
        from: IntermediateSymbol<*>?,
    ): Try {
        return addConstraint(
            relation = relation,
            name = name,
            from = from?.let { it to false }
        )
    }
}

interface AbstractQuadraticMechanismModel<V : RealNumber<V>> : AbstractLinearMechanismModel<V> {
    /**
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: MathQuadraticInequality,
        name: String? = null,
        from: Pair<IntermediateSymbol<*>, Boolean>? = null
    ): Try

    fun addConstraint(
        relation: MathQuadraticInequality,
        name: String? = null,
        from: IntermediateSymbol<*>?
    ): Try {
        return addConstraint(
            relation = relation,
            name = name,
            from = from?.let { it to false }
        )
    }
}

// Backward compatibility: typealias aliases
typealias AbstractLinearMechanismModelF64 = AbstractLinearMechanismModel<Flt64>
typealias AbstractQuadraticMechanismModelF64 = AbstractQuadraticMechanismModel<Flt64>

interface SingleObjectMechanismModel<V : RealNumber<V>> : MechanismModel<V> {
    override val objectFunction: SingleObject<*>
}

typealias SingleObjectMechanismModelF64 = SingleObjectMechanismModel<Flt64>

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

private fun <C : Constraint<Flt64, *>> validateDualById(
    constraints: List<C>,
    dualById: Map<String, Flt64>,
    log: org.apache.logging.log4j.kotlin.KotlinLogger
) {
    // Detect duplicate constraint names — multiple constraints sharing the same name
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
    // Detect names in dualById that don't match any constraint — likely a caller error.
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

private suspend fun <RC, SO, C, S> dumpMechanismPartsAsync(
    metaModel: MetaModelF64,
    relationConstraints: List<RC>,
    subObjects: List<SO>,
    scope: CoroutineScope,
    callBack: MechanismModelDumpingStatusCallBack?,
    memoryCheckInSingleTask: Boolean,
    createConstraint: (RC) -> C,
    createSubObject: (SO) -> S
): Pair<List<C>, List<S>> {
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

class LinearMechanismModel<V : RealNumber<V>>(
    internal val parent: LinearMetaModel<V>,
    override var name: String,
    constraints: List<LinearConstraintImpl>,
    override val objectFunction: SingleObject<LinearSubObject<Flt64>>,
    override val tokens: LegacyAbstractTokenTable
) : BasicMechanismModel<V>(name, tokens), AbstractLinearMechanismModel<V>, SingleObjectMechanismModel<V> {
    private val logger = logger()

    /**
     * Constraints storage. Inherits query helpers (numVariables) from BasicMechanismModel.
     */
    private val _constraints = constraints.toMutableList()
    internal val concurrent by parent.configuration::concurrent
    @Suppress("UNCHECKED_CAST")
    override val constraints: List<Constraint<V, *>> get() = _constraints as List<Constraint<V, *>>
    /** Directly typed access to the underlying linear constraints. */
    internal val linearConstraints: List<LinearConstraintImpl> get() = _constraints

    companion object {
        private val logger = logger()

        @Suppress("DEPRECATION")
        suspend operator fun invoke(
            metaModel: LinearMetaModelF64,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<LinearMechanismModelF64> {
            logger.info { "Creating LinearMechanismModelF64 for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(
                tokens = metaModel.tokens,
                fixedVariables = fixedVariables,
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
                LinearMechanismModelF64(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = metaModel._relationConstraints.map {
                        LinearConstraintImpl(
                            relation = LinearRelationImpl(it.inequality.flattenData, it.inequality.comparison),
                            tokens = tokens
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        LinearSubObject(
                            category = it.category,
                            flattenData = LinearFlattenDataF64(it.polynomial.monomials, it.polynomial.constant),
                            tokens = tokens,
                            name = it.name
                        )
                    }),
                    tokens = tokens
                )
            }
            System.gc()

            logger.trace { "Registering symbols for $metaModel" }
            for ((i, symbol) in tokens.symbols.withIndex()) {
                (symbol as? MathFunctionSymbol<Flt64>)?.let { sym ->
                    sym.register(metaModel)
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
            logger.trace { "Symbols registered for $metaModel" }

            logger.info { "LinearMechanismModelF64 created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        @Suppress("DEPRECATION")
        private suspend fun dumpAsync(
            metaModel: LinearMetaModelF64,
            tokens: LegacyAbstractTokenTable,
            scope: CoroutineScope,
            callBack: MechanismModelDumpingStatusCallBack? = null
        ): LinearMechanismModelF64 {
            val (constraints, subObjects) = dumpMechanismPartsAsync(
                metaModel = metaModel,
                relationConstraints = metaModel._relationConstraints,
                subObjects = metaModel._subObjects,
                scope = scope,
                callBack = callBack,
                memoryCheckInSingleTask = true,
                createConstraint = {
                    LinearConstraintImpl(
                        relation = LinearRelationImpl(it.inequality.flattenData, it.inequality.comparison),
                        tokens = tokens
                    )
                },
                createSubObject = {
                    LinearSubObject(
                        category = it.category,
                        flattenData = LinearFlattenDataF64(it.polynomial.monomials, it.polynomial.constant),
                        tokens = tokens,
                        name = it.name
                    )
                }
            )

            return LinearMechanismModelF64(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints.toMutableList(),
                objectFunction = SingleObject(metaModel.objectCategory, subObjects),
                tokens = tokens
            )
        }

        private suspend fun unfold(
            tokens: LegacyAbstractMutableTokenTable,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<LegacyAbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTableF64 -> {
                    val temp = tokens.copy() as MutableTokenTableF64
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = fixedVariables?.mapKeys { it.key },
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(TokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                is ConcurrentMutableTokenTableF64 -> {
                    val temp = tokens.copy() as ConcurrentMutableTokenTableF64
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = fixedVariables?.mapKeys { it.key },
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(ConcurrentTokenTable(temp))
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
        relation: MathLinearInequality,
        name: String?,
        from: Pair<IntermediateSymbol<*>, Boolean>?
    ): Try {
        _constraints.add(
            LinearConstraintImpl(
                relation = LinearRelationImpl(relation.flattenData, relation.comparison),
                tokens = tokens,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )
        )
        return ok
    }

    @Suppress("DEPRECATION")
    fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolution: LinearDualSolution
    ): List<MathLinearInequality> {
        val constants = linearConstraints.foldIndexed(Flt64.zero) { _, acc, constraint ->
            acc + (dualSolution[constraint] ?: Flt64.zero) * constraint.rhs
        }
        val polynomials = HashMap<AbstractVariableItem<*, *>, Flt64>()
        for (constraint in linearConstraints) {
            val dual = dualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedVariables) {
                    val coefficient = (dualSolution[constraint] ?: Flt64.zero) * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        polynomials[variable] = (polynomials[variable] ?: Flt64.zero) - coefficient
                    }
                }
            }
        }
        val rhs = MathLinearPolynomial(
            monomials = polynomials.map { MathLinearMonomial(it.value, it.key) },
            constant = constants
        )
        return when (this.objectFunction.category) {
            ObjectCategory.Maximum -> {
                listOf((MathLinearPolynomial(listOf(MathLinearMonomial(Flt64.one, objectVariable)), Flt64.zero) le rhs).normalize())
            }

            ObjectCategory.Minimum -> {
                listOf((MathLinearPolynomial(listOf(MathLinearMonomial(Flt64.one, objectVariable)), Flt64.zero) ge rhs).normalize())
            }
        }
    }

    @Suppress("DEPRECATION")
    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolution: LinearDualSolution
    ): List<MathLinearInequality> {
        var value = Flt64.zero
        var constants = Flt64.zero
        val polynomials = HashMap<AbstractVariableItem<*, *>, Flt64>()
        for (constraint in linearConstraints) {
            val dual = farkasDualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            value += dual * constraint.rhs
            constants += dual * constraint.rhs
            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedVariables) {
                    val coefficient = dual * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        polynomials[variable] = (polynomials[variable] ?: Flt64.zero) - coefficient
                    }
                    value -= dual * cell.coefficient * fixedVariables[variable]!!
                }
            }
        }
        if (value ls Flt64.zero) {
            logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
            constants *= -Flt64.one
            polynomials.replaceAll { _, v -> -v }
        }
        val lhs = MathLinearPolynomial(
            monomials = polynomials.map { MathLinearMonomial(it.value, it.key) },
            constant = constants
        )
        return listOf((lhs le Flt64.zero).normalize())
    }

    /**
     * Generate optimal Benders cut using constraint-name-based dual solution lookup.
     *
     * Convenience overload of [generateOptimalCut] that accepts a `Map<String, Flt64>`
     * (constraint name → dual value) instead of `Map<LinearConstraint, Flt64>`.
     * Use when the caller has dual values keyed by constraint name (e.g., from
     * serialized results, cross-process communication, or log output) rather than
     * direct Constraint object references.
     *
     * @param objectVariable  the objective variable (theta) to project onto
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param dualSolutionById  constraint name → dual value mapping
     * @return list of linear cuts
     */
    fun generateOptimalCutById(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolutionById: Map<String, Flt64>
    ): List<MathLinearInequality> {
        validateDualById(linearConstraints, dualSolutionById, logger)
        val dualSolution: LinearDualSolution = buildMap {
            for (c in linearConstraints) {
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
     * (constraint name → Farkas dual value) instead of `Map<LinearConstraint, Flt64>`.
     *
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param farkasDualSolutionById  constraint name → Farkas dual value mapping
     * @return list of linear cuts
     */
    fun generateFeasibleCutById(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolutionById: Map<String, Flt64>
    ): List<MathLinearInequality> {
        validateDualById(linearConstraints, farkasDualSolutionById, logger)
        val farkasDualSolution: LinearDualSolution = buildMap {
            for (c in linearConstraints) {
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
     */
    fun generateOptimalCutFromOutput(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualValues: Solution,
        triadModel: LinearTriadModelView
    ): List<MathLinearInequality> {
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
     */
    fun generateFeasibleCutFromOutput(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualValues: Solution,
        triadModel: LinearTriadModelView
    ): List<MathLinearInequality> {
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
typealias LinearMechanismModelF64 = LinearMechanismModel<Flt64>

class QuadraticMechanismModel<V : RealNumber<V>>(
    internal val parent: QuadraticMetaModel<V>,
    override var name: String,
    constraints: List<QuadraticConstraintImpl>,
    override val objectFunction: SingleObject<QuadraticSubObject<Flt64>>,
    override val tokens: LegacyAbstractTokenTable
) : BasicMechanismModel<V>(name, tokens), AbstractQuadraticMechanismModel<V>, SingleObjectMechanismModel<V> {
    private val logger = logger()

    /**
     * Constraints storage. Inherits query helpers (numVariables) from BasicMechanismModel.
     */
    private val _constraints = constraints.toMutableList()
    internal val concurrent by parent.configuration::concurrent
    @Suppress("UNCHECKED_CAST")
    override val constraints: List<Constraint<V, *>> get() = _constraints as List<Constraint<V, *>>
    /** Directly typed access to the underlying quadratic constraints. */
    internal val quadraticConstraints: List<QuadraticConstraintImpl> get() = _constraints

    companion object {
        private val logger = logger()

        @Suppress("DEPRECATION")
        suspend operator fun invoke(
            metaModel: QuadraticMetaModelF64,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<QuadraticMechanismModelF64> {
            logger.info { "Creating QuadraticMechanismModelF64 for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(
                tokens = metaModel.tokens,
                fixedVariables = fixedVariables,
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
                QuadraticMechanismModelF64(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = metaModel._relationConstraints.map {
                        QuadraticConstraintImpl(
                            relation = QuadraticRelationImpl(it.inequality.flattenData, it.inequality.comparison),
                            tokens = tokens
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        QuadraticSubObject(
                            category = it.category,
                            flattenData = LinearFlattenDataF64(it.polynomial.monomials, it.polynomial.constant).toQuadraticFlattenData(),
                            tokens = tokens,
                            name = it.name
                        )
                    }),
                    tokens = tokens
                )
            }
            System.gc()

            logger.trace { "Registering symbols for $metaModel" }
            for ((i, symbol) in tokens.symbols.withIndex()) {
                (symbol as? MathFunctionSymbol<Flt64>)?.let { sym ->
                    sym.register(metaModel)
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
            logger.trace { "Symbols registered for $metaModel" }

            logger.info { "QuadraticMechanismModelF64 created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        @Suppress("DEPRECATION")
        private suspend fun dumpAsync(
            metaModel: QuadraticMetaModelF64,
            tokens: LegacyAbstractTokenTable,
            scope: CoroutineScope,
            callBack: MechanismModelDumpingStatusCallBack? = null
        ): QuadraticMechanismModelF64 {
            val (constraints, subObjects) = dumpMechanismPartsAsync(
                metaModel = metaModel,
                relationConstraints = metaModel._relationConstraints,
                subObjects = metaModel._subObjects,
                scope = scope,
                callBack = callBack,
                memoryCheckInSingleTask = false,
                createConstraint = {
                    QuadraticConstraintImpl(
                        relation = QuadraticRelationImpl(it.inequality.flattenData, it.inequality.comparison),
                        tokens = tokens
                    )
                },
                createSubObject = {
                    QuadraticSubObject(
                        category = it.category,
                        flattenData = LinearFlattenDataF64(it.polynomial.monomials, it.polynomial.constant).toQuadraticFlattenData(),
                        tokens = tokens,
                        name = it.name
                    )
                }
            )

            return QuadraticMechanismModelF64(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints.toMutableList(),
                objectFunction = SingleObject(metaModel.objectCategory, subObjects),
                tokens = tokens
            )
        }

        private suspend fun unfold(
            tokens: LegacyAbstractMutableTokenTable,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<LegacyAbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTableF64 -> {
                    val temp = tokens.copy() as MutableTokenTableF64
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = fixedVariables?.mapKeys { it.key },
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(TokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                is ConcurrentMutableTokenTableF64 -> {
                    val temp = tokens.copy() as ConcurrentMutableTokenTableF64
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = fixedVariables?.mapKeys { it.key },
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(ConcurrentTokenTable(temp))
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
        relation: MathLinearInequality,
        name: String?,
        from: Pair<IntermediateSymbol<*>, Boolean>?
    ): Try {
        _constraints.add(
            relation.toQuadraticConstraint(
                tokens = tokens,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )
        )
        return ok
    }

    override fun addConstraint(
        relation: MathQuadraticInequality,
        name: String?,
        from: Pair<IntermediateSymbol<*>, Boolean>?
    ): Try {
        _constraints.add(
            QuadraticConstraintImpl(
                relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
                tokens = tokens,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )
        )
        return ok
    }

    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    fun generateOptimalCut(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolution: QuadraticDualSolution,
    ): Ret<List<Any>> {
        val constants = quadraticConstraints.fold(Flt64.zero) { acc, constraint ->
            acc + (dualSolution[constraint] ?: Flt64.zero) * constraint.rhs
        }
        val linearPolynomial = HashMap<AbstractVariableItem<*, *>, Flt64>()
        val quadraticPolynomial = HashMap<OrderedVariablePair, Flt64>()
        for (constraint in quadraticConstraints) {
            val dual = dualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            for (cell in constraint.lhs) {
                val variable1 = cell.token1.variable
                val variable2 = cell.token2?.variable
                if (variable2 == null) {
                    if (variable1 in fixedVariables) {
                        val projected = -dual * cell.coefficient
                        if (projected neq Flt64.zero) {
                            linearPolynomial[variable1] = (linearPolynomial[variable1] ?: Flt64.zero) + projected
                        }
                    }
                } else if (variable1 in fixedVariables && variable2 in fixedVariables) {
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
            val rhs = MathLinearPolynomial(
                monomials = linearPolynomial
                    .filterValues { it neq Flt64.zero }
                    .map { MathLinearMonomial(it.value, it.key) },
                constant = constants
            )
            val lhs = MathLinearPolynomial(
                monomials = listOf(MathLinearMonomial(Flt64.one, objectVariable)),
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

        val rhs = MathQuadraticPolynomial(
            monomials = linearPolynomial
                .filterValues { it neq Flt64.zero }
                .map { MathQuadraticMonomial.linear(it.value, it.key) } +
                quadraticPolynomial
                    .filterValues { it neq Flt64.zero }
                    .map { MathQuadraticMonomial.quadratic(it.value, it.key.first, it.key.second) },
            constant = constants
        )
        val lhs = MathQuadraticPolynomial(
            monomials = listOf(MathQuadraticMonomial.linear(Flt64.one, objectVariable)),
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
    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolution: QuadraticDualSolution,
    ): Ret<List<Any>> {
        var value = Flt64.zero
        var constants = Flt64.zero
        val linearPolynomial = HashMap<AbstractVariableItem<*, *>, Flt64>()
        val quadraticPolynomial = HashMap<OrderedVariablePair, Flt64>()
        for (constraint in quadraticConstraints) {
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
                    if (variable1 in fixedVariables) {
                        val projected = -dual * cell.coefficient
                        if (projected neq Flt64.zero) {
                            linearPolynomial[variable1] = (linearPolynomial[variable1] ?: Flt64.zero) + projected
                        }
                        value -= dual * cell.coefficient * fixedVariables[variable1]!!
                    }
                } else if (variable1 in fixedVariables && variable2 in fixedVariables) {
                    val projected = -dual * cell.coefficient
                    if (projected neq Flt64.zero) {
                        val key = OrderedVariablePair.of(variable1, variable2)
                        quadraticPolynomial[key] = (quadraticPolynomial[key] ?: Flt64.zero) + projected
                    }
                    value -= dual * cell.coefficient * fixedVariables[variable1]!! * fixedVariables[variable2]!!
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
            val lhs = MathLinearPolynomial(
                monomials = linearPolynomial
                    .filterValues { it neq Flt64.zero }
                    .map { MathLinearMonomial(it.value, it.key) },
                constant = constants
            )
            return Ok(listOf((lhs le Flt64.zero).normalize()))
        }

        val lhs = MathQuadraticPolynomial(
            monomials = linearPolynomial
                .filterValues { it neq Flt64.zero }
                .map { MathQuadraticMonomial.linear(it.value, it.key) } +
                quadraticPolynomial
                    .filterValues { it neq Flt64.zero }
                    .map { MathQuadraticMonomial.quadratic(it.value, it.key.first, it.key.second) },
            constant = constants
        )
        val rhs = MathQuadraticPolynomial<Flt64>(emptyList(), Flt64.zero)
        return Ok(listOf((lhs le rhs).normalize()))
    }

    /**
     * Generate optimal Benders cut using constraint-name-based dual solution lookup.
     *
     * Convenience overload of [generateOptimalCut] that accepts a `Map<String, Flt64>`
     * (constraint name → dual value) instead of `Map<QuadraticConstraint, Flt64>`.
     * Use when the caller has dual values keyed by constraint name (e.g., from
     * serialized results, cross-process communication, or log output) rather than
     * direct Constraint object references.
     *
     * @param objective        the objective value of the sub-problem solution
     * @param objectVariable   the objective variable (theta) to project onto
     * @param fixedVariables   variables fixed in the sub-problem and their values
     * @param dualSolutionById constraint name → dual value mapping
     * @return list of cuts (linear or quadratic inequalities)
     */
    fun generateOptimalCutById(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolutionById: Map<String, Flt64>
    ): Ret<List<Any>> {
        validateDualById(quadraticConstraints, dualSolutionById, logger)
        val dualSolution: QuadraticDualSolution = buildMap {
            for (c in quadraticConstraints) {
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
     * (constraint name → Farkas dual value) instead of `Map<QuadraticConstraint, Flt64>`.
     *
     * @param fixedVariables         variables fixed in the sub-problem and their values
     * @param farkasDualSolutionById constraint name → Farkas dual value mapping
     * @return list of cuts (linear or quadratic inequalities)
     */
    fun generateFeasibleCutById(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolutionById: Map<String, Flt64>
    ): Ret<List<Any>> {
        validateDualById(quadraticConstraints, farkasDualSolutionById, logger)
        val farkasDualSolution: QuadraticDualSolution = buildMap {
            for (c in quadraticConstraints) {
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
     */
    fun generateOptimalCutFromOutput(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualValues: Solution,
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
     */
    fun generateFeasibleCutFromOutput(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualValues: Solution,
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
typealias QuadraticMechanismModelF64 = QuadraticMechanismModel<Flt64>




