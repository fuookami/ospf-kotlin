# BPP2D Domain — Block Loading Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp2d-domain-block-loading-context` provides 2D space model, simple/complex block generators, DFS and multi-row space-splitting searches.

## Public API

`Space2` / `Block2` — region and block models
`SimpleBlockGenerator` — whole-row regular blocks
`ComplexBlockGenerator` — heterogeneous row blocks
`DepthFirstSearchAlgorithm2D` / `MultiRowHeuristicAlgorithm2D` — space splitting
`BlockLoadingContext` — context facade
`requireRotationAllowed` — rotation guard

## Parent Module

[OSPF Kotlin Framework BPP2D](../README.md)
