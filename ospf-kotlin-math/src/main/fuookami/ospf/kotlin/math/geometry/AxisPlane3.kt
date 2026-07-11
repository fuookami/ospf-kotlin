/**
 * 三维主平面
 * 3D Principal Plane
 *
 * 定义三维几何空间中的主平面枚举（XY、XZ、YZ），为纯几何概念，可跨域迁移。
 * Defines principal plane enum (XY, XZ, YZ) in 3D geometric space, a pure geometry concept migratable across domains.
*/
package fuookami.ospf.kotlin.math.geometry

/**
 * 三维主平面纯几何定义，可跨域迁移。
 * Pure principal plane definition in 3D, migratable across domains.
 *
 * @property firstAxis 平面第一轴 / First axis of the plane
 * @property secondAxis 平面第二轴 / Second axis of the plane
 * @property normalAxis 平面法向轴 / Normal axis of the plane
*/
enum class AxisPlane3(
    val firstAxis: Axis3,
    val secondAxis: Axis3,
    val normalAxis: Axis3
) {
    /** XY 平面，法向为 Z / XY plane, normal is Z */
    XY(Axis3.X, Axis3.Y, Axis3.Z),
    /** XZ 平面，法向为 Y / XZ plane, normal is Y */
    XZ(Axis3.X, Axis3.Z, Axis3.Y),
    /** YZ 平面，法向为 X / YZ plane, normal is X */
    YZ(Axis3.Y, Axis3.Z, Axis3.X);

    /**
     * 判断指定轴是否属于该平面
     * Check whether the specified axis belongs to this plane
     *
     * @param axis 待检测的轴 / The axis to check
     * @return 是否属于该平面 / Whether the axis belongs to this plane
    */
    fun contains(axis: Axis3): Boolean {
        return axis == firstAxis || axis == secondAxis
    }
}
