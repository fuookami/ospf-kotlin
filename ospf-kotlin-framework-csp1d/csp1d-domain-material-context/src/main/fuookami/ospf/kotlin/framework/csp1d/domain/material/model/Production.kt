package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.utils.math.*

interface Production {
    val id: String?
    val width: List<Flt64>
    val length: FltX?
    val unitWeight: FltX?
}
