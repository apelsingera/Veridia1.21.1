package net.gera.veridiafoods.util;

import net.gera.veridiafoods.VeridiaFoods;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static class Blocks {
    }

    public static class Items {
        public static final TagKey<Item> KNIVES = createTag("knives");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID, name));
        }
    }
}