package net.gera.veridia.common.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.gera.veridia.common.aibuff.handlers.WitherBuffHandler;
import net.gera.veridia.common.aibuff.levelzombie.LevelHuskConfig;
import net.gera.veridia.common.aibuff.levelzombie.LevelHuskHandler;
import net.gera.veridia.common.aibuff.levelzombie.LevelZombieConfig;
import net.gera.veridia.common.aibuff.levelzombie.LevelZombieHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Именованные мобы ("пресеты"), которых можно просто назвать вместо длинных тегов, например в гробе
 * (PresetMob:"zombie_level_20"). Два вида:
 * <ul>
 *   <li>встроенные - описаны кодом, потому что мобы мода (уровни зомби, визербафф) настраиваются не одним NBT;</li>
 *   <li>свои - создаются командой /preset create и хранятся в config/veridia_mob_presets.json (общий для всех
 *       миров, чтобы структуры с PresetMob работали везде); файл можно править руками: имя -> NBT одной строкой.</li>
 * </ul>
 */
public final class MobPresets {
    private static final Logger LOGGER = LogUtils.getLogger();
    // Без экранирования кавычек и апострофов: SNBT в файле остаётся читаемым и правится руками
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "veridia_mob_presets.json";

    private static final Map<String, MobFactory> BUILT_IN = new LinkedHashMap<>();
    private static final List<String> BUILT_IN_SUMMARY = new ArrayList<>();
    private static final Map<String, CompoundTag> CUSTOM = new TreeMap<>();
    private static boolean customLoaded;

    static {
        registerLevels("zombie_level_", LevelZombieConfig.MIN_LEVEL, LevelZombieConfig.MAX_LEVEL,
                level -> finalizedMob(EntityType.ZOMBIE, zombie -> LevelZombieHandler.applyLevel(zombie, level)));
        registerLevels("husk_level_", LevelHuskConfig.MIN_LEVEL, LevelHuskConfig.MAX_LEVEL,
                level -> finalizedMob(EntityType.HUSK, husk -> LevelHuskHandler.applyLevel(husk, level)));
        register("witherbuff", finalizedMob(EntityType.WITHER_SKELETON, skeleton -> skeleton.addTag(WitherBuffHandler.TAG)));
        register("witherbreaker", finalizedMob(EntityType.WITHER_SKELETON, skeleton -> skeleton.addTag(WitherBuffHandler.BREAKER_TAG)));
    }

    private MobPresets() {
    }

    private static void register(String name, MobFactory factory) {
        BUILT_IN.put(name, factory);
        BUILT_IN_SUMMARY.add(name);
    }

    private static void registerLevels(String prefix, int minLevel, int maxLevel, IntFunction<MobFactory> factoryForLevel) {
        for (int level = minLevel; level <= maxLevel; level++) {
            BUILT_IN.put(prefix + level, factoryForLevel.apply(level));
        }
        BUILT_IN_SUMMARY.add(prefix + minLevel + ".." + maxLevel);
    }

    // Моб так же, как из яйца мода: создаётся, проходит finalizeSpawn и только потом получает свою настройку
    private static <T extends Mob> MobFactory finalizedMob(EntityType<T> type, Consumer<T> customize) {
        return new MobFactory(type, (level, pos) -> {
            T mob = type.create(level);
            if (mob == null) {
                return null;
            }
            mob.moveTo(pos.x, pos.y, pos.z);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
            customize.accept(mob);
            return mob;
        });
    }

    /** Пресет по имени: сначала встроенный, потом свой. */
    public static Optional<MobFactory> find(String name) {
        MobFactory builtIn = BUILT_IN.get(name);
        if (builtIn != null) {
            return Optional.of(builtIn);
        }
        loadCustomOnce();
        CompoundTag nbt = CUSTOM.get(name);
        if (nbt == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SummonedMob.fromNbt(nbt));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Пресет моба '{}' не работает: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    public static boolean isBuiltIn(String name) {
        return BUILT_IN.containsKey(name);
    }

    /** Встроенные пресеты одной строкой-подсказкой: "zombie_level_1..35", "witherbuff" и так далее. */
    public static List<String> builtInSummary() {
        return BUILT_IN_SUMMARY;
    }

    public static Collection<String> customNames() {
        loadCustomOnce();
        return CUSTOM.keySet();
    }

    /**
     * Сохраняет свой пресет (или заменяет одноимённый свой).
     *
     * @param nbt NBT вида {id:"minecraft:zombie", ...}
     * @throws IllegalArgumentException имя занято встроенным пресетом или NBT не описывает существующую сущность
     */
    public static void saveCustom(String name, CompoundTag nbt) {
        if (isBuiltIn(name)) {
            throw new IllegalArgumentException("имя «" + name + "» занято встроенным пресетом");
        }
        SummonedMob.fromNbt(nbt);
        loadCustomOnce();
        CUSTOM.put(name, nbt.copy());
        writeCustom();
    }

    public static boolean removeCustom(String name) {
        loadCustomOnce();
        boolean removed = CUSTOM.remove(name) != null;
        if (removed) {
            writeCustom();
        }
        return removed;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static void loadCustomOnce() {
        if (customLoaded) {
            return;
        }
        customLoaded = true;
        Path file = file();
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }
            json.entrySet().forEach(entry -> readCustom(entry.getKey(), entry.getValue().getAsString()));
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Не удалось прочитать {}: {}", file, e.getMessage());
        }
    }

    // Один плохой пресет в файле не должен ронять остальные
    private static void readCustom(String name, String snbt) {
        try {
            CUSTOM.put(name, TagParser.parseTag(snbt));
        } catch (CommandSyntaxException e) {
            LOGGER.warn("Пресет моба '{}' в {} пропущен: {}", name, FILE_NAME, e.getMessage());
        }
    }

    private static void writeCustom() {
        JsonObject json = new JsonObject();
        CUSTOM.forEach((name, nbt) -> json.addProperty(name, nbt.toString()));
        try (Writer writer = Files.newBufferedWriter(file())) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            LOGGER.warn("Не удалось сохранить {}: {}", file(), e.getMessage());
        }
    }
}
