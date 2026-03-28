package me.ogsammenr.skyblock.listener;

import me.ogsammenr.skyblock.SkyblockMain;
import me.ogsammenr.skyblock.manager.IslandProtection;
import me.ogsammenr.skyblock.model.Island;
import me.ogsammenr.skyblock.model.IslandAction;
import me.ogsammenr.skyblock.model.IslandSetting;
import me.ogsammenr.skyblock.world.IslandRegistry;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

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
            ItemStack handItem = player.getItemInHand(hand);

            IslandAction action = switch (block) {
                case DoorBlock d -> IslandAction.USE_DOORS;
                case FenceGateBlock f -> IslandAction.USE_GATES;
                case TrapDoorBlock t -> IslandAction.USE_TRAPDOORS;
                case ButtonBlock b -> IslandAction.USE_BUTTONS;
                case LeverBlock l -> IslandAction.USE_LEVERS;
                case BedBlock b -> IslandAction.USE_BEDS;
                case AnvilBlock a -> IslandAction.USE_ANVILS;
                case BeaconBlock b -> IslandAction.USE_BEACONS;
                case BrewingStandBlock b -> IslandAction.USE_BREWING_STANDS;
                case EnchantingTableBlock e -> IslandAction.USE_ENCHANTING_TABLE;
                case CraftingTableBlock c -> IslandAction.USE_WORKBENCHES;
                case JukeboxBlock j -> IslandAction.USE_JUKEBOX;
                case NoteBlock n -> IslandAction.USE_NOTE_BLOCK;
                case DropperBlock d -> IslandAction.USE_DROPPERS;
                case DispenserBlock d -> IslandAction.USE_DISPENSERS;
                case HopperBlock h -> IslandAction.USE_HOPPERS;
                case ChestBlock c -> IslandAction.USE_CONTAINERS;
                case BarrelBlock b -> IslandAction.USE_CONTAINERS;
                case ShulkerBoxBlock s -> IslandAction.USE_CONTAINERS;
                case DiodeBlock d -> IslandAction.USE_REDSTONE_ITEMS;
                case RedStoneWireBlock r -> IslandAction.USE_REDSTONE_ITEMS;
                case TurtleEggBlock t -> IslandAction.TURTLE_EGGS;
                default -> IslandAction.PLACE_BLOCK;
            };

            if (handItem.getItem() instanceof BucketItem) {
                if (handItem.getItem() == Items.BUCKET) {
                    FluidState fluidState = world.getFluidState(pos);
                    if (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER)) action = IslandAction.COLLECT_WATER;
                    else if (fluidState.is(Fluids.LAVA) || fluidState.is(Fluids.FLOWING_LAVA)) action = IslandAction.COLLECT_LAVA;
                } else {
                    action = IslandAction.USE_BUCKETS;
                }
            }

            if (!IslandProtection.canPerformAction(serverPlayer, pos, action)) {
                serverPlayer.sendSystemMessage(Component.literal("§cBu adada bu eşyayla etkileşime giremezsin!"));
                return InteractionResult.FAIL;
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

            if (entity instanceof Player) {
                if (!island.getSetting(IslandSetting.OVERWORLD_PVP)) {
                    serverPlayer.sendSystemMessage(Component.literal("§cBu adada PvP kapalı!"));
                    return InteractionResult.FAIL;
                }
            } else {
                IslandAction action = switch (entity) {
                    case Monster m -> IslandAction.HURT_MONSTERS;
                    case Animal a -> IslandAction.HURT_ANIMALS;
                    case Villager v -> IslandAction.HURT_VILLAGERS;
                    case ArmorStand a -> IslandAction.USE_ARMOR_STANDS;
                    case ItemFrame i -> IslandAction.USE_ITEM_FRAMES;
                    default -> null;
                };

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

            ItemStack handItem = player.getItemInHand(hand);

            IslandAction action = switch (entity) {
                case Villager v -> IslandAction.TRADE_WITH_VILLAGER;
                case ArmorStand a -> IslandAction.USE_ARMOR_STANDS;
                case ItemFrame i -> IslandAction.USE_ITEM_FRAMES;
                case AbstractHorse h -> player.isShiftKeyDown() ? IslandAction.MOUNT_INVENTORY : IslandAction.RIDE_ANIMALS;
                case Animal a -> {
                    Item item = handItem.getItem();
                    if (item == Items.NAME_TAG) yield IslandAction.USE_NAME_TAGS;
                    if (item == Items.LEAD) yield IslandAction.USE_LEASH;
                    if (a instanceof Sheep && item == Items.SHEARS) yield IslandAction.SHEAR_ANIMALS;
                    if (a instanceof Cow && item == Items.BUCKET) yield IslandAction.MILK_ANIMALS;
                    if (a.isFood(handItem)) yield IslandAction.BREED_ANIMALS;
                    yield null;
                }
                default -> null;
            };

            if (action != null && !island.canPerformAction(serverPlayer.getUUID(), action)) {
                serverPlayer.sendSystemMessage(Component.literal("§cBu varlıkla etkileşime giremezsin!"));
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private static void registerItemEvents() {
        // --- 5. ELDEKİ EŞYAYI HAVAYA/BOŞLUĞA KULLANMA ---
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            Item item = stack.getItem();

            IslandAction action = switch (item) {
                case SpawnEggItem s -> IslandAction.USE_SPAWN_EGGS;
                case EnderpearlItem e -> IslandAction.USE_ENDERPEARLS;
                case ThrowablePotionItem t -> IslandAction.THROW_POTIONS;
                case Item i when i == Items.EGG -> IslandAction.THROW_EGGS;
                case Item i when i == Items.CHORUS_FRUIT -> IslandAction.EAT_CHORUS_FRUIT;
                default -> null;
            };

            if (action != null) {
                BlockPos pos = serverPlayer.blockPosition();
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