package me.ogsammenr.skyblock.model;

/**
 * Adadaki oyuncuların yapabileceği eylemleri (Aksiyonları) temsil eder.
 * Rol tabanlı koruma (Role-based Protection) sistemi için kullanılır.
 */
public enum IslandAction {
    // --- Blok ve Çevre Etkileşimi ---
    BREAK_BLOCK,
    PLACE_BLOCK,
    TRAMPLE_CROPS,
    FIRE_EXTINGUISH,
    IGNITE_FIRE,

    // --- Kullanım ve Temel Etkileşim ---
    USE_DOORS,
    USE_GATES,
    USE_TRAPDOORS,
    USE_ANVILS,
    USE_BEDS,
    USE_BEACONS,
    USE_BREWING_STANDS,
    USE_JUKEBOX,
    USE_NOTE_BLOCK,

    // --- Depolama ve Konteynerler ---
    USE_CONTAINERS,    // Sandık, Fıçı, Shulker vb.
    USE_DISPENSERS,
    USE_DROPPERS,
    USE_HOPPERS,

    // --- Kızıltaş (Redstone) Mekanizmaları ---
    USE_BUTTONS,
    USE_LEVERS,
    USE_PRESSURE_PLATES,
    USE_REDSTONE_ITEMS,

    // --- Eşya Kullanımı ---
    USE_BUCKETS,
    COLLECT_LAVA,
    COLLECT_WATER,
    USE_SPAWN_EGGS,
    THROW_EGGS,
    THROW_POTIONS,
    USE_ENDERPEARLS,
    EAT_CHORUS_FRUIT,
    USE_NAME_TAGS,
    USE_LEASH,

    // --- Varlık (Entity) Etkileşimi ---
    HURT_ANIMALS,
    HURT_MONSTERS,
    HURT_VILLAGERS,
    BREED_ANIMALS,
    MILK_ANIMALS,
    SHEAR_ANIMALS,
    RIDE_ANIMALS,
    MOUNT_INVENTORY,
    TRADE_WITH_VILLAGER,
    USE_ARMOR_STANDS,
    USE_ITEM_FRAMES,

    // --- Dünya ve Mekanikler ---
    LOCK_ISLAND,
    ITEM_DROP,
    ITEM_PICKUP,
    EXPERIENCE_PICKUP, // Yazım hatası düzeltildi (EXPERINCE -> EXPERIENCE)
    FISH_SCOOPING,
    FROST_WALKER,
    USE_NETHER_PORTAL,
    USE_END_PORTAL,
    TURTLE_EGGS
}
