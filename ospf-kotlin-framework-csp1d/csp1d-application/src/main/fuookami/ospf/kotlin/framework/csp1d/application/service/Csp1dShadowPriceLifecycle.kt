package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.extractShadowPrice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.AbstractCsp1dShadowPriceArguments
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.AbstractCsp1dShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dCGPipelineList
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.toShadowPriceMap

/**
 * CSP1D 影子价格生命周期 / CSP1D shadow price lifecycle
 *
 * 统一管理 LP 对偶值提取、框架 AbstractShadowPriceMap 填充
 * 和轻量级 ShadowPriceMap 转换。
 *
 * 优先通过 CGPipeline refresh / extractor 机制提取影子价格，
 * 这是主路径。constraint-name registry 作为兼容 fallback 保留，
 * 用于非 CGPipeline 约束（如扩展管线注入的普通 Pipeline）。
 *
 * Unified management of LP dual value extraction, framework AbstractShadowPriceMap
 * population and lightweight ShadowPriceMap conversion.
 *
 * Prioritizes CGPipeline refresh / extractor mechanism for shadow price extraction,
 * which is the primary path. The constraint-name registry is retained as a compatible
 * fallback for non-CGPipeline constraints (e.g. extension pipelines that are plain Pipeline).
 *
 * @param V 数值类型 / Numeric value type
 * @property vSample V 样本值，用于 Flt64 → V 显式转换 / V sample value for explicit Flt64 → V conversion
 * @property cgPipelines 列生成管线列表，主路径提取影子价格 / CG pipeline list, primary path for shadow price extraction
 */
class Csp1dShadowPriceLifecycle<V : RealNumber<V>>(
    private val vSample: V,
    private val cgPipelines: Csp1dCGPipelineList = emptyList()
) {
    /**
     * 约束名 → 影子价格键注册表（兼容 fallback）/ Constraint name → shadow price key registry (compatible fallback)
     *
     * 用于非 CGPipeline 约束的影子价格提取。CGPipeline 约束通过 constraint.args
     * 自动关联，不需要此 registry。
     *
     * Used for shadow price extraction of non-CGPipeline constraints. CGPipeline constraints
     * are automatically associated via constraint.args and do not need this registry.
     */
    @Deprecated(
        message = "Prefer CGPipeline refresh / extractor mechanism; this registry is only for non-CGPipeline constraints",
        replaceWith = ReplaceWith("cgPipelines refresh")
    )
    val registry: MutableMap<String, Csp1dShadowPriceKey> = LinkedHashMap()

    /**
     * 框架兼容影子价格映射 / Framework-compatible shadow price map
     *
     * 在 extract 阶段填充，可供 CGPipeline 体系或 extractor 消费。
     */
    val frameworkShadowPriceMap: AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments> =
        Csp1dDefaultShadowPriceMap()

    /**
     * 从 LP 对偶解提取影子价格并填充两级映射 / Extract shadow prices from LP dual solution and populate both maps
     *
     * 优先使用 CGPipeline refresh / extractor 机制（主路径），
     * 然后用 constraint-name registry 补充非 CG 约束的影子价格（fallback）。
     *
     * LP 对偶值是 Flt64，通过 solverValueLike 显式转换为 V，禁止直接把 dualValue 强转为 V。
     * LP dual values are Flt64; explicit conversion to V via solverValueLike is required.
     * Directly casting dualValue to V is forbidden.
     *
     * @param model 元模型 / Meta model
     * @param dualSolution LP 对偶解 / LP dual solution
     * @return 轻量级影子价格映射（pricing 消费） / Lightweight shadow price map (for pricing consumption)
     */
    fun extractFromDualSolution(
        model: AbstractLinearMetaModel<Flt64>,
        dualSolution: Map<Constraint<Flt64, Linear>, Flt64>
    ): ShadowPriceMap<V> {
        // 主路径：通过 CGPipeline refresh / extractor 提取
        // Primary path: extract via CGPipeline refresh / extractor
        if (cgPipelines.isNotEmpty()) {
            when (val result = extractShadowPrice(
                shadowPriceMap = frameworkShadowPriceMap,
                pipelineList = cgPipelines,
                model = model,
                shadowPrices = dualSolution
            )) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> {}
                is fuookami.ospf.kotlin.utils.functional.Failed -> {
                    // CGPipeline 提取失败，回退到 registry
                    // CGPipeline extraction failed, fall back to registry
                }
                is fuookami.ospf.kotlin.utils.functional.Fatal -> {
                    // CGPipeline 提取致命错误，回退到 registry
                    // CGPipeline extraction fatal, fall back to registry
                }
            }
        }

        // Fallback：用 constraint-name registry 补充非 CG 约束的影子价格
        // Fallback: supplement non-CG constraint shadow prices via constraint-name registry
        @Suppress("DEPRECATION")
        if (registry.isNotEmpty()) {
            val metaDual = dualSolution.toMeta()
            for ((constraint, dualValue) in metaDual.constraints) {
                val constraintName = when (constraint) {
                    is LinearInequalityConstraint<*> -> constraint.constraintName
                    is QuadraticInequalityConstraint<*> -> constraint.constraintName
                    else -> continue
                }
                val key = registry[constraintName] ?: continue
                // 只补充 CGPipeline 未覆盖的 key
                // Only supplement keys not already covered by CGPipeline
                if (frameworkShadowPriceMap[key] == null) {
                    frameworkShadowPriceMap.put(ShadowPrice(key, dualValue))
                }
            }
        }

        // 从 framework map 转换为轻量级 ShadowPriceMap<V>
        // Convert from framework map to lightweight ShadowPriceMap<V>
        return frameworkShadowPriceMap.toShadowPriceMap { value -> solverValueLike(vSample, value) }
    }

    /**
     * 兼容旧签名：从 LP 对偶解提取影子价格（不传 model）
     *
     * 当无法提供 model 时使用 registry fallback。
     * 优先使用带 model 参数的重载版本。
     *
     * @param dualSolution LP 对偶解 / LP dual solution
     * @return 轻量级影子价格映射 / Lightweight shadow price map
     */
    fun extractFromDualSolution(
        dualSolution: Map<Constraint<Flt64, Linear>, Flt64>
    ): ShadowPriceMap<V> {
        @Suppress("DEPRECATION")
        if (registry.isEmpty() && cgPipelines.isEmpty()) {
            return ShadowPriceMap(emptyMap())
        }

        // 无 model 时只能用 registry fallback
        @Suppress("DEPRECATION")
        val prices = HashMap<Csp1dShadowPriceKey, V>()
        for ((constraint, dualValue) in dualSolution) {
            val key = registry[constraint.name] ?: continue
            val vDual = solverValueLike(vSample, dualValue)
            frameworkShadowPriceMap.put(ShadowPrice(key, dualValue))
            val existingValue = prices[key]
            prices[key] = if (existingValue != null) existingValue + vDual else vDual
        }
        return ShadowPriceMap(prices)
    }

    /**
     * Flt64 → V 显式转换 / Explicit Flt64 → V conversion
     */
    fun convertDualValue(dualValue: Flt64): V = solverValueLike(vSample, dualValue)
}

/**
 * CSP1D 默认影子价格映射 / CSP1D default shadow price map
 *
 * 框架 AbstractShadowPriceMap 的具体实现，用于统一承载 LP 对偶值。
 * Concrete implementation of framework AbstractShadowPriceMap for unified LP dual value hosting.
 */
class Csp1dDefaultShadowPriceMap
    : AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>()

/**
 * 从 ProduceContext 的 extractShadowPriceMap 委托到 lifecycle / Delegate from ProduceContext extractShadowPriceMap to lifecycle
 *
 * 为兼容已有 extractShadowPriceMap 签名提供的桥接方法。
 * Bridge method to maintain backward compatibility with existing extractShadowPriceMap signature.
 */
@Suppress("DEPRECATION")
fun <V : RealNumber<V>> Csp1dShadowPriceLifecycle<V>.extractFromDualSolutionCompat(
    dualSolution: Map<Constraint<Flt64, Linear>, Flt64>,
    @Suppress("UNUSED_PARAMETER") shadowPriceKeys: Map<String, Csp1dShadowPriceKey>
): ShadowPriceMap<V> {
    // lifecycle 自身已持有 registry 和 cgPipelines，忽略外部传入的 shadowPriceKeys 参数
    // lifecycle already holds registry and cgPipelines; the externally passed shadowPriceKeys parameter is ignored
    return extractFromDualSolution(dualSolution)
}

@Suppress("UNCHECKED_CAST")
private fun <V : RealNumber<V>> solverValueLike(sample: V, value: Flt64): V {
    return when (sample) {
        is Flt64 -> value as V
        is fuookami.ospf.kotlin.math.algebra.number.FltX -> value.toFltX() as V
        else -> throw IllegalArgumentException("Unsupported RealNumber type: ${sample::class}")
    }
}
