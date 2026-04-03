package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.LuminousIntensity

/**
 * 发光强度单位 / Luminous intensity units
 */

object Candela : PhysicalUnit() {
    override val name: String = "candela"
    override val symbol: String = "cd"

    override val quantity = LuminousIntensity
    override val scale = Scale()
}