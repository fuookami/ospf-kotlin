/**
 * 方向轴排列桥接。
 * Orientation axis permutation bridge.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.QuantityAxisPermutation3
/**
 * Converts this orientation to the corresponding 3D axis permutation.
 * 将方向转换为对应的三维轴排列。
 *
 * @return The axis permutation for this orientation. / 对应方向的轴排列。
*/
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
