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
import fuookami.ospf.kotlin.math.symbol.operation.normalize
import fuookami.ospf.kotlin.math.symbol.inequality.le
import fuookami.ospf.kotlin.math.symbol.inequality.ge
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.core.error.CoreError
import fuookami.ospf.kotlin.core.error.ModelError
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticInequality
import fuookami.ospf.kotlin.core.token.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.token.register
import fuookami.ospf.kotlin.core.model.intermediate.MemoryCleanupPolicy
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.Quadratic
import fuookami.ospf.kotlin.core.token.ConcurrentMutableTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

sealed interface MechanismModel<V> : AutoCloseable where V : RealNumber<V>, V : NumberField<V> {
    val name: String
    val constraints: List<Constraint<V, *>>
    val objectFunction: Object
    val tokens: AbstractTokenTable<V>

    override fun close() {
        tokens.close()
    }
}

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

interface SingleObjectMechanismModel<V> : MechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
    override val objectFunction: SingleObject<SubObject<V>>
}


private fun validateDualById(
    constraints: List<Constraint<*, *>>,
    dualById: Map<String, *>,
    log: org.apache.logging.log4j.kotlin.KotlinLogger
) {
    // Detect duplicate constraint names - multiple constraints sharing the same name
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
    // Detect names in dualById that don't match any constraint - likely a caller error.
    val constraintNames = nameCounts.keys
    for (name in dualById.keys) {
        if (name !in constraintNames) {
            log.warn { "dualSolutionById contains name '$name' which does not match any constraint in the model; it will be ignored" }
        }
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
            MemoryCleanupPolicy.cleanupAfterModelBuilt()

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
            MemoryCleanupPolicy.cleanupAfterSymbolRegistration()
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
                            Ok(mechanismTokenTableAs<V, Flt64>(TokenTable(temp)))
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
                            Ok(mechanismTokenTableAs<V, Flt64>(ConcurrentTokenTable(temp)))
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
        val flattenData = relation.toLinearFlattenData().getOrElse { return Failed(Err(ErrorCode.IllegalArgument, it.message ?: "Failed to flatten linear inequality")) }
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

    fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<V, Linear>, V>
    ): List<LinearInequality<V>> {
        return buildLinearOptimalCut(
            constraints = linearConstraints,
            objectCategory = this.objectFunction.category,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualSolution,
            zero = parent.converter.zero,
            one = parent.converter.one
        )
    }

    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<V, Linear>, V>
    ): List<LinearInequality<V>> {
        return buildLinearFeasibleCut(
            constraints = linearConstraints,
            fixedVariables = fixedVariables,
            farkasDualSolution = farkasDualSolution,
            zero = parent.converter.zero,
            one = parent.converter.one,
            logger = logger
        )
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

    internal fun generateOptimalCutById(
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
        return generateOptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    internal fun generateFeasibleCutById(
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
        return generateFeasibleCut(fixedVariables, dualSolution)
    }

    fun generateFlt64OptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        // 求解器边界：对偶解来自求解器原生类型。 / Solver boundary: dual solution from solver raw type.
        val dualByConstraint: MutableMap<Constraint<V, Linear>, V> = LinkedHashMap()
        for (constraint in linearConstraints) {
            val typed = SolverBoundaryCasts.linearConstraintAsFlt64(constraint)
            val dual = dualSolution[typed] ?: continue
            if (dual neq Flt64.zero) {
                dualByConstraint[constraint] = parent.converter.intoValue(dual)
            }
        }

        return generateOptimalCut(
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualByConstraint
        ).map { toFlt64LinearCut(it) }
    }

    fun generateFlt64FeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        // 求解器边界：Farkas 对偶解来自求解器原生类型。 / Solver boundary: Farkas dual solution from solver raw type.
        val dualByConstraint: MutableMap<Constraint<V, Linear>, V> = LinkedHashMap()
        for (constraint in linearConstraints) {
            val typed = SolverBoundaryCasts.linearConstraintAsFlt64(constraint)
            val dual = farkasDualSolution[typed] ?: continue
            if (dual neq Flt64.zero) {
                dualByConstraint[constraint] = parent.converter.intoValue(dual)
            }
        }

        return generateFeasibleCut(
            fixedVariables = fixedVariables,
            farkasDualSolution = dualByConstraint
        ).map { toFlt64LinearCut(it) }
    }

    /**
     * 从求解器原始对偶输出生成最优 Benders cut。
     * Generate optimal Benders cut from raw solver dual output.
     *
     * 该求解器边界入口接收原始对偶值，并通过 [LinearTriadModelView.tidyDualSolution]
     * 映射回约束对象，随后委托给 [generateFlt64OptimalCut]。
     * This solver-boundary entry accepts raw dual values, maps them back to
     * Constraint objects through [LinearTriadModelView.tidyDualSolution], then
     * delegates to [generateFlt64OptimalCut].
     *
     * @param objectVariable  the objective variable (theta) to project onto
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param dualValues      raw dual values from the solver output
     * @param triadModel      the LinearTriadModel containing origin mapping
     * @return list of linear cuts
     *
     * 求解器边界：dualValues 与返回值使用 Flt64，因为它们表示求解器原始输出。
     * Solver boundary: dualValues and return type are Flt64 because they represent raw solver output.
     */
    internal fun generateOptimalCutFromOutput(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualValues: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        triadModel: LinearTriadModelView
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        val dualSolution = triadModel.tidyDualSolution(dualValues)
        return generateFlt64OptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    /**
     * 从求解器原始 Farkas 对偶输出生成可行 Benders cut。
     * Generate feasible Benders cut from raw solver Farkas dual output.
     *
     * 该求解器边界入口接收原始 Farkas 对偶值，并通过 [LinearTriadModelView.tidyDualSolution]
     * 映射回约束对象，随后委托给 [generateFlt64FeasibleCut]。
     * This solver-boundary entry accepts raw Farkas dual values, maps them back
     * to Constraint objects through [LinearTriadModelView.tidyDualSolution], then
     * delegates to [generateFlt64FeasibleCut].
     *
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param farkasDualValues raw Farkas dual values from the solver output
     * @param triadModel      the LinearTriadModel containing origin mapping
     * @return list of linear cuts
     *
     * 求解器边界：farkasDualValues 与返回值使用 Flt64，因为它们表示求解器原始输出。
     * Solver boundary: farkasDualValues and return type are Flt64 because they represent raw solver output.
     */
    internal fun generateFeasibleCutFromOutput(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualValues: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        triadModel: LinearTriadModelView
    ): List<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>> {
        val farkasDualSolution = triadModel.tidyDualSolution(farkasDualValues)
        return generateFlt64FeasibleCut(fixedVariables, farkasDualSolution)
    }

    val numConstraints: Int get() = _constraints.size

    override fun close() {
        tokens.close()
    }

    override fun toString(): String {
        return name
    }
}

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
            MemoryCleanupPolicy.cleanupAfterModelBuilt()

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
            MemoryCleanupPolicy.cleanupAfterSymbolRegistration()
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
                            Ok(mechanismTokenTableAs<V, Flt64>(TokenTable(temp)))
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
                            Ok(mechanismTokenTableAs<V, Flt64>(ConcurrentTokenTable(temp)))
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
        val flattenData = relation.toLinearFlattenData().getOrElse { return Failed(Err(ErrorCode.IllegalArgument, it.message ?: "Failed to flatten linear inequality")) }
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

    fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<V, Quadratic>, V>
    ): List<Any> {
        return buildQuadraticOptimalCut(
            constraints = quadraticConstraints,
            objectCategory = this.objectFunction.category,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualSolution,
            zero = parent.converter.zero,
            one = parent.converter.one
        )
    }

    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<V, Quadratic>, V>
    ): List<Any> {
        return buildQuadraticFeasibleCut(
            constraints = quadraticConstraints,
            fixedVariables = fixedVariables,
            farkasDualSolution = farkasDualSolution,
            zero = parent.converter.zero,
            one = parent.converter.one,
            logger = logger
        )
    }

    private fun toFlt64Cut(cut: Any): Any {
        val linearCut = SolverBoundaryCasts.linearInequalityAs<V>(cut)
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

        val quadraticCut = SolverBoundaryCasts.quadraticInequalityAs<V>(cut)
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

    internal fun generateOptimalCutById(
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
        return generateOptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    internal fun generateFeasibleCutById(
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
        return generateFeasibleCut(fixedVariables, dualSolution)
    }

    fun generateFlt64OptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
    ): Ret<List<Any>> {
        val dualByConstraint: MutableMap<Constraint<V, Quadratic>, V> = LinkedHashMap()
        for (constraint in quadraticConstraints) {
            val typed = SolverBoundaryCasts.quadraticConstraintAsFlt64(constraint)
            val dual = dualSolution[typed] ?: continue
            if (dual neq Flt64.zero) {
                dualByConstraint[constraint] = parent.converter.intoValue(dual)
            }
        }
        val cuts = generateOptimalCut(
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualByConstraint
        )
        val cutsFlt64 = cuts.map { cut -> toFlt64Cut(cut) }
        return Ok(cutsFlt64)
    }

    fun generateFlt64FeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>,
    ): Ret<List<Any>> {
        val dualByConstraint: MutableMap<Constraint<V, Quadratic>, V> = LinkedHashMap()
        for (constraint in quadraticConstraints) {
            val typed = SolverBoundaryCasts.quadraticConstraintAsFlt64(constraint)
            val dual = farkasDualSolution[typed] ?: continue
            if (dual neq Flt64.zero) {
                dualByConstraint[constraint] = parent.converter.intoValue(dual)
            }
        }
        val cuts = generateFeasibleCut(
            fixedVariables = fixedVariables,
            farkasDualSolution = dualByConstraint
        )
        val cutsFlt64 = cuts.map { cut -> toFlt64Cut(cut) }
        return Ok(cutsFlt64)
    }

    /**
     * 从求解器原始对偶输出生成最优 Benders cut。
     * Generate optimal Benders cut from raw solver dual output.
     *
     * 该求解器边界入口接收原始对偶值，并通过 [QuadraticTetradModelView.tidyDualSolution]
     * 映射回约束对象，随后委托给 [generateFlt64OptimalCut]。
     * This solver-boundary entry accepts raw dual values, maps them back to
     * Constraint objects through [QuadraticTetradModelView.tidyDualSolution], then
     * delegates to [generateFlt64OptimalCut].
     *
     * @param objective       the objective value of the sub-problem solution
     * @param objectVariable  the objective variable (theta) to project onto
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param dualValues      raw dual values from the solver output
     * @param tetradModel     the QuadraticTetradModel containing origin mapping
     * @return list of cuts (linear or quadratic inequalities)
     *
     * 求解器边界：dualValues 与返回值使用 Flt64，因为它们表示求解器原始输出。
     * Solver boundary: dualValues and return type are Flt64 because they represent raw solver output.
     */
    internal fun generateOptimalCutFromOutput(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualValues: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        tetradModel: QuadraticTetradModelView
    ): Ret<List<Any>> {
        val dualSolution = tetradModel.tidyDualSolution(dualValues)
        return generateFlt64OptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    /**
     * 从求解器原始 Farkas 对偶输出生成可行 Benders cut。
     * Generate feasible Benders cut from raw solver Farkas dual output.
     *
     * 该求解器边界入口接收原始 Farkas 对偶值，并通过 [QuadraticTetradModelView.tidyDualSolution]
     * 映射回约束对象，随后委托给 [generateFlt64FeasibleCut]。
     * This solver-boundary entry accepts raw Farkas dual values, maps them back
     * to Constraint objects through [QuadraticTetradModelView.tidyDualSolution], then
     * delegates to [generateFlt64FeasibleCut].
     *
     * @param fixedVariables    variables fixed in the sub-problem and their values
     * @param farkasDualValues  raw Farkas dual values from the solver output
     * @param tetradModel       the QuadraticTetradModel containing origin mapping
     * @return list of cuts (linear or quadratic inequalities)
     *
     * 求解器边界：farkasDualValues 与返回值使用 Flt64，因为它们表示求解器原始输出。
     * Solver boundary: farkasDualValues and return type are Flt64 because they represent raw solver output.
     */
    internal fun generateFeasibleCutFromOutput(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualValues: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
        tetradModel: QuadraticTetradModelView
    ): Ret<List<Any>> {
        val farkasDualSolution = tetradModel.tidyDualSolution(farkasDualValues)
        return generateFlt64FeasibleCut(fixedVariables, farkasDualSolution)
    }

    val numConstraints: Int get() = _constraints.size

    override fun close() {
        tokens.close()
    }

    override fun toString(): String {
        return name
    }
}

