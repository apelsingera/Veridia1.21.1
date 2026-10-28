package net.gera.veridia.common.villager;

import com.google.common.collect.ImmutableSet;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, Veridia.MOD_ID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, Veridia.MOD_ID);
    public static final Holder<PoiType> SAGE_POI = POI_TYPES.register("sage_poi",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.COOKING_TABLE.get().getStateDefinition()
                    .getPossibleStates()),1,1));
    public static final Holder<VillagerProfession> SAGE = VILLAGER_PROFESSIONS.register("sage_villager",
            () -> new VillagerProfession("sage_villager", holder -> holder.value() == SAGE_POI.value(),
                    poiTypeHolder -> poiTypeHolder.value() == SAGE_POI.value(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_YES));
    public static final int STROP_MAX_WORKERS = 3;
    public static final int STROP_VALID_RANGE = 3;
    public static final Holder<PoiType> STROP_POI = POI_TYPES.register("strop_poi",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.STROP.get().getStateDefinition()
                    .getPossibleStates()), STROP_MAX_WORKERS, STROP_VALID_RANGE));
    public static final Holder<VillagerProfession> WARRIOR = VILLAGER_PROFESSIONS.register("warrior_villager",
            () -> new VillagerProfession("warrior_villager", holder -> holder.value() == STROP_POI.value(),
                    poiTypeHolder -> poiTypeHolder.value() == STROP_POI.value(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_ARMORER));
    public static final int NAILER_MAX_WORKERS = 1;
    public static final int NAILER_VALID_RANGE = 1;

    public static final Holder<PoiType> NAILER_POI = POI_TYPES.register("nailer_poi",
            () -> new PoiType(ImmutableSet.copyOf(Blocks.CRAFTER.getStateDefinition()
                    .getPossibleStates()), NAILER_MAX_WORKERS, NAILER_VALID_RANGE));
    public static final Holder<VillagerProfession> NAILER = VILLAGER_PROFESSIONS.register("nailer_villager",
            () -> new VillagerProfession("nailer_villager", holder -> holder.value() == NAILER_POI.value(),
                    poiTypeHolder -> poiTypeHolder.value() == NAILER_POI.value(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH));
    public static final int LUMBERJACK_MAX_WORKERS = 1;
    public static final int LUMBERJACK_VALID_RANGE = 1;

    public static final Holder<PoiType> LUMBERJACK_POI = POI_TYPES.register("lumberjack_poi",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.LUMBERJACK_LOG.get().getStateDefinition()
                    .getPossibleStates()), LUMBERJACK_MAX_WORKERS, LUMBERJACK_VALID_RANGE));
    public static final Holder<VillagerProfession> LUMBERJACK = VILLAGER_PROFESSIONS.register("lumberjack_villager",
            () -> new VillagerProfession("lumberjack_villager", holder -> holder.value() == LUMBERJACK_POI.value(),
                    poiTypeHolder -> poiTypeHolder.value() == LUMBERJACK_POI.value(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_FLETCHER));

    /** Не привязан ни к одной профессии (ни один villager не пытается его "занять" как рабочее
     * место) - служит только маркером для VillageScan.collectVillagePoi, чтобы building_materials/
     * food_storage находились по точной позиции через PoiManager, а не сканом высоты колонки,
     * который под крышей сарая до склада просто не достаёт (см. отчёт игрока "складов 0"). */
    public static final Holder<PoiType> WORKER_STORAGE_POI = POI_TYPES.register("worker_storage_poi",
            () -> new PoiType(ImmutableSet.<net.minecraft.world.level.block.state.BlockState>builder()
                    .addAll(ModBlocks.BUILDING_MATERIALS.get().getStateDefinition().getPossibleStates())
                    .addAll(ModBlocks.FOOD_STORAGE.get().getStateDefinition().getPossibleStates())
                    .build(), 1, 1));

    /** Профессии, для которых открывается кастомный VillagerProfessionScreen (кнопка на
     * MerchantScreen) - используется и на клиенте (видимость кнопки), и на сервере (проверка
     * перед openMenu, чтобы нельзя было открыть окно пакетом с произвольным id жителя). */
    public static boolean isEligibleForProfessionScreen(@Nullable VillagerProfession profession) {
        return profession == VillagerProfession.FARMER
                || profession == WARRIOR.value()
                || profession == LUMBERJACK.value()
                || profession == NAILER.value();
    }

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}