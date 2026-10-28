package net.gera.veridia.common.util;

import net.gera.veridia.Veridia;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> NEEDS_ELECTRIC_TOOL = createTag("needs_electric_tool");
        public static final TagKey<Block> INCORRECT_FOR_ELECTRIC_TOOL = createTag("incorrect_for_electric_tool");
        public static final TagKey<Block> COPPER_BLOCKS = createTag("copper_blocks");
        public static final TagKey<Block> BUSHES = createTag("bushes");
        /** Земля Пепельных пустошей: на чём стоят и что заменяют фичи биома (кучи магмы, шипы). */
        public static final TagKey<Block> ASH_WASTES_GROUND = createTag("ash_wastes_ground");
        /** Обожжённые бревна и древесина (в том числе очищенные): из них делаются обожжённые доски. */
        public static final TagKey<Block> ASHEN_LOGS = createTag("ashen_logs");
        /** Очищенные обожжённые бревно и древесина (получаются выплавкой пепельных). */
        public static final TagKey<Block> CHARRED_LOGS = createTag("charred_logs");
        /** Что быстро пилит молибденовая ножовка (и с чего при этом выпадает добыча): цепи, решётки, заборы и т.п. */
        public static final TagKey<Block> MINEABLE_WITH_HACKSAW = createTag("mineable/hacksaw");
        /** Деревянные кладовые Farmer's Delight: наши кладовые тоже в нём, чтобы мод считал их своими. */
        public static final TagKey<Block> FARMERSDELIGHT_WOODEN_CABINETS =
                BlockTags.create(ResourceLocation.fromNamespaceAndPath("farmersdelight", "cabinets/wooden"));
        /** Общий тег блоков молибдена (c:storage_blocks/molybdenum): по нему рецепты других модов видят наш блок. */
        public static final TagKey<Block> MOLYBDENUM_STORAGE_BLOCKS = common("storage_blocks/molybdenum");

        private static TagKey<Block> common(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", name));
        }

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> KNIVES = createTag("knives");
        public static final TagKey<Item> GEM = createTag("gem");
        /** Что можно вставить в слот апгрейда экстрактора (см. ExtractorRecipe.upgrade). */
        public static final TagKey<Item> EXTRACTOR_UPGRADES = createTag("extractor_upgrades");
        public static final TagKey<Item> ASHEN_LOGS = createTag("ashen_logs");
        public static final TagKey<Item> CHARRED_LOGS = createTag("charred_logs");
        public static final TagKey<Item> FARMERSDELIGHT_WOODEN_CABINETS =
                ItemTags.create(ResourceLocation.fromNamespaceAndPath("farmersdelight", "cabinets/wooden"));
        /** Общие теги молибдена (c:...): слиток, самородок, лист, блок - по ним рецепты других модов видят наш молибден. */
        public static final TagKey<Item> MOLYBDENUM_INGOTS = common("ingots/molybdenum");
        public static final TagKey<Item> MOLYBDENUM_NUGGETS = common("nuggets/molybdenum");
        public static final TagKey<Item> MOLYBDENUM_PLATES = common("plates/molybdenum");
        public static final TagKey<Item> MOLYBDENUM_STORAGE_BLOCKS = common("storage_blocks/molybdenum");

        private static TagKey<Item> common(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", name));
        }

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name));
        }
    }
}