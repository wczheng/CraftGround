package com.kyhsgeekcode.minecraftenv.mixin;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(HungerManager.class)
public class SurvivalHungerMixin {
    // The second 80 is starvation; leave native food healing unchanged.
    @ModifyConstant(method = "update", constant = @Constant(intValue = 80, ordinal = 1))
    private int starvationInterval(int vanilla, PlayerEntity player) {
        return player.getCommandTags().contains("vafm_survival") ? 20 : vanilla;
    }
}
