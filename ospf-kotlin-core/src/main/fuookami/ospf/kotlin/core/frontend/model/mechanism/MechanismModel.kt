package fuookami.ospf.kotlin.core.frontend.model.mechanism

import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.*

sealed interface MechanismModel {
    val name: String
    val constraints: List<Constraint>
    val objectFunction: Object
    val tokens: AbstractTokenTable
}

interface AbstractLinearMechanismModel : MechanismModel {
    fun addConstraint(
        constraint: LinearInequality,
        name: String? = null,
        from: IntermediateSymbol? = null,
    ): Try
}

interface AbstractQuadraticMechanismModel : AbstractLinearMechanismModel {
    override fun addConstraint(
        constraint: LinearInequality,
        name: String?,
        from: IntermediateSymbol?
    ): Try {
        return addConstraint(QuadraticInequality(constraint), name, from)
    }

    fun addConstraint(
        constraint: QuadraticInequality,
        name: String? = null,
        from: IntermediateSymbol? = null
    ): Try
}

interface SingleObjectMechanismModel : MechanismModel {
    override val objectFunction: SingleObject<*>
}

class LinearMechanismModel(
    internal val parent: LinearMetaModel,
    override var name: String,
    constraints: List<LinearConstraint>,
    override val objectFunction: SingleObject<LinearSubObject>,
    override val tokens: AbstractTokenTable
) : AbstractLinearMechanismModel, SingleObjectMechanismModel {
    private val logger = logger()

    companion object {
        private val logger = logger()

        suspend operator fun invoke(
            metaModel: LinearMetaModel,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null
        ): Ret<LinearMechanismModel> {
            logger.info { "Creating LinearMechanismModel for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(metaModel.tokens, fixedVariables, registrationStatusCallBack)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (Runtime.getRuntime().availableProcessors() > 2 && concurrent ?: metaModel.concurrent) {
                if (blocking ?: metaModel.dumpBlocking) {
                    runBlocking {
                        dumpAsync(metaModel, tokens, this)
                    }
                } else {
                    coroutineScope {
                        dumpAsync(metaModel, tokens, this)
                    }
                }
            } else {
                LinearMechanismModel(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = metaModel._constraints.map {
                        LinearConstraint(
                            inequality = it,
                            tokens = tokens,
                            origin = true
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        LinearSubObject(
                            category = it.category,
                            poly = it.polynomial,
                            tokens = tokens,
                            name = it.name
                        )
                    }),
                    tokens = tokens
                )
            }
            System.gc()

            logger.trace { "Registering symbols for $metaModel" }
            for (symbol in tokens.symbols) {
                (symbol as? LinearFunctionSymbol)?.let {
                    if (fixedVariables.isNullOrEmpty()) {
                        it.register(model)
                    } else {
                        it.register(model, fixedVariables as Map<Symbol, Flt64>)
                    }
                }
            }
            logger.trace { "Symbols registered for $metaModel" }

            logger.info { "LinearMechanismModel created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        private suspend fun dumpAsync(
            metaModel: LinearMetaModel,
            tokens: AbstractTokenTable,
            scope: CoroutineScope
        ): LinearMechanismModel {
            val factor1 = Flt64(metaModel._constraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
            val constraints = if (factor1 >= 1) {
                val segment = pow(UInt64.ten, factor1).toInt()
                (0..(metaModel._constraints.size / segment)).map { i ->
                    scope.async(Dispatchers.Default) {
                        val result = metaModel._constraints
                            .subList(
                                (i * segment),
                                minOf(metaModel._constraints.size, (i + 1) * segment)
                            ).map {
                                LinearConstraint(
                                    inequality = it,
                                    tokens = tokens,
                                    origin = true
                                )
                            }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            } else {
                metaModel._constraints.map {
                    scope.async(Dispatchers.Default) {
                        val result = listOf(
                            LinearConstraint(
                                inequality = it,
                                tokens = tokens,
                                origin = true
                            )
                        )
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            }
            val factor2 = Flt64(metaModel._subObjects.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
            val subObjects = if (factor2 >= 1) {
                val segment = pow(UInt64.ten, factor2).toInt()
                (0..(metaModel._subObjects.size / segment)).map { i ->
                    scope.async(Dispatchers.Default) {
                        val result = metaModel._subObjects
                            .subList(
                                (i * segment),
                                minOf(metaModel._subObjects.size, (i + 1) * segment)
                            ).map {
                                LinearSubObject(
                                    category = it.category,
                                    poly = it.polynomial,
                                    tokens = tokens,
                                    name = it.name
                                )
                            }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            } else {
                metaModel._subObjects.map {
                    scope.async(Dispatchers.Default) {
                        val result = listOf(
                            LinearSubObject(
                                category = it.category,
                                poly = it.polynomial,
                                tokens = tokens,
                                name = it.name
                            )
                        )
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            }

            return LinearMechanismModel(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints.flatMap { it.await() }.toMutableList(),
                objectFunction = SingleObject(metaModel.objectCategory, subObjects.flatMap { it.await() }),
                tokens = tokens
            )
        }

        private suspend fun unfold(
            tokens: AbstractMutableTokenTable,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTable -> {
                    val temp = tokens.copy() as MutableTokenTable
                    when (val result = tokens.symbols.register(temp, fixedVariables?.mapKeys { it.key as Symbol }, callBack)) {
                        is Ok -> {
                            Ok(TokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }
                    }
                }

                is ConcurrentMutableTokenTable -> {
                    val temp = tokens.copy() as ConcurrentMutableTokenTable
                    when (val result = tokens.symbols.register(temp, fixedVariables?.mapKeys { it.key as Symbol }, callBack)) {
                        is Ok -> {
                            Ok(ConcurrentTokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }
                    }
                }
            }
        }
    }

    private val _constraints = constraints.toMutableList()
    internal val concurrent by parent::concurrent
    override val constraints by ::_constraints

    override fun addConstraint(
        constraint: LinearInequality,
        name: String?,
        from: IntermediateSymbol?
    ): Try {
        name?.let { constraint.name = it }
        _constraints.add(LinearConstraint(constraint, tokens, from = from))
        return ok
    }

    fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolution: LinearDualSolution
    ): List<LinearInequality> {
        val constants = constraints.foldIndexed(Flt64.zero) { i, acc, constraint ->
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
                if (variable in fixedVariables) {
                    val coefficient = (dualSolution[constraint] ?: Flt64.zero) * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        polynomials[variable] = (polynomials[variable] ?: Flt64.zero) - coefficient
                    }
                }
            }
        }
        val rhs = LinearPolynomial(polynomials.map { LinearMonomial(it.value, it.key) }, constants)
        return when (this.objectFunction.category) {
            ObjectCategory.Maximum -> {
                listOf((objectVariable leq rhs).normalize())
            }

            ObjectCategory.Minimum -> {
                listOf((objectVariable geq rhs).normalize())
            }
        }
    }

    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolution: LinearDualSolution
    ): List<LinearInequality> {
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
        val lhs = LinearPolynomial(polynomials.map { LinearMonomial(it.value, it.key) }, constants)
        return listOf((lhs leq Flt64.zero).normalize())
    }

    override fun toString(): String {
        return name
    }
}

class QuadraticMechanismModel(
    internal val parent: QuadraticMetaModel,
    override var name: String,
    constraints: List<QuadraticConstraint>,
    override val objectFunction: SingleObject<QuadraticSubObject>,
    override val tokens: AbstractTokenTable
) : AbstractQuadraticMechanismModel, SingleObjectMechanismModel {
    private val logger = logger()

    companion object {
        private val logger = logger()

        suspend operator fun invoke(
            metaModel: QuadraticMetaModel,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null
        ): Ret<QuadraticMechanismModel> {
            logger.info { "Creating QuadraticMechanismModel for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(metaModel.tokens, fixedVariables, registrationStatusCallBack)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (Runtime.getRuntime().availableProcessors() > 2 && concurrent ?: metaModel.concurrent) {
                if (blocking ?: metaModel.dumpBlocking) {
                    runBlocking {
                        dumpAsync(metaModel, tokens, this)
                    }
                } else {
                    coroutineScope {
                        dumpAsync(metaModel, tokens, this)
                    }
                }
            } else {
                QuadraticMechanismModel(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = metaModel._constraints.map {
                        QuadraticConstraint(
                            inequality = it,
                            tokens = tokens,
                            origin = true
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        QuadraticSubObject(
                            category = it.category,
                            poly = it.polynomial,
                            tokens = tokens,
                            name = it.name
                        )
                    }),
                    tokens = tokens
                )
            }
            System.gc()

            logger.trace { "Registering symbols for $metaModel" }
            for (symbol in tokens.symbols) {
                (symbol as? LinearFunctionSymbol)?.let { symbol ->
                    if (fixedVariables.isNullOrEmpty()) {
                        symbol.register(model)
                    } else {
                        symbol.register(model, fixedVariables.mapKeys { it.key as Symbol })
                    }
                }
                (symbol as? QuadraticFunctionSymbol)?.let { symbol ->
                    if (fixedVariables.isNullOrEmpty()) {
                        symbol.register(model)
                    } else {
                        symbol.register(model, fixedVariables.mapKeys { it.key as Symbol })
                    }
                }
            }
            logger.trace { "Symbols registered for $metaModel" }

            logger.info { "QuadraticMechanismModel created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        private suspend fun dumpAsync(
            metaModel: QuadraticMetaModel,
            tokens: AbstractTokenTable,
            scope: CoroutineScope
        ): QuadraticMechanismModel {
            val factor1 = Flt64(metaModel._constraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
            val constraints = if (factor1 >= 1) {
                val segment = pow(UInt64.ten, factor1).toInt()
                (0..(metaModel._constraints.size / segment)).map { i ->
                    scope.async(Dispatchers.Default) {
                        val result = metaModel._constraints
                            .subList(
                                (i * segment),
                                minOf(metaModel._constraints.size, (i + 1) * segment)
                            ).map {
                                QuadraticConstraint(
                                    inequality = it,
                                    tokens = tokens,
                                    origin = true
                                )
                            }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            } else {
                metaModel._constraints.map {
                    scope.async(Dispatchers.Default) {
                        val result = listOf(
                            QuadraticConstraint(
                                inequality = it,
                                tokens = tokens,
                                origin = true
                            )
                        )
                        result
                    }
                }
            }
            val factor2 = Flt64(metaModel._subObjects.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
            val subObjects = if (factor2 >= 1) {
                val segment = pow(UInt64.ten, factor2).toInt()
                (0..(metaModel._subObjects.size / segment)).map { i ->
                    scope.async(Dispatchers.Default) {
                        val result = metaModel._subObjects
                            .subList(
                                (i * segment),
                                minOf(metaModel._subObjects.size, (i + 1) * segment)
                            ).map {
                                QuadraticSubObject(
                                    category = it.category,
                                    poly = it.polynomial,
                                    tokens = tokens,
                                    name = it.name
                                )
                            }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            } else {
                metaModel._subObjects.map {
                    scope.async(Dispatchers.Default) {
                        val result = listOf(
                            QuadraticSubObject(
                                category = it.category,
                                poly = it.polynomial,
                                tokens = tokens,
                                name = it.name
                            )
                        )
                        result
                    }
                }
            }

            return QuadraticMechanismModel(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints.flatMap { it.await() }.toMutableList(),
                objectFunction = SingleObject(metaModel.objectCategory, subObjects.flatMap { it.await() }),
                tokens = tokens
            )
        }

        private suspend fun unfold(
            tokens: AbstractMutableTokenTable,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTable -> {
                    val temp = tokens.copy() as MutableTokenTable
                    when (val result = tokens.symbols.register(temp, fixedVariables?.mapKeys { it.key as Symbol }, callBack)) {
                        is Ok -> {
                            Ok(TokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }
                    }
                }

                is ConcurrentMutableTokenTable -> {
                    val temp = tokens.copy() as ConcurrentMutableTokenTable
                    when (val result = tokens.symbols.register(temp, fixedVariables?.mapKeys { it.key as Symbol }, callBack)) {
                        is Ok -> {
                            Ok(ConcurrentTokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }
                    }
                }
            }
        }
    }

    private val _constraints = constraints.toMutableList()
    internal val concurrent by parent::concurrent
    override val constraints by ::_constraints

    override fun addConstraint(
        constraint: QuadraticInequality,
        name: String?,
        from: IntermediateSymbol?
    ): Try {
        name?.let { constraint.name = it }
        _constraints.add(QuadraticConstraint(constraint, tokens, from = from))
        return ok
    }

    fun generateOptimalCut(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolution: QuadraticDualSolution,
    ): Ret<List<Inequality<*, *>>> {
        TODO("not implemented yet")
    }

    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolution: QuadraticDualSolution,
    ): Ret<List<Inequality<*, *>>> {
        TODO("not implemented yet")
    }
}
