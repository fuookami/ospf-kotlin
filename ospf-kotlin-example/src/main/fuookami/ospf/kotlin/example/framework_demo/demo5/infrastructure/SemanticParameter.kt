package fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * demo5 语义参数。 / demo5 semantic parameters.
 *
 * 封装 demo5 运行所需的全部策略和限制参数。 / Encapsulates all policy and limit parameters needed for a demo5 run.
 *
 * @property instanceName 实例名称 / Instance name
 * @property customerCount 客户数量 / Customer count
 * @property costPolicy 成本策略标识（"demo17" 或 "solomon"） / Cost policy identifier ("demo17" or "solomon")
 * @property timeLimit 时间上限 / Time limit
 * @property nodeLimit 节点上限 / Node limit
 * @property relativeGapTolerance 相对 gap 容差 / Relative gap tolerance
 * @property maxColumnsPerPricing 每次定价最大列数 / Max columns per pricing
 * @property maxCGIterationsPerNode 每节点最大 CG 迭代次数 / Max CG iterations per node
 * @property solver 求解器名称 / Solver name
 */
data class SemanticParameter(
    val instanceName: String = "demo17-25",
    val customerCount: Int = 25,
    val costPolicy: String = "demo17",
    val timeLimit: Duration = Duration.INFINITE,
    val nodeLimit: Int = Int.MAX_VALUE,
    val relativeGapTolerance: Flt64 = Flt64(1e-4),
    val maxColumnsPerPricing: Int = Int.MAX_VALUE,
    val maxCGIterationsPerNode: Int = 1000,
    val solver: String = System.getProperty("ospf.demo5.solver", "gurobi")
)
