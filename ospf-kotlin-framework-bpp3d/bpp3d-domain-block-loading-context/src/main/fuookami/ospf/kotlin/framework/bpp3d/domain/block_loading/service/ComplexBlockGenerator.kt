/**
 * 复杂块生成器。
 * Complex block generator.
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

/**
 * 复杂块生成器，用于将简单块按照 X、Y、Z 轴方向合并为复杂块。
 * Complex block generator for merging simple blocks into complex blocks along X, Y, and Z axes.
 *
 * @property config 复杂块生成配置 / Configuration for complex block generation
*/
class ComplexBlockGenerator(
    val config: Config
) {

    /**
     * 复杂块生成配置，控制各轴方向的合并开关及谓词。
     * Configuration for complex block generation, controlling merge toggles and predicates per axis.
     *
     * @property withX 是否启用 X 轴方向合并 / Whether to enable merging along X axis
     * @property withY 是否启用 Y 轴方向合并 / Whether to enable merging along Y axis
     * @property withZ 是否启用 Z 轴方向合并 / Whether to enable merging along Z axis
     * @property predicateX X 轴方向合并谓词 / Predicate for merging along X axis
     * @property predicateY Y 轴方向合并谓词 / Predicate for merging along Y axis
     * @property predicateZ Z 轴方向合并谓词 / Predicate for merging along Z axis
    */
    data class Config(
        val withX: Boolean = true,
        val withY: Boolean = true,
        val withZ: Boolean = false,
        val predicateX: (Block, Block) -> Boolean = { lhs, rhs -> lhs.height eq rhs.height && lhs.depth eq rhs.depth },
        val predicateY: (Block, Block) -> Boolean = { lhs, rhs -> lhs.width eq rhs.width && lhs.depth eq rhs.depth },
        val predicateZ: (Block, Block) -> Boolean = { lhs, rhs -> lhs.width eq rhs.width && lhs.height eq rhs.height }
    ) {
        companion object {
            operator fun invoke(builder: ConfigBuilder): Config {
                return Config().new(builder)
            }
        }

        /**
         * 使用可空参数创建新配置，空值则回退到当前值。
         * Create a new config with nullable parameters, falling back to current values for nulls.
         *
         * @param withX 是否启用 X 轴方向合并 / Whether to enable merging along X axis
         * @param withY 是否启用 Y 轴方向合并 / Whether to enable merging along Y axis
         * @param withZ 是否启用 Z 轴方向合并 / Whether to enable merging along Z axis
         * @param predicateX X 轴方向合并谓词 / Predicate for merging along X axis
         * @param predicateY Y 轴方向合并谓词 / Predicate for merging along Y axis
         * @param predicateZ Z 轴方向合并谓词 / Predicate for merging along Z axis
         * @return 新配置实例 / New config instance
        */
        fun new(
            withX: Boolean? = null,
            withY: Boolean? = null,
            withZ: Boolean? = null,
            predicateX: ((Block, Block) -> Boolean)? = null,
            predicateY: ((Block, Block) -> Boolean)? = null,
            predicateZ: ((Block, Block) -> Boolean)? = null,
        ): Config {
            return Config(
                withX = withX ?: this.withX,
                withY = withY ?: this.withY,
                withZ = withZ ?: this.withZ,
                predicateX = predicateX ?: this.predicateX,
                predicateY = predicateY ?: this.predicateY,
                predicateZ = predicateZ ?: this.predicateZ
            )
        }

        /**
         * 使用配置构造器创建新配置。
         * Create a new config from a config builder.
         *
         * @param builder 配置构造器 / Config builder
         * @return 新配置实例 / New config instance
        */
        fun new(builder: ConfigBuilder): Config {
            return new(
                withX = builder.withX,
                withY = builder.withY,
                withZ = builder.withZ,
                predicateX = builder.predicateX,
                predicateY = builder.predicateY,
                predicateZ = builder.predicateZ
            )
        }
    }

    /**
     * 复杂块生成配置构造器，用于通过上下文函数构建配置。
     * Config builder for constructing configuration via context functions.
     *
     * @property withX 是否启用 X 轴方向合并 / Whether to enable merging along X axis
     * @property withY 是否启用 Y 轴方向合并 / Whether to enable merging along Y axis
     * @property withZ 是否启用 Z 轴方向合并 / Whether to enable merging along Z axis
     * @property predicateX X 轴方向合并谓词 / Predicate for merging along X axis
     * @property predicateY Y 轴方向合并谓词 / Predicate for merging along Y axis
     * @property predicateZ Z 轴方向合并谓词 / Predicate for merging along Z axis
    */
    data class ConfigBuilder(
        var withX: Boolean? = null,
        var withY: Boolean? = null,
        var withZ: Boolean? = null,
        var predicateX: ((Block, Block) -> Boolean)? = null,
        var predicateY: ((Block, Block) -> Boolean)? = null,
        var predicateZ: ((Block, Block) -> Boolean)? = null,
    ) {
        companion object {
            /**
             * 通过上下文函数构造配置构造器。
             * Construct a config builder via a context function.
             *
             * @param func 上下文函数 / Context function
             * @return 配置构造器实例 / Config builder instance
            */
            operator fun invoke(func: ConfigBuilder.() -> Unit): ConfigBuilder {
                val builder = ConfigBuilder()
                func(builder)
                return builder
            }
        }

        operator fun invoke(): Config {
            return Config(this)
        }
    }

    companion object {
        /**
         * 通过上下文函数构造配置构造器。
         * Construct a config builder via a context function.
         *
         * @param builder 上下文函数 / Context function
         * @return 配置构造器实例 / Config builder instance
        */
        fun buildConfig(builder: ConfigBuilder.() -> Unit): ConfigBuilder {
            val config = ConfigBuilder()
            builder(config)
            return config
        }
    }

    /**
     * 执行复杂块生成。
     * Execute complex block generation.
     *
     * @param items 可用物品及其数量映射 / Map of available items and their quantities
     * @param space 容器空间形状 / Container space shape
     * @param simpleBlocks 待合并的简单块列表 / List of simple blocks to merge
     * @param restWeight 剩余可承载重量 / Remaining load-bearing weight
     * @return 生成的复杂块列表 / Generated list of complex blocks
    */
    operator fun invoke(
        items: Map<Item, UInt64>,
        space: Container3Shape,
        simpleBlocks: List<Block>,
        restWeight: FltX = FltX.maximum
    ): List<ComplexBlock> {
        if (simpleBlocks.isEmpty()) {
            return emptyList()
        }

        val candidateBlocks = simpleBlocks.filter {
            space.enabled(it) && enough(items, it) && (it.weight.value leq restWeight)
        }
        if (candidateBlocks.isEmpty()) {
            return emptyList()
        }

        val complexBlocks = LinkedHashSet<ComplexBlock>()
        for (lhs in candidateBlocks) {
            for (rhs in candidateBlocks) {
                if (config.withX && config.predicateX(lhs, rhs)) {
                    mergeAlongX(lhs, rhs)?.let { merged ->
                        if (enabled(items, space, merged, restWeight)) {
                            complexBlocks.add(merged)
                        }
                    }
                }
                if (config.withY && config.predicateY(lhs, rhs)) {
                    mergeAlongY(lhs, rhs)?.let { merged ->
                        if (enabled(items, space, merged, restWeight)) {
                            complexBlocks.add(merged)
                        }
                    }
                }
                if (config.withZ && config.predicateZ(lhs, rhs)) {
                    mergeAlongZ(lhs, rhs)?.let { merged ->
                        if (enabled(items, space, merged, restWeight)) {
                            complexBlocks.add(merged)
                        }
                    }
                }
            }
        }
        return complexBlocks.toList()
    }

    /**
     * 沿 X 轴方向合并两个块。
     * Merge two blocks along the X axis.
     *
     * @param lhs 左侧块 / Left-hand side block
     * @param rhs 右侧块 / Right-hand side block
     * @return 合并后的复杂块，若无法获取视图则返回 null / Merged complex block, or null if view unavailable
    */
    private fun mergeAlongX(lhs: Block, rhs: Block): ComplexBlock? {
        val lhsView = lhs.view() ?: return null
        val rhsView = rhs.view() ?: return null
        return ComplexBlock(
            blocks = listOf(
                blockPlacement3Of(view = lhsView, position = point3FltX()),
                blockPlacement3Of(
                    view = rhsView,
                    position = point3FltX(x = lhs.width.value, unit = lhs.width.unit)
                )
            )
        )
    }

    /**
     * 沿 Y 轴方向合并两个块。
     * Merge two blocks along the Y axis.
     *
     * @param lhs 左侧块 / Left-hand side block
     * @param rhs 右侧块 / Right-hand side block
     * @return 合并后的复杂块，若无法获取视图则返回 null / Merged complex block, or null if view unavailable
    */
    private fun mergeAlongY(lhs: Block, rhs: Block): ComplexBlock? {
        val lhsView = lhs.view() ?: return null
        val rhsView = rhs.view() ?: return null
        return ComplexBlock(
            blocks = listOf(
                blockPlacement3Of(view = lhsView, position = point3FltX()),
                blockPlacement3Of(
                    view = rhsView,
                    position = point3FltX(y = lhs.height.value, unit = lhs.height.unit)
                )
            )
        )
    }

    /**
     * 沿 Z 轴方向合并两个块。
     * Merge two blocks along the Z axis.
     *
     * @param lhs  左侧块 / Left-hand side block
     * @param rhs  右侧块 / Right-hand side block
     * @return     合并后的复杂块，若无法获取视图则返回 null / Merged complex block, or null if view unavailable
    */
    private fun mergeAlongZ(lhs: Block, rhs: Block): ComplexBlock? {
        val lhsView = lhs.view() ?: return null
        val rhsView = rhs.view() ?: return null
        return ComplexBlock(
            blocks = listOf(
                blockPlacement3Of(view = lhsView, position = point3FltX()),
                blockPlacement3Of(
                    view = rhsView,
                    position = point3FltX(z = lhs.depth.value, unit = lhs.depth.unit)
                )
            )
        )
    }

    /**
     * 检查复杂块是否可用。
     * Check whether a complex block is enabled for placement.
     *
     * @param items       可用物品及其数量映射 / Map of available items and their quantities
     * @param space       容器空间形状 / Container space shape
     * @param block       待检查的块 / Block to check
     * @param restWeight  剩余可承载重量 / Remaining load-bearing weight
     * @return            可用则返回 true，否则返回 false / True if enabled, false otherwise
    */
    private fun enabled(
        items: Map<Item, UInt64>,
        space: Container3Shape,
        block: Block,
        restWeight: FltX
    ): Boolean {
        return space.enabled(block) && enough(items, block) && (block.weight.value leq restWeight)
    }

    /**
     * 检查块的物品数量是否足够。
     * Check whether the block's item amounts do not exceed available quantities.
     *
     * @param items  可用物品及其数量映射 / Map of available items and their quantities
     * @param block  待检查的块 / Block to check
     * @return       数量足够则返回 true，否则返回 false / True if enough, false otherwise
    */
    private fun enough(items: Map<Item, UInt64>, block: Block): Boolean {
        for ((item, amount) in block.amounts) {
            val actual = item as? Item ?: return false
            if (amount > (items[actual] ?: UInt64.zero)) {
                return false
            }
        }
        return true
    }
}
