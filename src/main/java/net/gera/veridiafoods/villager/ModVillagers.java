package net.gera.veridiafoods.villager;

import com.google.common.collect.ImmutableSet;
import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, VeridiaFoods.MOD_ID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, VeridiaFoods.MOD_ID);

    public static final Holder<PoiType> SAGE_POI = POI_TYPES.register("sage_poi",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.COOKING_TABLE.get().getStateDefinition()
                    .getPossibleStates()),1,1));

    public static final Holder<VillagerProfession> SAGE = VILLAGER_PROFESSIONS.register("sage_villager",
            () -> new VillagerProfession("sage_villager", holder -> holder.value() == SAGE_POI.value(),
                    poiTypeHolder -> poiTypeHolder.value() == SAGE_POI.value(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_YES));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
