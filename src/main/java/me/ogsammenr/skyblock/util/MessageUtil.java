package me.ogsammenr.skyblock.util;

import me.ogsammenr.skyblock.model.IslandAction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MessageUtil {
        /**
        * Renk kodlarını çevirir. Minecraft'ta renk kodları genellikle '§' sembolü ile başlar.
        * Örneğin, "§cKırmızı Metin" ifadesi kırmızı renkte bir metin oluşturur.
        *
        * @param message Renk kodları içeren mesaj
        * @return Renk kodları çevrilmiş mesaj
        */
        public static String translateColorCodes(String message) {
            if (message == null) return null;
            return message.replace('&', '§');
        }

    /**
     * Oyuncuya engellenen eylemin türüne göre özel bir Action Bar mesajı gönderir.
     */
    public static void sendDenyMessage(ServerPlayer player, IslandAction action) {
        String message = switch (action) {
            case BREAK_BLOCK -> "Bu adada blok kırma yetkiniz yok!";
            case PLACE_BLOCK -> "Bu adada blok yerleştiremezsiniz!";
            case USE_CONTAINERS -> "Bu adadaki sandıkları ve depoları açamazsınız!";
            case MOUNT_INVENTORY -> "Bu adada araç ve binek envanterlerine erişemezsiniz!";
            case RIDE_ANIMALS -> "Bu adada binekleri ve araçları kullanamazsınız!";
            case PLACE_VEHICLE -> "Bu adada bot veya vagon yerleştiremezsiniz!";
            case HURT_ANIMALS -> "Bu adada hayvanlara zarar veremezsiniz!";
            case HURT_MONSTERS -> "Bu adada canavarlara zarar veremezsiniz!";
            case HURT_VILLAGERS -> "Bu adada köylülere zarar veremezsiniz!";
            case USE_SPAWN_EGGS -> "Bu adada çağırıcı yumurta kullanamazsınız!";
            case USE_BUCKETS, COLLECT_WATER, COLLECT_LAVA -> "Bu adada kova kullanamazsınız!";
            case IGNITE_FIRE, FIRE_EXTINGUISH -> "Bu adada ateşle etkileşime geçemezsiniz!";
            case TRADE_WITH_VILLAGER -> "Bu adadaki köylülerle ticaret yapamazsınız!";
            case USE_DOORS, USE_GATES, USE_TRAPDOORS -> "Bu adadaki kapıları kullanamazsınız!";
            case USE_REDSTONE_ITEMS, USE_BUTTONS, USE_LEVERS -> "Bu adadaki mekanizmaları kullanamazsınız!";
            case USE_ENDERPEARLS, THROW_POTIONS, THROW_EGGS -> "Bu adada fırlatılabilir eşya kullanamazsınız!";
            case USE_BEDS -> "Bu adada yatak kullanamazsınız!";
            case ITEM_DROP -> "Bu adada eşya fırlatamazsınız!";
            case USE_LEASH -> "Bu adada kayış kullanamazsınız!";
            case USE_ANVILS -> "Bu adada örs kullanamazsınız!";
            case LOCK_ISLAND -> "Bu adaya giriş yapamazsınız!";
            case USE_BEACONS -> "Bu adada fener kullanamazsınız!";
            case USE_HOPPERS ->  "Bu adada huni kullanamazsınız!";
            case MILK_ANIMALS -> "Bu adada hayvanları sağamazsınız!";
            case USE_DROPPERS -> "Bu adada bırakıcı kullanamazsınız!";
            case SHEAR_ANIMALS -> "Bu adada hayvanları kırpamazsınız!";
            case USE_NAME_TAGS -> "Bu adada isim etiketi kullanamazsınız!";
            case USE_DISPENSERS -> "Bu adada fırlatıcı kullanamazsınız!";
            case USE_END_PORTAL -> "Bu adada End Portal'ı kullanamazsınız!";
            case USE_NOTE_BLOCK -> "Bu adada nota bloğu kullanamazsınız!";
            case USE_FISHING_ROD -> "Bu adada olta kullanamazsınız!";
            case BREED_ANIMALS -> "Bu adada hayvanları çiftleştiremezsiniz!";
            case USE_NETHER_PORTAL -> "Bu adada Nether Portal'ı kullanamazsınız!";
            case USE_BREWING_STANDS -> "Bu adada iksir standı kullanamazsınız!";
            case EAT_CHORUS_FRUIT -> "Bu adada Chorus Fruit yiyemezsiniz!";
            case USE_JUKEBOX -> "Bu adada müzik kutusu kullanamazsınız!";
            case INTERACT_SHELFS -> "Bu adada raflarla etkileşime geçemezsiniz!";
            case USE_COMPOSTERS -> "Bu adada kompostör kullanamazsınız!";
            case BREAK_VEHICLE -> "Bu adada bot veya vagon kıramazsınız!";

            default -> "Bu adada bu eylemi gerçekleştirmek için yetkiniz bulunmuyor!";
        };

        player.displayClientMessage(Component.literal("§c" + message), true);
    }
}
