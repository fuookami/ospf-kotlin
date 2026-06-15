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
    fun get(
        material: Material<V>,
        collector: GenerationCollector<V>
    ): List<List<CuttingPlanSlice<V>>>?

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
