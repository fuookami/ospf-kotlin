package fuookami.ospf.kotlin.example.framework_demo.demo5.infrastructure.dto

/**
 * demo5 输入 DTO。 / demo5 input DTO.
 *
 * @property instanceName 实例名称（"demo17-25"、"demo17-100" 或 Solomon 文件路径） / Instance name
 * @property customerCount 客户数量（仅 Demo17 适用） / Customer count (Demo17 only)
 * @property costPolicy 成本策略标识 / Cost policy identifier
 * @property timeLimitSeconds 时间上限（秒） / Time limit in seconds
 * @property nodeLimit 节点上限 / Node limit
 * @property relativeGapTolerance 相对 gap 容差 / Relative gap tolerance
 */
data class Input(
    val instanceName: String = "demo17-25",
    val customerCount: Int = 25,
    val costPolicy: String = "demo17",
    val timeLimitSeconds: Double = Double.POSITIVE_INFINITY,
    val nodeLimit: Int = Int.MAX_VALUE,
    val relativeGapTolerance: Double = 1e-4
)
