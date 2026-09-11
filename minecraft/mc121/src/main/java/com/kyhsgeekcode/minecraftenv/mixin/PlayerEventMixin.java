package com.kyhsgeekcode.minecraftenv.mixin;

import com.kyhsgeekcode.minecraftenv.PlayerEventTracker;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class PlayerEventMixin {
    @Unique private float craftground$healthBeforeDamage;

    // PlayerEntity calls this after finishing use; capture before shrinking the stack.
    @Inject(method = "eatFood", at = @At("HEAD"))
    private void ateFood(World world, ItemStack stack, FoodComponent food,
                         CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            PlayerEventTracker.record(player.getUuid(),
                    "ate/" + Registries.ITEM.getId(stack.getItem()));
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void beforeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        craftground$healthBeforeDamage = ((LivingEntity) (Object) this).getHealth();
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void afterDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (!target.getWorld().isClient && target.getHealth() < craftground$healthBeforeDamage
                && source.getAttacker() instanceof ServerPlayerEntity player) {
            PlayerEventTracker.record(player.getUuid(),
                    "hit/" + Registries.ENTITY_TYPE.getId(target.getType()));
        }
    }
}
