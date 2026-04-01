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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static me.ogsammenr.skyblock.util.MessageUtil.sendDenyMessage;

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

            IslandAction action = switch (state.getBlock()) {
                case BaseFireBlock f -> IslandAction.FIRE_EXTINGUISH;
                default -> IslandAction.BREAK_BLOCK;
            };

            if (!IslandProtection.canPerformAction(serverPlayer, pos, action)) {
                sendDenyMessage(serverPlayer, action);
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

            IslandAction action = getActionForBlock(block);

            if (action == null) {
                action = getActionForHandItem(handItem.getItem(), world, pos);
                // Eğer blok veya kova koyulacaksa koordinatı tıklanan yüzeye göre kaydır
                if (action == IslandAction.PLACE_BLOCK || action == IslandAction.USE_BUCKETS ||
                        action == IslandAction.IGNITE_FIRE || action == IslandAction.PLACE_VEHICLE) {
                    pos = pos.relative(hitResult.getDirection());
                }
            }

            if (action != null && !IslandProtection.canPerformAction(serverPlayer, pos, action)) {
                sendDenyMessage(serverPlayer, action);
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

            if (entity instanceof Player) {
                Island island = IslandRegistry.getIslandAt(pos);
                if (island != null && !island.getSetting(IslandSetting.OVERWORLD_PVP)) {
                    serverPlayer.sendSystemMessage(Component.literal("§cBu adada PvP kapalı!"));
                    return InteractionResult.FAIL;
                }
            } else {
                IslandAction action = getActionForEntityAttack(entity);
                if (action != null && !IslandProtection.canPerformAction(serverPlayer, pos, action)) {
                    sendDenyMessage(serverPlayer, action);
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
            ItemStack handItem = player.getItemInHand(hand);

            IslandAction action = getActionForEntityInteract(entity, handItem, player.isShiftKeyDown());

            if (action != null && !IslandProtection.canPerformAction(serverPlayer, pos, action)) {
                sendDenyMessage(serverPlayer, action);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private static void registerItemEvents() {
        // --- 5. ELDEKİ EŞYAYI HAVAYA/SIVIYA KULLANMA ---
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            Item item = stack.getItem();

            // 1. DÜZELTME: Eşya bir Kova VEYA Bot ise, işlemi Raycast metoduna devret
            if (item instanceof BucketItem || item instanceof BoatItem) {
                return handleRaycastItemInteraction(serverPlayer, world, item);
            }

            // 2. Kova ve Bot harici diğer eşyalar (Yumurta, İksir vb.)
            IslandAction action = getActionForItemUsage(item);

            if (action != null) {
                BlockPos pos = serverPlayer.blockPosition();
                if (!IslandProtection.canPerformAction(serverPlayer, pos, action)) {
                    sendDenyMessage(serverPlayer, action);
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.PASS;
        });
    }

    // ===================================================================================
    // YARDIMCI (HELPER) METOTLAR - Eylem tespitini merkezi ve okunabilir hale getirir
    // ===================================================================================

    private static IslandAction getActionForBlock(Block block) {
        return switch (block) {
            case DoorBlock d -> IslandAction.USE_DOORS;
            case FenceGateBlock f -> IslandAction.USE_GATES;
            case TrapDoorBlock t -> IslandAction.USE_TRAPDOORS;
            case ButtonBlock b -> IslandAction.USE_BUTTONS;
            case LeverBlock l -> IslandAction.USE_LEVERS;
            case BedBlock b -> IslandAction.USE_BEDS;
            case AnvilBlock a -> IslandAction.USE_ANVILS;
            case BeaconBlock b -> IslandAction.USE_BEACONS;
            case BrewingStandBlock b -> IslandAction.USE_BREWING_STANDS;
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
            case DaylightDetectorBlock d -> IslandAction.USE_REDSTONE_ITEMS;
            case ChiseledBookShelfBlock cbs-> IslandAction.INTERACT_SHELFS;
            case ShelfBlock sb -> IslandAction.INTERACT_SHELFS;
            case ComposterBlock c -> IslandAction.USE_COMPOSTERS;

            default -> null;
        };
    }

    private static IslandAction getActionForHandItem(Item item, Level world, BlockPos pos) {
        return switch (item) {
            case SpawnEggItem s -> IslandAction.USE_SPAWN_EGGS;

            case SignItem s -> IslandAction.PLACE_BLOCK;
            case BlockItem b -> IslandAction.PLACE_BLOCK;
            case HangingEntityItem h -> IslandAction.PLACE_BLOCK;
            case HoeItem h -> IslandAction.PLACE_BLOCK;
            case ShovelItem s -> IslandAction.PLACE_BLOCK;
            case AxeItem a -> IslandAction.PLACE_BLOCK;

            case Item i when i == Items.FLINT_AND_STEEL || i == Items.FIRE_CHARGE -> IslandAction.IGNITE_FIRE;

            default -> null;
        };
    }

    private static IslandAction getActionForEntityAttack(Entity entity) {
        return switch (entity) {
            case Monster m -> IslandAction.HURT_MONSTERS;
            case Animal a -> IslandAction.HURT_ANIMALS;
            case Villager v -> IslandAction.HURT_VILLAGERS;
            case ArmorStand a -> IslandAction.USE_ARMOR_STANDS;
            case ItemFrame i -> IslandAction.USE_ITEM_FRAMES;
            case AbstractBoat b -> IslandAction.BREAK_VEHICLE;
            case AbstractMinecart m -> IslandAction.BREAK_VEHICLE;
            default -> null;
        };
    }

    private static IslandAction getActionForEntityInteract(Entity entity, ItemStack handItem, boolean isShiftKeyDown) {
        return switch (entity) {
            case Villager v -> IslandAction.TRADE_WITH_VILLAGER;
            case ArmorStand a -> IslandAction.USE_ARMOR_STANDS;
            case ItemFrame i -> IslandAction.USE_ITEM_FRAMES;

            // Atlar, Eşekler ve Katırlar
            case AbstractHorse h -> isShiftKeyDown ? IslandAction.MOUNT_INVENTORY : IslandAction.RIDE_ANIMALS;

            case AbstractChestBoat cb -> {
                // Eğer oyuncu eğiliyorsa VEYA botun içinde biri varsa (doluysa)
                if (isShiftKeyDown || !cb.getPassengers().isEmpty()) {
                    yield IslandAction.MOUNT_INVENTORY; // Sandık açmaya çalışıyor
                }
                // Boşsa ve eğilmiyorsa
                yield IslandAction.RIDE_ANIMALS; // Binmeye çalışıyor
            }
            case AbstractBoat b -> IslandAction.RIDE_ANIMALS;

            case MinecartChest cm -> IslandAction.MOUNT_INVENTORY;
            case MinecartHopper hm -> IslandAction.MOUNT_INVENTORY;
            case Minecart m -> IslandAction.RIDE_ANIMALS;

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
    }

    private static IslandAction getActionForItemUsage(Item item) {
        return switch (item) {
            case SpawnEggItem s -> IslandAction.USE_SPAWN_EGGS;
            case EnderpearlItem e -> IslandAction.USE_ENDERPEARLS;
            case ThrowablePotionItem t -> IslandAction.THROW_POTIONS;
            case EggItem e -> IslandAction.THROW_EGGS;
            case FishingRodItem f -> IslandAction.USE_FISHING_ROD;
            case MinecartItem m -> IslandAction.PLACE_VEHICLE; // YENİ EKLENEN SATIR
            case Item i when i == Items.CHORUS_FRUIT -> IslandAction.EAT_CHORUS_FRUIT;
            default -> null;
        };
    }

    // ===================================================================================
    // KOVA VE BOT (ARAÇ) ÖZEL KORUMASI (RAYCAST İLE)
    // ===================================================================================
    private static InteractionResult handleRaycastItemInteraction(ServerPlayer player, Level world, Item item) {
        // Botlar tüm sıvıları (ANY), Boş kova sadece sıvı kaynaklarını (SOURCE_ONLY), Dolu kova sıvı aramaz (NONE)
        ClipContext.Fluid fluidMode;
        if (item instanceof BoatItem) {
            fluidMode = ClipContext.Fluid.ANY;
        } else if (item == Items.BUCKET) {
            fluidMode = ClipContext.Fluid.SOURCE_ONLY;
        } else {
            fluidMode = ClipContext.Fluid.NONE;
        }

        // Işın (Raycast) Hesaplaması
        double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(range));

        BlockHitResult hitResult = world.clip(
                new ClipContext(start, end, ClipContext.Block.OUTLINE, fluidMode, player)
        );

        // Eğer ışın bir yere çarptıysa (Suya veya Bloğa)
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos targetPos = hitResult.getBlockPos();
            IslandAction action;

            if (item instanceof BoatItem) {
                // Oyuncu bot yerleştiriyor
                action = IslandAction.PLACE_VEHICLE;
            }
            else if (item == Items.BUCKET) {
                // Nereden sıvı alıyor?
                FluidState fluidState = world.getFluidState(targetPos);
                if (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER)) action = IslandAction.COLLECT_WATER;
                else if (fluidState.is(Fluids.LAVA) || fluidState.is(Fluids.FLOWING_LAVA)) action = IslandAction.COLLECT_LAVA;
                else action = IslandAction.USE_BUCKETS;
            }
            else {
                // Nereye sıvı koyuyor?
                action = IslandAction.USE_BUCKETS;
                targetPos = targetPos.relative(hitResult.getDirection());
            }

            // Hedeflenen bloğun koordinatı (targetPos) üzerinden yetki kontrolü
            if (!IslandProtection.canPerformAction(player, targetPos, action)) {
                sendDenyMessage(player, action);
                // İptal edildikten sonra istemcinin (Client) kafasının karışmasını önle
                player.inventoryMenu.broadcastChanges();
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }

}