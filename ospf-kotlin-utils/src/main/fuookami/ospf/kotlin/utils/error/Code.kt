package fuookami.ospf.kotlin.utils.error

enum class ErrorCode(private val code: UByte) {
    None(0x00U),
    AuthenticationError(0x01U),

    NotAFile(0x10U),
    NotADirectory(0x11U),
    FileNotFound(0x12U),
    DirectoryUnusable(0x13U),
    FileExtensionNotMatched(0x14U),
    DataNotFound(0x15U),
    DataEmpty(0x16U),
    EnumVisitorEmpty(0x17U),
    UniqueBoxLocked(0x18U),
    UniqueRefLocked(0x19U),
    SerializationFailed(0x1aU),
    DeserializationFailed(0x1bU),

    TokenExisted(0x20U),
    SymbolRepetitive(0x21U),
    LackOfPipelines(0x22U),
    SolverNotFound(0x23U),
    OREngineEnvironmentLost(0x24U),
    OREngineConnectionOvertime(0x25U),
    OREngineModelingException(0x26U),
    OREngineSolvingException(0x27U),
    OREngineTerminated(0x28U),
    ORModelInfeasible(0x29U),
    ORModelUnbounded(0x2aU),
    ORModelInfeasibleOrUnbounded(0x2aU),
    ORSolutionInvalid(0x2cU),

    ApplicationFailed(0x30U),
    ApplicationError(0x31U),
    ApplicationException(0x32U),
    ApplicationStopped(0x33U),
    IllegalArgument(0x34U),

    Other(0xfeU),
    Unknown(0xffU);

    companion object {
        fun from(code: UByte) = ErrorCode.entries.first { it.code == code }
        fun from(code: ULong) = ErrorCode.entries.first { it.code.toULong() == code }
    }

    fun toUByte() = this.code
    fun toULong() = this.code.toULong()

    override fun toString(): String {
        return this.name
    }
}

// todo: find localize way to set default message