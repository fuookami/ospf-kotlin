package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

/** Category of a unit load device (ULD). */
enum class ULDCategory {
    Pallet,
    Container
}

/** Standard ULD type codes used in air cargo operations. */
enum class ULDCode {
    PAG,
    PAJ,
    PMC,
    PRA,
    PLA,
    PYB,
    P1P,
    PZA,
    PGA,

    SZX {
        override val category = ULDCategory.Container
    },
    DQF {
        override val category = ULDCategory.Container
    },
    AAY {
        override val category = ULDCategory.Container
    },
    AAD {
        override val category = ULDCategory.Container
    },
    RTE {
        override val category = ULDCategory.Container
    },
    AAX {
        override val category = ULDCategory.Container
    },
    AKE {
        override val category = ULDCategory.Container
    },
    DPE {
        override val category = ULDCategory.Container
    },
    P6P {
        override val category = ULDCategory.Container
    },
    LAY {
        override val category = ULDCategory.Container
    },
    ALF {
        override val category = ULDCategory.Container
    },
    AMA {
        override val category = ULDCategory.Container
    },
    AMD {
        override val category = ULDCategory.Container
    },
    FQA {
        override val category = ULDCategory.Container
    };

    open val category = ULDCategory.Pallet
}

/** A unit load device (ULD) identified by name and optional code, with caching for reuse. */
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
