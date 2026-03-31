package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.model.IslandAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin extends Entity {


    public ExperienceOrbMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * SADECE FİZİKSEL TEMAS KORUMASI:
     * XP küresi vanilla mekanikleriyle oyuncuyu takip eder (sunucuya ek yük binmez).
     * Ancak oyuncuya değdiği an, oyuncunun yetkisi yoksa metot iptal edilir
     * ve XP küresi oyuncuya tecrübe puanı verip yok olamaz.
     */
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Kontrolü orb'un bulunduğu konuma göre yapıyoruz
            if (!IslandProtection.canPerformAction(serverPlayer, this.blockPosition(), IslandAction.EXPERIENCE_PICKUP)) {
                ci.cancel();
            }
        }
    }
}