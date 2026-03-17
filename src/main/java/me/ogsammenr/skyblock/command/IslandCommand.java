package me.ogsammenr.skyblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.ogsammenr.skyblock.level.AsyncLevelCalculator;
import me.ogsammenr.skyblock.level.snapshot.IslandSnapshot;
import me.ogsammenr.skyblock.level.snapshot.SnapshotEngine;
import me.ogsammenr.skyblock.manager.IslandManager;
import me.ogsammenr.skyblock.manager.MenuManager;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class IslandCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("is")
                .executes(IslandCommand::handleBaseCommand)

                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                        .executes(IslandCommand::handleTestGenerators)
                )

                .then(Commands.literal("menu")
                        .executes(IslandCommand::handleOpenMenu)
                )
                .then(Commands.literal("values")
                        .executes(IslandCommand::handleValuesCommand)
                )
                .then(Commands.literal("level")
                        .executes(IslandCommand::handleLevelCommand)
                )

        );
    }

    private static int handleBaseCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if(!source.isPlayer()){
            source.sendSystemMessage(Component.literal("§c bu komut sadece oyuncular tarafindan kullanilabilir"));
            return 0;
        }
        ServerPlayer player = source.getPlayer();

        player.sendSystemMessage(Component.literal("§eAdana ışınlanıyorsun..."));

        IslandManager.spawnPlayerToIsland(player);

        return 1;
    }

    private static int handleTestGenerators(CommandContext<CommandSourceStack> context) {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        CommandSourceStack source= context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.literal("§c bu komut sadece oyuncular tarafindan kullanilabilir"));
            return 0;
        }
        ServerPlayer player = source.getPlayer();

        for (int i = 0; i < amount; i++) {
            // Test için rastgele UUID'ler oluşturuyoruz
            UUID testUuid = UUID.randomUUID();
            Island island = IslandRegistry.getOrCreateIsland(testUuid);
            IslandManager.generateSmartIsland(player.level(), island, "classic_island");
            player.sendSystemMessage(Component.literal("§aTest adası " + (i + 1) + " oluşturuldu: " + island.getCenter().toShortString()));
        }

        player.sendSystemMessage(Component.literal("§aBaşarıyla " + amount + " adet test adası oluşturuldu!"));
        return 1;
    }

    private static int handleOpenMenu(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.literal("§c bu komut sadece oyuncular tarafindan kullanilabilir"));
            return 0;
        }
        ServerPlayer player = source.getPlayer();

        MenuManager.openMenu(player, "island_menu");
        player.sendSystemMessage(Component.literal("§eAda menüsü açıldı!"));
        return 1;
    }

    private static int handleLevelCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // Komutu konsol kullanmasın diye güvenlik önlemi
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.literal("§cBu komut sadece oyuncular tarafindan kullanilabilir."));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        ServerLevel level = source.getLevel();
        UUID playerId = player.getUUID();

        try {
            // 1. Adayı Çek ve NULL KONTROLÜ YAP (KRİTİK ZIRH)
            Island is = IslandRegistry.getIslandByPlayer(playerId);

            if (is == null) {
                player.sendSystemMessage(Component.literal("§cHata: Kendinize ait veya üyesi olduğunuz bir ada bulunamadı!"));
                return 0; // Kodu burada durdur, aşağı inmesine izin verme.
            }

            // Ek Güvenlik: Merkez koordinatının null olma ihtimaline karşı
            if (is.getCenter() == null) {
                player.sendSystemMessage(Component.literal("§cAdanızın merkez koordinatı bozuk! Lütfen kurucuya bildirin."));
                return 0;
            }

            // 2. Sınırları Güvenle Hesapla
            int minX = is.getCenter().getX() - is.getProtectionRadius();
            int minZ = is.getCenter().getZ() - is.getProtectionRadius();
            int maxX = is.getCenter().getX() + is.getProtectionRadius();
            int maxZ = is.getCenter().getZ() + is.getProtectionRadius();

            // 3. İşlemi Başlat
            player.sendSystemMessage(Component.literal("§eAda seviyen hesaplaniyor... Lutfen bekle."));

            // MAIN THREAD: Snapshot
            IslandSnapshot snapshot = SnapshotEngine.takeSnapshot(level, minX, minZ, maxX, maxZ);

            // ASYNC THREAD: Hesaplama
            AsyncLevelCalculator.calculateIslandLevel(level.getServer(), is.getIslandUUID(), snapshot, player);

            return 1;

        } catch (Exception e) {
            // Eğer sistem yine de çökerse, hatayı yutma ve oyuncuya bildir!
            player.sendSystemMessage(Component.literal("§cKomut çalışırken sistemsel bir hata oluştu: §f" + e.getMessage()));
            System.err.println("[Skyblock] /is level komutunda kritik hata:");
            e.printStackTrace();
            return 0;
        }
    }

    private static int handleValuesCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.isPlayer()) {
            source.sendSystemMessage(Component.literal("§cBu komut sadece oyuncular tarafından kullanilabilir."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();

        MenuManager.openMenu(player, "values_menu");
        player.sendSystemMessage(Component.literal("§eBlock değerleri menüsü açıldı!"));
        return 1;
    }
}
