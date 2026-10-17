# BPP2D Application

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp2d-application` provides Column-generation orchestration, standard executors, application service, KPI analyzer and the real Gurobi integration test.

## Public API

`ColumnGenerationAlgorithm` — orchestration with seed columns and failure stages
`ColumnGenerationStandardExecutors` — RMP / final MILP / request builder
`ColumnGenerationApplicationService` — end-to-end entry point
`ColumnGenerationPackingAnalyzer` — area-based KPI
`GurobiDelegatingColumnGenerationSolver` / `GurobiColumnGeneration2DTest` — Gurobi wiring and integration test

## Parent Module

[OSPF Kotlin Framework BPP2D](../README.md)
