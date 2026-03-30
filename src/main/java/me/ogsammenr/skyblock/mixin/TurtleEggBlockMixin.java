package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TurtleEggBlock.class)
public abstract class TurtleEggBlockMixin {

    /**
     * Yumurtanın üzerine düşüldüğünde tetiklenir.
     */
    @Inject(
            method = "fallOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        if (level.isClientSide()) return;

        if (checkProtection(level, pos, entity)) {
            ci.cancel();
        }
    }

    /**
     * Yumurtanın üzerinde yüründüğünde/zıplandığında tetiklenir.
     */
    @Inject(
            method = "stepOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onStepOn(Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        if (level.isClientSide()) return;

        if (checkProtection(level, pos, entity)) {
            ci.cancel();
        }
    }

    /**
     * Ortak koruma kontrolü.
     * @return true ise eylem engellenmeli (cancel edilmeli).
     */
    private boolean checkProtection(Level level, BlockPos pos, Entity entity) {
        var island = IslandRegistry.getIslandAt(pos);
        if (island == null) return false;

        // 1. Eylemi yapan bir oyuncu ise yetkisini kontrol et
        if (entity instanceof ServerPlayer player) {
            return !IslandProtection.canPerformAction(player, pos, IslandAction.TURTLE_EGGS);
        }

        // 2. Eylemi yapan bir mob veya başka bir varlıksa adayı koru
        // (İstenirse bu kısım IslandSetting.MONSTER_GRIEFING gibi bir ayara da bağlanabilir)
        return true;
    }
}