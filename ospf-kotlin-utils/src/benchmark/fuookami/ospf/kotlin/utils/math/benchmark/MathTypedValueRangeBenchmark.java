package fuookami.ospf.kotlin.utils.math.benchmark;

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64;
import fuookami.ospf.kotlin.utils.math.algebra.value_range.ClosedIntervalKind;
import fuookami.ospf.kotlin.utils.math.algebra.value_range.OpenIntervalKind;
import fuookami.ospf.kotlin.utils.math.algebra.value_range.RuntimeIntervalKind;
import fuookami.ospf.kotlin.utils.math.algebra.value_range.TypedValueRange;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class MathTypedValueRangeBenchmark {
    private TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind> typedMain;
    private TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind> typedPeer;
    private TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind> dynamicMain;
    private TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind> dynamicPeer;

    @Setup(Level.Trial)
    public void setup() {
        typedMain = TypedValueRangeBenchmarkOps.typedClosedOpen(0.0, 100.0);
        typedPeer = TypedValueRangeBenchmarkOps.typedClosedOpen(30.0, 120.0);
        dynamicMain = TypedValueRangeBenchmarkOps.dynamicClosedOpen(0.0, 100.0);
        dynamicPeer = TypedValueRangeBenchmarkOps.dynamicClosedOpen(30.0, 120.0);
    }

    @Benchmark
    public TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind> constructTypedClosedOpen() {
        return TypedValueRangeBenchmarkOps.typedClosedOpen(0.0, 100.0);
    }

    @Benchmark
    public TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind> constructDynamicClosedOpen() {
        return TypedValueRangeBenchmarkOps.dynamicClosedOpen(0.0, 100.0);
    }

    @Benchmark
    public boolean containsTypedInside() {
        return TypedValueRangeBenchmarkOps.contains(typedMain, 50.0);
    }

    @Benchmark
    public boolean containsDynamicInside() {
        return TypedValueRangeBenchmarkOps.contains(dynamicMain, 50.0);
    }

    @Benchmark
    public boolean containsTypedBoundary() {
        return TypedValueRangeBenchmarkOps.contains(typedMain, 100.0);
    }

    @Benchmark
    public boolean containsDynamicBoundary() {
        return TypedValueRangeBenchmarkOps.contains(dynamicMain, 100.0);
    }

    @Benchmark
    public TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind> shiftTypedPlus() {
        return TypedValueRangeBenchmarkOps.shiftTyped(typedMain, 1.0);
    }

    @Benchmark
    public TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind> shiftDynamicPlus() {
        return TypedValueRangeBenchmarkOps.shiftDynamic(dynamicMain, 1.0);
    }

    @Benchmark
    public TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind> intersectTypedSameKind() {
        return TypedValueRangeBenchmarkOps.intersectTyped(typedMain, typedPeer);
    }

    @Benchmark
    public TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind> intersectDynamicRuntimeKind() {
        return TypedValueRangeBenchmarkOps.intersectDynamic(dynamicMain, dynamicPeer);
    }

    @Benchmark
    public TypedValueRange<Flt64, ClosedIntervalKind, OpenIntervalKind> signedScaleTypedPositive() {
        return TypedValueRangeBenchmarkOps.signedScaleTypedPositive(typedMain, 2.0);
    }

    @Benchmark
    public TypedValueRange<Flt64, ?, ?> signedScaleTypedNegative() {
        return TypedValueRangeBenchmarkOps.signedScaleTypedNegative(typedMain, -2.0);
    }

    @Benchmark
    public TypedValueRange<Flt64, RuntimeIntervalKind, RuntimeIntervalKind> signedScaleDynamicPositive() {
        return TypedValueRangeBenchmarkOps.signedScaleDynamicPositive(dynamicMain, 2.0);
    }
}