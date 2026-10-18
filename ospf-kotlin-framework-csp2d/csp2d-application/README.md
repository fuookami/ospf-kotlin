# ospf-kotlin-framework-csp2d-application

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`csp2d-application` provides Problem definition, the MILP path, the iterative column generation path (residual-demand driven), solution recovery and the real Gurobi integration test.

## Boundary

Part of the [csp2d](../README.md) framework. The generic layering discipline keeps `FltX` out of the domain modules;
it appears only in the modelling boundary (`produce`) and in `application`. The 2D geometry is shared through
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README.md).

## Parent Module

[OSPF Kotlin Framework CSP2D](../README.md)
