package me.ogsammenr.skyblock.manager;

import com.google.gson.Gson;
import me.ogsammenr.skyblock.model.MenuData;
import me.ogsammenr.skyblock.ui.BaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static me.ogsammenr.skyblock.SkyblockMain.LOGGER;

public class MenuManager {
    private static final Gson GSON = new Gson();

    // Verileri ve oluşturucuları (Constructor) RAM'de tutacağımız Cache yapıları
    private static final Map<String, MenuData> MENU_CACHE = new HashMap<>();
    private static final Map<String, Constructor<? extends BaseMenu>> CONSTRUCTOR_CACHE = new HashMap<>();

    private static final String MENU_HANDLER_PACKAGE = "me.ogsammenr.skyblock.ui.menus.";

    public static void loadAllMenus(File menuFolder) {
        MENU_CACHE.clear();
        CONSTRUCTOR_CACHE.clear();

        if (!menuFolder.exists() && !menuFolder.mkdirs()) {
            LOGGER.error("Menü klasörü oluşturulamadı: {}", menuFolder.getAbsolutePath());
            return;
        }

        File[] menuFiles = menuFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (menuFiles == null) return;

        for (File file : menuFiles) {
            String menuName = file.getName().replace(".json", "");
            try (FileReader reader = new FileReader(file)) {
                MenuData data = GSON.fromJson(reader, MenuData.class);

                // 1. Handler ismini al, yoksa varsayılan (DefaultMenu) kullan
                String handlerClassName = (data.handler != null && !data.handler.isEmpty())
                        ? data.handler
                        : "DefaultMenu";

                // 2. Sınıfı bul ve Constructor'ı (Oluşturucu) önbelleğe al
                String fullClassName = MENU_HANDLER_PACKAGE + handlerClassName;
                Class<?> clazz = Class.forName(fullClassName);

                if (!BaseMenu.class.isAssignableFrom(clazz)) {
                    throw new ClassCastException(fullClassName + " sınıfı BaseMenu'den türetilmemiş!");
                }

                // Tür güvenliğini (Type-Safety) sağlayarak constructor'ı alıyoruz
                Constructor<? extends BaseMenu> constructor = clazz.asSubclass(BaseMenu.class)
                        .getConstructor(ServerPlayer.class, MenuData.class);

                // 3. Başarılıysa Cache'e ekle
                MENU_CACHE.put(menuName, data);
                CONSTRUCTOR_CACHE.put(menuName, constructor);

                LOGGER.info("Menü başarıyla yüklendi ve doğrulandı: {} (Handler: {})", menuName, handlerClassName);

            } catch (ClassNotFoundException e) {
                LOGGER.error("Menü yüklenemedi ({}). Handler sınıfı bulunamadı: {}", file.getName(), e.getMessage());
            } catch (NoSuchMethodException e) {
                LOGGER.error("Menü yüklenemedi ({}). Handler sınıfında (ServerPlayer, MenuData) parametreli Constructor yok!", file.getName());
            } catch (Exception e) {
                LOGGER.error("Menü okunurken bilinmeyen hata oluştu: {}", file.getName(), e);
            }
        }
        LOGGER.info("===== Toplam {} menü sorunsuz yüklendi =====", MENU_CACHE.size());
    }

    public static void openMenu(ServerPlayer player, String menuName) {
        MenuData menuData = MENU_CACHE.get(menuName);
        Constructor<? extends BaseMenu> constructor = CONSTRUCTOR_CACHE.get(menuName);

        if (menuData == null || constructor == null) {
            player.sendSystemMessage(Component.literal("§cHata: '" + menuName + "' isimli menü bulunamadı veya bozuk!"));
            return;
        }

        try {
            // Sadece önbellekten Constructor'ı çağırıp Instance (Nesne) üretiyoruz (Çok Hızlı)
            BaseMenu menuInstance = constructor.newInstance(player, menuData);
            menuInstance.open();
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cMenü açılırken sistemsel bir hata oluştu. Lütfen yetkiliye bildirin."));
            LOGGER.error("'{}' menüsü örneği (instance) oluşturulurken hata yaşandı", menuName, e);
        }
    }
}
