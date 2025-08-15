package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object KilogramPerCubicMeter : DerivedPhysicalUnit(Kilogram / CubicMeter) {
    override val name = "kilogram per cubic meter"
    override val symbol = "kgpm3"

    override val quantity = MassDensity
}

object KilogramPerLiter : DerivedPhysicalUnit(Kilogram / Liter) {
    override val name = "kilogram per liter"
    override val symbol = "kgpL"

    override val quantity = MassDensity
}

object KilogramPerCubicCentimeter : DerivedPhysicalUnit(Kilogram / CubicCentimeter) {
    override val name = "kilogram per cubic meter"
    override val symbol = "kgpm3"

    override val quantity = MassDensity
}

object GramPerCubicCentimeter : DerivedPhysicalUnit(Gram / CubicCentimeter) {
    override val name = "gram per cubic meter"
    override val symbol = "gpm3"

    override val quantity = MassDensity
}
