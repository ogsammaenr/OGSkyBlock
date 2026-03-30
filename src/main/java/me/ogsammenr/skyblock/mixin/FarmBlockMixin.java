package me.ogsammenr.skyblock.mixin;

// Kendi projenden gelen importlar (Yolların doğruluğunu kontrol et)
import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.world.IslandRegistry;

// Mojang 1.21 Importları
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {

    /**
     * Bir varlık tarım arazisinin (FarmBlock) üzerine düştüğünde/zıpladığında tetiklenir.
     */
    @Inject(
            method = "fallOn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFallOn(Level level, BlockState blockState, BlockPos blockPos, Entity entity, double d, CallbackInfo ci) {
        // Sadece sunucu tarafında (Server-Side) işlem yap
        if (level.isClientSide()) return;

        Island island = IslandRegistry.getIslandAt(blockPos);

        // Eğer zıplanan tarla bir adanın sınırları içerisindeyse
        if (island != null) {

            // 1. Zıplayan kişi bir OYUNCU ise yetkisini kontrol et
            if (entity instanceof ServerPlayer player) {
                if (!IslandProtection.canPerformAction(player, blockPos, IslandAction.TRAMPLE_CROPS)) {
                    ci.cancel(); // Oyuncunun tarlayı bozma yetkisi yoksa iptal et
                }
            }
            // 2. Zıplayan varlık bir MOB, HAYVAN veya EŞYA ise direkt iptal et
            // (Adadaki ekinlerin yaratıklar yüzünden bozulmasını engeller)
            else {
                ci.cancel();
            }
        }
    }
}