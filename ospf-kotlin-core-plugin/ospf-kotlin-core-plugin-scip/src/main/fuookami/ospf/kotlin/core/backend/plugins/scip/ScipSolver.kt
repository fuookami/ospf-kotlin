package fuookami.ospf.kotlin.core.backend.plugins.scip

import java.util.*
import java.io.*
import jscip.*
import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*

abstract class ScipSolver {
    companion object {
        internal var loadedLibrary = false

        init {
            try {
                System.loadLibrary("jscip")
                loadedLibrary = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private val winLibraries = listOf("tbb", "libscip", "jscip")
        private val unixLibraries = listOf("libgcg", "libgmp", "libpthread", "libgfortran", "libquadmath", "libopenblas", "libtbb", "libsplexshared", "libscip", "libjscip")

        fun loadLibraryInJar() {
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
                Library.loadInJar(libFullName, File(System.getProperty("user.dir"), libFullName).absolutePath)
            }
        }
    }

    protected lateinit var scip: Scip
    protected lateinit var status: SolverStatus
    protected var solvingTime: Duration? = null

    protected open fun finalize() {
        scip.free()
    }

    protected suspend fun init(name: String): Try {
        if (!loadedLibrary) {
            try {
                System.loadLibrary("jscip")
                loadedLibrary = true
            } catch (e: Exception) {
                return Failed(ErrorCode.SolverNotFound, "failed to load jscip library")
            }
        }
        scip = Scip()
        scip.create(name)
        return ok
    }

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
                SolverStatus.SolvingException
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
