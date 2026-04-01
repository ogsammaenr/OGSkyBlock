package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasePressurePlateBlock.class)
public abstract class BasePressurePlateBlockMixin {

    /**
     * checkPressed: Basınç plakasının durumunu (Açık/Kapalı) ve redstone gücünü güncelleyen ana metottur.
     */
    @Inject(
            method = "checkPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCheckPressed(Entity entity, Level level, BlockPos pos, BlockState state, int currentSignal, CallbackInfo ci) {
        // İstemciyi (Client) yoksay
        if (level.isClientSide()) return;

        // Plakayı tetiklemeye çalışan varlık bir oyuncuysa (null veya eşya değilse)
        if (entity instanceof ServerPlayer player) {
            Island island = IslandRegistry.getIslandAt(pos);

            // Oyuncunun yetkisi yoksa, plakanın çökmesini ve sinyal üretmesini kökünden engelle
            if (island != null && !IslandProtection.canPerformAction(player, pos, IslandAction.USE_PRESSURE_PLATES)) {
                ci.cancel();
            }
        }
    }
}