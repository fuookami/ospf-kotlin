package fuookami.ospf.kotlin.core.frontend.model.mechanism

import kotlin.math.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*

sealed interface Model<Cell : MonomialCell<Cell, C>, C : Category> {
    val name: String
    val constraints: List<Constraint<C>>
    val objectFunction: Object<C>
    val tokens: TokenTable<Cell, C>

    fun addConstraint(constraint: Inequality<Cell, C>, name: String = "")
}

typealias AbstractLinearModel = Model<LinearMonomialCell, Linear>

interface SingleObjectModel<Cell : MonomialCell<Cell, C>, C : Category> : Model<Cell, C> {
    override val objectFunction: SingleObject<C>
}

typealias AbstractSingleObjectLinearModel = SingleObjectModel<LinearMonomialCell, Linear>

class LinearModel(
    private val parent: LinearMetaModel,
    override var name: String,
    private val _constraints: MutableList<LinearConstraint>,
    override val objectFunction: SingleObject<Linear>,
    override val tokens: LinearTokenTable
) : AbstractSingleObjectLinearModel {
    companion object {
        suspend operator fun invoke(metaModel: LinearMetaModel): Ret<LinearModel> {
            val tokens = when (val result = unfold(metaModel.tokens)) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }

            val model = coroutineScope {
                val constraints = metaModel.constraints.map {
                    async(Dispatchers.Default) {
                        LinearConstraint(it, tokens)
                    }
                }
                val subObjects = metaModel.subObjects.map {
                    async(Dispatchers.Default) {
                        LinearSubObject(
                            it.category,
                            it.polynomial,
                            tokens,
                            it.name
                        )
                    }
                }

                LinearModel(
                    metaModel,
                    metaModel.name,
                    constraints.map { it.await() }.toMutableList(),
                    SingleObject(metaModel.objectCategory, subObjects.map { it.await() }),
                    tokens
                )
            }


            for (symbol in tokens.symbols) {
                if (symbol is LinearFunctionSymbol) {
                    symbol.register(model)
                }
            }

            return Ok(model)
        }

        private suspend fun unfold(tokens: LinearMutableTokenTable): Ret<LinearTokenTable> {
            val temp = tokens.copy()
            for (symbol in temp.symbols) {
                if (symbol is LinearFunctionSymbol) {
                    when (val result = symbol.register(temp)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                }
            }

            val completedSymbols = HashSet<Symbol<*, *>>()
            var dependencies = tokens.symbols.associateWith { it.dependencies.toMutableSet() }.toMap()

            return coroutineScope {
                var readySymbols = dependencies.filter { it.value.isEmpty() }.keys
                dependencies = dependencies.filterValues { it.isNotEmpty() }.toMap()
                while (readySymbols.isNotEmpty()) {
                    val thisJobs = readySymbols.map {
                        launch(Dispatchers.Default) {
                            it.prepare()
                            it.cells
                        }
                    }
                    completedSymbols.addAll(readySymbols)
                    val newReadySymbols = dependencies.filter {
                        !completedSymbols.contains(it.key) && it.value.all { dependency ->
                            readySymbols.contains(
                                dependency
                            )
                        }
                    }.keys.toSet()
                    dependencies = dependencies.filter { !newReadySymbols.contains(it.key) }
                    for ((_, dependency) in dependencies) {
                        dependency.removeAll(readySymbols)
                    }
                    readySymbols = newReadySymbols
                    thisJobs.forEach { it.join() }
                }

                Ok(LinearTokenTable(temp))
            }
        }
    }

    override val constraints by ::_constraints

    override fun addConstraint(constraint: Inequality<LinearMonomialCell, Linear>, name: String) {
        constraint.name = name
        constraints.add(LinearConstraint(constraint, tokens))
    }
}

class MultiObjectLinearModel {

}
