package net.gera.veridia.server.xray;

import net.gera.veridia.common.block.ModBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.slf4j.Logger;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public final class AntiXray {

    private static final Logger LOGGER = LogUtils.getLogger();

    private AntiXray() {
    }

    static final Set<Block> HIDDEN_BLOCKS = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS,
            ModBlocks.VERDANT_FLUORITE_ORE.get(),
            ModBlocks.PURPURITE_ORE.get(),
            ModBlocks.NETHER_BARIUM_ORE.get()
    );

    public static volatile boolean ENABLED = true;
    private static final Set<UUID> BYPASS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static void setBypass(UUID playerId, boolean bypass) {
        if (bypass) {
            BYPASS.add(playerId);
        } else {
            BYPASS.remove(playerId);
        }
    }
    public static boolean hasBypass(UUID playerId) {
        return BYPASS.contains(playerId);
    }
    public static LevelChunkSection[] obfuscateSections(ServerLevel level, LevelChunk chunk, ServerPlayer player) {
        if (!ENABLED || BYPASS.contains(player.getUUID())) {
            return null;
        }
        LevelChunkSection[] originalSections = chunk.getSections();
        LevelChunkSection[] result = null;
        int hiddenCount = 0;
        for (int sectionIndex = 0; sectionIndex < originalSections.length; sectionIndex++) {
            LevelChunkSection section = originalSections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int sectionMinY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            LevelChunkSection obfuscated = null;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!HIDDEN_BLOCKS.contains(state.getBlock())) {
                            continue;
                        }
                        BlockPos worldPos = new BlockPos(
                                (chunk.getPos().x << 4) + x,
                                sectionMinY + y,
                                (chunk.getPos().z << 4) + z);
                        if (!isFullyObscured(level, worldPos)) {
                            continue;
                        }
                        if (obfuscated == null) {
                            obfuscated = cloneSection(section);
                        }
                        obfuscated.setBlockState(x, y, z, fillerFor(level, worldPos));
                        hiddenCount++;
                    }
                }
            }
            if (obfuscated != null) {
                if (result == null) {
                    result = originalSections.clone();
                }
                result[sectionIndex] = obfuscated;
            }
        }
        return result;
    }
    private static LevelChunkSection cloneSection(LevelChunkSection section) {
        PalettedContainer<BlockState> source = section.getStates();
        PalettedContainer<BlockState> states = source.recreate();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    states.set(x, y, z, source.get(x, y, z));
                }
            }
        }
        PalettedContainerRO<Holder<Biome>> biomes = section.getBiomes();
        return new LevelChunkSection(states, biomes);
    }
    private static boolean isFullyObscured(ServerLevel level, BlockPos pos) {
        return isOpaque(level, pos.above())
                && isOpaque(level, pos.below())
                && isOpaque(level, pos.north())
                && isOpaque(level, pos.south())
                && isOpaque(level, pos.east())
                && isOpaque(level, pos.west());
    }
    private static boolean isOpaque(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty()
                && state.getLightBlock(level, pos) >= 15;
    }
    private static BlockState fillerFor(ServerLevel level, BlockPos pos) {
        if (level.dimension().equals(net.minecraft.world.level.Level.NETHER)) {
            return Blocks.NETHERRACK.defaultBlockState();
        }
        if (level.dimension().equals(net.minecraft.world.level.Level.END)) {
            return Blocks.END_STONE.defaultBlockState();
        }
        return pos.getY() < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }
}