package fuookami.ospf.kotlin.framework.bpp1d.domain.packing

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.bpp.infrastructure.PackingValueAdapter

/** 一维渲染件，全部坐标采用方案声明的单位。 / Render item using the schema's declared unit.
 * @property itemId 物品 / Item
 * @property start 起点 / Start
 * @property length 长度 / Length
 */
data class RenderItem1DTO(val itemId: String, val start: Double, val length: Double)

/** 渲染箱。 / Render bin.
 * @property id 箱身份 / Bin identity
 * @property binTypeId 箱型 / Bin type
 * @property length 箱长 / Bin length
 * @property items 放置 / Placements
 */
data class RenderBin1DTO(val id: String, val binTypeId: String, val length: Double, val items: List<RenderItem1DTO>)

/** 一维外部渲染契约。 / External 1D render contract.
 * @property lengthUnit 长度单位 / Length unit
 * @property bins 箱体 / Bins
 * @property utilization 长度利用率 / Length utilization
 */
data class RenderSchema1DTO(val lengthUnit: String, val bins: List<RenderBin1DTO>, val utilization: Double)

/** 渲染数值转换边界。 / Render numeric conversion boundary.
 * @property adapter 数值适配 / Numeric adapter
 */
class PackingRendererAdapter<V : FloatingNumber<V>>(private val adapter: PackingValueAdapter<V>) {
    /** 输出前复核显式一维几何。 / Validate explicit 1D geometry before rendering.
     * @param result 领域结果 / Domain result
     * @param unit 输出单位 / Output unit
     * @return 渲染契约或失败 / Render contract or failure
     */
    fun toSchema(result: PackingResult<V>, unit: PhysicalUnit): Ret<RenderSchema1DTO> {
        val bins = arrayListOf<RenderBin1DTO>()
        for (plan in result.plans) {
            val length = when (val result = adapter.quantity(plan.bin.type.length, unit)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            var end = 0.0
            val items = arrayListOf<RenderItem1DTO>()
            for (placement in plan.placements) {
                val start = when (val result = adapter.quantity(placement.start, unit)) {
                    is Ok -> result.value.toDouble()
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val size = when (val result = adapter.quantity(placement.length, unit)) {
                    is Ok -> result.value.toDouble()
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                if (start < end || size <= 0 || start + size > length.toDouble() + 1e-8) return Failed(ErrorCode.IllegalArgument, "渲染几何无效 / Invalid render geometry")
                items.add(RenderItem1DTO(placement.itemId, start, size))
                end = start + size
            }
            bins.add(RenderBin1DTO(id = plan.bin.id, binTypeId = plan.bin.type.id, length = length.toDouble(), items = items))
        }
        return Ok(RenderSchema1DTO(lengthUnit = unit.symbol ?: unit.name ?: unit.toString(), bins = bins, utilization = adapter.toSolver(result.utilization).toDouble()))
    }
}
