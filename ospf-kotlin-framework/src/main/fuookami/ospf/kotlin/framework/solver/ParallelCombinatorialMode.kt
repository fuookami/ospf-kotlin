/**
 * 并行组合求解模式
 * Parallel combinatorial solving mode
 *
 * 控制并行组合求解器如何处理多个求解器的结果。
 * Controls how parallel combinatorial solvers handle results from multiple solvers.
*/
package fuookami.ospf.kotlin.framework.solver

/**
 * 并行组合求解模式
 * Parallel combinatorial solving mode
*/
enum class ParallelCombinatorialMode {
    /**
     * 取第一个成功结果
     * Take the first successful result
    */
    First,
    /**
     * 取最优结果
     * Take the best result
    */
    Best
}