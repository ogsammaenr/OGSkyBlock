package me.ogsammenr.skyblock;

import me.ogsammenr.skyblock.data.IslandStorage;
import me.ogsammenr.skyblock.command.IslandCommand;
import me.ogsammenr.skyblock.listener.ProtectionListener;
import me.ogsammenr.skyblock.manager.BlockValueManager;
import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.manager.MenuManager;
import me.ogsammenr.skyblock.model.IslandAction;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SkyblockMain implements ModInitializer{
	public static final String MOD_ID = "ogskyblock";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceKey<Level> SKYBLOCK_WORLD_KEY = ResourceKey.create(
			Registries.DIMENSION,
			Identifier.parse(MOD_ID + ":skyblock_world")
	);

	public static String ADMIN_PERMISSION = "skyblock.admin";
	public static String BREAK_BYPASS_PERMISSION = "skyblock.bypass.break";
	public static String INTERACT_BYPASS_PERMISSION = "skyblock.bypass.interact";

	@Override
	public void onInitialize() {


		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			player.sendSystemMessage(Component.literal("§eSkyblock sunucusuna hoş geldin!\n§7/§eis §7yazarak adana ışınlanabilirsin!"));
		});

		// Sunucu başlarken verileri yükle
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			IslandStorage.load(server);
			BlockValueManager.loadValues();

			extractDefaultMenu("test_pagination_menu.json");
			extractDefaultMenu("island_values_menu.json");
			extractDefaultMenu("island_menu.json");
			extractDefaultMenu("island_settings_menu.json");
			extractDefaultMenu("island_settings_general_menu.json");
			extractDefaultMenu("island_settings_action_menu.json");

			File menuFolder = server.getServerDirectory().resolve("config/OGSkyBlock/menus").toFile();
			MenuManager.loadAllMenus(menuFolder);
		});

		// Sunucu kapanırken verileri kaydet
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			IslandStorage.save(server);
		});

		// Sunucu periyodik save alırken (Örn: oto-kayıt veya /save-all komutu) verileri kaydet
		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
			IslandStorage.save(server);
		});

		// Ada koruması
		ProtectionListener.register();

		// Komutları kaydet
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			IslandCommand.register(dispatcher);
		});

		LOGGER.info("SkyblockCore Has Been Initialized!");
	}



	/**
	 * JAR içerisindeki varsayılan menü dosyasını config klasörüne çıkartır.
	 * @param fileName Çıkartılacak dosyanın adı (Örn: "island_menu.json")
	 */
	private static void extractDefaultMenu(String fileName) {
		// Hedef klasör: sunucu_dizini/config/OGSkyBlock/menus
		Path configDir = FabricLoader.getInstance().getConfigDir().resolve("OGSkyBlock/menus");
		Path targetFile = configDir.resolve(fileName);

		// Eğer dosya zaten mevcutsa (sunucu sahibi düzenlemişse) işlemi iptal et ve üzerine yazma
		if (Files.exists(targetFile)) {
			return;
		}

		try {
			// Hedef klasör zinciri yoksa oluştur (mkdirs mantığı)
			Files.createDirectories(configDir);

			// JAR içindeki resources/default_menus/ yolundan dosyayı okumak için Stream aç
			String resourcePath = "/default_menus/" + fileName;
			try (InputStream in = SkyblockMain.class.getResourceAsStream(resourcePath)) {
				if (in == null) {
					LOGGER.warn("[Skyblock] Uyarı: Varsayılan menü dosyası JAR içinde bulunamadı -> {}", resourcePath);
					return;
				}

				// Stream'den gelen veriyi hedefe kopyala
				Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
				LOGGER.info("[Skyblock] Varsayılan menü oluşturuldu: {}", fileName);
			}
		} catch (Exception e) {
			LOGGER.error("[Skyblock] Varsayılan menü dosyası ({}) çıkartılırken hata oluştu!", fileName, e);
		}
	}


}