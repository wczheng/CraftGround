package com.kyhsgeekcode.minecraftenv.mixin;

import com.kyhsgeekcode.minecraftenv.MouseInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Food-only use: native duration/consumption, no raycast interaction or auto-selection. */
@Mixin(MinecraftClient.class)
public class EatActionMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void eatHeldFood(CallbackInfo ci) {
        if (!MouseInfo.INSTANCE.getEat()) return;
        ci.cancel();
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null || client.interactionManager == null
                || client.player.isUsingItem() || !client.player.canConsume(false)
                || !client.player.getMainHandStack().contains(DataComponentTypes.FOOD)) return;
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
    }
}
