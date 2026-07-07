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
    Outer,
    Inner
}

/**
 * Package category describing the physical form of the package.
 * 包装类别，描述包装的物理形态。
 */
enum class PackageCategory {
    HardBox,
    Pallet,
    SoftBox,
    Filler
}

/**
 * Package type enumeration with associated category.
 * 包装类型枚举，附带关联的包装类别。
 */
enum class PackageType {
    DutyCorrugatedBoardPedal {
        override val category = PackageCategory.HardBox
    },

    WoodenContainer {
        override val category = PackageCategory.HardBox
    },

    HoneycombBox {
        override val category = PackageCategory.HardBox
    },

    Pallet {
        override val category = PackageCategory.Pallet
    },

    CartonPallet {
        override val category = PackageCategory.Pallet
    },

    CartonContainer {
        override val category = PackageCategory.SoftBox
    },

    PackingFoam {
        override val category = PackageCategory.Filler
    };

    abstract val category: PackageCategory
}
