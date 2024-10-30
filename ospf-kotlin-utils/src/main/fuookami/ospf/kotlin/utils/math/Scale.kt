package fuookami.ospf.kotlin.utils.math

data class Scale(
    val scales: List<Pair<Flt64, Flt64>> = emptyList()
) {
    constructor(base: Flt64, index: Flt64) : this(listOf(base to index))

    val value by lazy {
        scales.fold(Flt64.one) { acc, scale ->
            acc * scale.first.pow(scale.second).toFlt64()
        }
    }

    operator fun times(other: Flt64): Scale {
        if (other eq Flt64.zero) {
            return Scale(Flt64.zero, Flt64.one)
        }
        val index = scales.indexOfFirst { it.first eq other }
        val newScales = if (index == -1) {
            scales + listOf(other to Flt64.one)
        } else {
            scales.mapIndexed { i, it ->
                if (index == i) {
                    it.first to (it.second + Flt64.one)
                } else {
                    it
                }
            }
        }
        return Scale(newScales.sortedBy { it.first })
    }

    operator fun div(other: Flt64): Scale {
        if (other eq Flt64.zero) {
            throw ArithmeticException("Cannot divide by zero")
        }
        val scale = scales.find { it.first eq other }
        val newScales = if (scale != null) {
            scales.map {
                if (scale == it) {
                    it.first to (it.second - Flt64.one)
                } else {
                    it
                }
            }
        } else {
            scales + listOf(other to -Flt64.one)
        }
        return Scale(newScales.sortedBy { it.first })
    }

    operator fun times(other: Scale): Scale {
        val newScales = this.scales.toMutableList()
        // todo: binary search and merge insert
        for (scale in other.scales) {
            val index = newScales.indexOfFirst { it.first eq scale.first }
            if (index == -1) {
                newScales.add(scale)
            } else {
                newScales[index] = scale.first to (newScales[index].second + scale.second)
            }
        }
        return Scale(newScales.sortedBy { it.first })
    }

    operator fun div(other: Scale): Scale {
        val newScales = this.scales.toMutableList()
        // todo: binary search and merge insert
        for (scale in other.scales) {
            val index = newScales.indexOfFirst { it.first eq scale.first }
            if (index == -1) {
                newScales.add(scale.first to -scale.second)
            } else {
                newScales[index] = scale.first to (newScales[index].second - scale.second)
            }
        }
        return Scale(newScales.sortedBy { it.first })
    }
}
