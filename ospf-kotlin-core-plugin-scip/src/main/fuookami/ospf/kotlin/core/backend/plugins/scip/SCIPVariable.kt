package fuookami.ospf.kotlin.core.backend.plugins.scip

import fuookami.ospf.kotlin.core.frontend.variable.*
import jscip.*;

enum class SCIPVariable {
    Binary {
        override fun toSCIPVar(): SCIP_Vartype = SCIP_Vartype.SCIP_VARTYPE_BINARY
    },
    Integer {
        override fun toSCIPVar(): SCIP_Vartype = SCIP_Vartype.SCIP_VARTYPE_INTEGER
    },
    Continuous {
        override fun toSCIPVar(): SCIP_Vartype = SCIP_Vartype.SCIP_VARTYPE_CONTINUOUS
    };

    companion object {
        operator fun invoke(type: VariableType<*>): SCIPVariable {
            return when(type) {
                is fuookami.ospf.kotlin.core.frontend.variable.Binary -> {
                    Binary
                }
                is Ternary, is BalancedTernary, is fuookami.ospf.kotlin.core.frontend.variable.Integer, is UInteger -> {
                    Integer
                }
                is Percentage, is fuookami.ospf.kotlin.core.frontend.variable.Continues, is UContinues -> {
                    Continuous
                }
            }
        }
    }

    abstract fun toSCIPVar(): SCIP_Vartype
}
