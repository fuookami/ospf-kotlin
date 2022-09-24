package fuookami.ospf.kotlin.utils.error

import fuookami.ospf.kotlin.utils.math.UInt64

enum class ErrorCode(private val code: UInt64) {
    None(UInt64(0x00U)),

    NotAFile(UInt64(0x100U)),
    NotADirectory(UInt64(0x101U)),
    FileNotFound(UInt64(0x102U)),
    DirectoryUnusable(UInt64(0x103U)),
    FileExtensionNotMatched(UInt64(0x104U)),
    DataNotFound(UInt64(0x105U)),
    DataEmpty(UInt64(0x106U)),
    EnumVisitorEmpty(UInt64(0x107U)),
    UniqueBoxLocked(UInt64(0x108U)),
    UniqueRefLocked(UInt64(0x109U)),
    SerializationFail(UInt64(0x10aU)),
    DeserializationFail(UInt64(0x10bU)),

    LackOfPipelines(UInt64(0x1000U)),
    SolverNotFound(UInt64(0x1001U)),
    OREngineEnvironmentLost(UInt64(0x1002U)),
    OREngineConnectionOvertime(UInt64(0x1003U)),
    OREngineModelingException(UInt64(0x1004U)),
    OREngineSolvingException(UInt64(0x1005U)),
    OREngineTerminated(UInt64(0x1006U)),
    ORModelNoSolution(UInt64(0x1007U)),
    ORModelUnbounded(UInt64(0x1008U)),
    ORSolutionInvalid(UInt64(0x1009U)),

    ApplicationFail(UInt64(0x10000U)),
    ApplicationError(UInt64(0x10001U)),
    ApplicationException(UInt64(0x10002U)),
    ApplicationStop(UInt64(0x10003U)),

    Other(UInt64.maximum - UInt64.one),
    Unknown(UInt64.maximum);

    companion object {
        fun from(code: UInt64) = ErrorCode.values().first { it.code == code }
    }

    fun toUInt64() = this.code
}

// todo: find localize way to set default message
