# BPP2D Domain — Packing Context

:us: English | :cn: [简体中文](README_ch.md)

## Overview

`bpp2d-domain-packing-context` provides Final packing: geometry guard (bounds, interior overlap, rotation legality), packer, material summary and renderer adapter.

## Public API

`PackingGeometryGuard` — bounds, overlap and rotation validation
`Packer` — sheets to packed sheets with loading order
`PackingResult` / `PackingAggregation` / `PackedItem`
`PackingRendererAdapter` — renderer DTO output with actual area

## Parent Module

[OSPF Kotlin Framework BPP2D](../README.md)
