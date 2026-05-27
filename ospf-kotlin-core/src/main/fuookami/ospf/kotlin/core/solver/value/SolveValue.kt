/**
 * 求解值转换策略
 * Solve value conversion policy
 */
package fuookami.ospf.kotlin.core.solver.value

/**
 * 求解值转换策略枚举，控制 Flt64 到 Double 的转换行为。
 * Solve value conversion policy enum, controlling Flt64 to Double conversion behavior.
 */
enum class SolveValueConversionPolicy {
    Strict,
    AllowRounding
}
