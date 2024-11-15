package copt

data object COPT {
    const val MINIMIZE = 1
    const val MAXIMIZE = -1

    const val INFINITY = 1e30
    const val UNDEFINED = 1e40

    const val BUFF_SIZE = 1000

    const val LESS_EQUAL = 'L'
    const val GREATER_EQUAL = 'G'
    const val EQUAL = 'E'
    const val FREE = 'N'
    const val RANGE = 'R'

    const val CONTINUOUS = 'C'
    const val BINARY = 'B'
    const val INTEGER = 'I'

    const val SOS_TYPE1 = 1
    const val SOS_TYPE2 = 2

    const val INDICATOR_IF = 1
    const val INDICATOR_ONLY_IF = 2
    const val INDICATOR_IF_AND_ONLY_IF = 3

    const val CONE_QUAD = 1
    const val CONE_RQUAD = 2

    const val EXPCONE_PRIMAL = 3
    const val EXPCONE_DUAL = 4

    const val CALL_BACK_CONTEXT_MIP_RELAX = 1
    const val CALL_BACK_CONTEXT_MIP_SOL = 2
    const val CALL_BACK_CONTEXT_MIP_NODE = 3
    const val CALL_BACK_CONTEXT_INCUMBENT = 4

    enum class RetCode(val value: Int) {
        Ok(0),
        Memory(1),
        File(2),
        Invalid(3),
        License(4),
        Internal(5),
        Thread(6),
        Sever(7),
        NonConvex(8)
    }

    enum class BasisStatus(val value: Int) {
        Lower(0),
        Basic(1),
        Upper(2),
        SuperBasic(3),
        Fixed(4)
    }

    enum class Status(val value: Int) {
        Unstarted(0),
        Optimal(1),
        Infeasible(2),
        Unbounded(3),
        InfeasibleOrUnbounded(4),
        Numerical(5),
        NodeLimit(6),
        Imprecise(7),
        TimeOut(8),
        Unfinished(9),
        Interrupted(10),
    }

    enum class CallBackInfo(val key: String) {
        BestObj("BestObj"),
        BestBound("BestBnd"),
        HasIncumbents("HasIncumbents"),
        Incumbent("Incumbent"),
        MipCandidate("MipCandidate"),
        MipCandObj("MipCandObj"),
        RelaxSolution("RelaxSolution"),
        RelaxSolObj("RelaxSolObj"),
        NodeStatus("NodeStatus"),
    }

    enum class DoubleParam(val key: String) {
        TimeLimit("TimeLimit"),
        SolTimeLimit("SolTimeLimit"),
        MatrixTol("MatrixTol"),
        FeasTol("FeasTol"),
        DualTol("DualTol"),
        IntTol("IntTol"),
        PDLPTol("PDLPTol"),
        RelGap("RelGap"),
        AbsGap("AbsGap"),
        TuneTimeLimit("TuneTimeLimit"),
        TuneTargetTime("TuneTargetTime"),
        TuneTargetRelGap("TuneTargetRelGap"),
    }

    enum class IntParam(val key: String) {
        Logging("Logging"),
        LogToConsole("LogToConsole"),
        Presolve("Presolve"),
        Scaling("Scaling"),
        Dualize("Dualize"),
        LpMethod("LpMethod"),
        GPUMode("GPUMode"),
        GPUDevice("GPUDevice"),
        ReqFarkasRay("ReqFarkasRay"),
        DualPrice("DualPrice"),
        DualPerturb("DualPerturb"),
        CutLevel("CutLevel"),
        RootCutLevel("RootCutLevel"),
        TreeCutLevel("TreeCutLevel"),
        RootCutRounds("RootCutRounds"),
        NodeCutRounds("NodeCutRounds"),
        HeurLevel("HeurLevel"),
        RoundingHeurLevel("RoundingHeurLevel"),
        DivingHeurLevel("DivingHeurLevel"),
        FAPHeurLevel("FAPHeurLevel"),
        SubMipHeurLevel("SubMipHeurLevel"),
        StrongBranching("StrongBranching"),
        ConflictAnalysis("ConflictAnalysis"),
        NodeLimit("NodeLimit"),
        MipTasks("MipTasks"),
        BarHomogeneous("BarHomogeneous"),
        BarOrder("BarOrder"),
        BarStart("BarStart"),
        BarIterLimit("BarIterLimit"),
        Threads("Threads"),
        BarThreads("BarThreads"),
        SimplexThreads("SimplexThreads"),
        CrossoverThreads("CrossoverThreads"),
        Crossover("Crossover"),
        SDMethod("SDMethod"),
        IISMethod("IISMethod"),
        FeasRelaxMode("FeasRelaxMode"),
        MipStartMode("MipStartMode"),
        MipStartNodeLimit("MipStartNodeLimit"),
        TuneMethod("TuneMethod"),
        TuneMode("TuneMode"),
        TuneMeasure("TuneMeasure"),
        TunePermutes("TunePermutes"),
        TuneOutputLevel("TuneOutputLevel"),
        LazyConstraints("LazyConstraints"),
    }

    enum class DoubleAttr(val key: String) {
        SolvingTime("SolvingTime"),
        ObjConst("ObjConst"),
        LpObjVal("LpObjval"),
        BestObj("BestObj"),
        BestBound("BestBnd"),
        BestGap("BestGap"),
        FeasRelaxObj("FeasRelaxObj")
    }

    enum class IntAttr(val key: String) {
        Cols("Cols"),
        PSDCols("PSDCols"),
        Rows("Rows"),
        Elems("Elems"),
        QElems("QElems"),
        PSDElems("PSDElems"),
        SymMats("SymMats"),
        Bins("Bins"),
        Ints("Ints"),
        Soss("Soss"),
        Cones("Cones"),
        ExpCones("ExpCones"),
        QConstrs("QConstrs"),
        PSDConstrs("PSDConstrs"),
        LMIConstrs("LMIConstrs"),
        Indicators("Indicators"),
        IISCols("IISCols"),
        IISRows("IISRows"),
        IISSOSs("IISSOSs"),
        IISIndicators("IISIndicators"),
        ObjSense("ObjSense"),
        LpStatus("LpStatus"),
        MipStatus("MipStatus"),
        SimplexIter("SimplexIter"),
        BarrierIter("BarrierIter"),
        NodeCnt("NodeCnt"),
        PoolSols("PoolSols"),
        TuneResults("TuneResults"),
        HasLpSol("HasLpSol"),
        HasDualFarkas("HasDualFarkas"),
        HasPrimalRay("HasPrimalRay"),
        HasBasis("HasBasis"),
        HasMipSol("HasMipSol"),
        HasQObj("HasQObj"),
        HasPSDObj("HasPSDObj"),
        HasIIS("HasIIS"),
        HasFeasRelaxSol("HasFeasRelaxSol"),
        IsMIP("IsMIP"),
        IsMinIIS("IsMinIIS"),
    }

    enum class DoubleInfo(val key: String) {
        Obj("Obj"),
        LB("LB"),
        UB("UB"),
        Value("Value"),
        Slack("Slack"),
        Dual("Dual"),
        RedCost("RedCost"),
        DualFarkas("DualFarkas"),
        PrimalRay("PrimalRay"),
        RelaxLB("RelaxLB"),
        RelaxUB("RelaxUB"),
        RelaxValue("RelaxValue"),
    }

    enum class Client(val key: String) {
        CaFile("CaFile"),
        CertFile("CertFile"),
        CertKeyFile("CertKeyFile"),
        Cluster("Cluster"),
        Floating("Floating"),
        Password("Password"),
        Port("Port"),
        Priority("Priority"),
        WaitTime("WaitTime"),
        WebServer("WebServer"),
        WebLicenseId("WebLicenseId"),
        WebAccessKey("WebAccessKey"),
        WebTokenDuration("WebTokenDuration"),
    }
}

fun EnvrConfig.set(key: COPT.Client, value: String) {
    this.set(key.key, value)
}

fun EnvrConfig.set(key: COPT.DoubleAttr, value: Double) {
    this.set(key.key, value.toString())
}

fun EnvrConfig.set(key: COPT.IntAttr, value: Int) {
    this.set(key.key, value.toString())
}

fun CallbackBase.get(key: COPT.CallBackInfo): Double {
    return this.getDblInfo(key.key)
}

fun Model.get(key: COPT.DoubleParam): Double {
    return this.getDblParam(key.key)
}

fun Model.set(key: COPT.DoubleParam, value: Double) {
    this.setDblParam(key.key, value)
}

fun Model.get(key: COPT.IntParam): Int {
    return this.getIntParam(key.key)
}

fun Model.set(key: COPT.IntParam, value: Int) {
    this.setIntParam(key.key, value)
}

fun Model.get(key: COPT.DoubleAttr): Double {
    return this.getDblAttr(key.key)
}

fun Model.get(key: COPT.IntAttr): Int {
    return this.getIntAttr(key.key)
}

fun Var.get(key: COPT.DoubleInfo): Double {
    return this.get(key.key)
}

fun Constraint.get(key: COPT.DoubleInfo): Double {
    return this.get(key.key)
}
