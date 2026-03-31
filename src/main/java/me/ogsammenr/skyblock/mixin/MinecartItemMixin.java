package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.model.IslandAction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartItem.class)
public abstract class MinecartItemMixin {

    /**
     * Vagon eşyası (MinecartItem) herhangi bir bloğa (özellikle raylara)
     * sağ tıklandığında, oyun motoru vagonu dünyaya çağırmadan hemen önce tetiklenir.
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void onPlaceMinecart(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();

        if (player instanceof ServerPlayer serverPlayer) {
            // context.getClickedPos() doğrudan tıklanan rayın net koordinatını verir.
            // Bu sayede pos.relative() kaymalarından kurtulmuş oluruz.
            if (!IslandProtection.canPerformAction(serverPlayer, context.getClickedPos(), IslandAction.PLACE_VEHICLE)) {

                serverPlayer.sendSystemMessage(Component.literal("§cBu adada vagon yerleştiremezsin!"));

                // İstemciye ve Sunucuya bu işlemin kesinlikle BAŞARISIZ olduğunu bildiriyoruz
                cir.setReturnValue(InteractionResult.FAIL);
            }
        }
    }
}