package fuookami.ospf.kotlin.core.frontend.model.mechanism

import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*

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

        suspend operator fun invoke(metaModel: LinearMetaModel, concurrent: Boolean? = null): Ret<LinearMechanismModel> {
            logger.info { "Creating LinearMechanismModel for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(metaModel.tokens)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (concurrent ?: metaModel.concurrent) {
                coroutineScope {
                    val constraints = metaModel._constraints.map {
                        async(Dispatchers.Default) {
                            LinearConstraint(it, tokens)
                        }
                    }
                    val subObjects = metaModel._subObjects.map {
                        async(Dispatchers.Default) {
                            LinearSubObject(
                                it.category,
                                it.polynomial,
                                tokens,
                                it.name
                            )
                        }
                    }

                    LinearMechanismModel(
                        metaModel,
                        metaModel.name,
                        constraints.map { it.await() }.toMutableList(),
                        SingleObject(metaModel.objectCategory, subObjects.map { it.await() }),
                        tokens
                    )
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

            logger.trace { "Registering symbols for $metaModel" }
            for (symbol in tokens.symbols) {
                (symbol as? LinearFunctionSymbol)?.register(model)
            }
            logger.trace { "Symbols registered for $metaModel" }

            logger.info { "LinearMechanismModel created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        private suspend fun unfold(tokens: AbstractMutableTokenTable): Ret<AbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTable -> {
                   val temp = tokens.copy() as MutableTokenTable
                    when (val result = tokens.symbols.register(temp)) {
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
                    when (val result = tokens.symbols.register(temp)) {
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

        suspend operator fun invoke(metaModel: QuadraticMetaModel, concurrent: Boolean? = null): Ret<QuadraticMechanismModel> {
            logger.info { "Creating QuadraticMechanismModel for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(metaModel.tokens)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (concurrent ?: metaModel.concurrent) {
                coroutineScope {
                    val constraints = metaModel._constraints.map {
                        async(Dispatchers.Default) {
                            QuadraticConstraint(it, tokens)
                        }
                    }
                    val subObjects = metaModel._subObjects.map {
                        async(Dispatchers.Default) {
                            QuadraticSubObject(
                                it.category,
                                it.polynomial,
                                tokens,
                                it.name
                            )
                        }
                    }

                    QuadraticMechanismModel(
                        metaModel,
                        metaModel.name,
                        constraints.map { it.await() }.toMutableList(),
                        SingleObject(metaModel.objectCategory, subObjects.map { it.await() }),
                        tokens
                    )
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

        private suspend fun unfold(tokens: AbstractMutableTokenTable): Ret<AbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTable -> {
                    val temp = tokens.copy() as MutableTokenTable
                    when (val result = tokens.symbols.register(temp)) {
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
                    when (val result = tokens.symbols.register(temp)) {
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
