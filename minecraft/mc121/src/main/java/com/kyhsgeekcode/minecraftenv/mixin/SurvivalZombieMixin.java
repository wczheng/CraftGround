package com.kyhsgeekcode.minecraftenv.mixin;

import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public class SurvivalZombieMixin {
    @Inject(method = "burnsInDaylight", at = @At("HEAD"), cancellable = true)
    private void daylight(CallbackInfoReturnable<Boolean> result) {
        if (((ZombieEntity) (Object) this).getCommandTags().contains("vafm_survival")) {
            result.setReturnValue(false);
        }
    }
}
