@file:Suppress("DEPRECATION")

/**
 * 包装类型基础设施。
 * Package type infrastructure.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

/**
 * Package classification indicating whether the package is for outer or inner use.
 * 包装分类，表示包装是外包装还是内包装。
*/
enum class PackageClassification {
    /** Outer packaging / 外包装 */
    Outer,
    /** Inner packaging / 内包装 */
    Inner
}

/**
 * Package category describing the physical form of the package.
 * 包装类别，描述包装的物理形态。
*/
enum class PackageCategory {
    /** Hard box package / 硬箱包装 */
    HardBox,
    /** Pallet package / 托盘包装 */
    Pallet,
    /** Soft box package / 软箱包装 */
    SoftBox,
    /** Filler material / 填充物 */
    Filler
}

/**
 * Package type enumeration with associated category.
 * 包装类型枚举，附带关联的包装类别。
*/
enum class PackageType {
    /** Duty corrugated board pedal / 免税瓦楞纸板踏板 */
    DutyCorrugatedBoardPedal {
        override val category = PackageCategory.HardBox
    },

    /** Wooden container / 木制容器 */
    WoodenContainer {
        override val category = PackageCategory.HardBox
    },

    /** Honeycomb box / 蜂窝箱 */
    HoneycombBox {
        override val category = PackageCategory.HardBox
    },

    /** Pallet / 托盘 */
    Pallet {
        override val category = PackageCategory.Pallet
    },

    /** Carton pallet / 纸箱托盘 */
    CartonPallet {
        override val category = PackageCategory.Pallet
    },

    /** Carton container / 纸箱容器 */
    CartonContainer {
        override val category = PackageCategory.SoftBox
    },

    /** Packing foam / 包装泡沫 */
    PackingFoam {
        override val category = PackageCategory.Filler
    };

    abstract val category: PackageCategory
}
