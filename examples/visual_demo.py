"""Record a scripted first-person CraftGround run as an animated GIF."""

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

import craftground
from craftground import ActionSpaceVersion, InitialEnvironmentConfig
from craftground.environment.action_space import no_op_v2


OUTPUT_DIR = Path("artifacts")

CASES = [
    (
        "craftground_first_person.gif",
        [],
        [
            ("Forward", 30, {"forward": True}),
            ("Turn right", 20, {"camera_yaw": 6.0}),
            ("Run + jump", 30, {"forward": True, "sprint": True, "jump": True}),
        ],
    ),
    (
        "craftground_look_around.gif",
        [],
        [
            ("Look left", 20, {"camera_yaw": -5.0}),
            ("Look right", 40, {"camera_yaw": 5.0}),
            ("Look left", 20, {"camera_yaw": -5.0}),
        ],
    ),
    (
        "craftground_strafe.gif",
        [],
        [
            ("Strafe left", 25, {"left": True}),
            ("Strafe right", 50, {"right": True}),
            ("Strafe left", 25, {"left": True}),
        ],
    ),
    (
        "craftground_camera_pitch.gif",
        [],
        [
            ("Look up", 15, {"camera_pitch": -4.0}),
            ("Look down", 30, {"camera_pitch": 4.0}),
            ("Look up", 15, {"camera_pitch": -4.0}),
        ],
    ),
    (
        "craftground_attack.gif",
        [
            "item replace entity @p weapon.mainhand with minecraft:diamond_sword",
            "execute as @p at @s run summon minecraft:zombie ^ ^ ^4 {NoAI:1b,Silent:1b}",
        ],
        [
            ("Approach", 20, {"forward": True}),
            ("Attack", 35, {"attack": True}),
            ("Step back", 20, {"back": True}),
        ],
    ),
]


def record_case(env, output, setup_commands, sequence) -> None:
    if setup_commands:
        env.add_commands(setup_commands)
        env.step(no_op_v2())

    frames = []
    for label, steps, changes in sequence:
        for _ in range(steps):
            action = no_op_v2()
            action.update(changes)
            observation, _, _, _, _ = env.step(action)
            image = Image.fromarray(np.asarray(observation["pov"], dtype=np.uint8))
            state = observation["full"]
            draw = ImageDraw.Draw(image)
            draw.rectangle((0, 0, 300, 38), fill="black")
            draw.text(
                (8, 5),
                f"API: {label}\nXYZ: {state.x:.1f}, {state.y:.1f}, {state.z:.1f}",
                fill="white",
            )
            frames.append(image)

    assert frames and frames[0].size == (640, 360)
    frames[0].save(
        output,
        save_all=True,
        append_images=frames[1:],
        duration=80,
        loop=0,
    )
    print(f"Saved {len(frames)} first-person frames to {output}")


def main() -> None:
    env = craftground.make(
        InitialEnvironmentConfig(image_width=640, image_height=360),
        action_space_version=ActionSpaceVersion.V2_MINERL_HUMAN,
        mc_version="1.21",
    )
    try:
        env.reset()
        OUTPUT_DIR.mkdir(exist_ok=True)
        for filename, setup_commands, sequence in CASES:
            record_case(env, OUTPUT_DIR / filename, setup_commands, sequence)
    finally:
        env.close()


if __name__ == "__main__":
    main()
