package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*

data class Parameter(
    // 业务目标参数：重心优化上下文
    val macRangeC: Flt64,
    val longitudinalBalance: Flt64,
    val B737LongitudinalBalance: Flt64,
    val lateralBalance: Flt64,
    val horizontalStabilizerWarn: Flt64,
    // 业务目标参数：软性安全上下文
    val ballastWeight: Flt64,
    val emptyHated: Flt64,
    val besideDoorMainPosition: Flt64,
    val dividedEmpty: Flt64,
    // 业务目标参数：装卸效率上下文,
    val adviceLoadAmount: Flt64,
    val adviceLoadWeight: Flt64,
    val sameFlowTransferIn: Flt64,
    val sameFlowTransferOut: Flt64,
    val itemOrder: Flt64,
    val trailerChange: Flt64,
    val trailerCircling: Flt64,
    // 业务目标参数：货物时效上下文
    val priority: Flt64,
    val priorityCategory: Flt64,
    // 业务目标参数：余度上下文
    val experimentalLongitudinalBalance: Flt64,
    val redundancyRange: Flt64,
) {
    companion object {
        operator fun invoke(
            // 业务目标参数：重心优化上下文
            macRangeC: Flt64? = null,
            longitudinalBalance: Flt64? = null,
            B737LongitudinalBalance: Flt64? = null,
            lateralBalance: Flt64? = null,
            horizontalStabilizerWarn: Flt64? = null,
            // 业务目标参数：软性安全上下文
            ballastWeight: Flt64? = null,
            emptyHated: Flt64? = null,
            besideDoorMainPosition: Flt64? = null,
            dividedEmpty: Flt64? = null,
            // 业务目标参数：装卸效率上下文
            adviceLoadAmount: Flt64? = null,
            adviceLoadWeight: Flt64? = null,
            sameFlowTransferIn: Flt64? = null,
            sameFlowTransferOut: Flt64? = null,
            itemOrder: Flt64? = null,
            trailerChange: Flt64? = null,
            trailerCircling: Flt64? = null,
            // 业务目标参数：货物时效上下文
            priority: Flt64? = null,
            priorityCategory: Flt64? = null,
            // 业务目标参数：余度上下文
            experimentalLongitudinalBalance: Flt64? = null,
            redundancyRange: Flt64? = null,
        ): Parameter {
            TODO("not implemented yet")
        }
    }
}

