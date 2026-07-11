package fuookami.ospf.kotlin.framework.csp1d.application.service

/**
 * 最终 MILP 求解状态 / Final MILP solve status
*/
enum class Csp1dFinalMilpStatus {
    /** 尚未尝试最终 MILP / Final MILP is not attempted */
    NotAttempted,
    /** 最终 MILP 已求解 / Final MILP is solved */
    Solved,
    /** 最终 MILP 失败 / Final MILP failed */
    Failed
}
