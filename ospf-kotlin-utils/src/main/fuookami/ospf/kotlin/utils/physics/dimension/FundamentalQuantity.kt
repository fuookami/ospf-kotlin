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
        override fun toString() = "n"
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

data class FundamentalQuantity(val dimension: FundamentalQuantityDimension, val index: Int = 1) {
    operator fun plus(rhs: FundamentalQuantity): FundamentalQuantity {
        assert(dimension == rhs.dimension)
        return FundamentalQuantity(dimension, this.index + rhs.index)
    }

    operator fun minus(rhs: FundamentalQuantity): FundamentalQuantity {
        assert(dimension == rhs.dimension)
        return FundamentalQuantity(dimension, this.index - rhs.index)
    }

    override fun toString() = "$dimension$index"
}
