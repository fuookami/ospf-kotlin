package fuookami.ospf.kotlin.core.model.basic

enum class ObjectCategory {
    Maximum {
        override val reverse get() = Minimum
        override fun toString() = "Maximum"
    },
    Minimum {
        override val reverse get() = Maximum
        override fun toString() = "Minimum"
    };

    abstract val reverse: ObjectCategory
}
