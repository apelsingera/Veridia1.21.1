package net.gera.veridia.common.compat.dynamictrees;

import com.dtteam.dynamictrees.api.registry.Registry;
import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.block.soil.SoilProperties;
import com.dtteam.dynamictrees.data.GatherDataHelper;
import com.dtteam.dynamictrees.data.provider.DTBlockLootSubProvider;
import com.dtteam.dynamictrees.data.provider.DTBlockStateProvider;
import com.dtteam.dynamictrees.data.provider.DTItemModelProvider;
import com.dtteam.dynamictrees.data.provider.DTSpriteSourceProvider;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.Species;
import net.gera.veridia.Veridia;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Ресурсы блоков деревьев Dynamic Trees из нашего набора (trees/veridia): состояния блоков и модели веток, листвы,
 * саженцев и почвы, модели семян, таблицы добычи и теги. DT умеет сам составить их по JSON набора, поэтому здесь только
 * просьба сделать это для нашего мода. Звать только когда Dynamic Trees установлен (запуск runData).
 * <p>
 * Это то же, что делает {@link GatherDataHelper#gatherAllData}, с отличиями. Провайдеры переименованы: у DT и у нас
 * совпадают имена ("Block States: veridia"), а генератор данных не терпит одинаковых. Таблицы добычи DT добавляются
 * подтаблицей в наш общий провайдер лута (его имя менять нельзя), см. {@link #lootEntry}. Названия блоков и предметов
 * (lang) не генерируются: DT писал бы свой en_us.json поверх нашего, поэтому названия дописаны в lang вручную.
 */
public final class DynamicTreesData {
    private static final String NAME_PREFIX = "Dynamic Trees: ";

    private DynamicTreesData() {
    }

    /** Таблицы добычи ветвей, листвы и семян деревьев нашего набора - для общего LootTableProvider. */
    public static LootTableProvider.SubProviderEntry lootEntry(ExistingFileHelper fileHelper) {
        return new LootTableProvider.SubProviderEntry(
                lookup -> new DTBlockLootSubProvider(lookup, Veridia.MOD_ID, fileHelper) { },
                LootContextParamSets.BLOCK);
    }

    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        Registry<?>[] registries = {SoilProperties.REGISTRY, Family.REGISTRY, Species.REGISTRY, LeavesProperties.REGISTRY};

        GatherDataHelper.gatherTagData(Veridia.MOD_ID, event);
        generator.addProvider(event.includeClient(),
                new DTSpriteSourceProvider(output, lookup, Veridia.MOD_ID, fileHelper, registries));
        generator.addProvider(event.includeClient(),
                new DTBlockStateProvider(output, Veridia.MOD_ID, fileHelper, Arrays.asList(registries)) {
                    @Override
                    public String getName() {
                        return NAME_PREFIX + super.getName();
                    }
                });
        generator.addProvider(event.includeClient(),
                new DTItemModelProvider(output, Veridia.MOD_ID, fileHelper, Arrays.asList(registries)) {
                    @Override
                    public String getName() {
                        return NAME_PREFIX + super.getName();
                    }
                });
    }
}
