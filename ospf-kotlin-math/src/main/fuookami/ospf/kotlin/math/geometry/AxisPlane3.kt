package fuookami.ospf.kotlin.math.geometry

/**
 * 三维主平面纯几何定义，可跨域迁移。
 * Pure principal plane definition in 3D, migratable across domains.
 */
enum class AxisPlane3(
    val firstAxis: Axis3,
    val secondAxis: Axis3,
    val normalAxis: Axis3
) {
    XY(Axis3.X, Axis3.Y, Axis3.Z),
    XZ(Axis3.X, Axis3.Z, Axis3.Y),
    YZ(Axis3.Y, Axis3.Z, Axis3.X);

    fun contains(axis: Axis3): Boolean {
        return axis == firstAxis || axis == secondAxis
    }
}

