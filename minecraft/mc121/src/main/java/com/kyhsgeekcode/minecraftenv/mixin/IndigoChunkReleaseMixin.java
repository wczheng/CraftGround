package com.kyhsgeekcode.minecraftenv.mixin;

import java.util.Map;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Indigo's thread-local chunk context must not retain a disconnected client world. */
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.ChunkRenderInfo", remap = false)
public class IndigoChunkReleaseMixin {
  @Shadow BlockRenderView blockView;
  @Shadow BlockBufferAllocatorStorage builders;
  @Shadow Map<RenderLayer, BufferBuilder> buffers;

  @Inject(method = "release", at = @At("RETURN"), remap = false)
  private void releaseReferences(CallbackInfo ci) {
    blockView = null;
    builders = null;
    buffers = null;
  }
}
