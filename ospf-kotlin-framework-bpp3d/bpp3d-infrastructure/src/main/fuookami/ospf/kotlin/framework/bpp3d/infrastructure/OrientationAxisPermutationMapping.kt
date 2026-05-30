/**
 * 方向轴排列桥接。
 * Orientation axis permutation bridge.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.QuantityAxisPermutation3

fun Orientation.toAxisPermutation3(): QuantityAxisPermutation3 {
    return when (this) {
        Orientation.Upright -> QuantityAxisPermutation3.XYZ
        Orientation.UprightRotated -> QuantityAxisPermutation3.ZYX
        Orientation.Side -> QuantityAxisPermutation3.YXZ
        Orientation.SideRotated -> QuantityAxisPermutation3.ZXY
        Orientation.Lie -> QuantityAxisPermutation3.XZY
        Orientation.LieRotated -> QuantityAxisPermutation3.YZX
    }
}
