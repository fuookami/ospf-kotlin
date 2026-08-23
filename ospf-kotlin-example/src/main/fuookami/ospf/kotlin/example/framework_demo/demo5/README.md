# Demo 5: VRPTW Branch-and-Price

[中文](README_ch.md)

This demo wires the network-scheduling framework to a selectable Gurobi or SCIP column-generation solver. It supports Demo17 and Solomon-style inputs, explicit time and node limits, and returns framework solve statuses with route-level timing, load, distance, and cost details.

Run the SCIP integration profile:

```powershell
mvn -B -ntp -pl ospf-kotlin-example -am -Pdemo5-scip-bp test -T 0.75C
```

The SCIP profile requires the repository's `jscip` artifact and native runtime through `JSCIP_HOME` or `SCIP_HOME`.
