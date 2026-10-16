package com.kyhsgeekcode.minecraftenv.mixin;

import com.kyhsgeekcode.minecraftenv.VanillaFrames;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Section rebuilds, chunk loads and terrain updates all wait for a world frame to be scheduled. */
@Mixin(WorldRenderer.class)
public class TerrainDirtyMixin {
  @Inject(
      method = {"scheduleChunkRender", "method_52815", "scheduleTerrainUpdate"},
      at = @At("HEAD"))
  private void markTerrainDirty(CallbackInfo info) {
    VanillaFrames.terrainDirty = true;
  }
}
