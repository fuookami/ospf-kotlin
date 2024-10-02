package fuookami.ospf.kotlin.utils.physics.dimension

enum class FundamentalQuantityDimension {
    Length {
        override fun toString() = "L"
    },
    Mass {
        override fun toString() = "M"
    },
    Time {
        override fun toString() = "T"
    },
    Current {
        override fun toString() = "I"
    },
    Temperature {
        override fun toString() = "Θ"
    },
    SubstanceAmount {
        override fun toString() = "N"
    },
    LuminousIntensity {
        override fun toString() = "J"
    },
    Rad {
        override fun toString() = "rad"
    },
    Sr {
        override fun toString() = "sr"
    },
    Information {
        override fun toString() = "B"
    }
}

val L = FundamentalQuantityDimension.Length
val M = FundamentalQuantityDimension.Mass
val T = FundamentalQuantityDimension.Time
val I = FundamentalQuantityDimension.Current
val Theta = FundamentalQuantityDimension.Temperature
val N = FundamentalQuantityDimension.SubstanceAmount
val J = FundamentalQuantityDimension.LuminousIntensity
val rad = FundamentalQuantityDimension.Rad
val sr = FundamentalQuantityDimension.Sr
val B = FundamentalQuantityDimension.Information

data class FundamentalQuantity(val dimension: FundamentalQuantityDimension, val index: Int = 1) {
    operator fun plus(rhs: FundamentalQuantity): FundamentalQuantity {
        assert(dimension == rhs.dimension)
        return FundamentalQuantity(dimension, this.index + rhs.index)
    }

    operator fun minus(rhs: FundamentalQuantity): FundamentalQuantity {
        assert(dimension == rhs.dimension)
        return FundamentalQuantity(dimension, this.index - rhs.index)
    }

    operator fun unaryMinus(): FundamentalQuantity {
        return FundamentalQuantity(dimension, -index)
    }

    override fun toString() = "$dimension$index"
}

operator fun FundamentalQuantityDimension.times(index: Int) = FundamentalQuantity(this, index)
operator fun FundamentalQuantity.times(index: Int) = FundamentalQuantity(dimension, index * this.index)
operator fun FundamentalQuantity.div(index: Int) = FundamentalQuantity(dimension, this.index / index)
