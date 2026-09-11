package com.kyhsgeekcode.minecraftenv;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Completed server-side consumption and attributed hits, not button presses. */
public final class PlayerEventTracker {
    private static final Map<UUID, Map<String, Integer>> COUNTS = new ConcurrentHashMap<>();

    public static void record(UUID player, String event) {
        COUNTS.computeIfAbsent(player, key -> new ConcurrentHashMap<>())
                .merge(event, 1, Integer::sum);
    }

    public static Map<String, Integer> get(UUID player) {
        return COUNTS.getOrDefault(player, Map.of());
    }
}
