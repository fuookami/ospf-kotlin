package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

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
             * @param func  上下文函数
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
         * @param builder   上下文函数
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
        restWeight: Flt64 = Flt64.infinity
    ): List<ComplexBlock> {
        // todo: impl it
        return emptyList()
    }
}
