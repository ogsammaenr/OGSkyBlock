package me.ogsammenr.skyblock.ui.menus;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandRole;
import me.ogsammenr.skyblock.model.IslandSetting;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.BaseMenu;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GeneralSettingsMenu extends BaseMenu {
    private final Island island;

    public GeneralSettingsMenu(ServerPlayer player, MenuData menuData) {
        super(player, menuData);
        // Menüyü açan oyuncunun adasını buluyoruz
        this.island = IslandRegistry.getIslandAt(player.blockPosition());
    }

    @Override
    protected void handleAction(String action) {
        // Özel bir aksiyon eklemek istersen (Örn: Geri dön butonu) burayı kullanabilirsin.
        // Ayar değiştirmeleri customizeItem içindeki callback ile hallediyoruz.
    }

    @Override
    protected GuiElementBuilder customizeItem(GuiElementBuilder builder, MenuData.MenuItem item) {
        // Eğer oyuncunun adası yoksa veya aksiyon boşsa dokunma
        if (this.island == null || item.action == null) return builder;

        // Sadece "SETTING_" ile başlayan genel ayarları yakala
        if (item.action.startsWith("SETTING_")) {

            // "SETTING_FIRE_SPREAD" -> "FIRE_SPREAD" dönüşümü
            String settingName = item.action.replace("SETTING_", "");
            IslandSetting islandSetting;

            try {
                islandSetting = IslandSetting.valueOf(settingName);
            } catch (IllegalArgumentException e) {
                return builder; // JSON'a yanlış bir ayar ismi yazılmışsa yoksay
            }

            // Adanın mevcut ayarını (True/False) çekiyoruz
            boolean isEnabled = island.getSetting(islandSetting);

            // --- LORE (AÇIKLAMA) OLUŞTURMA ---
            builder.addLoreLine(Component.literal(""));

            // Ayarın durumuna göre dinamik yazı rengi ve metin
            if (isEnabled) {
                builder.addLoreLine(Component.literal("§7Durum: §aAÇIK"));
            } else {
                builder.addLoreLine(Component.literal("§7Durum: §cKAPALI"));
            }

            builder.addLoreLine(Component.literal(""));
            builder.addLoreLine(Component.literal("§eDeğiştirmek için tıkla!"));

            // --- TIKLAMA (CLICK) OLAYINI YAKALAMA ---
            builder.setCallback((index, type, slotAction, gui) -> {

                // Güvenlik: Ziyaretçiler veya Düşük Rütbeliler ayarları bozamasın
                IslandRole playerRole = island.getPlayerRole(player.getUUID());
                if (!playerRole.isAtLeast(IslandRole.COOP)) {
                    player.sendSystemMessage(Component.literal("§cAda ayarlarını değiştirmek için en az COOP olmalısın."));
                    return;
                }

                // Değeri tam tersine çevir (True ise False, False ise True yap)
                island.setSetting(islandSetting, !isEnabled);

                // Anında Yenilenme: BaseMenu kullandığımız için this.build() metodunu çağırarak
                // menüyü hiç kapatmadan saniyesinde yeni renklerle ekrana tekrar çizdiriyoruz!
                this.build();
            });
        }

        return builder;
    }
}
