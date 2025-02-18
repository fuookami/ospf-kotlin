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

object Inch : PhysicalUnit() {
    override val name = "inch"
    override val symbol = "in"

    override val quantity = Length
    override val system = SI
    override val scale = Centimeter.scale * Scale(Flt64(2.54), Flt64.one)
}

object Foot : PhysicalUnit() {
    override val name = "foot"
    override val symbol = "ft"

    override val quantity = Length
    override val system = SI
    override val scale = Inch.scale * Scale(Flt64(12.0), Flt64.one)
}

object Yard : PhysicalUnit() {
    override val name = "yard"
    override val symbol = "yd"

    override val quantity = Length
    override val system = SI
    override val scale = Foot.scale * Scale(Flt64(3.0), Flt64.one)
}

object Mile : PhysicalUnit() {
    override val name = "mile"
    override val symbol = "mi"

    override val quantity = Length
    override val system = SI
    override val scale = Yard.scale * Scale(Flt64(1760.0), Flt64.one)
}
