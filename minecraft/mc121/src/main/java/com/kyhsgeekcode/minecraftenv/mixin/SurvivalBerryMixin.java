package com.kyhsgeekcode.minecraftenv.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SweetBerryBushBlock.class)
public class SurvivalBerryMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void grow(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo callback) {
        if (world.getPlayers().stream().noneMatch(p -> p.getCommandTags().contains("vafm_survival"))) return;
        long time = Math.floorMod(world.getTimeOfDay(), 24000L);
        boolean night = time >= 13000 && time < 23000;
        int age = state.get(SweetBerryBushBlock.AGE);
        // Native random-tick selection remains stochastic; night growth is half as likely.
        if (age < 3 && (!night || random.nextBoolean())) {
            world.setBlockState(pos, state.with(SweetBerryBushBlock.AGE, age + 1), 2);
        }
        callback.cancel();
    }
}
