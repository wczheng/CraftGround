package com.kyhsgeekcode.minecraftenv.mixin;

import com.kyhsgeekcode.minecraftenv.ItemPickupTracker;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemPickupMixin {
    @Unique private Item craftground$item;
    @Unique private int craftground$before;

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void beforePickup(PlayerEntity player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.getWorld().isClient) {
            craftground$item = self.getStack().getItem();
            craftground$before = player.getInventory().count(craftground$item);
        }
    }

    @Inject(method = "onPlayerCollision", at = @At("RETURN"))
    private void afterPickup(PlayerEntity player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.getWorld().isClient) {
            int moved = player.getInventory().count(craftground$item) - craftground$before;
            ItemPickupTracker.record(player.getUuid(), Registries.ITEM.getId(craftground$item).toString(), moved);
        }
    }
}
