package net.gera.veridia.common.villager.storage;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.BuildingMaterialsBlockEntity;
import net.gera.veridia.common.block.entity.types.FoodStorageBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.UUID;
@SuppressWarnings("all")
public final class VillagerEmployment {

    private VillagerEmployment() {
    }

    private static final String WORK_ENABLED_KEY = "veridia_work_enabled";
    private static final String FIRED_PROFESSION_KEY = "veridia_fired_profession";
    private static final String FIRED_UNTIL_KEY = "veridia_fired_until";
    public static final int FIRE_COOLDOWN_TICKS = 20 * 60;
    public static final int GLOW_DURATION_TICKS = 20 * 8;

    private static final String LUMBERJACK_JOB_SITE_POS_KEY = "veridia_lumberjack_jobsite_pos";
    private static final String LUMBERJACK_JOB_SITE_DIM_KEY = "veridia_lumberjack_jobsite_dim";

    public static boolean isWorkEnabled(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        return !data.contains(WORK_ENABLED_KEY) || data.getBoolean(WORK_ENABLED_KEY);
    }

    public static void setWorkEnabled(Villager villager, boolean enabled) {
        villager.getPersistentData().putBoolean(WORK_ENABLED_KEY, enabled);
    }

    public static boolean isProfessionOnCooldown(Villager villager, VillagerProfession profession, ServerLevel level) {
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(FIRED_UNTIL_KEY)) return false;
        if (level.getGameTime() >= data.getLong(FIRED_UNTIL_KEY)) return false;

        ResourceLocation firedId = ResourceLocation.tryParse(data.getString(FIRED_PROFESSION_KEY));
        ResourceLocation thisId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        return firedId != null && firedId.equals(thisId);
    }

    public static void glow(ServerLevel level, UUID villagerId, int durationTicks) {
        if (level.getEntity(villagerId) instanceof Villager villager) {
            villager.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0));
        }
    }

    public static void fire(Villager villager, ServerLevel level) {
        VillagerProfession current = villager.getVillagerData().getProfession();
        ResourceLocation professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(current);
        CompoundTag data = villager.getPersistentData();

        data.remove(LUMBERJACK_JOB_SITE_POS_KEY);
        data.remove(LUMBERJACK_JOB_SITE_DIM_KEY);
        villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);

        VillagerStorageBinding.release(villager, level, StorageKind.MATERIALS,
                ModBlocks.BUILDING_MATERIALS.get(), BuildingMaterialsBlockEntity.class);
        VillagerStorageBinding.release(villager, level, StorageKind.FOOD,
                ModBlocks.FOOD_STORAGE.get(), FoodStorageBlockEntity.class);

        villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.NONE));
        setWorkEnabled(villager, true);

        if (professionId != null && current != VillagerProfession.NONE) {
            data.putString(FIRED_PROFESSION_KEY, professionId.toString());
            data.putLong(FIRED_UNTIL_KEY, level.getGameTime() + FIRE_COOLDOWN_TICKS);
        }
    }
}
