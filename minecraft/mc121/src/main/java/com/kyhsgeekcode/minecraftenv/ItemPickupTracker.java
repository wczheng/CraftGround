package com.kyhsgeekcode.minecraftenv;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Actual inventory transfers, including partial stacks omitted by vanilla stats. */
public final class ItemPickupTracker {
    private static final Map<UUID, Map<String, Integer>> COUNTS = new ConcurrentHashMap<>();

    public static void record(UUID player, String item, int count) {
        if (count > 0) COUNTS.computeIfAbsent(player, key -> new ConcurrentHashMap<>())
                .merge(item, count, Integer::sum);
    }

    public static Map<String, Integer> get(UUID player) {
        return COUNTS.getOrDefault(player, Map.of());
    }
}
