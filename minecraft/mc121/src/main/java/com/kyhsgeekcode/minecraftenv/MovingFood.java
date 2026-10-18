package com.kyhsgeekcode.minecraftenv;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * One tick-driven edible target per world, kept on the walkable ground around the player. No observations or
 * learner rewards here.
 */
public final class MovingFood {
    private static final Map<ServerWorld, Task> TASKS = new HashMap<>();
    public static final String EVENT = "ate/vafm:moving_food";
    // Mirrors vafm/envs/native_survival_scenes.py _standing_cells on the same WORLD_SURFACE heightmap.
    private static final Set<String> SOLID = Set.of("grass_block", "dirt", "coarse_dirt", "podzol", "rooted_dirt",
        "sand", "gravel", "stone", "andesite", "diorite", "granite");
    private static final Set<String> LOW_PLANTS = Set.of("short_grass", "fern", "dandelion", "poppy");
    private static final double LIFT = .25;  // the cake hovers this far above its ground
    private static final double HALF = .45;  // the food's footprint must stay on walkable cells

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(CommandManager.literal("movingfood").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("stop").executes(c -> {
                    stop(c.getSource().getWorld());
                    return 1;
                }))
                .then(CommandManager.literal("start")
                    .then(CommandManager.argument("config", StringArgumentType.greedyString()).executes(c -> {
                        ServerWorld world = c.getSource().getWorld();
                        JsonObject config = JsonParser.parseString(StringArgumentType.getString(c, "config")).getAsJsonObject();
                        stop(world);
                        Task task = new Task(world, config);
                        TASKS.put(world, task);
                        task.spawn(true);
                        c.getSource().sendFeedback(() -> Text.literal("MOVING_FOOD_READY"), false);
                        return 1;
                    })))));
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            Task task = TASKS.get(world);
            if (task != null) task.tick();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> TASKS.clear());
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!entity.getCommandTags().contains("vafm_moving_food")) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND || player.isSpectator()) return ActionResult.FAIL;
            if (world.isClient) return ActionResult.SUCCESS;
            Task task = TASKS.get((ServerWorld) world);
            if (task == null || task.target != entity || entity.isRemoved()
                    || !player.canConsume(false) || player.squaredDistanceTo(entity) > 16.) return ActionResult.FAIL;
            // Vanilla's targeted interaction packet provides aim/reach; discard before recording prevents duplicates.
            task.removeTarget();
            task.cooldown = 40;
            task.consumed++;
            PlayerEventTracker.record(player.getUuid(), EVENT);
            ((ServerPlayerEntity) player).playSound(net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EAT, 1.f, 1.f);
            return ActionResult.CONSUME;
        });
    }

    public static Map<String, Integer> statistics(ServerWorld world) {
        Map<String, Integer> out = new HashMap<>();
        out.put("moving_food_version", 2);
        Task task = TASKS.get(world);
        if (task != null) {
            out.put("moving_food/ticks", task.ticks);
            out.put("moving_food/cells", task.ground.size());
            out.put("moving_food/generation", task.generation);
            out.put("moving_food/consumed", task.consumed);
            if (task.target != null) {
                out.put("moving_food/x10000", (int) Math.round(task.target.getX() * 10000));
                out.put("moving_food/z10000", (int) Math.round(task.target.getZ() * 10000));
            }
        }
        return out;
    }

    private static void stop(ServerWorld world) {
        Task task = TASKS.remove(world);
        if (task != null) {
            task.removeTarget();
            task.pin(false);
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private static double centerX(long cell) {
        return (int) (cell >> 32) + .5;
    }

    private static double centerZ(long cell) {
        return (int) cell + .5;
    }

    private static final class Task {
        final ServerWorld world;
        final Random random;
        final double cx, cz, radius, speed, initialHeading;
        // Feet height of each cell in the player's |dh| <= 1 walkable component, cell centers inside the disk.
        final Map<Long, Integer> ground = new LinkedHashMap<>();
        final List<Long> respawns = new ArrayList<>();
        double initialX, initialZ;
        InteractionEntity target;
        DisplayEntity.BlockDisplayEntity display;
        double heading, turn;
        int ticks, cooldown, generation, consumed;

        Task(ServerWorld world, JsonObject config) {
            this.world = world;
            random = new Random(config.get("motion_seed").getAsLong());
            cx = config.get("cx").getAsDouble();
            cz = config.get("cz").getAsDouble();
            radius = config.get("radius").getAsDouble();
            speed = config.get("speed").getAsDouble();
            initialX = config.get("x").getAsDouble();
            initialZ = config.get("z").getAsDouble();
            initialHeading = config.get("heading").getAsDouble();
            if (!(radius >= 10 && radius <= 24 && speed >= 0 && speed <= .2)
                    || !Double.isFinite(cx + cz + initialX + initialZ + initialHeading)
                    || Math.hypot(initialX - cx, initialZ - cz) > radius - 3)
                throw new IllegalArgumentException("invalid moving food configuration");
            pin(true);
            explore();
            if (ground.isEmpty()) {
                pin(false);
                throw new IllegalArgumentException("moving food center is not on walkable ground");
            }
            for (long cell : ground.keySet())
                if (Math.hypot(centerX(cell) - cx, centerZ(cell) - cz) <= radius * .72) respawns.add(cell);
            if (respawns.isEmpty()) respawns.addAll(ground.keySet());
            if (!clear(initialX, initialZ)) {
                // Off walkable ground: start at the nearest walkable cell still inside the inward-steering ring.
                double best = Double.POSITIVE_INFINITY, x = initialX, z = initialZ;
                for (long cell : ground.keySet()) {
                    double distance = Math.hypot(centerX(cell) - x, centerZ(cell) - z);
                    if (distance < best && Math.hypot(centerX(cell) - cx, centerZ(cell) - cz) <= radius - 3) {
                        best = distance;
                        initialX = centerX(cell);
                        initialZ = centerZ(cell);
                    }
                }
                if (best == Double.POSITIVE_INFINITY) {
                    pin(false);
                    throw new IllegalArgumentException("no walkable start cell inside the moving food disk");
                }
            }
        }

        Integer standing(int x, int z) {
            int height = world.getChunk(x >> 4, z >> 4).getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x & 15, z & 15);
            String key = world.getBlockState(new BlockPos(x, height - 1, z)).getBlock().getTranslationKey();
            String name = key.substring(key.lastIndexOf('.') + 1);
            if (SOLID.contains(name)) return height;
            if (LOW_PLANTS.contains(name)) return height - 1;
            return null;
        }

        void explore() {
            int x0 = (int) Math.floor(cx), z0 = (int) Math.floor(cz);
            Integer start = standing(x0, z0);
            if (start == null) return;
            ground.put(key(x0, z0), start);
            ArrayDeque<int[]> frontier = new ArrayDeque<>();
            frontier.add(new int[] {x0, z0});
            while (!frontier.isEmpty()) {
                int[] cell = frontier.poll();
                int height = ground.get(key(cell[0], cell[1]));
                for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int x = cell[0] + step[0], z = cell[1] + step[1];
                    if (ground.containsKey(key(x, z)) || Math.hypot(x + .5 - cx, z + .5 - cz) > radius) continue;
                    Integer y = standing(x, z);
                    if (y != null && Math.abs(y - height) <= 1) {
                        ground.put(key(x, z), y);
                        frontier.add(new int[] {x, z});
                    }
                }
            }
        }

        boolean clear(double x, double z) {
            for (double dx : new double[] {-HALF, HALF})
                for (double dz : new double[] {-HALF, HALF})
                    if (!ground.containsKey(key((int) Math.floor(x + dx), (int) Math.floor(z + dz)))) return false;
            return true;
        }

        double lifted(double x, double z) {
            return ground.get(key((int) Math.floor(x), (int) Math.floor(z))) + LIFT;
        }

        void pin(boolean forced) {
            for (int x = (int) Math.floor((cx - radius - 2) / 16); x <= (int) Math.floor((cx + radius + 2) / 16); x++)
                for (int z = (int) Math.floor((cz - radius - 2) / 16); z <= (int) Math.floor((cz + radius + 2) / 16); z++)
                    world.setChunkForced(x, z, forced);
        }

        void spawn(boolean first) {
            long cell = first ? 0 : respawns.get(random.nextInt(respawns.size()));
            double x = first ? initialX : centerX(cell);
            double z = first ? initialZ : centerZ(cell);
            double y = lifted(x, z);
            heading = first ? initialHeading : random.nextDouble() * Math.PI * 2;
            turn = 0.;
            target = new InteractionEntity(EntityType.INTERACTION, world);
            display = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
            try {
                target.readNbt(StringNbtReader.parse("{width:0.9f,height:0.55f,response:1b}"));
                display.readNbt(StringNbtReader.parse("{block_state:{Name:\"minecraft:cake\"},teleport_duration:1,"
                    + "transformation:{translation:[-0.5f,0.0f,-0.5f],scale:[1.0f,1.0f,1.0f],"
                    + "left_rotation:[0.0f,0.0f,0.0f,1.0f],right_rotation:[0.0f,0.0f,0.0f,1.0f]}}"));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
                throw new IllegalStateException(error);
            }
            target.addCommandTag("vafm_moving_food");
            display.addCommandTag("vafm_moving_food_display");
            target.setPosition(x, y, z);
            display.setPosition(x, y, z);
            world.spawnEntity(target);
            world.spawnEntity(display);
            generation++;
        }

        void removeTarget() {
            if (target != null) target.discard();
            if (display != null) display.discard();
            target = null;
            display = null;
        }

        void tick() {
            ticks++;
            if (target == null) {
                if (--cooldown <= 0) spawn(false);
                return;
            }
            // Discarded or moved off the walkable ground by anything else (the fast reset teleports every entity away).
            if (target.isRemoved() || display.isRemoved()
                    || !ground.containsKey(key((int) Math.floor(target.getX()), (int) Math.floor(target.getZ())))) {
                removeTarget();
                return;
            }
            if (ticks % 40 == 0) turn = (random.nextDouble() * 2 - 1) * .025;
            double dx = target.getX() - cx, dz = target.getZ() - cz;
            if (Math.hypot(dx, dz) > radius - 3) {
                double inward = Math.atan2(-dz, -dx);
                double error = Math.atan2(Math.sin(inward - heading), Math.cos(inward - heading));
                heading += Math.max(-.12, Math.min(.12, error));
            } else heading += turn;
            double x = target.getX() + speed * Math.cos(heading);
            double z = target.getZ() + speed * Math.sin(heading);
            if (!clear(x, z)) {
                // Water, trees, cliffs and the disk edge block the step: hold for this tick and turn away.
                heading += Math.PI * (.5 + random.nextDouble());
                x = target.getX();
                z = target.getZ();
            }
            // A one-block step between cells is climbed over four ticks rather than teleported.
            double y = target.getY() + Math.max(-.25, Math.min(.25, lifted(x, z) - target.getY()));
            target.setPosition(x, y, z);
            display.setPosition(x, y, z);
            display.velocityDirty = true;
            target.velocityDirty = true;
        }
    }
}
