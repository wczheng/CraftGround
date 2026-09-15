package com.kyhsgeekcode.minecraftenv.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Let shutdown service generation completions between chunk-unload retries. */
@Mixin(MinecraftServer.class)
public class ServerShutdownChunkBudgetMixin {
  @Redirect(method = "shutdown", at = @At(value = "INVOKE", target =
      "Lnet/minecraft/server/world/ServerChunkManager;tick(Ljava/util/function/BooleanSupplier;Z)V"))
  private void tickShutdownChunks(ServerChunkManager manager, BooleanSupplier unused, boolean tickChunks) {
    // Vanilla passes () -> true. An unsavable holder can then requeue itself
    // forever, starving the server tasks that release its generation references.
    // Match shutdown's existing 1ms scheduling slice; keep draining on later passes.
    long deadline = System.nanoTime() + 1_000_000L;
    manager.tick(() -> System.nanoTime() < deadline, tickChunks);
    // runTasksTillTickEnd gates this queue on its expired tick deadline.
    // Always allow one completion to release generation references here.
    manager.executeQueuedTasks();
  }
}
