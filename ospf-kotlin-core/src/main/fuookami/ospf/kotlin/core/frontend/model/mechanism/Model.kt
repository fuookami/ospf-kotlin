package fuookami.ospf.kotlin.core.frontend.model.mechanism

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*

sealed interface Model<C : Category> {
    val name: String
    val constraints: List<Constraint<C>>
    val objectFunction: Object<C>
    val tokens: TokenTable<C>

    fun addConstraint(constraint: Inequality<Linear>, name: String = "")
}

class LinearModel(
    private val parent: LinearMetaModel,
    override var name: String,
    override val constraints: ArrayList<LinearConstraint>,
    override val objectFunction: SingleObject<Linear>,
    override val tokens: TokenTable<Linear>
) : Model<Linear> {
    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        operator fun invoke(metaModel: LinearMetaModel): LinearModel {
            for (symbol in metaModel.tokens.symbols) {
                symbol.cells
            }

            val (constraints, subObjects) = if (metaModel.constraints.size > 100) {
                val constraintPromises = ArrayList<Channel<List<LinearConstraint>>>()
                val eighthLength = metaModel.constraints.size / 8 + 1
                for (i in 0 until 8) {
                    val promise = Channel<List<LinearConstraint>>(Channel.CONFLATED)
                    GlobalScope.launch {
                        val constraints = ArrayList<LinearConstraint>()
                        for (j in (i * eighthLength) until minOf((i + 1) * eighthLength, metaModel.constraints.size)) {
                            constraints.add(LinearConstraint(metaModel, metaModel.constraints[j], metaModel.tokens))
                        }
                        promise.send(constraints)
                    }
                    constraintPromises.add(promise)
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
                    constraints.addAll(runBlocking { promise.receive() })
                }
                Pair(constraints, subObjects)
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
