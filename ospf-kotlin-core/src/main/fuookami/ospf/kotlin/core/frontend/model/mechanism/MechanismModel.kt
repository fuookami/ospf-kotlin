package fuookami.ospf.kotlin.core.frontend.model.mechanism

import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticConstraint

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
    companion object {
        private val logger = logger()

        suspend operator fun invoke(
            metaModel: LinearMetaModel,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null
        ): Ret<LinearMechanismModel> {
            logger.info { "Creating LinearMechanismModel for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(metaModel.tokens, registrationStatusCallBack)) {
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
                (symbol as? LinearFunctionSymbol)?.register(model)
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
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTable -> {
                    val temp = tokens.copy() as MutableTokenTable
                    when (val result = tokens.symbols.register(temp, callBack)) {
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
                    when (val result = tokens.symbols.register(temp, callBack)) {
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
            registrationStatusCallBack: RegistrationStatusCallBack? = null
        ): Ret<QuadraticMechanismModel> {
            logger.info { "Creating QuadraticMechanismModel for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(metaModel.tokens, registrationStatusCallBack)) {
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
                (symbol as? LinearFunctionSymbol)?.register(model)
                (symbol as? QuadraticFunctionSymbol)?.register(model)
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
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTable -> {
                    val temp = tokens.copy() as MutableTokenTable
                    when (val result = tokens.symbols.register(temp, callBack)) {
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
                    when (val result = tokens.symbols.register(temp, callBack)) {
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
