package me.ogsammenr.skyblock.mixin;

import me.ogsammenr.skyblock.model.IslandSetting;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {


    @Inject(
            method = "checkSpawnRules",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onCheckSpawnRules(
            EntityType<?> entityType,
            ServerLevelAccessor serverLevelAccessor,
            EntitySpawnReason entitySpawnReason,
            BlockPos blockPos,
            RandomSource randomSource,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Sadece DOĞAL doğmaları ve CHUNK OLUŞUMU sırasındaki doğmaları hedef alıyoruz.
        if (entitySpawnReason == EntitySpawnReason.NATURAL || entitySpawnReason == EntitySpawnReason.CHUNK_GENERATION) {
            // Sadece düşman yaratıkları engellemek için kategori kontrolü
            if (entityType.getCategory() == MobCategory.MONSTER) {

                // DÜZELTME 2: pos yerine parametredeki blockPos kullanıldı
                var island = IslandRegistry.getIslandAt(blockPos);

                // Eğer ada varsa ve canavar doğma ayarı kapalıysa, doğmayı reddet
                if (island != null && !island.getSetting(IslandSetting.MONSTER_SPAWNING)) {
                    cir.setReturnValue(false); // Spawn iptal edildi.
                }
            }
        }
    }
}