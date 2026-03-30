package me.ogsammenr.skyblock.model;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Island {
    private final UUID islandUUID;
    private final BlockPos center;
    private BlockPos spawnPoint;

    // PlayerUUID -> IslandRole
    private final Map<UUID, IslandRole> members = new HashMap<>();
    // IslandAction -> Minimum Required Role
    private final Map<IslandAction, IslandRole> permissions = new HashMap<>();
    // IslandSetting -> Boolean (Genel Ada Ayarları: Açık/Kapalı)
    private final Map<IslandSetting, Boolean> settings = new HashMap<>();

    /**
     * create new island with default permissions
     *
     * @param islandUUID islands unique identifier
     * @param ownerUUID island owner unique identifier
     * @param center island center position (bedrock coordinates)
     */
    public Island(UUID islandUUID, UUID ownerUUID, BlockPos center) {
        this.islandUUID = islandUUID;
        this.center = center;
        this.spawnPoint = center.above();

        this.members.put(ownerUUID, IslandRole.OWNER);

        setDefaultPermissions();
        setDefaultSettings();
    }

    /**
     * @param playerUUID the UUID of the player whose role we want to check
     * @return the role of the player on the island, or VISITOR if they are not a member
     */
    public IslandRole getPlayerRole(UUID playerUUID) {
        return members.getOrDefault(playerUUID, IslandRole.VISITOR);
    }

    /**
     * Check if a player can perform a specific action based on their role and the island's permissions.
     *
     * @param playerUUID the UUID of the player attempting the action
     * @param action the action the player is trying to perform
     * @return true if the player has the required role to perform the action, false otherwise
     */
    public boolean canPerformAction(UUID playerUUID, IslandAction action) {
        IslandRole playerRole = getPlayerRole(playerUUID);
        IslandRole requiredRole = permissions.getOrDefault(action, IslandRole.OWNER);
        return playerRole.isAtLeast(requiredRole);
    }

    /**
     * Adanın belirli bir ayarının açık olup olmadığını kontrol eder.
     */
    public boolean getSetting(IslandSetting setting) {
        // Varsayılan olarak kapalı (false) döner, ancak setDefaultSettings'de hepsini tanımladık.
        return settings.getOrDefault(setting, false);
    }

    /**
     * Adanın belirli bir ayarını değiştirir.
     */
    public void setSetting(IslandSetting setting, boolean value) {
        settings.put(setting, value);
    }

    /**
     * Belirli bir aksiyon için gereken minimum rütbeyi döndürür.
     */
    public IslandRole getPermission(IslandAction action) {
        // Eğer aksiyon tanımlanmamışsa güvenlik için OWNER (Kurucu) döndürür
        return permissions.getOrDefault(action, IslandRole.OWNER);
    }

    /**
     * Belirli bir aksiyonun gereksinim duyduğu rütbeyi değiştirir.
     */
    public void setPermission(IslandAction action, IslandRole role) {
        permissions.put(action, role);
    }

    // --------------------------------------

    public boolean isWithinBounds(BlockPos pos) {
        return Math.abs(pos.getX() - center.getX())<= getProtectionRadius() &&
                Math.abs(pos.getZ() - center.getZ()) <= getProtectionRadius();
    }

    /**
     * @return the center position of the island (bedrock coordinates)
     */
    public BlockPos getCenter() {
        return center;
    }

    /**
     * @return the spawn point of the island
     */
    public BlockPos getSpawnPoint() {
        return spawnPoint;
    }
    /**
     * Set the spawn point of the island.
     *
     * @param spawnPoint the new spawn point to set
     */
    public void setSpawnPoint(BlockPos spawnPoint) {
        this.spawnPoint = spawnPoint;
    }
    /**
     * @return the unique identifier of the island
     */
    public UUID getIslandUUID() {
        return islandUUID;
    }

    public int getProtectionRadius() {
        return 50; // Sabit bir koruma yarıçapı, istenirse dinamik hale getirilebilir
    }

    // --------------------------------------

    /**
     * Adanın varsayılan açık/kapalı ayarlarını belirler.
     */
    private void setDefaultSettings() {
        settings.put(IslandSetting.ANIMAL_SPAWNING, true);   // Hayvan doğuşu açık
        settings.put(IslandSetting.MONSTER_SPAWNING, true);  // Mob doğuşu açık (Mob farmı için)
        settings.put(IslandSetting.FIRE_SPREAD, false);      // Yangın yayılması kapalı (Grief önlemi)
        settings.put(IslandSetting.OVERWORLD_PVP, false);    // PvP varsayılan olarak kapalı
        settings.put(IslandSetting.NETHER_PVP, false);
        settings.put(IslandSetting.END_PVP, false);
        settings.put(IslandSetting.TNT_DAMAGE, false);
    }

    private void setDefaultPermissions() {
        // --- Blok ve Çevre Etkileşimi ---
        permissions.put(IslandAction.BREAK_BLOCK, IslandRole.MEMBER);
        permissions.put(IslandAction.PLACE_BLOCK, IslandRole.MEMBER);
        permissions.put(IslandAction.TRAMPLE_CROPS, IslandRole.MEMBER);
        permissions.put(IslandAction.FIRE_EXTINGUISH, IslandRole.MEMBER);
        permissions.put(IslandAction.IGNITE_FIRE, IslandRole.TRUSTED);

        // --- Kullanım ve Temel Etkileşim ---
        permissions.put(IslandAction.USE_DOORS, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_GATES, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_TRAPDOORS, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_ANVILS, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_BEDS, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_BEACONS, IslandRole.TRUSTED);
        permissions.put(IslandAction.USE_BREWING_STANDS, IslandRole.TRUSTED);
        permissions.put(IslandAction.USE_JUKEBOX, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_NOTE_BLOCK, IslandRole.MEMBER);

        // --- Depolama ve Konteynerler ---
        permissions.put(IslandAction.USE_CONTAINERS, IslandRole.TRUSTED); // Sandık, Fıçı vb.
        permissions.put(IslandAction.USE_DISPENSERS, IslandRole.TRUSTED);
        permissions.put(IslandAction.USE_DROPPERS, IslandRole.TRUSTED);
        permissions.put(IslandAction.USE_HOPPERS, IslandRole.TRUSTED);

        // --- Kızıltaş (Redstone) Mekanizmaları ---
        permissions.put(IslandAction.USE_BUTTONS, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_LEVERS, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_PRESSURE_PLATES, IslandRole.VISITOR); // Ziyaretçiler otomatik kapılardan geçebilsin
        permissions.put(IslandAction.USE_REDSTONE_ITEMS, IslandRole.TRUSTED); // Kızıltaş sistemini bozmamaları için

        // --- Eşya Kullanımı ---
        permissions.put(IslandAction.USE_BUCKETS, IslandRole.TRUSTED); // Lav/Su trollemesini önlemek için
        permissions.put(IslandAction.COLLECT_LAVA, IslandRole.TRUSTED);
        permissions.put(IslandAction.COLLECT_WATER, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_SPAWN_EGGS, IslandRole.COOP); // Etrafa rastgele mob doğurmayı engellemek için
        permissions.put(IslandAction.THROW_EGGS, IslandRole.MEMBER);
        permissions.put(IslandAction.THROW_POTIONS, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_ENDERPEARLS, IslandRole.MEMBER);
        permissions.put(IslandAction.EAT_CHORUS_FRUIT, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_NAME_TAGS, IslandRole.TRUSTED);
        permissions.put(IslandAction.USE_LEASH, IslandRole.TRUSTED);

        // --- Varlık (Entity) Etkileşimi ---
        permissions.put(IslandAction.HURT_ANIMALS, IslandRole.TRUSTED); // Hayvan çiftliğini korumak için
        permissions.put(IslandAction.HURT_MONSTERS, IslandRole.MEMBER); // Mob farmında kasılabilmeleri için
        permissions.put(IslandAction.HURT_VILLAGERS, IslandRole.OWNER); // Köylüleri kazara/kasten öldürmeyi engellemek için
        permissions.put(IslandAction.BREED_ANIMALS, IslandRole.MEMBER);
        permissions.put(IslandAction.MILK_ANIMALS, IslandRole.MEMBER);
        permissions.put(IslandAction.SHEAR_ANIMALS, IslandRole.MEMBER);
        permissions.put(IslandAction.RIDE_ANIMALS, IslandRole.MEMBER);
        permissions.put(IslandAction.MOUNT_INVENTORY, IslandRole.TRUSTED); // Atların envanterinden eyer/zırh çalınmasını önlemek için
        permissions.put(IslandAction.TRADE_WITH_VILLAGER, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_ARMOR_STANDS, IslandRole.TRUSTED); // Zırh askısından eşya çalınmasını önlemek için
        permissions.put(IslandAction.USE_ITEM_FRAMES, IslandRole.TRUSTED);  // Eşya çerçevelerini korumak için

        // --- Dünya ve Mekanikler ---
        permissions.put(IslandAction.LOCK_ISLAND, IslandRole.COOP); // Adayı kilitleme yetkisi
        permissions.put(IslandAction.ITEM_DROP, IslandRole.MEMBER);
        permissions.put(IslandAction.ITEM_PICKUP, IslandRole.MEMBER);
        permissions.put(IslandAction.EXPERIENCE_PICKUP, IslandRole.MEMBER);
        permissions.put(IslandAction.FISH_SCOOPING, IslandRole.MEMBER);
        permissions.put(IslandAction.FROST_WALKER, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_NETHER_PORTAL, IslandRole.MEMBER);
        permissions.put(IslandAction.USE_END_PORTAL, IslandRole.MEMBER);
        permissions.put(IslandAction.TURTLE_EGGS, IslandRole.TRUSTED); // Kaplumbağa yumurtalarını kırmamaları için
    }


}
