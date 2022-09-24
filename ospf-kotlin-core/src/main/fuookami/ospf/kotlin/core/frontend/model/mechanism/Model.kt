package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*

sealed interface Model<C: Category> {
    val name: String
    val constraints: List<Constraint<C>>
    val objectFunction: Object<C>
    val tokens: TokenTable<C>

    fun addConstraint(constraint: Inequality<Linear>, name: String = "")
}

class LinearModel(
    override var name: String,
    override val constraints: ArrayList<LinearConstraint>,
    override val objectFunction: SingleObject<Linear>,
    override val tokens: TokenTable<Linear>
) : Model<Linear> {

    companion object {
        operator fun invoke(metaModel: LinearMetaModel) : LinearModel {
            val constraints = ArrayList<LinearConstraint>()
            for (constraint in metaModel.constraints) {
                constraints.add(LinearConstraint(constraint, metaModel.tokens))
            }
            val subObjects = ArrayList<LinearSubObject>()
            for (subObject in metaModel.subObjects) {
                subObjects.add(LinearSubObject(subObject.category, subObject.polynomial, metaModel.tokens, subObject.name))
            }
            return LinearModel(metaModel.name, constraints, SingleObject(metaModel.objectCategory, subObjects), metaModel.tokens)
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
        constraints.add(LinearConstraint(constraint, tokens))
    }
}

class MultiObjectLinearModel {

}
