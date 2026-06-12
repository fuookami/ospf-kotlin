package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.AbstractCsp1dShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.AbstractCsp1dShadowPriceArguments
import fuookami.ospf.kotlin.framework.model.ShadowPrice

/**
 * CSP1D 影子价格生命周期 / CSP1D shadow price lifecycle
 *
 * 统一管理约束名到影子价格键的注册、LP 对偶值提取、框架 AbstractShadowPriceMap 填充
 * 和轻量级 ShadowPriceMap 转换，替代 application 层手动遍历 dualSolution 拼 map 的方式。
 *
 * Unified management of constraint-name-to-shadow-price-key registration, LP dual value
 * extraction, framework AbstractShadowPriceMap population and lightweight ShadowPriceMap
 * conversion, replacing the manual dualSolution iteration pattern in the application layer.
 *
 * 当前实现保留基于约束名的注册机制作为兼容 fallback，
 * 后续约束管线迁移至 CGPipeline 接口后可逐步替换为 constraint.args-based 提取。
 *
 * The current implementation retains the constraint-name-based registration mechanism as
 * a compatible fallback. Once constraint pipelines migrate to the CGPipeline interface,
 * constraint.args-based extraction can replace the name-based lookup incrementally.
 *
 * @param V 数值类型 / Numeric value type
 * @property vSample V 样本值，用于 Flt64 → V 显式转换 / V sample value for explicit Flt64 → V conversion
 */
class Csp1dShadowPriceLifecycle<V : RealNumber<V>>(
    private val vSample: V
) {
    /**
     * 约束名 → 影子价格键注册表 / Constraint name → shadow price key registry
     *
     * 由 DemandConstraintPipeline、MaterialConstraintPipeline、MachineConstraintPipeline
     * 在 register 阶段写入，extract 阶段读取。
     */
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
     * LP 对偶值是 Flt64，通过 solverValueLike 显式转换为 V，禁止 `dualValue as? V`。
     * LP dual values are Flt64; explicit conversion to V via solverValueLike is required.
     * Casting like `dualValue as? V` is forbidden.
     *
     * @param dualSolution LP 对偶解 / LP dual solution
     * @return 轻量级影子价格映射（pricing 消费） / Lightweight shadow price map (for pricing consumption)
     */
    fun extractFromDualSolution(
        dualSolution: Map<Constraint<Flt64, Linear>, Flt64>
    ): ShadowPriceMap<V> {
        val prices = HashMap<Csp1dShadowPriceKey, V>()
        for ((constraint, dualValue) in dualSolution) {
            val key = registry[constraint.name] ?: continue
            val vDual = solverValueLike(vSample, dualValue)
            // 填充框架 AbstractShadowPriceMap / Populate framework AbstractShadowPriceMap
            frameworkShadowPriceMap[key] = ShadowPrice(key, dualValue)
            // 填充轻量级 map / Populate lightweight map
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
fun <V : RealNumber<V>> Csp1dShadowPriceLifecycle<V>.extractFromDualSolutionCompat(
    dualSolution: Map<Constraint<Flt64, Linear>, Flt64>,
    @Suppress("UNUSED_PARAMETER") shadowPriceKeys: Map<String, Csp1dShadowPriceKey>
): ShadowPriceMap<V> {
    // lifecycle 自身已持有 registry，忽略外部传入的 shadowPriceKeys 参数
    // lifecycle already holds registry; the externally passed shadowPriceKeys parameter is ignored
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
