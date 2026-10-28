package net.gera.veridia.common.block.wood;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
public class ScrapWoodBlock extends Block {
    public static final MapCodec<ScrapWoodBlock> CODEC = simpleCodec(ScrapWoodBlock::new);
    public static final float HARDNESS = 1.5F;
    public static final float RESISTANCE = 2.5F;
    public ScrapWoodBlock(Properties properties) {
        super(properties);
    }
    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }}