# Forge 1.20.1 Full Poison Circle Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Forge 1.20.1 v1.1.0 with feature parity for the existing NeoForge 1.21.1 poison circle.

**Architecture:** Split server circle state, Forge SimpleChannel synchronization, client world renderer, detector UI, and optional Xaero adapters into focused classes. Port the existing pure geometry and prediction logic verbatim where mappings permit; expose a small immutable `CircleSnapshot` to client-only consumers.

**Tech Stack:** Java 17, Forge 47.4.10, Minecraft 1.20.1, JUnit 5, Xaero Minimap 26.4.2 and World Map 1.44.2 as compile-only dependencies.

## Global Constraints

- Forge target: Minecraft 1.20.1 / Forge 47.4.10 / Java 17.
- Xaero integrations must be optional and must not package Xaero classes.
- World boundary rendering must use world coordinates and be non-colliding.
- Release artifact name: `poisoncircle-forge-1.20.1-1.1.0.jar`.

---

### Task 1: Shared circle state and stage engine

**Files:**
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/CircleSnapshot.java`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/CirclePrediction.java`
- Modify: `forge-1.20.1/src/main/java/com/poisoncircleforge/PoisonCircleForge.java`
- Test: `forge-1.20.1/src/test/java/com/poisoncircleforge/CirclePredictionTest.java`

- [ ] Write tests for fixed waiting positions and linear centre/radius interpolation.
- [ ] Run the focused test and confirm it fails because `CirclePrediction` is absent.
- [ ] Implement immutable snapshots and interpolation, then route server ticks through the state engine.
- [ ] Run focused test and `gradlew test`.

### Task 2: Forge networking and client cache

**Files:**
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/PoisonCircleNetwork.java`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/CircleSyncMessage.java`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/PoisonCircleClientState.java`
- Test: `forge-1.20.1/src/test/java/com/poisoncircleforge/CircleSyncMessageTest.java`

- [ ] Write a codec round-trip test for all public circle fields.
- [ ] Verify it fails before the message implementation exists.
- [ ] Register a Forge `SimpleChannel`, send snapshots to tracking players and login players, and update client cache only on the client reception thread.
- [ ] Run all tests.

### Task 3: World-space barrier renderer and hit feedback

**Files:**
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/BarrierGeometry.java`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/PoisonCircleClientEvents.java`
- Test: `forge-1.20.1/src/test/java/com/poisoncircleforge/BarrierGeometryTest.java`

- [ ] Write geometry tests asserting a closed circle at supplied world coordinates and no origin/camera vertices.
- [ ] Verify tests fail before the geometry class exists.
- [ ] Render a red transparent vertical wall during `RenderLevelStageEvent`, using current interpolated snapshot positions; add red damage overlay and short camera shake from a damage packet.
- [ ] Run all tests and a Forge client launch smoke test.

### Task 4: Detector, teams, and private preview

**Files:**
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/SafeZoneDetectorItem.java`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/DetectorRevealMessage.java`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/DetectorScreen.java`
- Modify: `forge-1.20.1/src/main/java/com/poisoncircleforge/PoisonCircleForge.java`
- Test: `forge-1.20.1/src/test/java/com/poisoncircleforge/DetectorRulesTest.java`

- [ ] Write tests for survival consumption, creative non-consumption, and private reveal scope.
- [ ] Verify tests fail before detector logic exists.
- [ ] Register compass-model detector, give command, private next-next preview packet, and map-style screen with yellow self/green scoreboard-team members only.
- [ ] Run all tests.

### Task 5: Optional Xaero overlays

**Files:**
- Modify: `forge-1.20.1/build.gradle`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/XaeroCompatibility.java`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/XaeroMinimapCircleOverlay.java`
- Create: `forge-1.20.1/src/main/java/com/poisoncircleforge/XaeroWorldMapCircleOverlay.java`
- Test: `forge-1.20.1/src/test/java/com/poisoncircleforge/CircleMapGeometryTest.java`

- [ ] Write a test for a dense closed perimeter without a centre point.
- [ ] Verify it fails before map geometry exists.
- [ ] Add local Xaero jars as compile-only, register adapters only when ModList reports the companion Mod, and draw current red/next white/private cyan perimeters from the interpolated snapshot.
- [ ] Run tests and a client smoke test with both Xaero jars installed.

### Task 6: Commands, release, and installation

**Files:**
- Modify: `forge-1.20.1/src/main/java/com/poisoncircleforge/PoisonCircleForge.java`
- Modify: `forge-1.20.1/gradle.properties`
- Modify: `forge-1.20.1/src/main/resources/META-INF/mods.toml`

- [ ] Add missing stage-action command and ensure every time edit updates active state immediately.
- [ ] Preserve direct-health lethal damage, fifth-circle collapse and all-player-death stop.
- [ ] Set `mod_version=1.1.0`; run `gradlew clean test build`.
- [ ] Inspect the final Jar metadata, replace the installed `1.0.2` jar, commit, push, and create the `forge-1.20.1-v1.1.0` GitHub release.
