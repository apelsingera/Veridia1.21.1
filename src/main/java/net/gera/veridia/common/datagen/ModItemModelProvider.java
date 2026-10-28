package net.gera.veridia.common.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.mana.ManaProgression;
import net.gera.veridia.common.mana.UpgradeStep;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.List;


public class ModItemModelProvider extends ItemModelProvider {
    private static final LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();
    static {
        trimMaterials.put(TrimMaterials.QUARTZ, 0.1F);
        trimMaterials.put(TrimMaterials.IRON, 0.2F);
        trimMaterials.put(TrimMaterials.NETHERITE, 0.3F);
        trimMaterials.put(TrimMaterials.REDSTONE, 0.4F);
        trimMaterials.put(TrimMaterials.COPPER, 0.5F);
        trimMaterials.put(TrimMaterials.GOLD, 0.6F);
        trimMaterials.put(TrimMaterials.EMERALD, 0.7F);
        trimMaterials.put(TrimMaterials.DIAMOND, 0.8F);
        trimMaterials.put(TrimMaterials.LAPIS, 0.9F);
        trimMaterials.put(TrimMaterials.AMETHYST, 1.0F);
    }
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Veridia.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.DANDELIONS_ROOT.get());
        basicItem(ModItems.PLANT_CLUMP.get());
        basicItem(ModItems.FIBER_NET.get());
        basicItem(ModItems.TANNED_LEATHER.get());
        basicItem(ModItems.STRONG_CLOTH.get());
        basicItem(ModItems.STRONG_HANDLE.get());
        basicItem(ModItems.ALCHEMIST_UPGRADE.get());
        basicItem(ModItems.WARRIOR_UPGRADE.get());
        basicItem(ModItems.WORKER_UPGRADE.get());
        basicItem(ModItems.NETHER_STRING.get());
        basicItem(ModItems.WITHER_BONE.get());
        basicItem(ModItems.BRAIN.get());
        basicItem(ModItems.INFESTED_BRAIN.get());
        basicItem(ModItems.HEALTH_POWDER.get());
        basicItem(ModItems.ZOMBIE_EGG_WEAK.get());
        basicItem(ModItems.ZOMBIE_EGG_DANGEROUS.get());
        basicItem(ModItems.ZOMBIE_EGG_30.get());
        basicItem(ModItems.ZOMBIE_EGG_31.get());
        basicItem(ModItems.ZOMBIE_EGG_32.get());
        basicItem(ModItems.ZOMBIE_EGG_33.get());
        basicItem(ModItems.ZOMBIE_EGG_34.get());
        basicItem(ModItems.ZOMBIE_EGG_35.get());
        basicItem(ModItems.HUSK_EGG_WEAK.get());
        basicItem(ModItems.HUSK_EGG_DANGEROUS.get());
        basicItem(ModItems.HUSK_EGG_30.get());
        basicItem(ModItems.HUSK_EGG_31.get());
        basicItem(ModItems.HUSK_EGG_32.get());
        basicItem(ModItems.HUSK_EGG_33.get());
        basicItem(ModItems.HUSK_EGG_34.get());
        basicItem(ModItems.HUSK_EGG_35.get());
        basicItem(ModItems.CHAMOTTE_BRICK.get());
        basicItem(ModItems.AQUATIC_STRING.get());
        basicItem(ModItems.PLANT_STRING.get());
        basicItem(ModItems.NAUTILUS_GLAND.get());
        basicItem(ModItems.PRISMARINE_C_POWDER.get());
        basicItem(ModItems.KNOCKBACK_RING.get());
        basicItem(ModItems.KNOCKBACK_RING_2.get());
        basicItem(ModItems.KNOCKBACK_RING_3.get());
        basicItem(ModItems.KNOCKBACK_RING_4.get());
        basicItem(ModItems.KNOCKBACK_RING_5.get());
        basicItem(ModItems.NEURON_CLUMP.get());
        basicItem(ModItems.ANCIENT_DOCUMENT.get());
        basicItem(ModItems.PURPURITE.get());
        basicItem(ModItems.R_PURPURITE.get());
        basicItem(ModItems.AQUATIC_AMULET.get());
        basicItem(ModItems.AQUATIC_AMULET_2.get());
        basicItem(ModItems.AQUATIC_AMULET_3.get());
        basicItem(ModItems.AQUATIC_AMULET_4.get());
        basicItem(ModItems.AQUATIC_AMULET_5.get());
        basicItem(ModItems.AQUATIC_TOPAZ.get());
        basicItem(ModItems.R_AQUATIC_TOPAZ.get());
        basicItem(ModItems.PLASMAFITE.get());
        basicItem(ModItems.SCORCHING_PLASMAFITE.get());
        basicItem(ModItems.EYE_OF_SAURON.get());
        basicItem(ModItems.R_VERDANT_FLUORITE.get());
        basicItem(ModItems.VERDANT_FLUORITE.get());
        basicItem(ModItems.D_VERDANT_FLUORITE.get());
        basicItem(ModItems.VERIDITE.get());
        basicItem(ModItems.RAW_BARIUM.get());
        basicItem(ModItems.BARIUM_INGOT.get());
        basicItem(ModItems.BARIUM_POWDER.get());
        basicItem(ModItems.MOLYBDENUM_SCRAP.get());
        basicItem(ModItems.MOLYBDENUM_INGOT.get());
        basicItem(ModItems.MOLYBDENUM_NUGGET.get());
        basicItem(ModItems.MOLYBDENUM_SHEET.get());
        // Предмет решётки - плоская текстура блока, как у железной
        withExistingParent("molybdenum_bars", mcLoc("item/generated")).texture("layer0", modLoc("block/molybdenum_bars"));
        handheldItem(ModItems.MOLYBDENUM_HACKSAW.get());
        basicItem(ModItems.DISMAL_CORE.get());
        basicItem(ModItems.QUARTZ_POWDER.get());
        handheldItem(ModItems.SLOW_KILLER.get());
        basicItem(ModItems.SLOW_KILLER_2.get());
        basicItem(ModItems.SLOW_KILLER_3.get());
        basicItem(ModItems.SLOW_KILLER_4.get());
        basicItem(ModItems.SLOW_KILLER_5.get());
        basicItem(ModItems.EYE_OF_SAURON_2.get());
        basicItem(ModItems.EYE_OF_SAURON_3.get());
        basicItem(ModItems.EYE_OF_SAURON_4.get());
        basicItem(ModItems.EYE_OF_SAURON_5.get());
        handheldItem(ModItems.CRUDE_KNIFE.get());
        handheldItem(ModItems.SS_KNIFE.get());
        handheldItem(ModItems.CRUDE_HAMMER.get());
        handheldItem(ModItems.VERIDITE_PICKAXE.get());
        handheldItem(ModItems.SMELTING_PICKAXE.get());
        basicItem(ModItems.GARLIC_SEEDS.get());
        basicItem(ModItems.HEMP_SEEDS.get());
        basicItem(ModItems.TOMATO_CHERRY.get());
        basicItem(ModItems.RED_ONION.get());
        basicItem(ModItems.GARLIC.get());
        basicItem(ModItems.BELL_PEPPER_SEEDS.get());
        basicItem(ModItems.RED_BELL_PEPPER.get());
        basicItem(ModItems.RED_BELL_PEPPER_SLICE.get());
        basicItem(ModItems.BLUE_BERRIES.get());
        basicItem(ModItems.GOOSE_BERRIES.get());
        basicItem(ModItems.CRANBERRIES.get());
        basicItem(ModItems.CUMACUMA_BERRY.get());
        basicItem(ModItems.HORSE_MEAT.get());
        basicItem(ModItems.PIRANHA.get());
        basicItem(ModBlocks.ASHEN_GRASS.asItem());
        basicItem(ModItems.GOLITAH_TIGERFISH.get());
        basicItem(ModItems.DEAD_FISH.get());
        basicItem(ModItems.COOKED_HORSE_MEAT.get());
        basicItem(ModItems.BESH.get());
        basicItem(ModItems.GROUND_PORK.get());
        basicItem(ModItems.PORK_PATTY.get());
        basicItem(ModItems.COOKED_PORK_PATTY.get());
        basicItem(ModItems.PORKCHOP_SLICE.get());
        basicItem(ModItems.GROUND_BEEF.get());
        basicItem(ModItems.BEEF_PATTY.get());
        basicItem(ModItems.COOKED_BEEF_PATTY.get());
        basicItem(ModItems.OMEN_REPRISE_DISK.get());
        basicItem(ModItems.SLOT_POCKET.get());
        basicItem(ModItems.PROFUNDUSTONE_CHUNK.get());
        basicItem(ModItems.TASTY_MEAT_MIX.get());
        basicItem(ModItems.UNFINISHED_STUFFED_PEPPER.get());
        basicItem(ModItems.BLOODSTONE.get());
        basicItem(ModItems.STUFFED_PEPPER.get());
        basicItem(ModItems.BEEF_BURGER.get());
        basicItem(ModItems.CHICKEN_LEGS.get());
        basicItem(ModItems.BEEF_W_POTATOES.get());
        basicItem(ModItems.PIQUANT_ELITE_FISH_DISH.get());
        basicItem(ModItems.NICE_BREAKFAST.get());
        basicItem(ModItems.ETERNAL_PASS.get());
        basicItem(ModItems.LIMITED_PASS.get());
        basicItem(ModItems.JOB_APPLICATION.get());
        basicItem(ModItems.CREATIVE_BONE_MEAL.get());
        basicItem(ModItems.FIRING_CONTRACT.get());
        basicItem(ModItems.CREATIVE_NAILER_WAND.get());

        trimmedArmorItem(ModItems.VERIDITE_HELMET);
        trimmedArmorItem(ModItems.VERIDITE_CHESTPLATE);
        trimmedArmorItem(ModItems.VERIDITE_LEGGINGS);
        trimmedArmorItem(ModItems.VERIDITE_BOOTS);

        basicItem(ModItems.DOUBLE_COPPER_INGOT.get());
        basicItem(ModItems.DOUBLE_IRON_INGOT.get());
        basicItem(ModItems.DOUBLE_GOLD_INGOT.get());
        basicItem(ModItems.DOUBLE_DIAMOND.get());
        basicItem(ModItems.QUAD_COPPER_INGOT.get());
        basicItem(ModItems.QUAD_IRON_INGOT.get());
        basicItem(ModItems.QUAD_GOLD_INGOT.get());
        basicItem(ModItems.QUAD_DIAMOND.get());

        basicItem(ModItems.CHEESE.get());
        basicItem(ModItems.BUTTER.get());
        basicItem(ModItems.BOILED_EGG.get());
        basicItem(ModItems.EGG_BUCKET.get());
        basicItem(ModItems.BOILED_EGG_BUCKET.get());

        basicItem(ModItems.EXOTIC_SEASONING1.get());
        basicItem(ModItems.EXOTIC_SEASONING2.get());
        basicItem(ModItems.EXOTIC_SEASONING3.get());

        basicItem(ModItems.BLUEBERRY_PIE.get());
        basicItem(ModItems.BLUEBERRY_PIE_SLICE.get());
        basicItem(ModItems.GOOSEBERRY_PIE.get());
        basicItem(ModItems.GOOSEBERRY_PIE_SLICE.get());
        basicItem(ModItems.CRANBERRY_PIE.get());
        basicItem(ModItems.CRANBERRY_PIE_SLICE.get());

        basicItem(ModItems.WHEAT_FLOUR.get());
        basicItem(ModItems.DOUGH.get());
        basicItem(ModItems.PASTA.get());
        basicItem(ModItems.TASTY_PASTA.get());
        basicItem(ModItems.PASTA_BUCKET.get());
        basicItem(ModItems.COOKED_PASTA_BUCKET.get());
        basicItem(ModItems.CARROT_PASTA.get());
        basicItem(ModItems.RONALDO_PASTA.get());

        basicItem(ModItems.POTATO_PUREE.get());

        basicItem(ModItems.MILK_N_BUTTER.get());
        basicItem(ModItems.BLUE_BERRY_JUICE.get());
        basicItem(ModItems.SUPER_BLUE_BERRY_JUICE.get());
        basicItem(ModItems.CRANBERRY_JUICE.get());
        basicItem(ModItems.SUPER_CRANBERRY_JUICE.get());
        basicItem(ModItems.GOOSE_BERRY_JUICE.get());
        basicItem(ModItems.SUPER_GOOSE_BERRY_JUICE.get());

        buttonItem(ModBlocks.SCULK_BUTTON, ModBlocks.SCULK_PLANKS);
        buttonItem(ModBlocks.CHARRED_BUTTON, ModBlocks.CHARRED_PLANKS);
        fenceItem(ModBlocks.CHARRED_FENCE, ModBlocks.CHARRED_PLANKS);
        buttonItem(ModBlocks.ASHEN_BUTTON, ModBlocks.ASHEN_PLANKS);
        fenceItem(ModBlocks.ASHEN_FENCE, ModBlocks.ASHEN_PLANKS);
        fenceItem(ModBlocks.SCULK_FENCE, ModBlocks.SCULK_PLANKS);
        wallItem(ModBlocks.SCULK_WALL, ModBlocks.SCULK_PLANKS);
        wallItem(ModBlocks.SINGE_STONE_WALL, ModBlocks.SINGE_STONE);
        wallItem(ModBlocks.SINGE_COBBLESTONE_WALL, ModBlocks.SINGE_COBBLESTONE);
        withExistingParent(ModBlocks.BLOODY_RUNESTONE.getId().getPath(), modLoc("block/bloody_runestone"));
        withExistingParent(ModBlocks.SPIRITUAL_RUNESTONE.getId().getPath(), modLoc("block/spiritual_runestone"));
        wallItem(ModBlocks.SINGE_STONE_BRICK_WALL, ModBlocks.SINGE_STONE_BRICKS);
        wallItem(ModBlocks.MOSSY_SINGE_STONE_BRICK_WALL, ModBlocks.MOSSY_SINGE_STONE_BRICKS);
        wallItem(ModBlocks.CRACKED_SINGE_STONE_BRICK_WALL, ModBlocks.CRACKED_SINGE_STONE_BRICKS);
        wallItem(ModBlocks.SINGE_STONE_BRICK_TILE_WALL, ModBlocks.SINGE_STONE_BRICK_TILES);
        wallItem(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_WALL, ModBlocks.MOSSY_SINGE_STONE_BRICK_TILES);
        wallItem(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_WALL, ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILES);

        basicItem(ModBlocks.SCULK_DOOR.asItem());
        basicItem(ModBlocks.CHARRED_DOOR.asItem());
        basicItem(ModBlocks.ASHEN_DOOR.asItem());

        saplingItem(ModBlocks.SCULK_SPROUT);
        saplingItem(ModBlocks.SCULK_MUSHROOM);

        for (UpgradeStep step : ManaProgression.STEPS) {
            runeItem(step.registryName(), step.registryName());
        }
        for (String tier : List.of("newbie_mantle", "experienced_mantle")) {
            runeItem(tier + "_helmet", tier + "_hood");
            runeItem(tier + "_chestplate", tier + "_robe");
            runeItem(tier + "_boots", tier + "_boots");
        }
        basicItem(ModItems.MUTANT_SPAWN_EGG.get());
        // Яйцо воина без своей текстуры: ванильная заготовка яйца, цвета берутся из самого предмета
        withExistingParent("ominous_warrior_spawn_egg", ResourceLocation.parse("item/template_spawn_egg"));
        withExistingParent("brutal_ominous_warrior_spawn_egg", ResourceLocation.parse("item/template_spawn_egg"));
        withExistingParent("cinder_roamer_spawn_egg", ResourceLocation.parse("item/template_spawn_egg"));
        basicItem(ModItems.MANA_AWAKENING_STONE.get());
        basicItem(ModItems.SUSPICIOUS_SOUP.get());
        basicItem(ModItems.NETHER_DELICACY.get());
        handheldItem(ModItems.INFERNAL_STAFF.get());
        handheldItem(ModItems.INFERNAL_STAFF_2.get());
        handheldItem(ModItems.INFERNAL_STAFF_3.get());
        handheldItem(ModItems.INFERNAL_STAFF_4.get());
        handheldItem(ModItems.INFERNAL_STAFF_5.get());
        ItemModelBuilder prismCooldownModel = withExistingParent("blinding_prism_cooldown",
                        ResourceLocation.parse("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "item/blinding_prism_cooldown"));

        withExistingParent("blinding_prism", ResourceLocation.parse("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "item/blinding_prism"))
                .override()
                        .predicate(ResourceLocation.fromNamespaceAndPath("minecraft", "cooldown"), 0.001F)
                        .model(prismCooldownModel)
                        .end();
    }
    private void runeItem(String registryName, String textureName) {
        withExistingParent(registryName, ResourceLocation.parse("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "item/" + textureName));
    }

    private ItemModelBuilder vanillaIconItem(String registryName, String vanillaTextureName) {
        return withExistingParent(registryName, ResourceLocation.parse("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath("minecraft", "item/" + vanillaTextureName));
    }

    private void saplingItem(DeferredBlock<Block> item) {
        withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "block/" + item.getId().getPath()));
    }
    public void buttonItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/button_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void fenceItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void wallItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/wall_inventory"))
                .texture("wall",  ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }
    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,"item/" + item.getId().getPath()));
    }
    // Shoutout to El_Redstoniano for making this
    private void trimmedArmorItem(DeferredItem<ArmorItem> itemDeferredItem) {
        final String MOD_ID = Veridia.MOD_ID; // Change this to your mod id

        if(itemDeferredItem.get() instanceof ArmorItem armorItem) {
            trimMaterials.forEach((trimMaterial, value) -> {
                float trimValue = value;

                String armorType = switch (armorItem.getEquipmentSlot()) {
                    case HEAD -> "helmet";
                    case CHEST -> "chestplate";
                    case LEGS -> "leggings";
                    case FEET -> "boots";
                    default -> "";
                };

                String armorItemPath = armorItem.toString();
                String trimPath = "trims/items/" + armorType + "_trim_" + trimMaterial.location().getPath();
                String currentTrimName = armorItemPath + "_" + trimMaterial.location().getPath() + "_trim";
                ResourceLocation armorItemResLoc = ResourceLocation.parse(armorItemPath);
                ResourceLocation trimResLoc = ResourceLocation.parse(trimPath); // minecraft namespace
                ResourceLocation trimNameResLoc = ResourceLocation.parse(currentTrimName);

                // This is used for making the ExistingFileHelper acknowledge that this texture exist, so this will
                // avoid an IllegalArgumentException
                existingFileHelper.trackGenerated(trimResLoc, PackType.CLIENT_RESOURCES, ".png", "textures");

                // Trimmed armorItem files
                getBuilder(currentTrimName)
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", armorItemResLoc.getNamespace() + ":item/" + armorItemResLoc.getPath())
                        .texture("layer1", trimResLoc);

                // Non-trimmed armorItem file (normal variant)
                this.withExistingParent(itemDeferredItem.getId().getPath(),
                                mcLoc("item/generated"))
                        .override()
                        .model(new ModelFile.UncheckedModelFile(trimNameResLoc.getNamespace()  + ":item/" + trimNameResLoc.getPath()))
                        .predicate(mcLoc("trim_type"), trimValue).end()
                        .texture("layer0",
                                ResourceLocation.fromNamespaceAndPath(MOD_ID,
                                        "item/" + itemDeferredItem.getId().getPath()));
            });
        }
    }
}