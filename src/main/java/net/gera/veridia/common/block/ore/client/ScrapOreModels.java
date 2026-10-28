package net.gera.veridia.common.block.ore.client;

import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.ore.RawMolybdenumBlock;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Подменяет запечённую модель выросшего блока сырой молибденовой руды (natural = true) на {@link ScrapOreModel}.
 * Модель поставленного игроком блока (natural = false) не трогается: он остаётся обычным кубом.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class ScrapOreModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ScrapOreModels() {
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BlockState plainState = ModBlocks.RAW_MOLYBDENUM_BLOCK.get().defaultBlockState();
        BlockState naturalState = plainState.setValue(RawMolybdenumBlock.NATURAL, true);
        ModelResourceLocation plainKey = BlockModelShaper.stateToModelLocation(plainState);
        ModelResourceLocation naturalKey = BlockModelShaper.stateToModelLocation(naturalState);
        ModelResourceLocation scrapKey = ModelResourceLocation.inventory(ModItems.MOLYBDENUM_SCRAP.getId());

        BakedModel plain = models.get(plainKey);
        BakedModel natural = models.get(naturalKey);
        BakedModel scrapItem = models.get(scrapKey);
        if (plain == null || natural == null || scrapItem == null) {
            LOGGER.warn("Модель руды молибдена не заменена: нет модели {} / {} / {}", plainKey, naturalKey, scrapKey);
            return;
        }
        models.put(naturalKey, new ScrapOreModel(natural, ScrapTemplate.from(scrapItem), cubeQuads(plain, plainState)));
    }

    // Все грани куба: по одной на каждую из шести сторон
    private static List<BakedQuad> cubeQuads(BakedModel cubeModel, BlockState state) {
        RandomSource random = RandomSource.create(0L);
        List<BakedQuad> quads = new ArrayList<>(6);
        for (Direction side : Direction.values()) {
            quads.addAll(cubeModel.getQuads(state, side, random));
        }
        return List.copyOf(quads);
    }
}
