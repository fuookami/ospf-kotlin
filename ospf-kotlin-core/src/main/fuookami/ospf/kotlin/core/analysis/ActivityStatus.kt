/** Activity conclusions for the baseline assignment. / 基线赋值的活动性结论。 */
package fuookami.ospf.kotlin.core.analysis

/**
 * 原始模型成员的活动性状态。 / Activity state of an original model member.
 *
 * 违规状态与非活动状态明确区分：非活动成员满足约束且远离边界，违规成员则超出声明的关系或值域。
 * / A violation is kept distinct from an inactive member: an inactive member is satisfied and away
 * from its boundary, while a violated member is outside the declared relation or domain.
 */
enum class ActivityStatus {
    /** The boundary is reached within the configured tolerance. / 在配置容差内达到边界。 */
    Active,

    /** The boundary is close but outside the active tolerance. / 接近边界但超出活动容差。 */
    NearlyActive,

    /** The member is satisfied and has material distance from its boundary. / 满足约束且距离边界较远。 */
    Inactive,

    /** The member is satisfied, but no natural scalar slack exists. / 成员满足，但不存在自然标量松弛。 */
    SatisfiedWithoutSlackMetric,

    /** The member is violated by the supplied baseline assignment. / 基线赋值违反成员语义。 */
    Violated,

    /** The assignment was absent or could not be evaluated. / 缺少赋值或无法求值。 */
    Unknown
}
