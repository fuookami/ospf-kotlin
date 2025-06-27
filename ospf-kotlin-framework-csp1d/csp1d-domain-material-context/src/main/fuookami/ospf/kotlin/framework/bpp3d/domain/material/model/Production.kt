package fuookami.ospf.kotlin.framework.bpp3d.domain.material.model

import fuookami.ospf.kotlin.utils.math.*

interface Production {
    val id: String?
    val width: List<Flt64>
    val length: FltX?
    val unitWeight: FltX?
}
