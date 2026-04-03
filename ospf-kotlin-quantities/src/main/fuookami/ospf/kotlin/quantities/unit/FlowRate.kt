package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.FlowRate

object CubicMeterPerSecond : DerivedPhysicalUnit(CubicMeter / Second) {
    override val name = "cubic meter per second"
    override val symbol = "m3ps"

    override val quantity = FlowRate
}

object LiterPerSecond : DerivedPhysicalUnit(Liter / Second) {
    override val name = "liter per second"
    override val symbol = "Lps"

    override val quantity = FlowRate
}
