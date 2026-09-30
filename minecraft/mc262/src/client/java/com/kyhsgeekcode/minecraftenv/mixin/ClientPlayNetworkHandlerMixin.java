package com.kyhsgeekcode.minecraftenv.mixin;

import com.kyhsgeekcode.minecraftenv.GetMessagesInterface;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin implements GetMessagesInterface {
  @Shadow private ClientLevel level;

  @Unique private volatile String lastDeathMessage = "";

  @Override
  public String getLastDeathMessage() {
    return lastDeathMessage;
  }

  @Override
  public void setLastDeathMessage(String message) {
    lastDeathMessage = message;
  }

  @Inject(method = "handlePlayerCombatKill", at = @At("HEAD"), cancellable = false)
  public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
    Entity entity = this.level.getEntity(packet.playerId());
    if (entity == ((ClientCommonNetworkHandlerClientAccessor) this).getClient().player) {
      setLastDeathMessage(packet.message().getString());
    }
  }
}
