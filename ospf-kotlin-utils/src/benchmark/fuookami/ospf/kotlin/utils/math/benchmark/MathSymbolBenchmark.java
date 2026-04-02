package fuookami.ospf.kotlin.utils.math.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class MathSymbolBenchmark {
    private SymbolBenchmarkOps.SymbolBenchmarkState state;
    private SymbolBenchmarkOps.PolynomialArithmeticState arithmeticState;

    @Setup(Level.Trial)
    public void setup() {
        state = SymbolBenchmarkOps.createState();
        arithmeticState = SymbolBenchmarkOps.createArithmeticState();
    }

    // ===== Original benchmarks =====

    @Benchmark
    public int compileEvalCanonical() {
        return SymbolBenchmarkOps.compileEvalHash(state);
    }

    @Benchmark
    public int compileGradientCanonical() {
        return SymbolBenchmarkOps.compileGradientHash(state);
    }

    @Benchmark
    public double evaluateOrderedCanonical() {
        return SymbolBenchmarkOps.evaluateOrderedAsDouble(state);
    }

    @Benchmark
    public int gradientCanonicalSize() {
        return SymbolBenchmarkOps.gradientSize(state);
    }

    @Benchmark
    public int matrixFormCanonicalSize() {
        return SymbolBenchmarkOps.matrixFormSize(state);
    }

    @Benchmark
    public double invokeCompiledEval() {
        return SymbolBenchmarkOps.invokeCompiledEvalAsDouble(state);
    }

    @Benchmark
    public int invokeCompiledGradientSize() {
        return SymbolBenchmarkOps.invokeCompiledGradientSize(state);
    }

    // ===== S-PERF-2: Polynomial arithmetic benchmarks =====

    @Benchmark
    public int polynomialPlus() {
        return SymbolBenchmarkOps.polynomialPlus(arithmeticState);
    }

    @Benchmark
    public int polynomialMinus() {
        return SymbolBenchmarkOps.polynomialMinus(arithmeticState);
    }

    @Benchmark
    public int polynomialTimesScalar() {
        return SymbolBenchmarkOps.polynomialTimesScalar(arithmeticState);
    }

    @Benchmark
    public int polynomialDivScalar() {
        return SymbolBenchmarkOps.polynomialDivScalar(arithmeticState);
    }

    @Benchmark
    public int combineTermsStress() {
        return SymbolBenchmarkOps.combineTermsStress(arithmeticState);
    }
}