package fuookami.ospf.kotlin.utils.math

import fuookami.ospf.kotlin.utils.functional.*

private typealias ScaleBase = Either<FltX, RtnX>

data class Scale(
    val scales: List<Pair<ScaleBase, FltX>> = emptyList()
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

        operator fun invoke(base: FltX, index: FltX): Scale {
            val base : ScaleBase = Either.Left(base)
            return Scale(listOf(base to index))
        }

        operator fun invoke(base: FltX, index: Int = 1): Scale {
            return Scale(base, FltX(index.toLong()))
        }

        operator fun invoke(base: Double, index: Int = 1): Scale {
            return Scale(FltX(base), index)
        }

        operator fun invoke(base: Int, index: Int = 1): Scale {
            return Scale(FltX(base.toLong()), index)
        }

        operator fun invoke(base: RtnX, index: FltX): Scale {
            val base : ScaleBase = Either.Right(base)
            return Scale(listOf(base to index))
        }

        operator fun invoke(base: RtnX, index: Int = 1): Scale {
            val base : ScaleBase = Either.Right(base)
            return Scale(listOf(base to FltX(index.toLong())))
        }

        private fun List<Pair<ScaleBase, FltX>>.sort(): List<Pair<ScaleBase, FltX>> {
            return sortedBy {
                when (val base = it.first) {
                    is Either.Left -> base.value
                    is Either.Right -> base.value.toFltX()
                }
            }
        }

        private fun List<Pair<ScaleBase, FltX>>.tidy(): List<Pair<ScaleBase, FltX>> {
            return this
                .filter { it.second neq FltX.zero }
                .sort()
        }
    }

    val value by lazy {
        scales.fold(FltX.one) { acc, scale ->
            acc * when (val base = scale.first) {
                is Either.Left -> base.value.pow(scale.second)
                is Either.Right -> base.value.pow(scale.second)
            }
        }.stripTrailingZeros()
    }

    operator fun times(other: FltX): Scale {
        if (other eq FltX.zero) {
            return Scale(FltX.zero, FltX.one)
        }
        val index = scales.indexOfFirst {
            when (val base = it.first) {
                is Either.Left -> base.value eq other
                else -> false
            }
        }
        val newScales = if (index == -1) {
            val newBase : ScaleBase = Either.Left(other)
            scales + listOf(newBase to FltX.one)
        } else {
            scales.mapIndexed { i, it ->
                if (index == i) {
                    it.first to (it.second + FltX.one)
                } else {
                    it
                }
            }
        }
        return Scale(newScales.tidy())
    }

    operator fun times(other: RtnX): Scale {
        if (other eq RtnX.zero) {
            return Scale(FltX.zero, FltX.one)
        }
        val index = scales.indexOfFirst {
            when (val base = it.first) {
                is Either.Right -> base.value eq other
                else -> false
            }
        }
        val newScales = if (index == -1) {
            val newBase : ScaleBase = Either.Right(other)
            scales + listOf(newBase to FltX.one)
        } else {
            scales.mapIndexed { i, it ->
                if (index == i) {
                    it.first to (it.second + FltX.one)
                } else {
                    it
                }
            }
        }
        return Scale(newScales.tidy())
    }

    operator fun div(other: FltX): Scale {
        if (other eq FltX.zero) {
            throw ArithmeticException("Cannot divide by zero")
        }
        val scale = scales.find {
            when (val base = it.first) {
                is Either.Left -> base.value eq other
                else -> false
            }
        }
        val newScales = if (scale != null) {
            scales.map {
                if (scale == it) {
                    it.first to (it.second - FltX.one)
                } else {
                    it
                }
            }
        } else {
            val newBase : ScaleBase = Either.Left(other)
            scales + listOf(newBase to -FltX.one)
        }
        return Scale(newScales.tidy())
    }

    operator fun div(other: RtnX): Scale {
        if (other eq RtnX.zero) {
            throw ArithmeticException("Cannot divide by zero")
        }
        val scale = scales.find {
            when (val base = it.first) {
                is Either.Right -> base.value eq other
                else -> false
            }
        }
        val newScales = if (scale != null) {
            scales.map {
                if (scale == it) {
                    it.first to (it.second - FltX.one)
                } else {
                    it
                }
            }
        } else {
            val newBase : ScaleBase = Either.Right(other)
            scales + listOf(newBase to -FltX.one)
        }
        return Scale(newScales.tidy())
    }

    operator fun times(other: Scale): Scale {
        val newScales = this.scales.toMutableList()
        // todo: binary search and merge insert
        for (scale in other.scales) {
            val index = newScales.indexOfFirst {
                when (val lBase = it.first) {
                    is Either.Left -> when (val rBase = scale.first) {
                        is Either.Left -> lBase.value eq rBase.value
                        is Either.Right -> false
                    }
                    is Either.Right -> when (val rBase = scale.first) {
                        is Either.Left -> false
                        is Either.Right -> lBase.value eq rBase.value
                    }
                }
            }
            if (index == -1) {
                newScales.add(scale)
            } else {
                newScales[index] = scale.first to (newScales[index].second + scale.second)
            }
        }
        return Scale(newScales.tidy())
    }

    operator fun div(other: Scale): Scale {
        val newScales = this.scales.toMutableList()
        // todo: binary search and merge insert
        for (scale in other.scales) {
            val index = newScales.indexOfFirst {
                when (val lBase = it.first) {
                    is Either.Left -> when (val rBase = scale.first) {
                        is Either.Left -> lBase.value eq rBase.value
                        is Either.Right -> false
                    }
                    is Either.Right -> when (val rBase = scale.first) {
                        is Either.Left -> false
                        is Either.Right -> lBase.value eq rBase.value
                    }
                }
            }
            if (index == -1) {
                newScales.add(scale.first to -scale.second)
            } else {
                newScales[index] = scale.first to (newScales[index].second - scale.second)
            }
        }
        return Scale(newScales.tidy())
    }
}

// for java
fun getValue(scale: Scale): FltX {
    return scale.value
}
