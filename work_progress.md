# Work Progress

Last verified: 2026-09-09T22:20:04+08:00 (Asia/Shanghai).

## Current objective and handoff

Minecraft1.21 native boundary fixes for the parent VAFM survival environment are implemented and accepted on Linux/Xvfb. No training. Matching Python proto/native sources required; mc262 is not verified. Parent evidence: `../artifacts/minimal-open-survival/`; concise report: [acceptance](../docs/native-survival-acceptance.md).

## Verified changes

Actual selected slot, server seed/tick/day clock and client day clock, use/actual partial-pickup counters, diagnostic block state; request-indexed tick synchronization; staged clock-ready reset supported by the parent adapter. Difficulty config is now applied, empty resource paths ignored, heightmap samples surface blocks. Native exit stops server writes before deletion; Python waits before escalating termination and captures descendants before launcher exit, because Gradle single-use daemons create separate process groups. Existing psutil handles descendant cleanup.

## Evidence and limits

Parent native mechanisms11/11, layouts6/6 with54food blocks, oracle24,000ticks and matched hunger controls PASS. Native close returns0 and removes worlds. Parent `tests/test_native_survival.py` now has6 passing checks, including graceful/SIGTERM/SIGKILL and real separate-session child cleanup. Eight additional held-out terrain worlds and72food blocks passed fixed replay. Previously leaking rejected ocean-world cleanup passes with zero task JVMs remaining; evidence in parent `evaluation-worlds/cleanup-result.json`. Existing pytest suite NOT RUN because pytest is absent. Earlier Apple Silicon visual-demo results remain historical, not reverified on this Linux host.

## Next actions and open questions

No active native blocker. Keep protocol/client/runtime aligned for parent training work; mc262 support needs its own validation if requested. Do not read server chunks on the render thread (paused-server deadlock). Vanilla PICKED_UP misses partial insertion; use actual inventory-transfer hook.

No new reusable skill. These changes are published with the parent VAFM survival release on the matching `world-setting-dreamerv3-audit` branch; the parent pins the exact revision.
