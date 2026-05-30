package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX

/** 生产物料接口，定义物料的基本属性 / Production material interface defining basic material properties */
interface Production {
    /** 物料标识 / Material identifier */
    val id: String?
    /** 物料宽度列表 / List of material widths */
    val width: List<Flt64>
    /** 物料长度 / Material length */
    val length: FltX?
    /** 单位重量 / Unit weight */
    val unitWeight: FltX?
}


