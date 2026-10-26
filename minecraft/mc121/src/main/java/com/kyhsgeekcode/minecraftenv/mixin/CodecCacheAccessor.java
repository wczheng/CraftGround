package com.kyhsgeekcode.minecraftenv.mixin;

import com.google.common.cache.LoadingCache;
import net.minecraft.util.dynamic.CodecCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CodecCache.class)
public interface CodecCacheAccessor {
    @Accessor("cache")
    LoadingCache<?, ?> getEntries();
}
