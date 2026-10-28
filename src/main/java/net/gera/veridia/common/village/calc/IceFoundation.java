package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Основание стены на льду. Обычный лёд тает от света рядом (факелы башен), и стена, стоящая на нём, оказалась бы над
 * водой. Поэтому лёд прямо под нижним блоком колонки стены (куска, подпорки, башни) меняется на плотный, который не
 * тает (VillageCalcSettings.ICE_FOUNDATION_BLOCK). Лёд считается землёй только вблизи деревни (VillageScan.isIceLand).
 * <p>
 * Куски стены закапываются на WALL_EMBED_DEPTH: их нижний ряд сам стоит на месте верхнего блока земли, то есть льда, и
 * ничего менять не нужно. Основание нужно там, где нижний блок стоит НАД льдом: у башни, у подпорок под куском на
 * неровном льду.
 */
public final class IceFoundation {

    private IceFoundation() {
    }

    public static BlockState state() {
        return VillageCalcSettings.ICE_FOUNDATION_BLOCK.defaultBlockState();
    }

    /** Позиция льда прямо под нижним блоком колонки (мировые X/Z, Y нижнего блока) или null, если менять нечего. */
    @Nullable
    public static BlockPos iceUnder(VillageScan scan, int worldX, int lowestY, int worldZ) {
        int cx = worldX - scan.minX();
        int cz = worldZ - scan.minZ();
        if (!scan.isIceLand(cx, cz) || scan.surfaceYAt(cx, cz) != lowestY - 1) {
            return null;
        }
        return new BlockPos(worldX, lowestY - 1, worldZ);
    }
}
