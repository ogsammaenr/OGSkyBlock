package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.SkyblockMain;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {

        if (player.level().isClientSide()) return;

        if (player instanceof ServerPlayer serverPlayer) {
            // Sadece Skyblock dünyasındaysak bu koruma çalışsın
            if (!serverPlayer.level().dimension().equals(SkyblockMain.SKYBLOCK_WORLD_KEY)) {
                return;
            }

            // Mixin uyguladığımız sınıf ItemEntity olduğu için, "this" anahtar kelimesi ile eşyanın kendisini alıyoruz
            ItemEntity itemEntity = (ItemEntity) (Object) this;
            BlockPos pos = itemEntity.blockPosition();

            // Eşyanın bulunduğu yerdeki adayı buluyoruz
            Island island = IslandRegistry.getIslandAt(pos);

            // Eğer eşya bir adanın sınırları içindeyse ve oyuncunun ITEM_PICKUP yetkisi yoksa...
            if (island != null && !island.canPerformAction(serverPlayer.getUUID(), IslandAction.ITEM_PICKUP)) {

                // Metodu anında durdur! Oyuncu eşyayı alamaz, eşya yerde kalmaya devam eder.
                ci.cancel();
            }
        }

    }
}
