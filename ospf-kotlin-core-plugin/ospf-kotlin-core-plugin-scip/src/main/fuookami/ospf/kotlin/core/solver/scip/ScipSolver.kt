/** SCIP 求解器基类 / SCIP solver base */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.scip

import java.io.File
import java.util.*
import kotlin.time.*
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.Library
import jscip.*

/** SCIP 求解器抽象基类，提供环境初始化、求解和状态分析的通用实现 / SCIP solver abstract base class, provides common implementation for environment initialization, solving, and status analysis */
@OptIn(ExperimentalTime::class)
abstract class ScipSolver : AutoCloseable {
    /** Companion object managing SCIP native library loading / 伴生对象，管理 SCIP 原生库加载 */
    companion object {
        internal var loadedLibrary = false

        init {
            try {
                System.loadLibrary("jscip")
                loadedLibrary = true
            } catch (_: Throwable) {
                loadedLibrary = false
            }
        }

        private val winLibraries = listOf("tbb", "libscip", "jscip")
        private val unixLibraries = listOf("libgcg", "libgmp", "libpthread", "libgfortran", "libquadmath", "libopenblas", "libtbb", "libsplexshared", "libscip", "libjscip")

        /** 从 JAR 包中加载 SCIP 原生库 / Load SCIP native library from JAR package */
        fun loadLibraryInJar(): Try {
            val systemType = System.getProperty("os.name")
            val libExtension = if (systemType.lowercase(Locale.getDefault()).indexOf("win") != -1) {
                "dll"
            } else if (systemType.lowercase(Locale.getDefault()).indexOf("mac") != -1) {
                "dylib"
            } else {
                "so"
            }
            val libs = if (systemType.lowercase(Locale.getDefault()).indexOf("win") != -1) {
                winLibraries
            } else if (systemType.lowercase(Locale.getDefault()).indexOf("mac") != -1) {
                emptyList()
            } else {
                unixLibraries
            }

            for (lib in libs) {
                val libFullName = "${lib}.${libExtension}"
                when (val result = Library.loadInJar(libFullName, File(System.getProperty("user.dir"), libFullName).absolutePath)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            return ok
        }
    }

    protected lateinit var scip: Scip
    protected lateinit var status: SolverStatus
    protected var solvingTime: Duration? = null

    /** 关闭 SCIP 求解器，释放资源 / Close SCIP solver, release resources */
    override fun close() {
        scip.free()
    }

    /**
     * 初始化 SCIP 求解器 / Initialize SCIP solver
     *
     * @param name 模型名称 / model name
     * @return 操作结果 / operation result
     */
    protected suspend fun init(name: String): Try {
        if (!loadedLibrary) {
            try {
                System.loadLibrary("jscip")
                loadedLibrary = true
            } catch (e: Throwable) {
                return Failed(ErrorCode.SolverNotFound, "failed to load jscip library")
            }
        }
        scip = Scip()
        scip.create(name)
        return ok
    }

    /**
     * 执行 SCIP 求解 / Execute SCIP solving
     *
     * @param threadNum 线程数 / number of threads
     * @return 操作结果 / operation result
     */
    protected suspend fun solve(threadNum: UInt64): Try {
        val begin = Clock.System.now()
        if (threadNum gr UInt64.one) {
            scip.solveConcurrent()
            val stage = scip.stage
            if (stage.swigValue() < SCIP_Stage.SCIP_STAGE_INITPRESOLVE.swigValue()) {
                scip.solve()
            }
        } else {
            scip.solve()
        }
        solvingTime = Clock.System.now() - begin

        return ok
    }

    /**
     * 分析 SCIP 求解状态 / Analyze SCIP solving status
     *
     * @return 操作结果 / Operation result
     */
    protected suspend fun analyzeStatus(): Try {
        val solution = scip.bestSol
        status = when (scip.status) {
            SCIP_Status.SCIP_STATUS_OPTIMAL -> {
                SolverStatus.Optimal
            }

            SCIP_Status.SCIP_STATUS_INFEASIBLE -> {
                SolverStatus.Infeasible
            }

            SCIP_Status.SCIP_STATUS_UNBOUNDED -> {
                SolverStatus.Unbounded
            }

            SCIP_Status.SCIP_STATUS_INFORUNBD -> {
                SolverStatus.InfeasibleOrUnbounded
            }

            else -> {
                if (solution != null) {
                    SolverStatus.Feasible
                } else {
                    SolverStatus.SolvingException
                }
            }
        }
        return ok
    }
}


