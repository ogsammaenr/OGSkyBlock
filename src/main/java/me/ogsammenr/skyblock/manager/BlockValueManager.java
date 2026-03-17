package me.ogsammenr.skyblock.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class BlockValueManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("Skyblock-core | BlockValueManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Object2IntMap<Block> BLOCK_VALUES = new Object2IntOpenHashMap<>();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("skyblock_core");
    private static final File CONFIG_FILE = CONFIG_DIR.resolve("block_values.json").toFile();

    /**
     * Read the JSON file and creates memory table
     *
     */
    public static void loadValues() {
        BLOCK_VALUES.clear();
        BLOCK_VALUES.defaultReturnValue(0);

        if(!CONFIG_FILE.exists()) {
            createDefaultConfig();
        }

        try ( FileReader reader = new FileReader(CONFIG_FILE) ) {
            Type type = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> rawdata = GSON.fromJson(reader, type);

            if(rawdata == null) {
                LOGGER.warn("Failed to load block values! defaulting to empty values.");
                return;
            }
            int loaderCount = 0;
            for (Map.Entry<String, Integer> entry : rawdata.entrySet()){

                Identifier idn = Identifier.tryParse(entry.getKey());
                if (idn != null && BuiltInRegistries.BLOCK.containsKey(idn)) {
                    Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(idn);
                    Block block = Blocks.AIR;
                    if(blockOpt.isPresent()) {
                        block = blockOpt.get();
                    } else {
                        LOGGER.warn("Unknown block ID : {}" , entry.getKey());
                    }

                    if(block != Blocks.AIR) {
                        BLOCK_VALUES.put(block, entry.getValue().intValue());
                        loaderCount++;
                    }

                } else{
                    LOGGER.warn("Failed to load block values! Unknown block ID: " + entry.getKey());
                }
            }
            LOGGER.info("Loaded " + loaderCount + " block values.");
        }catch (IOException e ) {
            LOGGER.error("failed to load block_values.json! defaulting to empty values.", e);
        }catch (Exception e) {
            LOGGER.error("JSON Parsing error in block_values.json! Wrong format. Defaulting to empty values.", e);
        }
    }

    /**
     *
     * @param block
     * @return block value
     */
    public static int getValue(Block block) {
        return BLOCK_VALUES.getInt(block);
    }

    private static void createDefaultConfig() {
        try {
            if (!CONFIG_DIR.toFile().exists()) {
                CONFIG_DIR.toFile().mkdir();
            }
            Map<String, Integer> defaultValues = Map.of(
                    "minecraft:cobblestone", 1,
                    "minecraft:diamond_block", 150,
                    "minecraft:beacon", 5000
            );

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(defaultValues, writer);
            }
            LOGGER.info("Default block_values.json file generated : {}" , CONFIG_FILE.getAbsolutePath());
        } catch (IOException e) {
        LOGGER.error("default block_values.json file not generated", e);
        }
    }


}
