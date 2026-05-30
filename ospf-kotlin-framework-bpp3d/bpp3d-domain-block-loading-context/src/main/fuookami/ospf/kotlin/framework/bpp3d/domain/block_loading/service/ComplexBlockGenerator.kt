@file:Suppress("DEPRECATION")

/**
 * 复杂块生成器。
 * Complex block generator.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Block
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ComplexBlock
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64

class ComplexBlockGenerator(
    val config: Config
) {
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
             * 通过上下文函数构造配置构造器
             *
             * @param func  上下文函�?
             * @return      配置构造器实例
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
         * 通过上下文函数构造配置构造器
         *
         * @param builder   上下文函�?
         * @return          配置构造器实例
         */
        fun buildConfig(builder: ConfigBuilder.() -> Unit): ConfigBuilder {
            val config = ConfigBuilder()
            builder(config)
            return config
        }
    }

    operator fun invoke(
        items: Map<Item, UInt64>,
        space: Container3Shape,
        simpleBlocks: List<Block>,
        restWeight: InfraNumber = infraInfinity()
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

    private fun mergeAlongX(lhs: Block, rhs: Block): ComplexBlock? {
        val lhsView = lhs.view() ?: return null
        val rhsView = rhs.view() ?: return null
        return ComplexBlock(
            blocks = listOf(
                QuantityPlacement3(lhsView, point3()),
                QuantityPlacement3(rhsView, point3(x = lhs.width.value))
            )
        )
    }

    private fun mergeAlongY(lhs: Block, rhs: Block): ComplexBlock? {
        val lhsView = lhs.view() ?: return null
        val rhsView = rhs.view() ?: return null
        return ComplexBlock(
            blocks = listOf(
                QuantityPlacement3(lhsView, point3()),
                QuantityPlacement3(rhsView, point3(y = lhs.height.value))
            )
        )
    }

    private fun mergeAlongZ(lhs: Block, rhs: Block): ComplexBlock? {
        val lhsView = lhs.view() ?: return null
        val rhsView = rhs.view() ?: return null
        return ComplexBlock(
            blocks = listOf(
                QuantityPlacement3(lhsView, point3()),
                QuantityPlacement3(rhsView, point3(z = lhs.depth.value))
            )
        )
    }

    private fun enabled(
        items: Map<Item, UInt64>,
        space: Container3Shape,
        block: Block,
        restWeight: InfraNumber
    ): Boolean {
        return space.enabled(block) && enough(items, block) && (block.weight.value leq restWeight)
    }

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


