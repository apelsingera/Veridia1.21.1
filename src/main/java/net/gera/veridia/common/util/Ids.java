package net.gera.veridia.common.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Ids {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private Ids() {
    }
    public static Item item(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            warnMissing("item", id);
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(key);
    }
    public static ItemStack stack(String id) {
        return stack(id, 1);
    }
    public static ItemStack stack(String id, int count) {
        Item item = item(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }
    public static boolean hasItem(String id) {
        return BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id));
    }
    public static Block block(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) {
            warnMissing("block", id);
            return Blocks.AIR;
        }
        return BuiltInRegistries.BLOCK.get(key);
    }
    public static boolean hasBlock(String id) {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(id));
    }
    public static TagKey<Item> itemTag(String id) {
        return ItemTags.create(ResourceLocation.parse(stripHash(id)));
    }
    public static TagKey<Block> blockTag(String id) {
        return BlockTags.create(ResourceLocation.parse(stripHash(id)));
    }
    public static DeferredItem<Item> itemRef(String id) {
        return DeferredItem.createItem(ResourceLocation.parse(id));
    }

    public static DeferredBlock<Block> blockRef(String id) {
        return DeferredBlock.createBlock(ResourceLocation.parse(id));
    }

    private static String stripHash(String id) {
        return id.startsWith("#") ? id.substring(1) : id;
    }

    private static void warnMissing(String kind, String id) {
        if (WARNED.add(kind + " " + id)) {
            LOGGER.warn("[Veridia] No {} with id {} (mod not installed or a typo), using air instead", kind, id);
        }
    }
}
