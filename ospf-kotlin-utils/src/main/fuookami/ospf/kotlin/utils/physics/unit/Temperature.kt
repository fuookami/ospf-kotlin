package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.Scale
import fuookami.ospf.kotlin.utils.physics.dimension.Temperature

/**
 * 温度单位 / Temperature units
 */

object Kelvin : PhysicalUnit() {
    override val name: String = "kelvin"
    override val symbol: String = "K"

    override val quantity = Temperature
    override val scale = Scale()
}

object Celsius : PhysicalUnit() {
    override val name: String = "celsius"
    override val symbol: String = "°C"

    override val quantity = Temperature
    override val scale = Scale()
}

object Fahrenheit : PhysicalUnit() {
    override val name: String = "fahrenheit"
    override val symbol: String = "°F"

    override val quantity = Temperature
    override val scale = Scale(5.0 / 9.0)
}

object Rankine : PhysicalUnit() {
    override val name: String = "rankine"
    override val symbol: String = "°R"

    override val quantity = Temperature
    override val scale = Scale(5.0 / 9.0)
}