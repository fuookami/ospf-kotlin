package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX

/** 产品，描述物料的规格属性 / Product describing material specification properties */
open class Product(
    /** 物料标识 / Material identifier */
    override val id: String,
    /** 产品名称 / Product name */
    val name: String,
    /** 宽度列表 / List of widths */
    override val width: List<Flt64>,
    /** 长度 / Length */
    override val length: FltX? = null,
    /** 单位重量 / Unit weight */
    override val unitWeight: FltX? = null,
    /** 重量，非val参数 / Weight, non-val parameter */
    weight: FltX? = null,
    /** 最大超产长度 / Maximum overproduce length */
    val maxOverProduceLength: FltX? = null,
    /** 是否动态长度 / Whether length is dynamic */
    val dynamicLength: Boolean = false
) : Production, ManualIndexed() {
    companion object {
        /** 创建产品实例 / Create a product instance */
        operator fun invoke(
            /** 物料标识 / Material identifier */
            id: String,
            /** 产品名称 / Product name */
            name: String,
            /** 宽度列表 / List of widths */
            width: List<Flt64>,
            /** 长度 / Length */
            length: FltX? = null,
            /** 单位重量 / Unit weight */
            unitWeight: FltX? = null,
            /** 重量 / Weight */
            weight: FltX? = null,
            /** 最大超产长度 / Maximum overproduce length */
            maxOverProduceLength: FltX? = null
        ): Product {
            return Product(
                id = id,
                name = name,
                width = width,
                length = length,
                unitWeight = unitWeight,
                weight = weight,
                maxOverProduceLength = maxOverProduceLength
            )
        }

        /** 创建动态长度产品实例 / Create a dynamic-length product instance */
        fun dynamicLengthOf(
            /** 物料标识 / Material identifier */
            id: String,
            /** 产品名称 / Product name */
            name: String,
            /** 宽度列表 / List of widths */
            width: List<Flt64>,
            /** 单位重量 / Unit weight */
            unitWeight: FltX? = null,
        ): Product {
            return Product(
                id = id,
                name = name,
                width = width,
                unitWeight = unitWeight,
                dynamicLength = true
            )
        }
    }

    init {
        if (dynamicLength) {
            assert(length == null && weight == null)
        }
    }

    val weight by lazy {
        weight ?: if (length != null && unitWeight != null) {
            length!! * unitWeight!! * width.max().toFltX()
        } else {
            null
        }
    }

    /** 根据指定宽度和长度计算重量 / Calculate weight for given width and length */
    fun weight(
        /** 宽度 / Width */
        width: Flt64,
        /** 长度 / Length */
        length: FltX? = this.length
    ): FltX? {
        return if (length != null && unitWeight != null) {
            length * unitWeight!! * width.toFltX()
        } else {
            null
        }
    }

    override fun toString(): String {
        return "$name${width.joinToString("x")}"
    }
}


