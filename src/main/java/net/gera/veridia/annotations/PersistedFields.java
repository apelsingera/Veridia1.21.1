package net.gera.veridia.annotations;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PersistedFields {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<Class<?>, List<Entry>> CACHE = new ConcurrentHashMap<>();

    private record Entry(Field field, String key, String[] aliases) {}

    private PersistedFields() {}

    public static void save(Object owner, CompoundTag tag) {
        List<Entry> entries = fieldsOf(owner.getClass());
        if (entries.isEmpty()) {
            return;
        }
        String cls = owner.getClass().getSimpleName();

        for (Entry e : entries) {
            Field f = e.field();
            String key = e.key();
            try {
                Object v = f.get(owner);
                if (v == null) {
                    continue;
                }
                Class<?> t = f.getType();
                if (t == boolean.class || t == Boolean.class) tag.putBoolean(key, (Boolean) v);
                else if (t == byte.class || t == Byte.class) tag.putByte(key, (Byte) v);
                else if (t == short.class || t == Short.class) tag.putShort(key, (Short) v);
                else if (t == int.class || t == Integer.class) tag.putInt(key, (Integer) v);
                else if (t == long.class || t == Long.class) tag.putLong(key, (Long) v);
                else if (t == float.class || t == Float.class) tag.putFloat(key, (Float) v);
                else if (t == double.class || t == Double.class) tag.putDouble(key, (Double) v);
                else if (t == String.class) tag.putString(key, (String) v);
                else if (t == UUID.class) tag.putUUID(key, (UUID) v);
                else if (t == BlockPos.class) tag.putLong(key, ((BlockPos) v).asLong());
                else if (t == ResourceLocation.class) tag.putString(key, v.toString());
                else if (Enum.class.isAssignableFrom(t)) tag.putString(key, ((Enum<?>) v).name());
            } catch (Exception ex) {
                LOG.error("[persisted] saving {}.{} failed", cls, f.getName(), ex);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void load(Object owner, CompoundTag tag) {
        List<Entry> entries = fieldsOf(owner.getClass());
        if (entries.isEmpty()) {
            return;
        }
        String cls = owner.getClass().getSimpleName();

        for (Entry e : entries) {
            Field f = e.field();
            Class<?> t = f.getType();

            String key = presentKey(tag, e, t, cls);
            if (key == null) {
                continue;
            }
            try {
                if (t == boolean.class || t == Boolean.class) f.set(owner, tag.getBoolean(key));
                else if (t == byte.class || t == Byte.class) f.set(owner, tag.getByte(key));
                else if (t == short.class || t == Short.class) f.set(owner, tag.getShort(key));
                else if (t == int.class || t == Integer.class) f.set(owner, tag.getInt(key));
                else if (t == long.class || t == Long.class) f.set(owner, tag.getLong(key));
                else if (t == float.class || t == Float.class) f.set(owner, tag.getFloat(key));
                else if (t == double.class || t == Double.class) f.set(owner, tag.getDouble(key));
                else if (t == String.class) f.set(owner, tag.getString(key));
                else if (t == UUID.class) f.set(owner, tag.getUUID(key));
                else if (t == BlockPos.class) f.set(owner, BlockPos.of(tag.getLong(key)));
                else if (t == ResourceLocation.class) {
                    ResourceLocation rl = ResourceLocation.tryParse(tag.getString(key));
                    if (rl != null) f.set(owner, rl);
                } else if (Enum.class.isAssignableFrom(t)) {
                    try {
                        f.set(owner, Enum.valueOf((Class) t, tag.getString(key)));
                    } catch (IllegalArgumentException ignored) {
                        LOG.warn("[persisted] {}.{}: unknown enum constant '{}'", cls, f.getName(), tag.getString(key));
                    }
                }
            } catch (Exception ex) {
                LOG.error("[persisted] loading {}.{} failed", cls, f.getName(), ex);
            }
        }
    }

    public static List<String> validate(Class<?> type) {
        List<String> problems = new ArrayList<>();
        scan(type, problems::add);
        return problems;
    }

    private static String presentKey(CompoundTag tag, Entry e, Class<?> type, String cls) {
        if (tag.contains(e.key())) {
            return typeMatches(tag, e.key(), type) ? e.key() : mismatch(e.key(), e.field(), cls);
        }
        for (String alias : e.aliases()) {
            if (tag.contains(alias)) {
                return typeMatches(tag, alias, type) ? alias : mismatch(alias, e.field(), cls);
            }
        }
        return null;
    }

    private static String mismatch(String key, Field f, String cls) {
        LOG.error("[persisted] {}.{}: key '{}' holds a different tag type in the save — default kept",
                cls, f.getName(), key);
        return null;
    }

    private static boolean typeMatches(CompoundTag tag, String key, Class<?> type) {
        if (type == UUID.class) return tag.hasUUID(key);
        if (type == BlockPos.class) return tag.contains(key, Tag.TAG_LONG);
        if (type == String.class || type == ResourceLocation.class || Enum.class.isAssignableFrom(type)) {
            return tag.contains(key, Tag.TAG_STRING);
        }
        return tag.contains(key, Tag.TAG_ANY_NUMERIC);
    }

    private static boolean isSupported(Class<?> t) {
        return t.isPrimitive()
                || Number.class.isAssignableFrom(t)
                || t == Boolean.class
                || t == String.class
                || t == UUID.class
                || t == BlockPos.class
                || t == ResourceLocation.class
                || Enum.class.isAssignableFrom(t);
    }

    private static List<Entry> fieldsOf(Class<?> type) {
        return CACHE.computeIfAbsent(type, root -> scan(root, problem -> LOG.error("[persisted] {}", problem)));
    }
    private static List<Entry> scan(Class<?> root, Consumer<String> problems) {
        List<Entry> out = new ArrayList<>();
        Map<String, String> keyOwners = new HashMap<>();

        for (Class<?> c = root; c != null && c != Object.class; c = c.getSuperclass()) {
            String prefix = c.isAnnotationPresent(PersistedPrefix.class)
                    ? c.getAnnotation(PersistedPrefix.class).value() + "."
                    : "";

            for (Field f : c.getDeclaredFields()) {
                Persisted meta = f.getAnnotation(Persisted.class);
                if (meta == null) {
                    continue;
                }
                String where = c.getSimpleName() + "." + f.getName();

                if (Modifier.isStatic(f.getModifiers())) {
                    problems.accept(where + ": @Persisted on a static field — ignored");
                    continue;
                }
                if (Modifier.isFinal(f.getModifiers())) {
                    problems.accept(where + ": @Persisted on a final field — ignored (load could never write it back)");
                    continue;
                }
                if (!isSupported(f.getType())) {
                    problems.accept(where + ": type " + f.getType().getName() + " is not supported — save it by hand");
                    continue;
                }

                String key = prefix + (meta.value().isEmpty() ? f.getName() : meta.value());
                String previous = keyOwners.put(key, where);
                if (previous != null) {
                    problems.accept("duplicate key '" + key + "': " + previous + " and " + where
                            + " — one silently overwrites the other");
                }

                String[] aliases = meta.aliases();
                String[] prefixed = new String[aliases.length];
                for (int i = 0; i < aliases.length; i++) {
                    prefixed[i] = prefix + aliases[i];
                }

                f.setAccessible(true);
                out.add(new Entry(f, key, prefixed));
            }
        }
        return out;
    }
}