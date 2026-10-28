package net.gera.veridia.common.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Consumer;


public class ModAdvancementProvider implements AdvancementProvider.AdvancementGenerator {

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver,
                         ExistingFileHelper existingFileHelper) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(
                        ModItems.VERIDITE.get(),
                        Component.translatable("advancement.veridia.materials_root.title"),
                        Component.translatable("advancement.veridia.materials_root.description"),
                        ResourceLocation.withDefaultNamespace("textures/block/amethyst_block.png"),
                        AdvancementType.TASK,
                        false, false, false)
                .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                .save(saver, id("materials/root"));

        AdvancementHolder rawPurpurite = material(saver, root, ModItems.R_PURPURITE, "raw_purpurite");
        AdvancementHolder purpurite = material(saver, rawPurpurite, ModItems.PURPURITE, "purpurite");

        AdvancementHolder rawTopaz = material(saver, root, ModItems.R_AQUATIC_TOPAZ, "raw_aquatic_topaz");
        material(saver, rawTopaz, ModItems.AQUATIC_TOPAZ, "aquatic_topaz");

        AdvancementHolder rawFluorite = material(saver, root, ModItems.R_VERDANT_FLUORITE, "raw_verdant_fluorite");
        AdvancementHolder fluorite = material(saver, rawFluorite, ModItems.VERDANT_FLUORITE, "verdant_fluorite");
        material(saver, fluorite, ModItems.D_VERDANT_FLUORITE, "dangerous_verdant_fluorite");

        AdvancementHolder plasmafite = material(saver, root, ModItems.PLASMAFITE, "plasmafite");
        material(saver, plasmafite, ModItems.SCORCHING_PLASMAFITE, "scorching_plasmafite");

        material(saver, root, ModItems.PROFUNDUSTONE_CHUNK, "profundustone_chunk");

        // "Veridite" - милстоун: сначала нужно добыть все 4 самоцвета (пурпурит / акватический
        // топаз / зелёный флюорит / плазмафит), и только потом слиток веридита закрывает ачивку.
        // Все 5 критериев складываются по AND (стратегия билдера по умолчанию), а parent = purpurite
        // ставит эту ачивку в дереве после ветки самоцветов.
        Advancement.Builder.advancement()
                .parent(purpurite)
                .display(
                        ModItems.VERIDITE.get(),
                        Component.translatable("advancement.veridia.veridite.title"),
                        Component.translatable("advancement.veridia.veridite.description"),
                        null,
                        AdvancementType.GOAL,
                        true, true, false)
                .addCriterion("has_purpurite", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PURPURITE.get()))
                .addCriterion("has_aquatic_topaz", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.AQUATIC_TOPAZ.get()))
                .addCriterion("has_verdant_fluorite", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.VERDANT_FLUORITE.get()))
                .addCriterion("has_plasmafite", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PLASMAFITE.get()))
                .addCriterion("has_veridite", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.VERIDITE.get()))
                .save(saver, id("materials/veridite"));

        // ---- Вкладка "Станции" (дерево по эскизу): cooking_table - корень вкладки (свой фон, без
        // родителя), от него две ветки: mythical_table -> runic_pedestal, и отдельно extractor.
        // Критерий у всех - placed_block (ачивка падает, когда игрок ставит блок).
        AdvancementHolder cookingTable = Advancement.Builder.advancement()
                .display(
                        ModBlocks.COOKING_TABLE.get(),
                        Component.translatable("advancement.veridia.cooking_table.title"),
                        Component.translatable("advancement.veridia.cooking_table.description"),
                        ResourceLocation.withDefaultNamespace("textures/block/crafting_table_top.png"),
                        AdvancementType.TASK,
                        true, true, false)
                .addCriterion("placed", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(ModBlocks.COOKING_TABLE.get()))
                .save(saver, id("stations/cooking_table"));

        AdvancementHolder mythicalTable = station(saver, cookingTable, ModBlocks.MYTHICAL_TABLE.get(), "mythical_table");
        station(saver, mythicalTable, ModBlocks.RUNIC_PEDESTAL.get(), "runic_pedestal");
        station(saver, cookingTable, ModBlocks.EXTRACTOR.get(), "extractor");
    }

    /** Ачивка "поставь эту станцию" (критерий placed_block); возвращает холдер для parent-цепочки. */
    private static AdvancementHolder station(Consumer<AdvancementHolder> saver, AdvancementHolder parent,
                                             Block block, String key) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(
                        block,
                        Component.translatable("advancement.veridia." + key + ".title"),
                        Component.translatable("advancement.veridia." + key + ".description"),
                        null,
                        AdvancementType.TASK,
                        true, true, false)
                .addCriterion("placed", ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block))
                .save(saver, id("stations/" + key));
    }

    private static AdvancementHolder material(Consumer<AdvancementHolder> saver, AdvancementHolder parent,
                                              DeferredItem<Item> item, String key) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(
                        item.get(),
                        Component.translatable("advancement.veridia." + key + ".title"),
                        Component.translatable("advancement.veridia." + key + ".description"),
                        null,
                        AdvancementType.TASK,
                        true, true, false)
                .addCriterion("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(item.get()))
                .save(saver, id("materials/" + key));
    }

    private static String id(String path) {
        return Veridia.MOD_ID + ":" + path;
    }
}