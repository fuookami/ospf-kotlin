package fuookami.ospf.kotlin.utils.physics.dimension

class DerivedQuantity(val quantities: List<FundamentalQuantity>) {
    constructor(dimension: FundamentalQuantityDimension) : this(arrayListOf(FundamentalQuantity(dimension))) {}
}
