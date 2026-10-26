package com.kyhsgeekcode.minecraftenv.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import net.minecraft.network.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class RenderMixin {
  private static final Logger LOGIN_LOGGER = LoggerFactory.getLogger("VAFM/LocalLogin");
  @Shadow private ClientConnection integratedServerConnection;

  @Inject(method = "render", at = @At("HEAD"))
  private void flushPendingLocalLogin(boolean tick, CallbackInfo callback) {
    MinecraftClient client = (MinecraftClient) (Object) this;
    ClientConnection connection = integratedServerConnection;
    // A paused loading screen can stop client ticks before the local channel
    // becomes active. Drain its queued handshake from a rendered frame so login
    // can finish; no world simulation or normal in-world networking is advanced.
    if (client.world == null
        && connection != null
        && connection.isOpen()
        && connection.getPacketListener() == null) {
      ClientConnectionQueuedTasksInvoker queued = (ClientConnectionQueuedTasksInvoker) connection;
      if (!queued.vafm$getQueuedTasks().isEmpty()) {
        LOGIN_LOGGER.info("Dispatching {} queued local login tasks", queued.vafm$getQueuedTasks().size());
        queued.vafm$flushQueuedTasks();
      }
    }
  }

  @Redirect(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;endWrite()V"))
  private void frameBufferEndWrite(Framebuffer instance) {
    // do nothing
  }

  @Redirect(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"))
  private void frameBufferDraw(Framebuffer instance, int width, int height) {
    // do nothing
  }

  @Redirect(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;swapBuffers()V"))
  private void windowSwapBuffers(Window instance) {
    RenderSystemPollEventsInvoker.pollEvents();
    RenderSystem.replayQueue();
    Tessellator.getInstance().clear();
    //        GLFW.glfwSwapBuffers(window);
    RenderSystemPollEventsInvoker.pollEvents();
  }
}
