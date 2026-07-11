package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

/**
 * 需求口径标签，仅用于决策/约束/分析标识 / Demand mode label for decision/constraint/analyzer semantics only
*/
enum class DemandMode {
    /** 卷数需求 / Roll demand */
    Roll,

    /** 重量需求 / Weight demand */
    Weight,

    /** 张数需求 / Sheet demand */
    Sheet
}
