package net.gera.veridia.common.compat.graveyard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

/**
 * Место гроба: две его половины (гроб занимает два блока, как кровать) и направление. Нога (foot) - половина с
 * содержимым, голова (head) лежит от неё в сторону facing. Свойство "part" берём по имени, чтобы не зависеть от
 * классов Graveyard при сборке.
 */
public record CoffinSite(BlockPos foot, BlockPos head, Direction facing) {
    private static final String PART_PROPERTY = "part";
    private static final String HEAD_PART = "head";

    /** Место гроба по одной из его половин: pos - позиция этой половины, state - её состояние. */
    public static CoffinSite of(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return isHead(state)
                ? new CoffinSite(pos.relative(facing.getOpposite()), pos, facing)
                : new CoffinSite(pos, pos.relative(facing), facing);
    }

    private static boolean isHead(BlockState state) {
        Property<?> part = state.getBlock().getStateDefinition().getProperty(PART_PROPERTY);
        return part != null && state.getValue(part) instanceof StringRepresentable named
                && HEAD_PART.equals(named.getSerializedName());
    }

    /** Середина гроба по горизонтали (на высоте ноги). */
    public Vec3 center() {
        return new Vec3((foot.getX() + head.getX()) / 2.0 + 0.5, foot.getY(), (foot.getZ() + head.getZ()) / 2.0 + 0.5);
    }

    /** Вдоль какой оси вытянут гроб. */
    public Direction.Axis axis() {
        return facing.getAxis();
    }
}
