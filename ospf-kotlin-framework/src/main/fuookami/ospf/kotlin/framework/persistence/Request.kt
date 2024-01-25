package fuookami.ospf.kotlin.framework.persistence

import fuookami.ospf.kotlin.utils.math.*

interface RequestDTO<T : RequestDTO<T>> {
    val id: String
}

interface ResponseDTO<T : ResponseDTO<T>> {
    val id: String
    val code: UInt64
    val msg: String
}
