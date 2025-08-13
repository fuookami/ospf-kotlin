package fuookami.ospf.kotlin.utils.math

data class Scale(
    val scales: List<Pair<FltX, FltX>> = emptyList()
) {
    companion object {
        val atto = Scale(10, -18)
        val femto = Scale(10, -15)
        val pico = Scale(10, -12)
        val nano = Scale(10, -9)
        val micro = Scale(10, -6)
        val milli = Scale(10, -3)
        val centi = Scale(10, -2)
        val deci = Scale(10, -1)
        val deca = Scale(10, 1)
        val hecto = Scale(10, 2)
        val kilo = Scale(10, 3)
        val mega = Scale(10, 6)
        val giga = Scale(10, 9)
        val tera = Scale(10, 12)
        val peta = Scale(10, 15)
        val exa = Scale(10, 18)
    }

    constructor(base: FltX, index: FltX) : this(listOf(base to index))
    constructor(base: FltX, index: Int = 1): this(listOf(base to FltX(index.toLong())))
    constructor(base: Double, index: Int = 1): this(listOf(FltX(base) to FltX(index.toLong())))
    constructor(base: Int, index: Int = 1): this(listOf(FltX(base.toLong()) to FltX(index.toLong())))

    val value by lazy {
        scales.fold(FltX.one) { acc, scale ->
            acc * scale.first.pow(scale.second)
        }
    }

    operator fun times(other: FltX): Scale {
        if (other eq FltX.zero) {
            return Scale(FltX.zero, FltX.one)
        }
        val index = scales.indexOfFirst { it.first eq other }
        val newScales = if (index == -1) {
            scales + listOf(other to FltX.one)
        } else {
            scales.mapIndexed { i, it ->
                if (index == i) {
                    it.first to (it.second + FltX.one)
                } else {
                    it
                }
            }
        }
        return Scale(newScales.sortedBy { it.first })
    }

    operator fun div(other: FltX): Scale {
        if (other eq FltX.zero) {
            throw ArithmeticException("Cannot divide by zero")
        }
        val scale = scales.find { it.first eq other }
        val newScales = if (scale != null) {
            scales.map {
                if (scale == it) {
                    it.first to (it.second - FltX.one)
                } else {
                    it
                }
            }
        } else {
            scales + listOf(other to -FltX.one)
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
