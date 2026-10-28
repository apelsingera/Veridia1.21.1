package net.gera.veridia.common.block.ore.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
/**
 * Модель выросшего блока сырой руды: обычный куб плюс торчащие из него куски скрапа и иногда повёрнутый кубик.
 * Куб рисуется как всегда (непрозрачный слой), а всё торчащее идёт отдельным списком граней без привязки к стороне и
 * в слое с вырезом, потому что спрайт скрапа частично прозрачный. Что именно торчит, решает {@link ScrapPlanner} в
 * {@link #getModelData}, где известны положение блока и его соседи.
 */
@SuppressWarnings("all")
final class ScrapOreModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<ScrapPlan> PLAN = new ModelProperty<>();
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());

    private final ScrapTemplate scrap;
    private final List<BakedQuad> cube;

    /**
     * @param cubeModel обычная модель блока (куб)
     * @param scrap     грани предмета скрапа
     * @param cube      шесть граней куба блока: заготовка для маленького кубика
     */
    ScrapOreModel(BakedModel cubeModel, ScrapTemplate scrap, List<BakedQuad> cube) {
        super(cubeModel);
        this.scrap = scrap;
        this.cube = cube;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ScrapPlan plan = ScrapPlanner.plan(level, pos, state, scrap.shape());
        return super.getModelData(level, pos, state, modelData).derive().with(PLAN, plan).build();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return RENDER_TYPES;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        if (side != null) {
            return isSolidPass(renderType) ? super.getQuads(state, side, rand, data, renderType) : List.of();
        }
        ScrapPlan plan = data.get(PLAN);
        if (plan == null || !isCutoutPass(renderType)) {
            return List.of();
        }
        return decorationQuads(plan);
    }

    private List<BakedQuad> decorationQuads(ScrapPlan plan) {
        List<BakedQuad> quads = new ArrayList<>();
        for (ScrapPlan.Piece piece : plan.pieces()) {
            List<BakedQuad> template = piece.kind() == ScrapPlan.Kind.SHARD ? scrap.quads() : cube;
            quads.addAll(DecorQuads.transformAll(template, piece.transform(), piece.light()));
        }
        return quads;
    }

    private static boolean isSolidPass(@Nullable RenderType renderType) {
        return renderType == null || renderType == RenderType.solid();
    }

    private static boolean isCutoutPass(@Nullable RenderType renderType) {
        return renderType == null || renderType == RenderType.cutout();
    }
}
