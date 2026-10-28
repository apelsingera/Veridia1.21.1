package net.gera.veridia.common.village.calc;

import net.gera.veridia.common.compat.dynamictrees.DynamicTreesCompat;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

public final class VillageBlocks {

    private VillageBlocks() {
    }
    public static boolean vegetationOrAir(BlockState state) {
        return !state.isAir()
                && !state.canBeReplaced()
                && !state.is(BlockTags.REPLACEABLE_BY_TREES)
                && !state.is(BlockTags.FLOWERS)
                && !state.is(ModTags.Blocks.BUSHES)
                && !state.is(BlockTags.SAPLINGS)
                && !isGroundPlant(state);
    }

    /** Мелкие растения, которых нет ни в одном ванильном теге "заменяемых" (они не replaceable): без этого
     * гриб или ягодный куст на клетке стены считались постройкой, и стена на этом месте просто не
     * появлялась (см. NailerWallQueue.isClear). */
    private static boolean isGroundPlant(BlockState state) {
        return state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.MOSS_CARPET);
    }

    public static boolean isTreePart(BlockState state) {
        return state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES)
                || state.is(Blocks.VINE)
                || state.is(Blocks.MANGROVE_ROOTS)
                || state.is(Blocks.BAMBOO)
                || DynamicTreesCompat.isDynamicTreesBlock(state);
    }

    /** Крона: листва и лианы - то, что висит на дереве, но не его ствол. */
    public static boolean isCanopy(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(Blocks.VINE)
                || DynamicTreesCompat.isDynamicLeaves(state);
    }
    /** Природный грунт: земля, песок, камень, руда, снег, гравий и т.п. - то, что лежит в земле само, а не поставлено
     * строителем. Руда и кальцит здесь обязательны: на горном склоне жила руды иначе считалась постройкой, и стена
     * обрывалась на ней (клетка с рудой исключалась из участков стены). */
    public static boolean isNaturalGround(BlockState state) {
        return isIceSheet(state)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.SNOW)
                || state.is(Tags.Blocks.ORES)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.DRIPSTONE_BLOCK);
    }

    /**
     * Обычный лёд замёрзших озёр и рек: по нему ходят, как по земле. Плотный и синий лёд сюда не входят: это материалы
     * снежной стены (NailerWallQueue.KNOWN_BLOCKS), и построенная из них стена считалась бы природной землёй. Какой лёд
     * вблизи деревни земля, а какой далеко и остаётся "постройкой" - решает скан (VillageScan.settleIce).
     */
    public static boolean isIceSheet(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE);
    }

    /** Имя исторически наоборот: true для НЕ природного пола, то есть для того, что поставлено строителем
     * (см. isNaturalGround). */
    public static boolean isNaturalFloor(BlockState state) {
        return !isNaturalGround(state);
    }
    public static boolean isStructure(BlockState state) {
        return vegetationOrAir(state) && !isTreePart(state) && isNaturalFloor(state);
    }
    public static boolean isClearable(BlockState state) {
        return isStructure(state);
    }

    /** isStructure с учётом места: брёвна построек деревни (крыша и каркас таёжных домов) - тоже постройка,
     * хотя по самому блоку это "дерево" (см. VillageBuildings). */
    public static boolean isStructureAt(ServerLevel level, BlockPos pos, BlockState state) {
        return isStructure(state) || VillageBuildings.isLogOfBuilding(level, pos, state);
    }

    /** isTreePart с учётом места: бревно здания деревни деревом не считается. */
    public static boolean isTreePartAt(ServerLevel level, BlockPos pos, BlockState state) {
        return isTreePart(state) && !VillageBuildings.isLogOfBuilding(level, pos, state);
    }
    public static boolean isLampLight(BlockState state) {
        return state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN);
    }
    public static boolean isLampPostBlock(BlockState state) {
        return isStructure(state) || state.is(BlockTags.LOGS);
    }
}