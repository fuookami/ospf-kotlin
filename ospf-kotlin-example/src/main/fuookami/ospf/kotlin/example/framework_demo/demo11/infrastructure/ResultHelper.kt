package fuookami.ospf.kotlin.example.framework_demo.demo11.infrastructure

import fuookami.ospf.kotlin.framework.infrastructure.RestfulApiResponse
import fuookami.ospf.kotlin.framework.infrastructure.PagedData
import fuookami.ospf.kotlin.framework.infrastructure.ApiError

/** API 响应映射器接口，用于将业务对象转换为 RESTful 响应 / API response mapper interface for converting business objects to RESTful responses */
interface ResultMapper<T, R> {
    /**
     * 将源数据转换为目标类型 / Converts source data to target type
     * @param value 待转换的源数据 / Source data to be converted
     * @return 转换后的目标类型数据 / Converted target type data
     */
    fun map(value: T): R
}

/**
 * 分页响应封装，包含数据列表、总数及分页信息 / Paged response wrapper containing data list, total count and pagination info
 * @property data 当前页的数据列表 / Data list of the current page
 * @property total 符合条件的数据总数 / Total count of matching data
 * @property page 当前页码（从 1 开始） / Current page number (starting from 1)
 * @property pageSize 每页数据条数 / Number of items per page
 */
data class PagedResponse<T>(
    val data: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)

/**
 * 返回成功的 RESTful API 响应 / Returns a successful RESTful API response
 * @param data 响应数据 / Response data
 * @return 包含数据的成功响应 / Successful response containing the data
 */
fun <T> success(data: T): RestfulApiResponse<T> {
    return RestfulApiResponse(
        data = data
    )
}

/**
 * 返回成功的 RESTful API 响应（无数据） / Returns a successful RESTful API response (no data)
 * @return 空成功的响应 / Empty successful response
 */
fun success(): RestfulApiResponse<Unit> {
    return RestfulApiResponse(
        data = Unit
    )
}

/**
 * 返回创建成功的 RESTful API 响应 / Returns a created-successfully RESTful API response
 * @param data 响应数据 / Response data
 * @return 包含创建状态码和数据的响应 / Response containing created status code and data
 */
fun <T> created(data: T): RestfulApiResponse<T> {
    return RestfulApiResponse(
        code = 201,
        data = data
    )
}

/**
 * 返回分页查询的 RESTful API 响应 / Returns a paged query RESTful API response
 * @param data 分页响应数据 / Paged response data
 * @return 包含分页数据的响应 / Response containing paged data
 */
fun <T> paged(data: PagedResponse<T>): RestfulApiResponse<PagedData<T>> {
    return RestfulApiResponse(
        data = PagedData(
            data = data.data,
            total = data.total,
            page = data.page,
            pageSize = data.pageSize
        )
    )
}

/**
 * 返回业务错误的 RESTful API 响应 / Returns a business-error RESTful API response
 * @param code 错误码 / Error code
 * @param message 错误描述 / Error description
 * @return 包含错误码和错误信息的响应 / Response containing error code and error message
 */
fun error(code: Int, message: String): RestfulApiResponse<ApiError> {
    return RestfulApiResponse(
        code = code,
        data = ApiError(
            code = code,
            message = message
        )
    )
}

/**
 * 返回 404 未找到的 RESTful API 响应 / Returns a 404 not-found RESTful API response
 * @param message 未找到的描述信息 / Description of the not-found error
 * @return 包含 404 状态码和错误信息的响应 / Response containing 404 status code and error info
 */
fun notFound(message: String = "Not Found"): RestfulApiResponse<ApiError> {
    return RestfulApiResponse(
        code = 404,
        data = ApiError(
            code = 404,
            message = message
        )
    )
}

/**
 * 返回 401 未授权的 RESTful API 响应 / Returns a 401 unauthorized RESTful API response
 * @param message 未授权的描述信息 / Description of the unauthorized error
 * @return 包含 401 状态码和错误信息的响应 / Response containing 401 status code and error info
 */
fun unauthorized(message: String = "Unauthorized"): RestfulApiResponse<ApiError> {
    return RestfulApiResponse(
        code = 401,
        data = ApiError(
            code = 401,
            message = message
        )
    )
}

/**
 * 返回 403 禁止访问的 RESTful API 响应 / Returns a 403 forbidden RESTful API response
 * @param message 禁止访问的描述信息 / Description of the forbidden error
 * @return 包含 403 状态码和错误信息的响应 / Response containing 403 status code and error info
 */
fun forbidden(message: String = "Forbidden"): RestfulApiResponse<ApiError> {
    return RestfulApiResponse(
        code = 403,
        data = ApiError(
            code = 403,
            message = message
        )
    )
}

/**
 * 返回 400 错误请求的 RESTful API 响应 / Returns a 400 bad-request RESTful API response
 * @param message 错误请求的描述信息 / Description of the bad-request error
 * @return 包含 400 状态码和错误信息的响应 / Response containing 400 status code and error info
 */
fun badRequest(message: String = "Bad Request"): RestfulApiResponse<ApiError> {
    return RestfulApiResponse(
        code = 400,
        data = ApiError(
            code = 400,
            message = message
        )
    )
}

/**
 * 返回 422 参数校验错误的 RESTful API 响应 / Returns a 422 validation-error RESTful API response
 * @param message 校验错误的描述信息 / Description of the validation error
 * @return 包含 422 状态码和错误信息的响应 / Response containing 422 status code and error info
 */
fun validationError(message: String = "Validation Error"): RestfulApiResponse<ApiError> {
    return RestfulApiResponse(
        code = 422,
        data = ApiError(
            code = 422,
            message = message
        )
    )
}

/**
 * 返回 500 服务器内部错误的 RESTful API 响应 / Returns a 500 internal-server-error RESTful API response
 * @param message 内部错误的描述信息 / Description of the internal error
 * @return 包含 500 状态码和错误信息的响应 / Response containing 500 status code and error info
 */
fun internalError(message: String = "Internal Server Error"): RestfulApiResponse<ApiError> {
    return RestfulApiResponse(
        code = 500,
        data = ApiError(
            code = 500,
            message = message
        )
    )
}

/**
 * 使用映射器将数据转换为目标类型并返回成功的 RESTful 响应 / Maps data to target type using mapper and returns a successful RESTful response
 * @param mapper 用于转换数据的映射器 / Mapper used to convert data
 * @return 包含转换后数据的成功响应 / Successful response containing the mapped data
 */
fun <T, R> T.mapTo(mapper: ResultMapper<T, R>): RestfulApiResponse<R> {
    return success(mapper.map(this))
}
