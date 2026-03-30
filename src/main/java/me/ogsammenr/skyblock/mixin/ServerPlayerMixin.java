package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.SkyblockMain;
import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.model.IslandAction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Artık doğrudan ServerPlayer sınıfına sızıyoruz. (Client kontrolüne veya cast işlemine gerek kalmadı!)
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    // 1. KORUMA: "Q" tuşu ile atmayı engeller (Hotbar'dan)
    @Inject(method = "drop(Z)V", at = @At("HEAD"), cancellable = true)
    private void onDropKeyItem(boolean dropAll, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        if (player.isCreative()) return;

        if (!player.level().dimension().equals(SkyblockMain.SKYBLOCK_WORLD_KEY)) return;

        if (!IslandProtection.canPerformAction(player, player.blockPosition(), IslandAction.ITEM_DROP)) {
            player.sendSystemMessage(Component.literal("§cBu adada yere eşya atamazsın!"));

            // İşlemi iptal et (Eşya sunucuda hiç silinmez)
            ci.cancel();

            // HAYALET EŞYA ÇÖZÜMÜ: O slotu zorla güncelliyoruz.
            // Hotbar slotları 0-8'dir ama ContainerMenu içinde 36-44 arasına denk gelir.
            int syncSlot = player.getInventory().getSelectedSlot() + 36;
            player.connection.send(new ClientboundContainerSetSlotPacket(
                    player.inventoryMenu.containerId,
                    player.inventoryMenu.getStateId(),
                    syncSlot,
                    player.getInventory().getSelectedItem()
            ));
        }
    }

    // 2. KORUMA: Envanterden Fırlatma (Item Deletion engellendi!)
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void onDropInventoryItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        if (player.isCreative()) return;

        if (!player.level().dimension().equals(SkyblockMain.SKYBLOCK_WORLD_KEY)) return;

        if (!IslandProtection.canPerformAction(player, player.blockPosition(),  IslandAction.ITEM_DROP)) {

            if (stack == null || stack.isEmpty()) return;

            player.sendSystemMessage(Component.literal("§cBu adada yere eşya atamazsın!"));

            // İşlemi iptal et (Eşya dünyaya düşmez)
            cir.setReturnValue(null);

            // Hayatta kalma modundaysa eşyayı geri ver
            // Önce envantere (boş bir slota) eklemeyi dene
            boolean wasAdded = player.getInventory().add(stack);

            // Eğer envanter ağzına kadar doluysa ve eklenemediyse (kaybolmasını önlemek için):
            // Eşyayı oyuncunun fare imlecine (cursor) yapıştır.
            if (!wasAdded) {
                player.containerMenu.setCarried(stack);
            }


            // Oyuncunun ekranındaki tüm menüleri zorla güncelle (Hayalet eşyayı önler)
            player.inventoryMenu.sendAllDataToRemote();
            player.containerMenu.sendAllDataToRemote();
        }
    }
}