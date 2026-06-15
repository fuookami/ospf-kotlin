/**
 * 包装类型基础设施。
 * Package type infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

enum class PackageClassification {
    Outer,
    Inner
}

enum class PackageCategory {
    HardBox,
    Pallet,
    SoftBox,
    Filler
}

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
