/** 约束规划模型注册扩展点 / Constraint-programming model registration extension point */
package fuookami.ospf.kotlin.core.model.constraint_programming

import fuookami.ospf.kotlin.core.model.mechanism.MetaConstraintGroup

/**
 * 可注册约束组的模型能力。该接口不限定模型内部约束表示，使 MetaModel 与后续 ConstraintProgrammingModel / Model capability for registering constraint groups.
 * 可以共享 framework 的 Pipeline 注册入口。 / The interface does not constrain the internal constraint representation, so MetaModel and the future ConstraintProgrammingModel can share the framework Pipeline registration entry point.
 */
interface ConstraintGroupRegistry {
    /**
     * 注册约束组，后续约束归属由具体模型定义。 / Register a constraint group; the model defines how subsequent constraints are associated.
     *
     * @param group 要注册的约束组 / The constraint group to register
     * @return 无返回值 / No return value
     */
    fun registerConstraintGroup(group: MetaConstraintGroup)
}
