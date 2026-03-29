package me.ogsammenr.skyblock.manager;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import static me.ogsammenr.skyblock.SkyblockMain.*;

public class IslandProtection {

    /**
     * Checks if the player can perform the given action at the specified position based on island ownership and permissions.
     * @param player The player attempting the action
     * @param pos The block position where the action is being attempted
     * @param action The type of action being attempted (e.g., breaking a block, interacting with a door)
     * @return true if the player can perform the action, false otherwise
     */
    public static boolean canPerformAction(ServerPlayer player, BlockPos pos, IslandAction action) {

        // 1. ADIM: Admin (Kurucu) Yetkisi Kontrolü
        // Eğer oyuncuda genel admin yetkisi varsa, hiçbir korumaya takılmadan her şeyi yapabilir.
        if (Permissions.check(player, ADMIN_PERMISSION, 4)) {
            return true;
        }

        // 2. ADIM: Aksiyona Özel Bypass (Aşma) Kontrolü
        boolean hasBypassPermission = switch (action) {

            // Fiziksel Blok Değiştirme Eylemleri -> BREAK_BYPASS yetkisi gerektirir
            case BREAK_BLOCK, PLACE_BLOCK, TRAMPLE_CROPS, FIRE_EXTINGUISH ->
                    Permissions.check(player, BREAK_BYPASS_PERMISSION, 4);

            // Kritik Ada Eylemleri -> Bypass edilemez! (Sadece adanın COOP/OWNER'ı veya üstteki ADMIN yapabilir)
            case LOCK_ISLAND -> false;

            // Geriye kalan 48 adet etkileşim (Sandık, Mob, Kapı, Kova vb.) -> INTERACT_BYPASS yetkisi gerektirir
            default ->
                    Permissions.check(player, INTERACT_BYPASS_PERMISSION, 4);
        };

        if (hasBypassPermission) {
            // İsteğe bağlı: Konsolu spama boğmamak için bu debug mesajını silebilir veya yoruma alabilirsin
            // System.out.println("Player " + player.getName().getString() + " has bypass permission for action " + action);
            return true;
        }

        // 3. ADIM: Dünya Kontrolü (Sadece Skyblock dünyasında koruma aktiftir)
        if (!player.level().dimension().equals(SKYBLOCK_WORLD_KEY)) {
            return true;
        }

        // 4. ADIM: Ada Sınırları ve Rütbe Kontrolü
        Island targetIsland = IslandRegistry.getIslandAt(pos);

        // Eğer tıklanan/kırılan yerde bir ada yoksa (boşluk veya spawn bölgesi vb.)
        // Koruma mantığına göre false (yasak) veya true (serbest) döndürebilirsin.
        // Genellikle adasız bölgelerde işlem yapmak yasaktır.
        if (targetIsland == null) {
            return false;
        }

        // Son olarak oyuncunun rütbesi bu aksiyona yetiyor mu kontrol et
        return targetIsland.canPerformAction(player.getUUID(), action);
    }
}
