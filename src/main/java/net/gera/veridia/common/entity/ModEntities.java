package net.gera.veridia.common.entity;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.rustynail.RustyNailEntity;
import net.gera.veridia.common.item.custom.heavy_projectile.HeavyProjectileEntity;
import net.gera.veridia.common.entity.mutant.MutantEntity;
import net.gera.veridia.common.entity.ominous.BrutalOminousWarriorEntity;
import net.gera.veridia.common.entity.ominous.OminousWarriorEntity;
import net.gera.veridia.common.entity.cinderroamer.CinderRoamerEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Veridia.MOD_ID);

    public static final Supplier<EntityType<HeavyProjectileEntity>> HEAVY_PROJECTILE =
            ENTITY_TYPES.register("heavy_projectile", () -> EntityType.Builder.<HeavyProjectileEntity>of(HeavyProjectileEntity::new, MobCategory.MISC)
                    .sized(0.45f, 0.45f).build("heavy_projectile"));
    public static final Supplier<EntityType<RustyNailEntity>> RUSTY_NAIL =
            ENTITY_TYPES.register("rusty_nail", () -> EntityType.Builder.<RustyNailEntity>of(RustyNailEntity::new, MobCategory.MISC)
                    .sized(0.2f, 0.5f)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build("rusty_nail"));
    public static final Supplier<EntityType<MutantEntity>> MUTANT =
            ENTITY_TYPES.register("mutant", () -> EntityType.Builder.of(MutantEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.2F)
                    .clientTrackingRange(16)
                    .build("mutant"));
    public static final Supplier<EntityType<OminousWarriorEntity>> OMINOUS_WARRIOR =
            ENTITY_TYPES.register("ominous_warrior", () -> EntityType.Builder.of(OminousWarriorEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.95F)
                    .eyeHeight(1.7F)
                    .clientTrackingRange(10)
                    .build("ominous_warrior"));
    // Высокий зловещий воин; пригнувшись ниже (см. BrutalOminousWarriorEntity.DUCKING_HEIGHT)
    public static final Supplier<EntityType<BrutalOminousWarriorEntity>> BRUTAL_OMINOUS_WARRIOR =
            ENTITY_TYPES.register("brutal_ominous_warrior", () -> EntityType.Builder.of(BrutalOminousWarriorEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.2F)
                    .eyeHeight(1.95F)
                    .clientTrackingRange(10)
                    .build("brutal_ominous_warrior"));
    public static final Supplier<EntityType<CinderRoamerEntity>> CINDER_ROAMER =
            ENTITY_TYPES.register("cinder_roamer", () -> EntityType.Builder.of(CinderRoamerEntity::new, MobCategory.CREATURE)
                    .sized(1.9F, 1.8F)
                    .eyeHeight(1.4F)
                    .clientTrackingRange(10)
                    .build("cinder_roamer"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    @EventBusSubscriber(modid = Veridia.MOD_ID)
    public static class AttributeRegistration {
        @SubscribeEvent
        public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.MUTANT.get(), MutantEntity.createAttributes().build());
            event.put(ModEntities.OMINOUS_WARRIOR.get(), OminousWarriorEntity.createAttributes().build());
            event.put(ModEntities.BRUTAL_OMINOUS_WARRIOR.get(), BrutalOminousWarriorEntity.createAttributes().build());
            event.put(ModEntities.CINDER_ROAMER.get(), CinderRoamerEntity.createAttributes().build());
        }

        // Естественный спавн зловещих воинов (в пепельных биомах, см. ModBiomes): на земле, в темноте, как у монстров
        @SubscribeEvent
        public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
            event.register(ModEntities.OMINOUS_WARRIOR.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(ModEntities.BRUTAL_OMINOUS_WARRIOR.get(), SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
}