package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

interface Load {
    val load: LinearSymbols1
    val overLoad: LinearSymbols1
    val lessLoad: LinearSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean
}
