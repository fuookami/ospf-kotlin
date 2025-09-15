package fuookami.ospf.kotlin.core.frontend.model.mechanism

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
