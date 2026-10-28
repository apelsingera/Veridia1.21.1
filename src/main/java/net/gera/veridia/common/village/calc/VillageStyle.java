package net.gera.veridia.common.village.calc;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * В каком стиле гвоздарь строит деревню: из каких шаблонов (стена, башня, дом) и из какого дерева заливка
 * (столбики-заглушки, подпорки, фундамент). Один стиль - одна папка у каждого вида шаблонов:
 * data/veridia/structure/{wall,towers,village}/&lt;folder&gt;/. Стили с одинаковым набором файлов -
 * новый стиль (пустыня, саванна...) это новая запись тут и новые папки.
 * <p>
 * Снежный стиль (SNOWY): шаблоны snowy/ сделаны из таёжных заменой грязи на снег и лёд (их можно подменять
 * своими файлами), а заливка и подпорки берут снег и лёд вместо грязи (MudSubstitutes).
 * <p>
 * Стиль определяет тип самой деревни из генерации мира (village_taiga -> TAIGA), а если структуры в
 * точке нет (проверка /calculate на пустом месте) - биом. Для стиля без шаблонов берутся равнинные.
 */
public enum VillageStyle {

    PLAINS("plains", "village_plains", ModBlocks.OAK_SCRAP_WOOD, Blocks.OAK_PLANKS, Blocks.STRIPPED_OAK_LOG,
            MudSubstitutes.MUD),
    TAIGA("taiga", "village_taiga", ModBlocks.SPRUCE_SCRAP_WOOD, Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_SPRUCE_LOG,
            MudSubstitutes.MUD),
    SNOWY("snowy", "village_snowy", ModBlocks.SPRUCE_SCRAP_WOOD, Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_SPRUCE_LOG,
            MudSubstitutes.SNOW_AND_ICE);

    /**
     * Чем стиль заменяет грязь в заливке стены (столбики-заглушки и подпорки, см. FillerDecoration и
     * NailerWallQueue.pickSupportBlock): плотную грязь, рыхлую грязь и кирпичи из грязи. В снежной деревне
     * вместо грязи снег и лёд, а кирпичей два вида (снежные и из плотного льда): они чередуются.
     */
    public record MudSubstitutes(Supplier<Block> packedMud, Supplier<Block> mud, List<Supplier<Block>> mudBricks) {
        static final MudSubstitutes MUD = new MudSubstitutes(() -> Blocks.PACKED_MUD, () -> Blocks.MUD,
                List.of(() -> Blocks.MUD_BRICKS));
        static final MudSubstitutes SNOW_AND_ICE = new MudSubstitutes(() -> Blocks.SNOW_BLOCK, () -> Blocks.PACKED_ICE,
                List.of(ModBlocks.SNOW_BRICKS::get, ModBlocks.PACKED_ICE_BRICKS::get));
    }

    private final String folder;
    private final String structureName;
    private final Supplier<Block> scrapWood;
    private final Block planks;
    private final Block strippedLog;
    private final MudSubstitutes mudSubstitutes;

    VillageStyle(String folder, String structureName, Supplier<Block> scrapWood, Block planks, Block strippedLog,
                 MudSubstitutes mudSubstitutes) {
        this.folder = folder;
        this.structureName = structureName;
        this.scrapWood = scrapWood;
        this.planks = planks;
        this.strippedLog = strippedLog;
        this.mudSubstitutes = mudSubstitutes;
    }

    /** Плотная грязь заливки (PACKED_MUD, в снежной деревне - снежный блок). */
    public Block packedMud() {
        return mudSubstitutes.packedMud().get();
    }

    /** Рыхлая грязь заливки (MUD, в снежной деревне - плотный лёд). */
    public Block mud() {
        return mudSubstitutes.mud().get();
    }

    /** Кирпичи заливки: один вид (MUD_BRICKS) или несколько, которые чередуются. */
    public List<Block> mudBricks() {
        return mudSubstitutes.mudBricks().stream().map(Supplier::get).toList();
    }

    /** Хлам-доски стиля - заливка между кусками стены, подпорки под ними и фундамент башни. */
    public Block scrapWood() {
        return scrapWood.get();
    }

    public Block planks() {
        return planks;
    }

    public Block strippedLog() {
        return strippedLog;
    }

    /** Кусок стены: name - имя файла из VillageCalcSettings (WALL_PIECE, ENTRANCE_PIECE_UPGRADED...). */
    public ResourceLocation wall(String name) {
        return id("wall/" + folder + "/" + name);
    }

    public ResourceLocation tower() {
        return id("towers/" + folder + "/" + VillageCalcSettings.TOWER_1);
    }

    /** Дом: name - из VillageCalcSettings.HOUSE_NAMES. */
    public ResourceLocation house(String name) {
        return id("village/" + folder + "/" + name);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, path);
    }

    /**
     * Стиль деревни около pos. Сначала структура-деревня ровно в этой точке; нет - ближайшая деревня рядом
     * (по её POI: игрок мог встать над деревней, на землю рядом или вообще в стороне - структура лежит
     * только в границах своих кусков, и в точке над ними её нет); совсем нет деревни - биом.
     */
    public static VillageStyle of(ServerLevel level, BlockPos pos) {
        VillageStyle style = byStructure(level, pos);
        if (style == null) {
            style = nearestVillageStyle(level, pos);
        }
        if (style == null) {
            // Биом на уровне моря, а не в точке: игрок может лететь высоко над землёй.
            BlockPos ground = new BlockPos(pos.getX(), level.getSeaLevel(), pos.getZ());
            style = styleOfBiome(level, ground);
        }
        return style.orPlainsIfMissing(level);
    }

    /** Заснеженная тайга строится как таёжная (в ней и генерируется таёжная деревня), остальные снежные биомы - снежные. */
    private static VillageStyle styleOfBiome(ServerLevel level, BlockPos ground) {
        Holder<Biome> biome = level.getBiome(ground);
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return TAIGA;
        }
        return biome.is(Tags.Biomes.IS_SNOWY) ? SNOWY : PLAINS;
    }

    /** Рядом с pos есть деревня из генерации мира (та же проверка, что определяет стиль). */
    public static boolean isVillageNear(ServerLevel level, BlockPos pos) {
        return byStructure(level, pos) != null || nearestVillageStyle(level, pos) != null;
    }

    @Nullable
    private static VillageStyle nearestVillageStyle(ServerLevel level, BlockPos pos) {
        return level.getPoiManager()
                .getInSquare(holder -> holder.is(PoiTypeTags.VILLAGE), pos,
                        VillageCalcSettings.VILLAGE_STYLE_SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .sorted(Comparator.comparingDouble(poi -> poi.distSqr(pos)))
                .map(poi -> byStructure(level, poi))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** По типу структуры-деревни из генерации мира; null, если в точке деревни нет. Деревня другого
     * типа (пустыня, саванна...), для которого стиля ещё нет, строится как равнинная. */
    private static VillageStyle byStructure(ServerLevel level, BlockPos pos) {
        StructureStart village = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        if (!village.isValid()) {
            return null;
        }
        ResourceLocation key = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(village.getStructure());
        for (VillageStyle style : values()) {
            if (key != null && key.getPath().equals(style.structureName)) {
                return style;
            }
        }
        return PLAINS;
    }

    /** Стиль без файла стены (папку удалили/не добавили) строить нечем - тогда равнинный. */
    private VillageStyle orPlainsIfMissing(ServerLevel level) {
        boolean present = level.getStructureManager().get(wall(VillageCalcSettings.WALL_PIECE)).isPresent();
        return present ? this : PLAINS;
    }
}