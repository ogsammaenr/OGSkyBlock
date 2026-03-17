package me.ogsammenr.skyblock;

import me.ogsammenr.skyblock.data.IslandStorage;
import me.ogsammenr.skyblock.command.IslandCommand;
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

public class SkyblockMain implements ModInitializer {
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

			File menuFolder = server.getServerDirectory().resolve("config/skyblock_core/menus").toFile();
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

		// Blok Kırma Koruması (Sol Tık)
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			LOGGER.info("oyuncu blok kırmaya çalışıyor: " + player.getName().getString() + " - Konum: " + pos);
			if (player instanceof ServerPlayer serverPlayer) {
				LOGGER.info("ServerPlayer instance'ı doğrulandı: " + serverPlayer.getName().getString());
				if (!IslandProtection.canPerformAction(serverPlayer, pos, IslandAction.BREAK_BLOCK)) {
					serverPlayer.sendSystemMessage(Component.literal("§cBu adada blok kırma yetkiniz yok!"));
					return false;
				}
			}
			return true;
		});

		// Blok Koyma ve Etkileşim Koruması (Sağ Tık - Sandık açma, kapı açma vb.)
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				BlockPos pos = hitResult.getBlockPos();
				Block block = world.getBlockState(pos).getBlock();

				IslandAction actionToDetermine;

				// Tıklanan blok bir sandık, fırın vb. ise:
				if (block instanceof ChestBlock /* İleride fırın vs eklenebilir */) {
					actionToDetermine = IslandAction.OPEN_CONTAINER;
				}
				// Tıklanan blok bir kapı, şalter vb. ise:
				else if (block instanceof DoorBlock) {
					actionToDetermine = IslandAction.INTERACT_DOOR;
				}
				// Hiçbiri değilse (Büyük ihtimalle elindeki bloku koymaya çalışıyor)
				else {
					actionToDetermine = IslandAction.PLACE_BLOCK;
				}

				if (!IslandProtection.canPerformAction(serverPlayer, pos, actionToDetermine)) {
					serverPlayer.sendSystemMessage(Component.literal("§cBu adada bu işlemi yapma yetkiniz yok!"));
					return InteractionResult.FAIL;
				}
			}
			return InteractionResult.PASS;
		});

		// Komutları kaydet
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			IslandCommand.register(dispatcher);
		});

		LOGGER.info("SkyblockCore Has Been Initialized!");
	}
}