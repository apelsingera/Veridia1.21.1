package net.gera.veridia.common.village.calc;

import net.gera.veridia.common.compat.dynamictrees.DynamicTreesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * Брёвна построек деревни. Блок #minecraft:logs сам по себе не отличает дерево от дома: у таёжных домов
 * крыша и каркас из брёвен, у сараев и колодцев тоже. Для рубки деревьев и проверок "клетка свободна" они
 * должны быть постройкой, а не деревом - иначе их вырубало вместе с соседними соснами, а кусок стены
 * "помещался" прямо в крышу. Различить можно только по месту: бревно внутри куска-здания структуры
 * деревни из генерации мира - часть дома.
 * <p>
 * Зданием считается кусок деревни, собранный из шаблона (дом, колодец, амбар, фонарь): не улица (она
 * подстраивается под рельеф - TERRAIN_MATCHING), не дерево/куст из feature и не пустышка. Ветки Dynamic
 * Trees это не касается: они не в теге logs.
 */
public final class VillageBuildings {

    private VillageBuildings() {
    }

    /** Бревно (не Dynamic Trees), которое лежит внутри здания деревни. */
    public static boolean isLogOfBuilding(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.is(BlockTags.LOGS) || DynamicTreesCompat.isDynamicTreesBlock(state)) {
            return false;
        }
        StructureStart village = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        if (!village.isValid()) {
            return false;
        }
        for (StructurePiece piece : village.getPieces()) {
            if (piece.getBoundingBox().isInside(pos) && isBuilding(piece)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBuilding(StructurePiece piece) {
        if (!(piece instanceof PoolElementStructurePiece pool)) {
            return false;
        }
        StructurePoolElement element = pool.getElement();
        return !(element instanceof FeaturePoolElement)
                && !(element instanceof EmptyPoolElement)
                && element.getProjection() == StructureTemplatePool.Projection.RIGID;
    }
}
