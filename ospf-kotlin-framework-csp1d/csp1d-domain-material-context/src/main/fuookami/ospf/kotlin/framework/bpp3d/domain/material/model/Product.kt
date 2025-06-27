package fuookami.ospf.kotlin.framework.bpp3d.domain.material.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*

open class Product(
    override val id: String,
    val name: String,
    override val width: List<Flt64>,
    override val length: FltX? = null,
    override val unitWeight: FltX? = null,
    weight: FltX? = null,
    val maxOverProduceLength: FltX? = null,
    val dynamicLength: Boolean = false
) : Production, ManualIndexed() {
    companion object {
        operator fun invoke(
            id: String,
            name: String,
            width: List<Flt64>,
            length: FltX? = null,
            unitWeight: FltX? = null,
            weight: FltX? = null,
            maxOverProduceLength: FltX? = null
        ): Product {
            return Product(id, name, width, length, unitWeight, weight, maxOverProduceLength)
        }

        fun dynamicLengthOf(
            id: String,
            name: String,
            width: List<Flt64>,
            unitWeight: FltX? = null,
        ): Product {
            return Product(id, name, width, unitWeight = unitWeight, dynamicLength = true)
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

    fun weight(width: Flt64, length: FltX? = this.length): FltX? {
        return if (length != null && unitWeight != null) {
            length * unitWeight!! * width.toFltX()
        } else {
            null
        }
    }

    override fun toString(): String {
        return "$name（${width.joinToString("，")}）"
    }
}
