package com.kyhsgeekcode.minecraftenv.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.dynamic.CodecCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DataComponentTypes.class)
public interface DataComponentTypesCacheAccessor {
    @Accessor("CACHE")
    static CodecCache getCodecCache() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
