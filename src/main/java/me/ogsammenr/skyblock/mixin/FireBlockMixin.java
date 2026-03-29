package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.model.IslandSetting;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    /**
     * Minecraft 1.21'de FireBlock.randomTick yerine tick metodu kullanılır.
     * Bu metod ateşin yayılma mantığını (spreading) ve yanma (burning) sürecini yönetir.
     */
    @Inject(
            method = "tick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFireTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // Ateşin bulunduğu konumdaki ada nesnesini alıyoruz
        var island = IslandRegistry.getIslandAt(pos);

        // Eğer bir ada bölgesindeysek ve FIRE_SPREAD ayarı kapalı (false) ise işlemi iptal et
        if (island != null && !island.getSetting(IslandSetting.FIRE_SPREAD)) {
            ci.cancel();
        }
    }
}