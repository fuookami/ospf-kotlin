package fuookami.ospf.kotlin.utils.functional

infix fun Boolean?.and(other: Boolean?) = if (this != null && other != null) {
    this && other
} else {
    null
}
