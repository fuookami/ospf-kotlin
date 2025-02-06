package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

interface Capacity {
    val loadWeight: LinearSymbols1
    val loadVolume: LinearSymbols1
    val loadDepth: LinearSymbols1
    val tailLoadingRate: LinearSymbols1
}
