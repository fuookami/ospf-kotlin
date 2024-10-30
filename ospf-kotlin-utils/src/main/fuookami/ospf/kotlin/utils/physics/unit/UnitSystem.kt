package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

interface UnitSystem {
    val name: String

    val scales: Map<FundamentalQuantityDimension, Scale>
}

data object SI : UnitSystem {
    override val name: String = "SI"

    override val scales: Map<FundamentalQuantityDimension, Scale> by lazy {
        FundamentalQuantityDimension.entries.associateWith { Scale() }
    }

    override fun toString(): String {
        return name
    }
}
