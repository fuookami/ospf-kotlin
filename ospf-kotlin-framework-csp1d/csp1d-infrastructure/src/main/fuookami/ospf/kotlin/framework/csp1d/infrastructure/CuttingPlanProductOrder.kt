package fuookami.ospf.kotlin.framework.csp1d.infrastructure

/**
 * Cutting plan product ordering strategy.
 * 中文切割方案产品排序方式
 */
enum class CuttingPlanProductOrder {
    /** Ascending order / 升序 */
    Asc,
    /** Descending order / 降序 */
    Desc,
    /** User-defined order / 用户自定义排序 */
    UserDefined
}
