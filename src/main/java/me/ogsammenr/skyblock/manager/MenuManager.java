package me.ogsammenr.skyblock.manager;

import com.google.gson.Gson;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.BaseMenu;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static me.ogsammenr.skyblock.SkyblockMain.LOGGER;

public class MenuManager {
    private static final Gson GSON = new Gson();
    private static final Map<String, MenuData> MENU_CACHE = new HashMap<>();
    private static final String MENU_HANDLER_PACKAGE = "me.ogsammenr.skyblock.ui.menus.";

    public static void loadAllMenus(File menuFolder) {
        MENU_CACHE.clear();
        if (!menuFolder.exists() && !menuFolder.mkdirs()) {
            LOGGER.error("Menu klasörü oluşturulamadı: {}", menuFolder.getAbsolutePath());
            return;
        }

        File[] menuFiles = menuFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (menuFiles == null) return;

        for (File file : menuFiles) {
            try (FileReader reader = new FileReader(file)) {
                MenuData data = GSON.fromJson(reader, MenuData.class);
                String menuName = file.getName().replace(".json", "");
                MENU_CACHE.put(menuName, data);
                LOGGER.info("Menü yüklendi: {}", menuName);
            } catch (Exception e) {
                LOGGER.error("Menü yüklenirken hata oluştu: {}", file.getName(), e);
            }
        }
        LOGGER.info("===== Toplam {} menü yüklendi =====", MENU_CACHE.size());
    }

    public static void openMenu(ServerPlayerEntity player, String menuName) {
        MenuData menuData = MENU_CACHE.get(menuName);
        if (menuData == null) {
            player.sendMessage(Text.literal("§cHata: '" + menuName + "' isimli menü bulunamadı!"), false);
            return;
        }

        try {
            String handlerClassName = menuData.handler;
            if (handlerClassName == null || handlerClassName.isEmpty()) {
                LOGGER.warn("'{}' menüsü için bir handler belirtilmemiş. Varsayılan menü kullanılamaz.", menuName);
                player.sendMessage(Text.literal("§cBu menü düzgün yapılandırılmamış."), false);
                return;
            }

            // Handler adından tam sınıf adını oluştur (örn: "IslandMenu" -> "me.ogsammenr.skyblock.ui.menus.IslandMenu")
            String fullClassName = MENU_HANDLER_PACKAGE + handlerClassName;

            Class<?> clazz = Class.forName(fullClassName);
            if (!BaseMenu.class.isAssignableFrom(clazz)) {
                 throw new ClassCastException("Handler sınıfı BaseMenu'den türetilmelidir: " + fullClassName);
            }
            
            Constructor<?> constructor = clazz.getConstructor(ServerPlayerEntity.class, MenuData.class);
            BaseMenu menuInstance = (BaseMenu) constructor.newInstance(player, menuData);
            menuInstance.open();

        } catch (Exception e) {
            player.sendMessage(Text.literal("§cMenü açılırken bir hata oluştu."), false);
            LOGGER.error("'{}' menüsü açılırken hata oluştu", menuName, e);
        }
    }
}
