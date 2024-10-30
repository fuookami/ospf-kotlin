package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Millimeter : PhysicalUnit() {
    override val name = "millimeter"
    override val symbol = "mm"

    override val quantity = Length
    override val system = SI
    override val scale = Scale(Flt64.ten, -Flt64.three)
}

object Centimeter : PhysicalUnit() {
    override val name = "centimeter"
    override val symbol = "cm"

    override val quantity = Length
    override val system = SI
    override val scale = Scale(Flt64.ten, -Flt64.two)
}

object Decimeter : PhysicalUnit() {
    override val name = "decimeter"
    override val symbol = "dm"

    override val quantity = Length
    override val system = SI
    override val scale = Scale(Flt64.ten, -Flt64.one)
}

object Meter : PhysicalUnit() {
    override val name = "meter"
    override val symbol = "m"

    override val quantity = Length
    override val system = SI
    override val scale = Scale()
}

object Kilometer : PhysicalUnit() {
    override val name = "kilometer"
    override val symbol = "km"

    override val quantity = Length
    override val system = SI
    override val scale = Scale(Flt64.ten, Flt64.three)
}
