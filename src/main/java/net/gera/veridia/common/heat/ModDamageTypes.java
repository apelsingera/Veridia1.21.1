package net.gera.veridia.common.heat;

import net.gera.veridia.Veridia;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Типы урона Veridia. JSON создаёт датаген (ModDatapackProvider): data/veridia/damage_type. Что урон от жара обходит
 * броню, говорит тег data/minecraft/tags/damage_type/bypasses_armor.json. Текст смерти - death.attack.veridia.heat.
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> HEAT = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "heat"));

    private ModDamageTypes() {
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(HEAT, new DamageType("veridia.heat", DamageScaling.NEVER, 0.0F));
    }

    /** Урон от жара: от лавовых озёр Пепельных равнин и во всём Незере (см. HeatHandler). */
    public static DamageSource heat(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HEAT));
    }
}
