package com.kyhsgeekcode.minecraftenv.mixin;

import net.minecraft.network.ClientConnection;
import java.util.Queue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientConnection.class)
public interface ClientConnectionQueuedTasksInvoker {
  @Accessor("queuedTasks")
  Queue<?> vafm$getQueuedTasks();

  @Invoker("handleQueuedTasks")
  void vafm$flushQueuedTasks();
}
