package fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration

import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.*

/** 按生态位构建小动作集，不混用连续与排列编码 / Build a small niche-based action set without mixing continuous and permutation encodings. */
object OrthogonalOperatorPortfolio {
    /** 连续组合：分布、自适应差分、围捕、重尾/对立、局部精调 / Continuous niches: distribution, adaptive DE, encircling, heavy-tail/opposition, intensification.
     * @param size 种群规模 / Population size
     * @return 五个生态位的配置 / Configuration for five niches
     */
    fun continuous(size: Int = 20): Ret<List<AlgorithmConfig>> {
        if (size < 4) return searchInvalid("orthogonal continuous portfolio requires at least four individuals")
        val selector = ComponentSpec("frr-mab")
        return Ok(listOf(
            AlgorithmConfig(
                id = "distribution", algorithm = ComponentSpec("cma-es", buildJsonObject { put("restartOnImport", true) }), size = size
            ),
            AlgorithmConfig(id = "adaptive-difference", algorithm = ComponentSpec("l-shade"), size = size),
            AlgorithmConfig(
                id = "encircling", algorithm = ComponentSpec("gwo"), size = size,
                updates = listOf(ComponentSpec("hho"), ComponentSpec("gwo")),
                mutations = listOf(ComponentSpec("none")), selection = selector
            ),
            AlgorithmConfig(
                id = "escape", algorithm = ComponentSpec("genetic"), size = size,
                updates = listOf(ComponentSpec("opposition"), ComponentSpec("de")),
                mutations = listOf(ComponentSpec("levy")), selection = selector
            ),
            AlgorithmConfig(
                id = "intensification", algorithm = ComponentSpec("genetic"), size = 1,
                updates = listOf(ComponentSpec("pattern-search")), mutations = listOf(ComponentSpec("none"))
            )
        ))
    }

    /** 排列组合，仅保留保序交叉与两种正交邻域 / Permutation portfolio retaining order crossover and two orthogonal mutations.
     * @param size 种群规模 / Population size
     * @return 排列配置，初始化仍需业务模型生成合法排列 / Permutation configuration; the domain model must initialize valid permutations
     */
    fun permutation(size: Int = 20): Ret<List<AlgorithmConfig>> {
        if (size <= 0) return searchInvalid("invalid permutation portfolio size")
        return Ok(listOf(AlgorithmConfig(
            id = "permutation", algorithm = ComponentSpec("genetic"), size = size,
            updates = listOf(ComponentSpec("order")),
            mutations = listOf(ComponentSpec("inversion"), ComponentSpec("swap")),
            selection = ComponentSpec("frr-mab")
        )))
    }
}
