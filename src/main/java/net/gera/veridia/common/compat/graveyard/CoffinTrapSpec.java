package net.gera.veridia.common.compat.graveyard;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.gera.veridia.common.preset.MobFactory;
import net.gera.veridia.common.preset.MobPresets;
import net.gera.veridia.common.preset.SummonedMob;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Настройка ловушки гроба из NBT его блок-сущности (задаётся в /setblock, в block_entity_data предмета или в
 * структуре):
 * <ul>
 *   <li>Mob - моб: {id:"minecraft:zombie", ...теги как у /summon} либо строка "zombie ~ ~ ~ {теги}" в формате
 *       аргументов /summon;</li>
 *   <li>PresetMob - имя пресета вместо Mob (встроенного или из /preset create), см. MobPresets;</li>
 *   <li>AppearTime - за сколько тиков моб вылезает из гроба (по умолчанию DEFAULT_APPEAR_TICKS);</li>
 *   <li>AppearParticle - id частицы, например "minecraft:ash" (по умолчанию она же); берутся только частицы без
 *       параметров, их появление всегда одинаковое.</li>
 * </ul>
 * Если заданы и Mob, и PresetMob, берётся Mob. Нет ни того, ни другого - гроб обычный.
 */
public record CoffinTrapSpec(MobFactory mob, int appearTicks, SimpleParticleType particle) {
    public static final String MOB_KEY = "Mob";
    public static final String PRESET_KEY = "PresetMob";
    public static final String APPEAR_TIME_KEY = "AppearTime";
    public static final String PARTICLE_KEY = "AppearParticle";

    /** Все ключи ловушки в NBT блок-сущности. */
    public static final List<String> TRAP_KEYS = List.of(MOB_KEY, PRESET_KEY, APPEAR_TIME_KEY, PARTICLE_KEY);

    public static final int DEFAULT_APPEAR_TICKS = 40;
    private static final int MIN_APPEAR_TICKS = 1;
    private static final int MAX_APPEAR_TICKS = 20 * 60;
    private static final ResourceLocation DEFAULT_PARTICLE = ResourceLocation.withDefaultNamespace("ash");

    /** Есть ли в данных блок-сущности ловушка (Mob или PresetMob). */
    public static boolean isArmed(CompoundTag trap) {
        return trap.contains(MOB_KEY) || trap.contains(PRESET_KEY);
    }

    /**
     * Разбирает данные ловушки.
     *
     * @throws IllegalArgumentException данные неверны (сообщение - что именно, для игрока и лога)
     */
    public static CoffinTrapSpec read(CompoundTag trap) {
        return new CoffinTrapSpec(readMob(trap), readAppearTicks(trap), readParticle(trap));
    }

    private static MobFactory readMob(CompoundTag trap) {
        if (trap.contains(MOB_KEY, Tag.TAG_COMPOUND)) {
            return SummonedMob.fromNbt(trap.getCompound(MOB_KEY));
        }
        if (trap.contains(MOB_KEY, Tag.TAG_STRING)) {
            return SummonedMob.fromNbt(parseSummonText(trap.getString(MOB_KEY)));
        }
        if (trap.contains(MOB_KEY)) {
            throw new IllegalArgumentException(MOB_KEY + " должен быть {id:\"...\", ...} или строкой как в /summon");
        }
        String presetName = trap.getString(PRESET_KEY);
        return MobPresets.find(presetName)
                .orElseThrow(() -> new IllegalArgumentException("нет пресета «" + presetName + "» (список: /preset list)"));
    }

    private static CompoundTag parseSummonText(String text) {
        try {
            return SummonedMob.parseSummonArguments(text);
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException(MOB_KEY + ": " + e.getMessage());
        }
    }

    private static int readAppearTicks(CompoundTag trap) {
        int ticks = trap.contains(APPEAR_TIME_KEY, Tag.TAG_ANY_NUMERIC) ? trap.getInt(APPEAR_TIME_KEY) : DEFAULT_APPEAR_TICKS;
        return Mth.clamp(ticks, MIN_APPEAR_TICKS, MAX_APPEAR_TICKS);
    }

    private static SimpleParticleType readParticle(CompoundTag trap) {
        String name = trap.contains(PARTICLE_KEY, Tag.TAG_STRING) ? trap.getString(PARTICLE_KEY) : DEFAULT_PARTICLE.toString();
        ResourceLocation id = ResourceLocation.tryParse(name);
        ParticleType<?> type = id == null ? null : BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null);
        if (type instanceof SimpleParticleType simple) {
            return simple;
        }
        throw new IllegalArgumentException(PARTICLE_KEY + ": нет частицы без параметров «" + name + "»");
    }
}
