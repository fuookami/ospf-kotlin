package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.model.*

/**
 * CSP1D 影子价格生命周期 / CSP1D shadow price lifecycle
 *
 * 统一管理 LP 对偶值提取、框架 AbstractShadowPriceMap 填充
 * 和轻量级 ShadowPriceMap 转换。
 *
 * 通过 CGPipeline refresh / extractor 机制提取影子价格。
 * 普通扩展若需要参与 pricing 影子价格，应建模为 CGPipeline。
 *
 * Unified management of LP dual value extraction, framework AbstractShadowPriceMap
 * population and lightweight ShadowPriceMap conversion.
 *
 * Extracts shadow prices through CGPipeline refresh / extractor mechanism.
 * Plain extensions that need pricing shadow prices should be modeled as CGPipeline.
 *
 * @param V 数值类型 / Numeric value type
 * @property domainValueSample 领域数值样本，用于 solver 值显式转换 / Domain value sample for explicit solver value conversion
 * @property cgPipelines 列生成管线列表，用于提取影子价格 / CG pipeline list for shadow price extraction
 */
class Csp1dShadowPriceLifecycle<V : RealNumber<V>>(
    private val domainValueSample: V,
    private val cgPipelines: List<CGPipeline<AbstractCsp1dShadowPriceArguments, AbstractLinearMetaModel<Flt64>, AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>>> = emptyList()
) {
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
     * 使用 CGPipeline refresh / extractor 机制提取。
     *
     * LP 对偶值是 Flt64，通过 convertSolverValue 显式转换为 V，禁止直接把 dualValue 强转为 V。
     * LP dual values are Flt64; explicit conversion to V via convertSolverValue is required.
     * Directly casting dualValue to V is forbidden.
     *
     * @param model 元模型 / Meta model
     * @param dualSolution LP 对偶解 / LP dual solution
     * @return 轻量级影子价格映射（pricing 消费） / Lightweight shadow price map (for pricing consumption)
     */
    fun extractFromDualSolution(
        model: AbstractLinearMetaModel<Flt64>,
        dualSolution: Map<Constraint<Flt64, Linear>, Flt64>
    ): Ret<ShadowPriceMap<V>> {
        // 通过 CGPipeline refresh / extractor 提取 / Extract via CGPipeline refresh / extractor
        if (cgPipelines.isNotEmpty()) {
            when (val result = extractShadowPrice(
                shadowPriceMap = frameworkShadowPriceMap,
                pipelineList = cgPipelines,
                model = model,
                shadowPrices = dualSolution
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        // 从 framework map 转换为轻量级 ShadowPriceMap<V>
        // Convert from framework map to lightweight ShadowPriceMap<V>
        return Ok(frameworkShadowPriceMap.toShadowPriceMap { value -> (convertSolverValue(domainValueSample, value) as Ok).value })
    }

    /**
     * 转换对偶值到领域数值 / Convert dual value to domain value
     */
    fun convertDualValue(dualValue: Flt64): V = (convertSolverValue(domainValueSample, dualValue) as Ok).value
}

/**
 * CSP1D 默认影子价格映射 / CSP1D default shadow price map
 *
 * 框架 AbstractShadowPriceMap 的具体实现，用于统一承载 LP 对偶值。
 * Concrete implementation of framework AbstractShadowPriceMap for unified LP dual value hosting.
 */
class Csp1dDefaultShadowPriceMap
    : AbstractCsp1dShadowPriceMap<AbstractCsp1dShadowPriceArguments>()
