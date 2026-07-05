/**
 * 定义包裹类型（包装箱/托盘），作为 BPP3D 基础设施层的类型标记接口。
 * Defines package types (box/pallet) as a type-marking interface in the BPP3D infrastructure layer.
 */
@file:JvmName("PackageType")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

sealed interface PackageType

object PackageTypeBox : PackageType
object PackageTypePallet : PackageType
