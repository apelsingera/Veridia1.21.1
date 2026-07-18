package net.gera.veridia.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.item.ModItems;
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

public class ModItemModelProvider extends ItemModelProvider {
    private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();
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
        basicItem(ModItems.PLANT_STRING.get());
        basicItem(ModItems.CRUDE_KNIFE.get());
        basicItem(ModItems.GARLIC_SEEDS.get());
        basicItem(ModItems.GARLIC.get());
        ///////*****METALS*****\\\\\\\
        basicItem(ModItems.BALLS.get());
        ///COPPER
        basicItem(ModItems.RAW_COPPER_SMALL.get());
        basicItem(ModItems.RAW_COPPER_MEDIUM.get());
        basicItem(ModItems.RAW_COPPER_BIG.get());
        basicItem(ModItems.IMPURE_COPPER_INGOT.get());
        basicItem(ModItems.COPPER_INGOT.get());
        basicItem(ModItems.COPPER_ROD.get());
        basicItem(ModItems.DOUBLED_COPPER_INGOT.get());
        //tools
        handheldItem(ModItems.COPPER_SWORD.get());
        handheldItem(ModItems.COPPER_PICKAXE.get());
        handheldItem(ModItems.COPPER_AXE.get());
        handheldItem(ModItems.COPPER_SHOVEL.get());
        handheldItem(ModItems.COPPER_HOE.get());
        //armor
        trimmedArmorItem(ModItems.COPPER_HELMET);
        trimmedArmorItem(ModItems.COPPER_CHESTPLATE);
        trimmedArmorItem(ModItems.COPPER_LEGGINGS);
        trimmedArmorItem(ModItems.COPPER_BOOTS);
        basicItem(ModItems.COPPER_HORSE_ARMOR.get());
        ///TIN
        basicItem(ModItems.RAW_TIN_SMALL.get());
        basicItem(ModItems.RAW_TIN_MEDIUM.get());
        basicItem(ModItems.RAW_TIN_BIG.get());
        basicItem(ModItems.IMPURE_TIN_INGOT.get());
        basicItem(ModItems.TIN_INGOT.get());
        buttonItem(ModBlocks.TIN_BUTTON, ModBlocks.TIN_BLOCK);
        fenceItem(ModBlocks.TIN_FENCE, ModBlocks.TIN_BLOCK);
        wallItem(ModBlocks.TIN_WALL, ModBlocks.TIN_BLOCK);
        basicItem(ModBlocks.TIN_DOOR.asItem());
        ///BRONZE
        basicItem(ModItems.IMPURE_BRONZE_INGOT.get());
        basicItem(ModItems.BRONZE_INGOT.get());
        //tools
        handheldItem(ModItems.BRONZE_SWORD.get());
        handheldItem(ModItems.BRONZE_PICKAXE.get());
        handheldItem(ModItems.BRONZE_AXE.get());
        handheldItem(ModItems.BRONZE_SHOVEL.get());
        handheldItem(ModItems.BRONZE_HOE.get());
        trimmedArmorItem(ModItems.BRONZE_HELMET);
        trimmedArmorItem(ModItems.BRONZE_CHESTPLATE);
        trimmedArmorItem(ModItems.BRONZE_LEGGINGS);
        trimmedArmorItem(ModItems.BRONZE_BOOTS);
        /// WROUGHT IRON
        basicItem(ModItems.IMPURE_WROUGHT_IRON_INGOT.get());
        basicItem(ModItems.WROUGHT_IRON_INGOT.get());
        /// GEMSTONES
        basicItem(ModItems.AZOTIC_TOPAZ.get());
        basicItem(ModItems.DATOLITE.get());
        //food/bushes/crops//
        basicItem(ModItems.BLUE_BERRIES.get());
        saplingItem(ModBlocks.SCULK_SPROUT);
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

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,"block/" + item.getId().getPath()));
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
}