package net.gera.veridia.common.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.loot.AddItemModifier;
import net.gera.veridia.common.loot.ReplaceItemModifier;
import net.gera.veridia.common.loot.ReplaceLootInBiomesModifier;
import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.gera.veridia.common.util.RandomCountLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Veridia.MOD_ID);
    }

    @Override
    protected void start() {
        this.add("garlic_seeds_from_fern",
                new AddItemModifier(new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.FERN).build(),
                        LootItemRandomChanceCondition.randomChance(0.2f).build()}, ModItems.GARLIC_SEEDS.get()));
        this.add("plant_clump_from_grass",
                new AddItemModifier(new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.SHORT_GRASS).build(),
                        LootItemRandomChanceCondition.randomChance(0.4f).build()}, ModItems.PLANT_CLUMP.get()));
            this.add("meat_from_horse",
                    new RandomCountLootModifier(
                            new LootItemCondition[]{
                                    new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("entities/horse")).build()
                            },
                            ModItems.HORSE_MEAT.get(),
                            2,
                            4
                    )
            );
        this.add("custom_item_from_fishing",
                new AddItemModifier(
                        new LootItemCondition[]{
                                new LootTableIdCondition.Builder(
                                        ResourceLocation.withDefaultNamespace("gameplay/fishing")
                                ).build(),
                                LootItemRandomChanceCondition.randomChance(0.20f).build()
                        },
                        ModItems.NAUTILUS_GLAND.get()
                )
        );

        this.add("custom_item2_from_fishing",
                new AddItemModifier(
                        new LootItemCondition[]{
                                new LootTableIdCondition.Builder(
                                        ResourceLocation.withDefaultNamespace("gameplay/fishing")
                                ).build(),
                                LootItemRandomChanceCondition.randomChance(0.20f).build()
                        },
                        Items.PRISMARINE_CRYSTALS
                )
        );

        this.add("wither_bone_from_wither_skeleton",
                new ReplaceItemModifier(
                        new LootItemCondition[]{
                                new LootTableIdCondition.Builder(
                                        ResourceLocation.withDefaultNamespace("entities/wither_skeleton")
                                ).build()
                        },
                        Items.BONE,
                        ModItems.WITHER_BONE.get()
                )
        );

        // Рыбалка в пепельных биомах целиком идёт по своей таблице (data/veridia/loot_table/gameplay/fishing/ash.json).
        // Идёт последним: заменяет и ванильный улов, и добавки модификаторов выше; nautilus_gland и призмарин из них
        // уже лежат в самой таблице отдельными пулами по 20%.
        this.add("ash_fishing",
                new ReplaceLootInBiomesModifier(
                        new LootItemCondition[]{
                                new LootTableIdCondition.Builder(
                                        ResourceLocation.withDefaultNamespace("gameplay/fishing")
                                ).build()
                        },
                        ModBiomes.ASH_BIOMES,
                        ResourceKey.create(Registries.LOOT_TABLE,
                                ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "gameplay/fishing/ash"))
                )
        );
        }
    }