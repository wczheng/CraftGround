package com.kyhsgeekcode.minecraftenv;

/**
 * Terrain work that only a world frame schedules or uploads: dirty sections, newly loaded chunks and
 * visibility updates. VanillaFrameMixin keeps the main loop's frames while it is pending.
 */
public final class VanillaFrames {
  public static boolean terrainDirty = true;
  public static long cameraSection = Long.MIN_VALUE;

  private VanillaFrames() {}
}
