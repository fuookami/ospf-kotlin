package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/**
 * 物料等价切片模板缓存接口 / Interface for material-equivalent slice template cache
 *
 * 顺序模式使用 [SequentialGenerationSliceTemplateCache]，并行模式使用 [ConcurrentGenerationSliceTemplateCache]。
 *
 * Sequential mode uses [SequentialGenerationSliceTemplateCache], parallel mode uses [ConcurrentGenerationSliceTemplateCache].
*/
internal interface GenerationSliceTemplateCache<V : RealNumber<V>> {

/**
 * get.
 * get。
 * @param material the cutting material whose equivalent slice templates are being looked up / 分切物料，用于查找其等价切片模板
 * @param collector the generation collector that collects, deduplicates, and prunes cutting plan candidates / 切割方案生成收集器，负责收集、去重和支配剪枝
 * @return the cached list of slice template groups for the equivalent material, or null if not cached / 该物料等价切片模板分组列表，若未缓存则返回 null
*/
    fun get(
        material: Material<V>,
        collector: GenerationCollector<V>
    ): List<List<CuttingPlanSlice<V>>>?

/**
 * put.
 * put。
 * @param material the cutting material to associate with the cached slice templates / 分切物料，与缓存的切片模板关联
 * @param templates the list of slice template groups to cache for the material / 要缓存的切片模板分组列表
*/
    fun put(
        material: Material<V>,
        templates: List<List<CuttingPlanSlice<V>>>
    )
}

/**
 * 顺序版切片模板缓存 / Sequential slice template cache
 *
 * 委托给 [GenerationMaterialSliceTemplateCache]。
*/
internal class SequentialGenerationSliceTemplateCache<V : RealNumber<V>>(
    private val delegate: GenerationMaterialSliceTemplateCache<V> = GenerationMaterialSliceTemplateCache()
) : GenerationSliceTemplateCache<V> {
    override fun get(material: Material<V>, collector: GenerationCollector<V>): List<List<CuttingPlanSlice<V>>>? {
        return delegate.get(material, collector)
    }

    override fun put(material: Material<V>, templates: List<List<CuttingPlanSlice<V>>>) {
        delegate.put(material, templates)
    }
}

/**
 * 并发版切片模板缓存 / Concurrent slice template cache
 *
 * 委托给 [ConcurrentGenerationMaterialSliceTemplateCache]。
*/
internal class ConcurrentGenerationSliceTemplateCache<V : RealNumber<V>>(
    private val delegate: ConcurrentGenerationMaterialSliceTemplateCache<V> = ConcurrentGenerationMaterialSliceTemplateCache()
) : GenerationSliceTemplateCache<V> {
    override fun get(material: Material<V>, collector: GenerationCollector<V>): List<List<CuttingPlanSlice<V>>>? {
        return delegate.get(material, collector)
    }

    override fun put(material: Material<V>, templates: List<List<CuttingPlanSlice<V>>>) {
        delegate.put(material, templates)
    }
}
