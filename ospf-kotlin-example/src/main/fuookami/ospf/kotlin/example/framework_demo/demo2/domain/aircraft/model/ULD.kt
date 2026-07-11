package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

/**
 * Category of a unit load device (ULD).
 * 单元装载设备（ULD）的类别。
*/
enum class ULDCategory {
    /** Pallet type / 板型 */
    Pallet,
    /** Container type / 箱型 */
    Container
}

/**
 * Standard ULD type codes used in air cargo operations.
 * 航空货运操作中使用的标准 ULD 类型代码。
*/
enum class ULDCode {
    /** PAG pallet / PAG 板 */
    PAG,
    /** PAJ pallet / PAJ 板 */
    PAJ,
    /** PMC pallet / PMC 板 */
    PMC,
    /** PRA pallet / PRA 板 */
    PRA,
    /** PLA pallet / PLA 板 */
    PLA,
    /** PYB pallet / PYB 板 */
    PYB,
    /** P1P pallet / P1P 板 */
    P1P,
    /** PZA pallet / PZA 板 */
    PZA,
    /** PGA pallet / PGA 板 */
    PGA,

    /** SZX container / SZX 箱 */
    SZX {
        override val category = ULDCategory.Container
    },
    /** DQF container / DQF 箱 */
    DQF {
        override val category = ULDCategory.Container
    },
    /** AAY container / AAY 箱 */
    AAY {
        override val category = ULDCategory.Container
    },
    /** AAD container / AAD 箱 */
    AAD {
        override val category = ULDCategory.Container
    },
    /** RTE container / RTE 箱 */
    RTE {
        override val category = ULDCategory.Container
    },
    /** AAX container / AAX 箱 */
    AAX {
        override val category = ULDCategory.Container
    },
    /** AKE container / AKE 箱 */
    AKE {
        override val category = ULDCategory.Container
    },
    /** DPE container / DPE 箱 */
    DPE {
        override val category = ULDCategory.Container
    },

    /** P6P container / P6P 箱 */
    P6P {
        override val category = ULDCategory.Container
    },

    /** LAY container / LAY 箱 */
    LAY {
        override val category = ULDCategory.Container
    },

    /** ALF container / ALF 箱 */
    ALF {
        override val category = ULDCategory.Container
    },

    /** AMA container / AMA 箱 */
    AMA {
        override val category = ULDCategory.Container
    },

    /** AMD container / AMD 箱 */
    AMD {
        override val category = ULDCategory.Container
    },

    /** FQA container / FQA 箱 */
    FQA {
        override val category = ULDCategory.Container
    };

    open val category = ULDCategory.Pallet
}

/**
 * A unit load device (ULD) identified by name and optional code, with caching for reuse.
 * 通过名称和可选代码标识的单元装载设备（ULD）（具有缓存以便重用）。
 *
 * @property name The name identifier of the ULD. / ULD 名称标识
 * @property code The optional ULD type code. / 可选的 ULD 类型代码
*/
data class ULD(
    val name: String,
    val code: ULDCode?
) {
    companion object {
        private val cache: MutableMap<String, ULD> = HashMap()

        operator fun invoke(code: ULDCode): ULD {
            return cache.getOrPut(code.name) {
                ULD(
                    name = code.name,
                    code = code
                )
            }
        }

        operator fun invoke(name: String): ULD {
            return cache.getOrPut(name) {
                ULD(
                    name = name,
                    code = ULDCode.entries.firstOrNull { it.name == name }
                )
            }
        }
    }

    val category = code?.category ?: ULDCategory.Pallet
}
