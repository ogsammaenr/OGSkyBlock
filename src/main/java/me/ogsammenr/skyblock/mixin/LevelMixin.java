package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.model.IslandSetting;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin {

    // 9 Parametreli ve void dönen explode metodunu hedefliyoruz. Geri dönüş tipi V (Void).
    @Inject(
            method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onExplode(
            Entity entity,
            DamageSource damageSource,
            ExplosionDamageCalculator calculator,
            double x, double y, double z,
            float radius, boolean fire,
            Level.ExplosionInteraction interaction,
            CallbackInfo ci
    ) {
        Level level = (Level) (Object) this;

        if (level.isClientSide()) {
            return;
        }

        // Eğer zaten NONE değilse (Sonsuz döngüyü önlemek için)
        if (interaction != Level.ExplosionInteraction.NONE) {

            BlockPos pos = BlockPos.containing(x, y, z);
            var island = IslandRegistry.getIslandAt(pos);

            // Adada TNT hasarı kapalıysa
            if (island != null && !island.getSetting(IslandSetting.TNT_DAMAGE)) {

                // 1. Orijinal hasarlı patlamayı durdur
                ci.cancel();

                // 2. Metodu, blok kırmayı engelleyen (NONE) interaction ile tekrar çağır
                level.explode(entity, damageSource, calculator, x, y, z, radius, fire, Level.ExplosionInteraction.NONE);
            }
        }
    }
}