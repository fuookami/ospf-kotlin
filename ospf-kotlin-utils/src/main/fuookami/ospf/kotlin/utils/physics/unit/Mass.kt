package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Microgram : DerivedPhysicalUnit(Gram * Scale.micro) {
    override val name: String = "microgram"
    override val symbol: String = "µg"
}

object Milligram : DerivedPhysicalUnit(Gram * Scale.milli) {
    override val name: String = "milligram"
    override val symbol: String = "mg"
}

object Point : DerivedPhysicalUnit(Milligram * 2) {
    override val name: String = "point"
    override val symbol: String = "pt"
}

object Carat : DerivedPhysicalUnit(Point * Scale(10, 2)) {
    override val name: String = "carat"
    override val symbol: String = "ct"
}

object Gram : PhysicalUnit() {
    override val name: String = "gram"
    override val symbol: String = "g"

    override val quantity = Mass
    override val system = SI
    override val scale = Scale(10, -3)
}

object Kilogram : PhysicalUnit() {
    override val name: String = "kilogram"
    override val symbol: String = "kg"

    override val quantity = Mass
    override val system = SI
    override val scale = Scale()
}

object Kintal : DerivedPhysicalUnit(Kilogram * Scale.hecto) {
    override val name: String = "kintal"
    override val symbol: String = "q"
}

object Ton : DerivedPhysicalUnit(Kilogram * Scale.kilo) {
    override val name: String = "ton"
    override val symbol: String = "t"
}

object Pound : DerivedPhysicalUnit(Gram * 453.59237) {
    override val name: String = "pound"
    override val symbol: String = "lb"
}

object Gran : DerivedPhysicalUnit(Pound * Scale(7000, -1)) {
    override val name: String = "gran"
    override val symbol: String = "gr"
}

object LongTon : DerivedPhysicalUnit(Pound * 2240) {
    override val name: String = "long ton"
    override val symbol: String = "lt"
}

object ShortTon : DerivedPhysicalUnit(Pound * 2000) {
    override val name: String = "short ton"
    override val symbol: String = "st"
}

object Stone : DerivedPhysicalUnit(Pound * 14) {
    override val name: String = "stone"
    override val symbol: String = "st"
}

object Ounce : DerivedPhysicalUnit(Pound * Scale(2, -4)) {
    override val name: String = "ounce"
    override val symbol: String = "oz"
}

object TroyOunce : DerivedPhysicalUnit(Pound * Scale(12, -1)) {
    override val name: String = "ounce"
    override val symbol: String = "oz.tr"
}

object Dram : DerivedPhysicalUnit(Ounce * Scale(2, -4)) {
    override val name: String = "dram"
    override val symbol: String = "dr"
}
