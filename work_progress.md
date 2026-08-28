# Work Progress

## Last verified

- 2026-08-28 21:42 CST (Asia/Shanghai)

## Current objective and status

- Create several distinct first-person GIF cases of CraftGround API control on the current Apple Silicon Mac.
- Status: complete; five scripted cases were recorded and visually checked.

## Handoff summary

- Use the existing Conda environment: `conda activate craftground`.
- Run experiments from the project root. An experiment must create the environment with `craftground.make(...)`, call `env.reset()` to launch Minecraft, and close it with `env.close()`.
- `examples/visual_demo.py` records five API-driven cases from `obs["pov"]` in one Minecraft session: movement, look-around, strafing, camera pitch, and zombie attack.
- Outputs are under `artifacts/craftground_*.gif`; the demo exited cleanly after saving all five files.
- Default to `mc_version="1.21"`; the 26.2 path is not part of the verified run.

## Active topics

- Minecraft 1.21 launch — complete — verified 2026-08-27 20:07 CST — launch works; client is stopped.
- First-person visualization — complete — verified 2026-08-28 21:42 CST — five GIF cases generated and visually checked.

## Recent verified milestones

- Built the Minecraft 1.21 native C++/JNI/Apple module and Gradle Java sources.
- Fixed Conda prefix discovery in `minecraft/mc121/build.gradle` and `minecraft/mc262/build.gradle`.
- Launched Minecraft 1.21 with Fabric through CraftGround and entered a generated local world.
- Relaunched at 1280x720 with a continuous Python no-op action loop so rendering keeps advancing.
- Added and ran `examples/visual_demo.py`; it records forward, turn, and run/jump actions with action and position overlays.
- Extended the same script to record look-around, strafe, camera-pitch, and sword-attack cases without restarting Minecraft between cases.

## Decisions and assumptions

- Minecraft 1.21 is the verified default runtime.
- Gradle reads `CONDA_PREFIX` from the process environment when `CRAFTGROUND_USE_CONDA=true`.

## Blockers and open questions

- No blocker for launching Minecraft.
- Automated screenshots are unavailable because macOS denies `screencapture` access to the executor.

## Exact next actions

1. View the five `artifacts/craftground_*.gif` outputs.
2. Re-run with `conda run --no-capture-output -n craftground python examples/visual_demo.py` when needed.

## Verification evidence

- CraftGround native module imports successfully and the Apple MPS backend is available.
- Launch output shows `CRAFTGROUND_READY 1280x720`; logs show Minecraft 1.21, IPC port 8001, integrated server startup, and local player login at 20:02 CST.
- After sending `SIGTERM` to the launch Gradle wrapper and Minecraft client, `pgrep` found no remaining CraftGround launcher or Minecraft client process.
- `examples/visual_demo.py` passed `py_compile` and `git diff --check`.
- Generated GIFs are all 640x360 with differing first/last frames: movement 80 frames, look-around 80, strafe 100, camera pitch 60, and attack 75.
- Four-frame contact sheets for every new case were visually inspected; labels, scene motion, sword, zombie, and attack animation render correctly.
- The recording command exited with status 0 after `env.close()`.

## Reusable workflow candidates

- None yet.
