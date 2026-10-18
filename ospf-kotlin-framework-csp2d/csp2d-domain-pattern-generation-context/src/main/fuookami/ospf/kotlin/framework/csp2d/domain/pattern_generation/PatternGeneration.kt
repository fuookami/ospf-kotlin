/**
 * 裁切图案生成契约。 / Cutting pattern generation contracts.
 *
 * 生成器按**物料**工作：定尺模式在一块板材上排样，卷材模式在「幅宽 × 消耗段高」上排样。
 * 三维框架的「层生成」在这里对应「图案生成」，但断头台工艺下图案必须同时带刀序。
 * / Generators work per material: in sheet mode a pattern is laid out on one sheet, and in coil mode on
 * "width by consumed segment height". The 3D frameworks' "layer generation" corresponds to pattern generation here, but
 * under a guillotine process a pattern must also carry its cut sequence.
 *
 * @property V 数值类型 / numeric type
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.pattern_generation

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Costar2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Material2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Pattern2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Product2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dValidationError
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2

/**
 * 图案生成请求。 / A pattern generation request.
 *
 * @property V 数值类型 / numeric type
 * @property material 目标物料 / target material
 * @property products 可选产品及其剩余件数 / candidate products and their remaining piece counts
 * @property process 裁切工艺 / cutting process
 * @property kerf 刀缝宽度 / kerf width
 * @property costars 可填充的副产物 / by-products available for filling
 * @property maxSegmentHeight 卷材模式的最大段高，为空表示按需求推导 / maximum segment height in coil mode, null meaning
 * derived from the demand
 * @property maxCandidates 最大候选数 / maximum candidate count
 * @property iteration 迭代序号 / iteration index
 * @property existingPatterns 历史图案，用于回放 / historical patterns for replay
 * @property shadowPrices 产品编号到影子价格的映射（按面积计价）/ product identifier to shadow price, priced by area
 * @property wasteWeight 余料权重 / waste weight
*/
data class PatternGenerationRequest2<V : FloatingNumber<V>>(
    val material: Material2<V>,
    val products: List<Pair<Product2<V>, UInt64>>,
    val kerf: Quantity<V>,
    val process: CuttingProcess2 = CuttingProcess2.Freeform,
    val costars: List<Costar2<V>> = emptyList(),
    val maxSegmentHeight: Quantity<V>? = null,
    val maxCandidates: Int = 32,
    val iteration: Int = 0,
    val existingPatterns: List<Pattern2<V>> = emptyList(),
    val shadowPrices: Map<String, V> = emptyMap(),
    val wasteWeight: V? = null
) {

    /**
     * 校验请求。 / Validate the request.
     *
     * @return 校验结果 / validation result
    */
    fun validate(): Try {
        if (quantityLess(kerf, quantityZeroOf(material.width))) {
            return Failed(
                Csp2dValidationError(detail = "刀缝不能为负：$kerf / The kerf must not be negative")
            )
        }
        when (val result = material.validate()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return result
        }
        if (products.isEmpty()) {
            return Failed(
                Csp2dValidationError(detail = "图案生成请求没有可选产品 / The request has no candidate product")
            )
        }
        for ((product, amount) in products) {
            when (val result = product.validate()) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return result
            }
            if (amount == UInt64.zero) {
                return Failed(
                    Csp2dValidationError(detail = "产品 ${product.id} 的剩余件数为零 / The remaining amount is zero")
                )
            }
        }
        return ok
    }

    /**
     * 料域高度上界。 / The region height upper bound.
     *
     * 定尺模式直接取板高；卷材模式最坏情况是每件各占一行，故取「全部件高之和 + 行间刀缝」，再由 `maxSegmentHeight` 截断。
     * / Sheet mode uses the sheet height directly. In coil mode the worst case puts every piece in its own row, so the
     * bound is the sum of all piece heights plus the row kerfs, truncated by `maxSegmentHeight`.
     *
     * @return 高度上界 / height upper bound
    */
    fun regionHeightBound(): Quantity<V> {
        val materialHeight = material.height
        if (materialHeight != null) {
            return materialHeight
        }
        val zero = quantityZeroOf(material.width)
        var total: Quantity<V> = zero
        // 上界必须计入件数：把每个件的「高度 + 刀缝」累加，因此最坏情况（每件单独一行）一定装得下。
        // The bound must account for the amounts: accumulate "height plus kerf" per piece, so that the worst case (one
        // piece per row) always fits.
        for ((product, amount) in products) {
            val count = amount.toInt().coerceAtMost(4096)
            for (index in 0 until count) {
                total = total + product.height + kerf
            }
        }
        if (quantityLessOrEqual(total, zero)) {
            return zero
        }
        val derived = total - kerf
        val limit = maxSegmentHeight
        if (limit != null && quantityGreater(limit, zero) && quantityLess(limit, derived)) {
            return limit
        }
        return derived
    }

    /**
     * 目标料域高度：定尺为板高，卷材为段高上界。 / The target region height: the sheet height in sheet mode and the
     * segment height bound in coil mode.
     *
     * @return 料域高度 / region height
    */
    fun targetRegionHeight(): Quantity<V> {
        return material.height ?: regionHeightBound()
    }
}

/**
 * 图案生成结果。 / A pattern generation result.
 *
 * @property V 数值类型 / numeric type
 * @property pattern 生成的图案 / the generated pattern
 * @property source 生成器名称 / generator name
 * @property reducedCost 检验数（面积口径），供定价排序 / area-valued reduced cost, used for pricing order
*/
data class PatternGenerationResult2<V : FloatingNumber<V>>(
    val pattern: Pattern2<V>,
    val source: String,
    val reducedCost: Quantity<V>? = null
)

/**
 * 图案生成器。 / A pattern generator.
*/
fun interface PatternGenerator2<V : FloatingNumber<V>> {

    /**
     * 生成候选图案。 / Generate candidate patterns.
     *
     * @param request 生成请求 / generation request
     * @return 候选图案列表 / candidate pattern list
    */
    fun generate(request: PatternGenerationRequest2<V>): Ret<List<PatternGenerationResult2<V>>>
}

/**
 * 计算图案的检验数（面积口径）。 / Compute the area-valued reduced cost of a pattern.
 *
 * 需求覆盖模型为 `min Σ 余料面积 · 使用次数 s.t. 覆盖 ≥ 需求`，对偶 `π ≥ 0`（按面积计价），因此
 * `rc = 余料面积 · wasteWeight − Σ_放置 π_产品 · 放置面积`。`rc` 为负表示该图案能改进目标。
 * / The coverage model is `min Σ waste area · usage s.t. coverage ≥ demand` with duals `π ≥ 0` priced by area, so
 * `rc = waste area · wasteWeight − Σ_placement π_product · placement area`. A negative value means the pattern can
 * improve the objective.
 *
 * 用「逐放置」而不是「逐产品乘以件数」求和，可以避免在泛型代码里把整数件数换算成数值类型。
 * / Summing per placement instead of per product times its count avoids converting integer counts into the numeric type
 * inside generic code.
 *
 * @param V 数值类型 / numeric type
 * @param pattern 裁切图案 / cutting pattern
 * @param shadowPrices 产品编号到影子价格的映射 / product identifier to shadow price
 * @param wasteWeight 余料权重 / waste weight
 * @return 检验数 / reduced cost
*/
fun <V : FloatingNumber<V>> patternReducedCost(
    pattern: Pattern2<V>,
    shadowPrices: Map<String, V>,
    wasteWeight: V
): Quantity<V> {
    var dual: Quantity<V> = quantityZeroOf(pattern.region.width)
    for (placement in pattern.placements) {
        val productId = placement.itemId ?: continue
        val price = shadowPrices[productId] ?: continue
        dual = dual + quantityScaledBy(placement.area, price)
    }
    val waste = quantityScaledBy(pattern.wasteArea, wasteWeight)
    return waste - dual
}

/**
 * 丢弃非改进图案，并按检验数升序排列。 / Drop non-improving patterns and order the rest by ascending reduced cost.
 *
 * @param V 数值类型 / numeric type
 * @param results 候选列表 / candidate list
 * @return 改进图案列表 / improving pattern list
*/
fun <V : FloatingNumber<V>> improvingPatterns(
    results: List<PatternGenerationResult2<V>>
): List<PatternGenerationResult2<V>> {
    return results
        .filter { result ->
            val reducedCost = result.reducedCost ?: return@filter true
            quantityLess(reducedCost, quantityZeroOf(reducedCost))
        }
        .sortedWith(
            Comparator { lhs, rhs ->
                val left = lhs.reducedCost ?: return@Comparator 0
                val right = rhs.reducedCost ?: return@Comparator 0
                when {
                    quantityLess(left, right) -> -1
                    quantityLess(right, left) -> 1
                    else -> 0
                }
            }
        )
}

/**
 * 图案去重键：放置集合与料域共同决定一个图案。 / Deduplication key of a pattern: the placement set and the region.
 *
 * @param V 数值类型 / numeric type
 * @param pattern 裁切图案 / cutting pattern
 * @return 去重键 / deduplication key
*/
fun <V : FloatingNumber<V>> patternKey(pattern: Pattern2<V>): String {
    val placements = pattern.placements
        .map { "${it.itemId}@${it.x}~${it.y}:${it.width}x${it.height}:${it.rotated}" }
        .sorted()
        .joinToString("|")
    return "${pattern.region.x},${pattern.region.y},${pattern.region.width}x${pattern.region.height}#$placements"
}

/**
 * 按去重键去重并截断候选数。 / Deduplicate by key and truncate to the candidate limit.
 *
 * @param V 数值类型 / numeric type
 * @param results 候选列表 / candidate list
 * @param limit 上限 / limit
 * @return 去重后的候选 / deduplicated candidates
*/
fun <V : FloatingNumber<V>> distinctPatterns(
    results: List<PatternGenerationResult2<V>>,
    limit: Int
): List<PatternGenerationResult2<V>> {
    val seen = LinkedHashSet<String>()
    val kept = ArrayList<PatternGenerationResult2<V>>(minOf(limit, results.size))
    for (result in results) {
        if (kept.size >= limit) {
            break
        }
        if (seen.add(patternKey(result.pattern))) {
            kept.add(result)
        }
    }
    return kept
}
