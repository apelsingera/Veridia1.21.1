//package private
package net.gera.veridia.common.village.calc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

interface Recorder {
    void record(BlockPos pos, BlockState state);
}