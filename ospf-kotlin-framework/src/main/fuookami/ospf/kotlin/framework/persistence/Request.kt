/**
 * 请求与响应 DTO 接口
 * Request and Response DTO interfaces
 *
 * 定义 F-bounded 泛型约束的请求和响应数据传输对象。
 * Defines F-bounded generic constrained request and response data transfer objects.
*/
package fuookami.ospf.kotlin.framework.persistence

import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 请求 DTO 接口（F-bounded 泛型）
 * Request DTO interface (F-bounded generic)
 *
 * @param T 自身类型，用于 F-bounded 泛型约束 / Self type for F-bounded generic constraint
*/
interface RequestDTO<T : RequestDTO<T>> {

    /** 请求标识 / Request identifier */
    val id: String
}

/**
 * 响应 DTO 接口（F-bounded 泛型）
 * Response DTO interface (F-bounded generic)
 *
 * @param T 自身类型，用于 F-bounded 泛型约束 / Self type for F-bounded generic constraint
*/
interface ResponseDTO<T : ResponseDTO<T>> {

    /** 响应标识 / Response identifier */
    val id: String

    /** 状态码 / Status code */
    val code: UInt64

    /** 消息 / Message */
    val msg: String
}


