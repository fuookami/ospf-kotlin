package fuookami.ospf.kotlin.utils.math.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class MathGeometryBenchmark {
    @Param({"32", "128"})
    public int pointCount;

    private GeometryBenchmarkOps.GeometryBenchmarkState state;

    @Setup(Level.Trial)
    public void setup() {
        state = GeometryBenchmarkOps.createState(pointCount);
    }

    @Benchmark
    public double distanceEuclidean2D() {
        return GeometryBenchmarkOps.distanceEuclidean2DAsDouble();
    }

    @Benchmark
    public double distanceManhattan2D() {
        return GeometryBenchmarkOps.distanceManhattan2DAsDouble();
    }

    @Benchmark
    public double distanceMinkowski3_2D() {
        return GeometryBenchmarkOps.distanceMinkowski3_2DAsDouble();
    }

    @Benchmark
    public int triangulate2DPointCloud() {
        return GeometryBenchmarkOps.triangulatePointCloudSize(state);
    }

    @Benchmark
    public int triangulate3DIsolines() {
        return GeometryBenchmarkOps.triangulateIsolinesSize(state);
    }
}