package me.ogsammenr.skyblock.listener;

import me.ogsammenr.skyblock.SkyblockMain;
import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.model.IslandSetting;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.*;

public class ProtectionListener {

    public static void register() {
        registerBlockEvents();
        registerEntityEvents();
        registerItemEvents();
    }

    private static void registerBlockEvents() {
        // --- 1. BLOK KIRMA ---
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            if (!IslandProtection.canPerformAction(serverPlayer, pos, IslandAction.BREAK_BLOCK)) {
                serverPlayer.sendSystemMessage(Component.literal("§cBu adada blok kırma yetkiniz yok!"));
                return false;
            }
            return true;
        });

        // --- 2. BLOK ETKİLEŞİMİ (SAĞ TIK) ---
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            IslandAction action = IslandAction.PLACE_BLOCK; // Varsayılan: Blok koyma denemesi

            // Tıklanan bloğa göre aksiyonu belirle
            if (block instanceof DoorBlock) action = IslandAction.USE_DOORS;
            else if (block instanceof FenceGateBlock) action = IslandAction.USE_GATES;
            else if (block instanceof TrapDoorBlock) action = IslandAction.USE_TRAPDOORS;
            else if (block instanceof ButtonBlock) action = IslandAction.USE_BUTTONS;
            else if (block instanceof LeverBlock) action = IslandAction.USE_LEVERS;
            else if (block instanceof BedBlock) action = IslandAction.USE_BEDS;
            else if (block instanceof AnvilBlock) action = IslandAction.USE_ANVILS;
            else if (block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock) {
                action = IslandAction.USE_CONTAINERS;
            }

            if (!IslandProtection.canPerformAction(serverPlayer, pos, action)) {
                serverPlayer.sendSystemMessage(Component.literal("§cBu adada bu eşyayla etkileşime giremezsin!"));
                return InteractionResult.FAIL; // İptal et
            }
            return InteractionResult.PASS;
        });
    }

    private static void registerEntityEvents() {
        // --- 3. VARLIKLARA SALDIRMA (SOL TIK) ---
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!world.dimension().equals(SkyblockMain.SKYBLOCK_WORLD_KEY)) return InteractionResult.PASS;

            BlockPos pos = entity.blockPosition();
            Island island = IslandRegistry.getIslandAt(pos);
            if (island == null) return InteractionResult.PASS;

            // Oyuncular arası savaş (PvP) kontrolü (Ayarlardan)
            if (entity instanceof Player) {
                if (!island.getSetting(IslandSetting.OVERWORLD_PVP)) {
                    serverPlayer.sendSystemMessage(Component.literal("§cBu adada PvP kapalı!"));
                    return InteractionResult.FAIL;
                }
            }
            // Varlık (Mob/Hayvan) korumaları (Aksiyonlardan)
            else {
                IslandAction action = null;
                if (entity instanceof Monster) action = IslandAction.HURT_MONSTERS;
                else if (entity instanceof Animal) action = IslandAction.HURT_ANIMALS;
                else if (entity instanceof Villager) action = IslandAction.HURT_VILLAGERS;
                else if (entity instanceof ArmorStand) action = IslandAction.USE_ARMOR_STANDS;
                else if (entity instanceof ItemFrame) action = IslandAction.USE_ITEM_FRAMES;

                if (action != null && !island.canPerformAction(serverPlayer.getUUID(), action)) {
                    serverPlayer.sendSystemMessage(Component.literal("§cBu adada buna zarar veremezsin!"));
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        // --- 4. VARLIKLARLA ETKİLEŞİM (SAĞ TIK) ---
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!world.dimension().equals(SkyblockMain.SKYBLOCK_WORLD_KEY)) return InteractionResult.PASS;

            BlockPos pos = entity.blockPosition();
            Island island = IslandRegistry.getIslandAt(pos);
            if (island == null) return InteractionResult.PASS;

            IslandAction action = null;
            if (entity instanceof Villager) action = IslandAction.TRADE_WITH_VILLAGER;
            else if (entity instanceof ArmorStand) action = IslandAction.USE_ARMOR_STANDS;
            else if (entity instanceof ItemFrame) action = IslandAction.USE_ITEM_FRAMES;
            // Not: İleride elindeki makasa göre SHEAR_ANIMALS vb. eklenebilir.

            if (action != null && !island.canPerformAction(serverPlayer.getUUID(), action)) {
                serverPlayer.sendSystemMessage(Component.literal("§cBu varlıkla etkileşime giremezsin!"));
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private static void registerItemEvents() {
        // --- 5. ELDEKİ EŞYAYI KULLANMA (SAĞ TIK BUKET/YUMURTA) ---
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            IslandAction action = null;

            if (stack.getItem() instanceof BucketItem) action = IslandAction.USE_BUCKETS;
            else if (stack.getItem() instanceof SpawnEggItem) action = IslandAction.USE_SPAWN_EGGS;

            if (action != null) {
                // Not: UseItemCallback anında bir blok lokasyonuna sahip değildir (havaya tıklanmış olabilir).
                // Tam güvenlik için oyuncunun bulunduğu adaya göre kontrol edilir.
                BlockPos pos = player.blockPosition();
                Island island = IslandRegistry.getIslandAt(pos);

                if (island != null && !island.canPerformAction(serverPlayer.getUUID(), action)) {
                    serverPlayer.sendSystemMessage(Component.literal("§cBu adada bu eşyayı kullanamazsın!"));
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

    }
}