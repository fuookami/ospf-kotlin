/**
 * 块装载领域上下文。 / Block loading domain context.
 *
 * 对应 BPP3D 的 `BlockLoadingContext`：组合简单块、复杂块与两类空间切分搜索，向候选列生成暴露统一的
 * 块/放置产出。
 * / Mirrors BPP3D `BlockLoadingContext`: combines simple blocks, complex blocks and the two space-splitting
 * searches, exposing a unified block/placement output to candidate column generation.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Block2
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Space2
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.service.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 块装载领域上下文。 / Block loading domain context.
 *
 * @param V 数值类型 / numeric type
 * @property simpleBlockGenerator 简单块生成器 / simple block generator
 * @property complexBlockGenerator 复杂块生成器 / complex block generator
 * @property depthFirstSearch DFS 空间切分 / DFS space split
 * @property multiRowSearch 多行启发式 / multi-row heuristic
*/
class BlockLoadingContext<V : FloatingNumber<V>>(
    private val simpleBlockGenerator: SimpleBlockGenerator = SimpleBlockGenerator(),
    private val complexBlockGenerator: ComplexBlockGenerator = ComplexBlockGenerator(),
    private val depthFirstSearch: DepthFirstSearchAlgorithm2D = DepthFirstSearchAlgorithm2D(),
    private val multiRowSearch: MultiRowHeuristicAlgorithm2D = MultiRowHeuristicAlgorithm2D()
) {

    /**
     * 生成局部块候选：对每个物品单独生成规则块。 / Generate local block candidates: regular blocks per item.
     *
     * @param items 物品与件数 / items and their amounts
     * @param space 目标区域 / target region
     * @param config 简单块配置 / simple block configuration
     * @return 块候选列表 / block candidate list
    */
    fun localBlocks(
        items: List<Pair<ActualItem<V>, Int>>,
        space: Space2<V>,
        config: SimpleBlockConfig = SimpleBlockConfig(),
        kerf: Quantity<V>? = null
    ): Ret<List<Block2<V>>> {
        val blocks = ArrayList<Block2<V>>()
        for ((item, _) in items) {
            when (val result = simpleBlockGenerator.invoke(item, space, config, kerf)) {
                is fuookami.ospf.kotlin.utils.functional.Ok -> blocks.addAll(result.value)
                is fuookami.ospf.kotlin.utils.functional.Failed ->
                    return fuookami.ospf.kotlin.utils.functional.Failed(result.error)

                is fuookami.ospf.kotlin.utils.functional.Fatal ->
                    return fuookami.ospf.kotlin.utils.functional.Fatal(result.errors)
            }
        }
        return ok(blocks)
    }

    /**
     * 生成全局块候选：多种物品拼接为异构块。 / Generate global block candidates: heterogeneous blocks from
     * multiple items.
     *
     * @param items 物品与件数 / items and their amounts
     * @param space 目标区域 / target region
     * @param withRotation 是否允许旋转候选 / whether rotated candidates are allowed
     * @return 块候选列表 / block candidate list
    */
    fun globalBlocks(
        items: List<Pair<ActualItem<V>, Int>>,
        space: Space2<V>,
        withRotation: Boolean = true,
        kerf: Quantity<V>? = null
    ): Ret<List<Block2<V>>> {
        return complexBlockGenerator.invoke(
            items = items.map { it.first },
            space = space,
            withRotation = withRotation,
            kerf = kerf
        )
    }

    /**
     * 执行 DFS 空间切分。 / Run the DFS space split.
     *
     * @param space 初始区域 / initial region
     * @param items 物品与件数 / items and their amounts
     * @param maxDepth 最大递归深度 / maximum recursion depth
     * @return 搜索结果 / search result
    */
    fun depthFirst(
        space: Space2<V>,
        items: List<Pair<ActualItem<V>, Int>>,
        maxDepth: Int = 8,
        kerf: Quantity<V>? = null
    ): Ret<SpaceSplit2Result<V>> {
        return depthFirstSearch.invoke(
            space = space,
            items = items,
            maxDepth = maxDepth,
            kerf = kerf
        )
    }

    /**
     * 执行多行启发式搜索。 / Run the multi-row heuristic search.
     *
     * @param space 初始区域 / initial region
     * @param items 物品与件数 / items and their amounts
     * @param maxRows 最大行数 / maximum row count
     * @return 搜索结果 / search result
    */
    fun multiRow(
        space: Space2<V>,
        items: List<Pair<ActualItem<V>, Int>>,
        maxRows: Int = 32,
        kerf: Quantity<V>? = null
    ): Ret<SpaceSplit2Result<V>> {
        return multiRowSearch.invoke(
            space = space,
            items = items,
            maxRows = maxRows,
            kerf = kerf
        )
    }
}