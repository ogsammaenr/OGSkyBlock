package me.ogsammenr.skyblock.ui.menus;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.ogsammenr.skyblock.manager.IslandManager;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.model.IslandRole;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.PaginatedMenu;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ActionSettingsMenu extends PaginatedMenu {
    private final Island island;

    public ActionSettingsMenu(ServerPlayer player, MenuData menuData) {
        super(player, menuData);
        // Menüyü açan oyuncunun adasını buluyoruz
        this.island = IslandRegistry.getIslandAt(player.blockPosition());
    }

    @Override
    public void handleCustomAction(String action) {
        // NEXT_PAGE ve PREVIOUS_PAGE zaten PaginatedMenu'de işleniyor.
        // Bu menüde ekstra düz bir buton tıklaması (Örn: Geri Dön) olursa buraya yazılabilir.
    }

    @Override
    protected GuiElementBuilder customizeItem(GuiElementBuilder builder, MenuData.MenuItem item) {
        // Eğer oyuncunun adası yoksa (hata durumu) veya eşyanın aksiyonu yoksa dokunma
        if (this.island == null || item.action == null) return builder;

        // Sadece "ACTION_" ile başlayan koruma ayarı eşyalarını yakala
        if (item.action.startsWith("ACTION_")) {

            // "ACTION_BREAK_BLOCK" -> "BREAK_BLOCK" dönüşümü
            String actionName = item.action.replace("ACTION_", "");
            IslandAction islandAction;

            try {
                islandAction = IslandAction.valueOf(actionName);
            } catch (IllegalArgumentException e) {
                return builder; // Eğer JSON'a yanlış bir isim yazılmışsa yoksay
            }

            // Mevcut ayarı adadan çekiyoruz
            IslandRole currentRole = island.getPermission(islandAction);

            // --- LORE (AÇIKLAMA) OLUŞTURMA ---
            builder.addLoreLine(Component.literal(""));
            builder.addLoreLine(Component.literal("§7Gerekli Rütbe:"));

            // Tüm rütbeleri döngüyle ekrana yazdır (Seçili olan Yeşil, diğerleri Gri)
            for (IslandRole role : IslandRole.values()) {
                if (role == currentRole) {
                    builder.addLoreLine(Component.literal("  §a▶ " + role.name()));
                } else {
                    builder.addLoreLine(Component.literal("  §8- " + role.name()));
                }
            }

            builder.addLoreLine(Component.literal(""));
            builder.addLoreLine(Component.literal("§bSol Tık §7ile rütbeyi yükselt"));
            builder.addLoreLine(Component.literal("§cSağ Tık §7ile rütbeyi düşür"));

            // --- TIKLAMA (CLICK) OLAYINI YAKALAMA (OVERRIDE) ---
            // BaseMenu'deki varsayılan tıklamayı iptal edip, kendi Sol/Sağ tık mantığımızı ekliyoruz
            builder.setCallback((index, type, slotAction, gui) -> {

                // Oyuncu yetkili (COOP veya OWNER) değilse ayarları değiştiremesin
                IslandRole playerRole = island.getPlayerRole(player.getUUID());
                if (!playerRole.isAtLeast(IslandRole.COOP)) {
                    player.sendSystemMessage(Component.literal("§cAda ayarlarını değiştirmek için en az COOP olmalısın."));
                    return;
                }

                if (type == ClickType.MOUSE_LEFT || type == ClickType.MOUSE_LEFT_SHIFT) {
                    // Sol Tık: Rütbeyi Yükselt (Weight + 1)
                    changeRole(islandAction, currentRole, 1);
                }
                else if (type == ClickType.MOUSE_RIGHT || type == ClickType.MOUSE_RIGHT_SHIFT) {
                    // Sağ Tık: Rütbeyi Düşür (Weight - 1)
                    changeRole(islandAction, currentRole, -1);
                }
            });
        }

        return builder;
    }

    /**
     * Rütbe ağırlığını (Weight) hesaplayıp güncelleyen yardımcı metod
     */
    private void changeRole(IslandAction action, IslandRole currentRole, int direction) {
        int newWeight = currentRole.getWeight() + direction;

        // Ağırlığı 0 (VISITOR) ile 4 (OWNER) arasında sınırlandır (Sınır dışına taşmayı engelle)
        newWeight = Math.max(0, Math.min(4, newWeight));

        // Yeni ağırlığa karşılık gelen rütbeyi bul
        IslandRole newRole = getRoleByWeight(newWeight);

        // Eğer rütbe gerçekten değiştiyse kaydet ve sayfayı anında yenile
        if (newRole != currentRole) {
            island.setPermission(action, newRole);

            // SGUI'nin gücü: Sayfayı anında tekrar çizdiriyoruz, böylece yeşil ok (▶) hareket ediyor!
            this.buildPage();
        }
    }

    /**
     * Ağırlık (Weight) numarasına göre IslandRole enum'unu bulur
     */
    private IslandRole getRoleByWeight(int weight) {
        for (IslandRole role : IslandRole.values()) {
            if (role.getWeight() == weight) {
                return role;
            }
        }
        return IslandRole.VISITOR; // Fallback (Hata durumu)
    }
}
