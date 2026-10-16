package com.kyhsgeekcode.minecraftenv.mixin;

import com.kyhsgeekcode.minecraftenv.VanillaFrames;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * VAFM_CAPTURE_ONLY_FRAMES=1 skips the main loop's per-tick world frame while in a world with no
 * overlay or screen. Every observation is drawn by MinecraftEnv's own render(client) right before
 * capture, so those frames are never read. Loading overlays and screens still render: the resource
 * reload overlay, for one, finishes itself from its render call. Frames also still render while
 * terrain work is pending (VanillaFrames) or the camera entered another section, because only a
 * world frame schedules section builds, uploads their meshes and refreshes section visibility;
 * skipping those left terrain missing for several decisions after a teleport.
 */
@Mixin(MinecraftClient.class)
public class VanillaFrameMixin {
  private static final boolean CAPTURE_ONLY = "1".equals(System.getenv("VAFM_CAPTURE_ONLY_FRAMES"));

  @Redirect(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/render/GameRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;Z)V"))
  private void vanillaFrame(GameRenderer renderer, RenderTickCounter counter, boolean tick) {
    MinecraftClient client = (MinecraftClient) (Object) this;
    if (!CAPTURE_ONLY || client.world == null || client.getOverlay() != null || client.currentScreen != null) {
      renderer.render(counter, tick);
      return;
    }
    long section =
        client.getCameraEntity() == null
            ? Long.MIN_VALUE
            : ChunkSectionPos.from(client.getCameraEntity()).asLong();
    if (VanillaFrames.terrainDirty
        || section != VanillaFrames.cameraSection
        || !client.worldRenderer.isTerrainRenderComplete()) {
      VanillaFrames.terrainDirty = false;
      VanillaFrames.cameraSection = section;
      renderer.render(counter, tick);
    }
  }
}
