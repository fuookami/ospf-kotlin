package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Gram : PhysicalUnit() {
    override val name: String = "gram"
    override val symbol: String = "g"

    override val quantity = Mass
    override val system = SI
    override val scale = Scale(Flt64.ten, -Flt64.three)
}

object Kilogram : PhysicalUnit() {
    override val name: String = "kilogram"
    override val symbol: String = "kg"

    override val quantity = Mass
    override val system = SI
    override val scale = Scale()
}

object Ton : PhysicalUnit() {
    override val name: String = "ton"
    override val symbol: String = "t"

    override val quantity = Mass
    override val system = SI
    override val scale = Scale(Flt64.ten, Flt64.three)
}
