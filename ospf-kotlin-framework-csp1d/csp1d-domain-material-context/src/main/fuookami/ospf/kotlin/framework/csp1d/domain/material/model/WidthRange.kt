package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange

data class WidthRange(
    val width: ValueRange<Flt64>,
    val step: Flt64
) {
    val upperBound get() = width.upperBound
    val lowerBound get() = width.lowerBound

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WidthRange

        if (width != other.width) return false
        if (step != other.step) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + step.hashCode()
        return result
    }
}




