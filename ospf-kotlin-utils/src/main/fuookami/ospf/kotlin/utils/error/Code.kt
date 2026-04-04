package fuookami.ospf.kotlin.utils.error

/**
 * 错误码枚举
 *
 * Enumeration of error codes used throughout the application.
 * Each error code is a unique UByte value.
 *
 * UTL-007: 修复重复的错误码值
 * UTL-007: Fixed duplicate error code values.
 *
 * @property code 错误码值 / Error code value
 */
enum class ErrorCode(private val code: UByte) {
    /**
     * 无错误
     *
     * No error.
     */
    None(0x00U),

    /**
     * 认证错误
     *
     * Authentication error.
     */
    AuthenticationError(0x01U),

    /**
     * 不是文件
     *
     * Not a file.
     */
    NotAFile(0x10U),

    /**
     * 不是目录
     *
     * Not a directory.
     */
    NotADirectory(0x11U),

    /**
     * 文件未找到
     *
     * File not found.
     */
    FileNotFound(0x12U),

    /**
     * 目录不可用
     *
     * Directory unusable.
     */
    DirectoryUnusable(0x13U),

    /**
     * 文件扩展名不匹配
     *
     * File extension not matched.
     */
    FileExtensionNotMatched(0x14U),

    /**
     * 数据未找到
     *
     * Data not found.
     */
    DataNotFound(0x15U),

    /**
     * 数据为空
     *
     * Data empty.
     */
    DataEmpty(0x16U),

    /**
     * 枚举访问器为空
     *
     * Enum visitor empty.
     */
    EnumVisitorEmpty(0x17U),

    /**
     * 唯一值容器已锁定
     *
     * Unique box locked.
     */
    UniqueBoxLocked(0x18U),

    /**
     * 唯一引用已锁定
     *
     * Unique reference locked.
     */
    UniqueRefLocked(0x19U),

    /**
     * 序列化失败
     *
     * Serialization failed.
     */
    SerializationFailed(0x1aU),

    /**
     * 反序列化失败
     *
     * Deserialization failed.
     */
    DeserializationFailed(0x1bU),

    /**
     * Token 已存在
     *
     * Token existed.
     */
    TokenExisted(0x20U),

    /**
     * 符号重复
     *
     * Symbol repetitive.
     */
    SymbolRepetitive(0x21U),

    /**
     * 缺少管道
     *
     * Lack of pipelines.
     */
    LackOfPipelines(0x22U),

    /**
     * 求解器未找到
     *
     * Solver not found.
     */
    SolverNotFound(0x23U),

    /**
     * OR 引擎环境丢失
     *
     * OR engine environment lost.
     */
    OREngineEnvironmentLost(0x24U),

    /**
     * OR 引擎连接超时
     *
     * OR engine connection overtime.
     */
    OREngineConnectionOvertime(0x25U),

    /**
     * OR 引擎建模异常
     *
     * OR engine modeling exception.
     */
    OREngineModelingException(0x26U),

    /**
     * OR 引擎求解异常
     *
     * OR engine solving exception.
     */
    OREngineSolvingException(0x27U),

    /**
     * OR 引擎已终止
     *
     * OR engine terminated.
     */
    OREngineTerminated(0x28U),

    /**
     * OR 模型不可行
     *
     * OR model infeasible.
     */
    ORModelInfeasible(0x29U),

    /**
     * OR 模型无界
     *
     * OR model unbounded.
     */
    ORModelUnbounded(0x2aU),

    /**
     * OR 模型不可行或无界
     *
     * OR model infeasible or unbounded.
     *
     * BUG FIX: 原值 0x2aU 与 ORModelUnbounded 重复，改为 0x2bU
     * FIX: Original value 0x2aU duplicates ORModelUnbounded, changed to 0x2bU
     */
    ORModelInfeasibleOrUnbounded(0x2bU),

    /**
     * OR 解无效
     *
     * OR solution invalid.
     *
     * 原值 0x2cU 保持不变
     * Original value 0x2cU unchanged.
     */
    ORSolutionInvalid(0x2cU),

    /**
     * 应用失败
     *
     * Application failed.
     */
    ApplicationFailed(0x30U),

    /**
     * 应用错误
     *
     * Application error.
     */
    ApplicationError(0x31U),

    /**
     * 应用异常
     *
     * Application exception.
     */
    ApplicationException(0x32U),

    /**
     * 应用已停止
     *
     * Application stopped.
     */
    ApplicationStopped(0x33U),

    /**
     * 非法参数
     *
     * Illegal argument.
     */
    IllegalArgument(0x34U),

    /**
     * 其他错误
     *
     * Other error.
     */
    Other(0xfeU),

    /**
     * 未知错误
     *
     * Unknown error.
     */
    Unknown(0xffU);

    companion object {
        /**
         * 从错误码值获取 ErrorCode
         *
         * Get ErrorCode from error code value.
         *
         * UTL-007: 增强健壮性，未知 code 返回 Unknown 而不是抛出异常
         * UTL-007: Enhanced robustness, returns Unknown for unknown codes instead of throwing exception.
         *
         * @param code 错误码值 / Error code value
         * @return 对应的 ErrorCode，未知时返回 Unknown / Corresponding ErrorCode, returns Unknown if not found
         */
        fun from(code: UByte) = ErrorCode.entries.find { it.code == code } ?: Unknown

        /**
         * 从 ULong 错误码值获取 ErrorCode
         *
         * Get ErrorCode from ULong error code value.
         *
         * @param code ULong 错误码值 / ULong error code value
         * @return 对应的 ErrorCode，未知时返回 Unknown / Corresponding ErrorCode, returns Unknown if not found
         */
        fun from(code: ULong) = ErrorCode.entries.find { it.code.toULong() == code } ?: Unknown
    }

    /**
     * 转换为 UByte
     *
     * Convert to UByte.
     *
     * @return UByte 错误码值 / UByte error code value
     */
    fun toUByte() = this.code

    /**
     * 转换为 ULong
     *
     * Convert to ULong.
     *
     * @return ULong 错误码值 / ULong error code value
     */
    fun toULong() = this.code.toULong()

    override fun toString(): String {
        return this.name
    }
}

// todo: find localize way to set default message