package fuookami.ospf.kotlin.core.frontend.model.mechanism

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*

sealed interface Model<C : Category> {
    val name: String
    val constraints: List<Constraint<C>>
    val objectFunction: Object<C>
    val tokens: TokenTable<C>

    fun addConstraint(constraint: Inequality<Linear>, name: String = "")
}

interface SingleObjectModel<C : Category> : Model<C> {
    override val objectFunction: SingleObject<C>
}

class LinearModel(
    private val parent: LinearMetaModel,
    override var name: String,
    override val constraints: ArrayList<LinearConstraint>,
    override val objectFunction: SingleObject<Linear>,
    override val tokens: TokenTable<Linear>
) : SingleObjectModel<Linear> {
    companion object {
        private val segmentAmount = 8

        suspend operator fun invoke(metaModel: LinearMetaModel): LinearModel {
            for (symbol in metaModel.tokens.symbols) {
                symbol.cells
            }

            val (constraints, subObjects) = if (metaModel.constraints.size > 100) {
                coroutineScope {
                    val constraintPromises = ArrayList<Deferred<List<LinearConstraint>>>()
                    val segmentSize = metaModel.constraints.size / segmentAmount
                    for (i in 0 until segmentAmount) {
                        val lb = i * segmentSize
                        val ub = if (i == segmentAmount - 1) {
                            metaModel.constraints.size
                        } else {
                            (i + 1) * segmentSize
                        }
                        constraintPromises.add(async(Dispatchers.Default) {
                            metaModel.constraints.subList(lb, ub).map { LinearConstraint(metaModel, it, metaModel.tokens) }
                        })
                    }

                    val subObjects = ArrayList<LinearSubObject>()
                    for (subObject in metaModel.subObjects) {
                        subObjects.add(
                            LinearSubObject(
                                metaModel,
                                subObject.category,
                                subObject.polynomial,
                                metaModel.tokens,
                                subObject.name
                            )
                        )
                    }

                    val constraints = ArrayList<LinearConstraint>()
                    for (promise in constraintPromises) {
                        constraints.addAll(promise.await())
                    }
                    return@coroutineScope Pair(constraints, subObjects)
                }
            } else {
                val constraints = ArrayList<LinearConstraint>()
                for (constraint in metaModel.constraints) {
                    constraints.add(LinearConstraint(metaModel, constraint, metaModel.tokens))
                }

                val subObjects = ArrayList<LinearSubObject>()
                for (subObject in metaModel.subObjects) {
                    subObjects.add(
                        LinearSubObject(
                            metaModel,
                            subObject.category,
                            subObject.polynomial,
                            metaModel.tokens,
                            subObject.name
                        )
                    )
                }
                Pair(constraints, subObjects)
            }

            return LinearModel(
                metaModel,
                metaModel.name,
                constraints,
                SingleObject(metaModel.objectCategory, subObjects),
                metaModel.tokens
            )
        }
    }

    init {
        for (symbol in tokens.symbols) {
            if (symbol is fuookami.ospf.kotlin.core.frontend.expression.symbol.Function) {
                symbol.register(this)
            }
        }
    }

    override fun addConstraint(constraint: Inequality<Linear>, name: String) {
        constraint.name = name
        constraints.add(LinearConstraint(parent, constraint, tokens))
    }
}

class MultiObjectLinearModel {

}
