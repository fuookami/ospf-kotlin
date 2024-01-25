package fuookami.ospf.kotlin.core.frontend.model.mechanism

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

            val constraints = metaModel.constraints.mapParallelly { LinearConstraint(it, tokens) }
            val subObjects = metaModel.subObjects.mapParallelly {
                LinearSubObject(
                    it.category,
                    it.polynomial,
                    tokens,
                    it.name
                )
            }

            return Ok(
                LinearModel(
                    metaModel,
                    metaModel.name,
                    constraints.toMutableList(),
                    SingleObject(metaModel.objectCategory, subObjects),
                    tokens
                )
            )
        }

        private fun unfold(tokens: LinearMutableTokenTable): Ret<LinearTokenTable> {
            val temp = tokens.copy()
            for (symbol in temp.symbols) {
                if (symbol is LinearFunctionSymbol) {
                    when (val result = symbol.register(temp)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                    symbol.cells
                } else {
                    symbol.cells
                }
            }
            return Ok(LinearTokenTable(temp))
        }
    }

    override val constraints by ::_constraints

    init {
        for (symbol in tokens.symbols) {
            if (symbol is LinearFunctionSymbol) {
                symbol.register(this)
            }
        }
    }

    override fun addConstraint(constraint: Inequality<LinearMonomialCell, Linear>, name: String) {
        constraint.name = name
        constraints.add(LinearConstraint(constraint, tokens))
    }
}

class MultiObjectLinearModel {

}
