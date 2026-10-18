/**
 * 裁切图案与裁切方案。 / Cutting patterns and cutting plans.
 *
 * 二维侧与一维的关键差异：一个**图案**是一块料上的完整二维排样（放置集合 + 可选切分树），
 * 一个**方案**是图案加上需求贡献与产能消耗。一维的「切片列表」在这里没有对应物。
 * / The key difference from the 1D side: a **pattern** is a complete 2D layout on one piece of material (a placement
 * set plus an optional cut tree), and a **plan** is that pattern plus demand contributions and capacity consumption.
 * There is no counterpart of the 1D "slice list" here.
 *
 * @property V 数值类型 / numeric type
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.material.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dValidationError
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CutTree2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Region2

/**
 * 裁切图案：一块料上的完整二维排样。 / A cutting pattern: a complete 2D layout on one piece of material.
 *
 * @property V 数值类型 / numeric type
 * @property id 图案编号 / pattern identifier
 * @property material 物料 / material
 * @property region 该图案占用的料域 / the region occupied by this pattern
 * @property placements 成品件放置 / finished part placements
 * @property costarPlacements 副产物放置 / by-product placements
 * @property cutTree 断头台切分树；任意排样为 null / guillotine cut tree; null for arbitrary nesting
 * @property process 裁切工艺 / cutting process
*/
data class Pattern2<V : FloatingNumber<V>>(
    val id: PatternId2,
    val material: Material2<V>,
    val region: Region2<V>,
    val placements: List<Placement2<V>>,
    val costarPlacements: List<Placement2<V>> = emptyList(),
    val cutTree: CutTree2<V>? = null,
    val process: CuttingProcess2 = CuttingProcess2.Freeform
) {

    /** 成品件占用面积 / area occupied by finished parts */
    val usedArea: Quantity<V> = quantitySum(placements.map { it.area }) ?: quantityZeroOf(region.width)

    /** 副产物占用面积 / area occupied by by-products */
    val costarArea: Quantity<V> = quantitySum(costarPlacements.map { it.area }) ?: quantityZeroOf(region.width)

    /**
     * 余料面积 = 料域面积 − 成品面积 − 副产物面积，下限为零。
     * / Remnant area: region area minus finished part area minus by-product area, floored at zero.
     *
     * @return 余料面积 / remnant area
    */
    val wasteArea: Quantity<V>
        get() {
            val occupied = usedArea + costarArea
            return if (quantityGreater(region.area, occupied)) region.area - occupied else quantityZeroOf(region.width)
        }

    /**
     * 成品件件数映射，按产品编号。 / Finished part counts indexed by product identifier.
     *
     * @return 件数映射 / piece count map
    */
    fun productAmounts(): Map<String, UInt64> {
        val result = LinkedHashMap<String, UInt64>()
        for (placement in placements) {
            val productId = placement.itemId ?: continue
            result[productId] = result[productId]?.let { UInt64(it.toInt() + 1) } ?: UInt64.one
        }
        return result
    }

    /**
     * 利用率：成品面积与料域面积之比。 / Utilization: finished part area over region area.
     *
     * 注意余料率与利用率互补，但副产物不计入利用率（副产物不是需求驱动的产出）。
     * / Note that the remnant ratio and the utilization are complementary, but by-products are excluded from the
     * utilization because they are not demand-driven output.
     *
     * @return 利用率 / utilization
    */
    fun utilization(): V {
        if (!quantityIsPositive(region.area)) {
            return region.width.value.constants.zero
        }
        return usedArea.value / region.area.value
    }

    /**
     * 校验图案：几何合法、互不重叠、工艺与切分树一致。 / Validate the pattern: legal geometry, pairwise disjoint
     * placement, and process consistent with the cut tree.
     *
     * @param kerf 刀缝宽度 / kerf width
     * @return 校验结果 / validation result
    */
    fun validate(kerf: Quantity<V>): Try {
        if (region.isDegenerate()) {
            return Failed(
                Csp2dValidationError(detail = "图案 $id 的料域退化：$region / Degenerate pattern region")
            )
        }
        when (val result = material.validate()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (process.requiresCutTree && cutTree == null) {
            return Failed(
                Csp2dValidationError(
                    detail = "图案 $id 使用断头台工艺 ${process.name} 但未提供切分树 / " +
                            "A guillotine process requires a cut tree"
                )
            )
        }
        if (placements.isEmpty() && costarPlacements.isEmpty()) {
            return Failed(
                Csp2dValidationError(detail = "图案 $id 没有任何放置 / The pattern has no placement")
            )
        }

        val regionBox = region.toBox()
        val all = placements + costarPlacements
        for ((index, placement) in all.withIndex()) {
            val inside = when (val result = placement.box().inside(regionBox)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (!inside) {
                return Failed(
                    Csp2dValidationError(
                        detail = "图案 $id 的第 ${index + 1} 个放置超出料域 $region / " +
                                "Placement ${index + 1} lies outside the pattern region"
                    )
                )
            }
        }
        for (lhs in all.indices) {
            for (rhs in (lhs + 1) until all.size) {
                val overlapped = when (val result = all[lhs].overlapped(all[rhs])) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                if (overlapped) {
                    return Failed(
                        Csp2dValidationError(
                            detail = "图案 $id 的第 ${lhs + 1} 与第 ${rhs + 1} 个放置重叠 / " +
                                    "Placements ${lhs + 1} and ${rhs + 1} overlap"
                        )
                    )
                }
            }
        }

        val tree = cutTree
        if (tree != null) {
            when (val result = tree.validate(process, kerf)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val expected = productAmounts()
            val actual = LinkedHashMap<String, Int>()
            for (leaf in tree.leaves()) {
                val pieceId = leaf.pieceId ?: continue
                actual[pieceId] = (actual[pieceId] ?: 0) + 1
            }
            if (actual.mapValues { it.value.toULong() } != expected.mapValues { it.value.toULong() }) {
                return Failed(
                    Csp2dValidationError(
                        detail = "图案 $id 的切分树叶子件数与放置不一致：树 $actual，放置 $expected / " +
                                "The cut tree leaves do not match the placements"
                    )
                )
            }
        }

        if (quantityGreater(usedArea + costarArea, region.area)) {
            return Failed(
                Csp2dValidationError(
                    detail = "图案 $id 的占用面积 ${usedArea + costarArea} 超过料域面积 ${region.area} / " +
                            "Occupied area exceeds the region area"
                )
            )
        }
        return ok
    }

    /**
     * 判断该图案的余料是否达到可复用阈值。 / Decide whether the remnant meets the reusable threshold.
     *
     * @return 是否可复用 / whether reusable
    */
    fun hasUsableRemnant(): Boolean {
        return material.isUsableRemnant(wasteArea)
    }
}

/**
 * 方案对某个产品需求的贡献。 / The contribution of a plan to one product demand.
 *
 * @property V 数值类型 / numeric type
 * @property product 产品 / product
 * @property amount 该方案一次产出的件数 / pieces produced by one usage of the plan
 * @property quantity 贡献量（按需求口径），为空表示与件数口径一致 / contribution quantity under the demand unit, null
 * meaning it equals the piece count
*/
data class CuttingPlanDemandContribution2<V : FloatingNumber<V>>(
    val product: Product2<V>,
    val amount: UInt64,
    val quantity: Quantity<V>? = null
)

/**
 * 裁切方案。 / A cutting plan.
 *
 * @property V 数值类型 / numeric type
 * @property id 方案编号 / plan identifier
 * @property material 物料 / material
 * @property machineId 设备编号 / machine identifier
 * @property pattern 裁切图案 / cutting pattern
 * @property contributions 需求贡献 / demand contributions
 * @property capacityConsumption 单次方案的产能消耗 / capacity consumed by one plan usage
*/
data class CuttingPlan2<V : FloatingNumber<V>>(
    val id: PlanId2,
    val material: Material2<V>,
    val machineId: MachineId2? = material.machineId,
    val pattern: Pattern2<V>,
    val contributions: List<CuttingPlanDemandContribution2<V>> = emptyList(),
    val capacityConsumption: Quantity<V>? = null
) {

    /** 成品件占用面积 / area occupied by finished parts */
    val usedArea: Quantity<V> get() = pattern.usedArea

    /** 余料面积 / remnant area */
    val wasteArea: Quantity<V> get() = pattern.wasteArea

    /** 利用率 / utilization */
    fun utilization(): V = pattern.utilization()

    /**
     * 校验方案：图案合法、贡献与图案一致、无重复产品。 / Validate the plan: the pattern is legal, the contributions
     * match the pattern, and no product is duplicated.
     *
     * @param kerf 刀缝宽度 / kerf width
     * @return 校验结果 / validation result
    */
    fun validate(kerf: Quantity<V>): Try {
        if (pattern.material.id.value != material.id.value) {
            return Failed(
                Csp2dValidationError(
                    detail = "方案 $id 的物料与图案物料不一致：${material.id} vs ${pattern.material.id} / " +
                            "Plan material does not match the pattern material"
                )
            )
        }
        when (val result = pattern.validate(kerf)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val duplicated = contributions.groupingBy { it.product.id.value }.eachCount().filterValues { it > 1 }.keys
        if (duplicated.isNotEmpty()) {
            return Failed(
                Csp2dValidationError(
                    detail = "方案 $id 的贡献存在重复产品：${duplicated.joinToString()} / " +
                            "Duplicate products in the plan contributions"
                )
            )
        }
        if (contributions.isNotEmpty()) {
            val actual = pattern.productAmounts().mapValues { it.value.toULong() }
            val declared = contributions.associate { it.product.id.value to it.amount.toULong() }
            if (actual != declared) {
                return Failed(
                    Csp2dValidationError(
                        detail = "方案 $id 的贡献与图案件数不一致：图案 $actual，贡献 $declared / " +
                                "The plan contributions do not match the pattern piece counts"
                    )
                )
            }
        }
        val capacity = capacityConsumption
        if (capacity != null && !quantityIsPositive(capacity)) {
            return Failed(
                Csp2dValidationError(
                    detail = "方案 $id 的产能消耗必须为正：$capacity / The capacity consumption must be positive"
                )
            )
        }
        return ok
    }
}

/**
 * 由图案构造方案，贡献按图案件数自动生成。 / Build a plan from a pattern, deriving the contributions from the pattern
 * piece counts.
 *
 * 自动生成贡献可以避免「方案与图案件数不一致」这类手工错误；需要覆盖时再显式传入。
 * / Deriving the contributions avoids hand-made inconsistencies between the plan and its pattern; pass them explicitly
 * only when an override is needed.
 *
 * @param V 数值类型 / numeric type
 * @param id 方案编号 / plan identifier
 * @param pattern 裁切图案 / cutting pattern
 * @param products 产品目录，按编号索引 / product catalogue indexed by identifier
 * @param capacityConsumption 单次方案的产能消耗 / capacity consumed by one plan usage
 * @return 方案或失败 / the plan, or failure
*/
fun <V : FloatingNumber<V>> cuttingPlan2Of(
    id: PlanId2,
    pattern: Pattern2<V>,
    products: Map<String, Product2<V>>,
    capacityConsumption: Quantity<V>? = null
): Ret<CuttingPlan2<V>> {
    val contributions = ArrayList<CuttingPlanDemandContribution2<V>>()
    for ((productId, amount) in pattern.productAmounts()) {
        val product = products[productId] ?: return Failed(
            Csp2dValidationError(
                detail = "图案 ${pattern.id} 引用了未知产品 $productId / The pattern references an unknown product"
            )
        )
        contributions.add(
            CuttingPlanDemandContribution2(
                product = product,
                amount = amount
            )
        )
    }
    return ok(
        CuttingPlan2(
            id = id,
            material = pattern.material,
            machineId = pattern.material.machineId,
            pattern = pattern,
            contributions = contributions,
            capacityConsumption = capacityConsumption
        )
    )
}

/**
 * 判断料域是否足够容纳给定尺寸的件。 / Decide whether a region can hold a piece of the given size.
 *
 * @param V 数值类型 / numeric type
 * @param region 料域 / region
 * @param width 件宽 / piece width
 * @param height 件高 / piece height
 * @param allowRotate 是否允许旋转 90° / whether a 90-degree rotation is allowed
 * @return 是否可容纳 / whether it fits
*/
fun <V : FloatingNumber<V>> fitsIn(
    region: Region2<V>,
    width: Quantity<V>,
    height: Quantity<V>,
    allowRotate: Boolean
): Boolean {
    val direct = quantityLessOrEqual(width, region.width) && quantityLessOrEqual(height, region.height)
    if (direct) {
        return true
    }
    if (!allowRotate) {
        return false
    }
    return quantityLessOrEqual(height, region.width) && quantityLessOrEqual(width, region.height)
}

/**
 * 判断余料是否达到阈值。 / Decide whether a remnant reaches the threshold.
 *
 * @param V 数值类型 / numeric type
 * @param area 余料面积 / remnant area
 * @param threshold 阈值，为空表示只要为正即可 / threshold; null means "any positive area"
 * @return 是否满足 / whether satisfied
*/
fun <V : FloatingNumber<V>> remnantReaches(area: Quantity<V>, threshold: Quantity<V>?): Boolean {
    if (threshold == null) {
        return quantityIsPositive(area)
    }
    return quantityGreaterOrEqual(area, threshold)
}
