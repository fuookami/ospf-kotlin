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
        name: String? = null
    ): Try
}

interface AbstractQuadraticMechanismModel : AbstractLinearMechanismModel {
    override fun addConstraint(
        constraint: LinearInequality,
        name: String?
    ): Try {
        return addConstraint(QuadraticInequality(constraint), name)
    }

    fun addConstraint(
        constraint: QuadraticInequality,
        name: String? = null
    ): Try
}

interface SingleObjectMechanismModel : MechanismModel {
    override val objectFunction: SingleObject
}

class LinearMechanismModel(
    internal val parent: LinearMetaModel,
    override var name: String,
    private val _constraints: MutableList<LinearConstraint>,
    override val objectFunction: SingleObject,
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
                    metaModel,
                    metaModel.name,
                    metaModel._constraints.map {
                        LinearConstraint(it, tokens)
                    }.toMutableList(),
                    SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        LinearSubObject(
                            it.category,
                            it.polynomial,
                            tokens,
                            it.name
                        )
                    }),
                    tokens
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
                                    tokens = tokens
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
                                tokens = tokens
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
                metaModel,
                metaModel.name,
                constraints.flatMap { it.await() }.toMutableList(),
                SingleObject(metaModel.objectCategory, subObjects.flatMap { it.await() }),
                tokens
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

    internal val concurrent by parent::concurrent
    override val constraints by ::_constraints

    override fun addConstraint(
        constraint: LinearInequality,
        name: String?
    ): Try {
        name?.let { constraint.name = it }
        _constraints.add(LinearConstraint(constraint, tokens))
        return ok
    }

    fun generateFeasibleCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolution: Solution,
    ): List<LinearInequality> {
        val rhs = MutableLinearPolynomial(constraints.foldIndexed(Flt64.zero) { i, acc, constraint ->
            acc + dualSolution[i] * constraint.rhs
        })
        for ((i, constraint) in constraints.withIndex()) {
            if (dualSolution[i] eq Flt64.zero) {
                continue
            }

            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedVariables) {
                    val coefficient = dualSolution[i] * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        rhs -= coefficient * variable
                    }
                }
            }
        }
        return listOf((objectVariable geq rhs).normalize())
    }

    fun generateInfeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolution: Solution,
    ): List<LinearInequality> {
        var value = Flt64.zero
        val lhs = MutableLinearPolynomial()
        for ((i, constraint) in constraints.withIndex()) {
            if (farkasDualSolution[i] eq Flt64.zero) {
                continue
            }

            value += farkasDualSolution[i] * constraint.rhs
            lhs += farkasDualSolution[i] * constraint.rhs
            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedVariables) {
                    val coefficient = farkasDualSolution[i] * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        lhs -= coefficient * variable
                    }
                    value -= farkasDualSolution[i] * cell.coefficient * fixedVariables[variable]!!
                }
            }
        }
        if (value ls Flt64.zero) {
            logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
            lhs *= -Flt64.one
        }
        return listOf((lhs leq Flt64.zero).normalize())
    }

    override fun toString(): String {
        return name
    }
}

class QuadraticMechanismModel(
    internal val parent: QuadraticMetaModel,
    override var name: String,
    private val _constraints: MutableList<QuadraticConstraint>,
    override val objectFunction: SingleObject,
    override val tokens: AbstractTokenTable
) : AbstractQuadraticMechanismModel, SingleObjectMechanismModel {
    companion object {
        private val logger = logger()

        suspend operator fun invoke(
            metaModel: QuadraticMetaModel,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: MutableMap<AbstractVariableItem<*, *>, Flt64>? = null,
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
                    metaModel,
                    metaModel.name,
                    metaModel._constraints.map {
                        QuadraticConstraint(it, tokens)
                    }.toMutableList(),
                    SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        QuadraticSubObject(
                            it.category,
                            it.polynomial,
                            tokens,
                            it.name
                        )
                    }),
                    tokens
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
                                    tokens = tokens
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
                                tokens = tokens
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
                metaModel,
                metaModel.name,
                constraints.flatMap { it.await() }.toMutableList(),
                SingleObject(metaModel.objectCategory, subObjects.flatMap { it.await() }),
                tokens
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

    internal val concurrent by parent::concurrent
    override val constraints by ::_constraints

    override fun addConstraint(
        constraint: QuadraticInequality,
        name: String?
    ): Try {
        name?.let { constraint.name = it }
        _constraints.add(QuadraticConstraint(constraint, tokens))
        return ok
    }
}
