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
    private val parent: LinearMetaModel,
    override var name: String,
    private val _constraints: MutableList<LinearConstraint>,
    override val objectFunction: SingleObject,
    override val tokens: TokenTable
) : AbstractLinearMechanismModel, SingleObjectMechanismModel {
    companion object {
        private val logger = logger()

        suspend operator fun invoke(metaModel: LinearMetaModel): Ret<LinearMechanismModel> {
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

            val model = coroutineScope {
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

            logger.trace { "Registering symbols for $metaModel" }
            for (symbol in tokens.symbols) {
                (symbol as? LinearFunctionSymbol)?.register(model)
            }
            logger.trace { "Symbols registered for $metaModel" }

            logger.info { "LinearMechanismModel created for $metaModel" }
            return Ok(model)
        }

        private suspend fun unfold(tokens: MutableTokenTable): Ret<TokenTable> {
            val temp = tokens.copy()
            return when (val result = tokens.symbols.register(temp)) {
                is Ok -> {
                    Ok(TokenTable(temp))
                }

                is Failed -> {
                    Failed(result.error)
                }
            }
        }
    }

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
    private val parent: QuadraticMetaModel,
    override var name: String,
    private val _constraints: MutableList<QuadraticConstraint>,
    override val objectFunction: SingleObject,
    override val tokens: TokenTable
) : AbstractQuadraticMechanismModel, SingleObjectMechanismModel {
    companion object {
        suspend operator fun invoke(metaMechanismModel: QuadraticMetaModel): Ret<QuadraticMechanismModel> {
            val tokens = when (val result = unfold(metaMechanismModel.tokens)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }

            val model = coroutineScope {
                val constraints = metaMechanismModel._constraints.map {
                    async(Dispatchers.Default) {
                        QuadraticConstraint(it, tokens)
                    }
                }
                val subObjects = metaMechanismModel._subObjects.map {
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
                    metaMechanismModel,
                    metaMechanismModel.name,
                    constraints.map { it.await() }.toMutableList(),
                    SingleObject(metaMechanismModel.objectCategory, subObjects.map { it.await() }),
                    tokens
                )
            }

            for (symbol in tokens.symbols) {
                (symbol as? LinearFunctionSymbol)?.register(model)
                (symbol as? QuadraticFunctionSymbol)?.register(model)
            }

            return Ok(model)
        }

        private suspend fun unfold(tokens: MutableTokenTable): Ret<TokenTable> {
            val temp = tokens.copy()
            return when (val result = tokens.symbols.register(temp)) {
                is Ok -> {
                    Ok(TokenTable(temp))
                }

                is Failed -> {
                    Failed(result.error)
                }
            }
        }
    }

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
