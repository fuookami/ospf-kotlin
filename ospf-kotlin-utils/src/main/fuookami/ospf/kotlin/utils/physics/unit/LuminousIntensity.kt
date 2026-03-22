package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.Scale
import fuookami.ospf.kotlin.utils.physics.dimension.LuminousIntensity

/**
 * 发光强度单位 / Luminous intensity units
 */

object Candela : PhysicalUnit() {
    override val name: String = "candela"
    override val symbol: String = "cd"

    override val quantity = LuminousIntensity
    override val scale = Scale()
}