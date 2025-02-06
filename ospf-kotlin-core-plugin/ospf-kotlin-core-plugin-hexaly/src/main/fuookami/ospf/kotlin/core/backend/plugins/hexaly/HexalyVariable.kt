package fuookami.ospf.kotlin.core.backend.plugins.hexaly

import com.hexaly.optimizer.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.core.frontend.variable.*

sealed interface HexalyVariable {
    companion object {
        operator fun invoke(model: HxModel, type: VariableType<*>, lb: Flt64, ub: Flt64): HexalyVariable {
            return when (type) {
                is fuookami.ospf.kotlin.core.frontend.variable.Binary -> {
                    Binary(model)
                }

                is Ternary, is BalancedTernary, is fuookami.ospf.kotlin.core.frontend.variable.Integer, is UInteger -> {
                    Integer(model, lb, ub)
                }

                is Percentage, is fuookami.ospf.kotlin.core.frontend.variable.Continuous, is UContinuous -> {
                    Continuous(model, lb, ub)
                }
            }
        }
    }

    fun toHexalyVariable(): HxExpression

    class Binary(private val model: HxModel) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.boolVar()
        }
    }

    class Integer(private val model: HxModel, private val lb: Flt64, private val ub: Flt64) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.intVar(lb.toDouble().toLong(), ub.toDouble().toLong())
        }
    }

    class Continuous(private val model: HxModel, private val lb: Flt64, private val ub: Flt64) : HexalyVariable {
        override fun toHexalyVariable(): HxExpression {
            return model.floatVar(lb.toDouble(), ub.toDouble())
        }
    }
}
