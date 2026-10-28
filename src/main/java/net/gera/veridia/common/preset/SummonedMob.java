package net.gera.veridia.common.preset;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Моб "как из /summon": id сущности плюс любые её NBT-теги. Создаётся ровно так же, как делает команда, в том
 * числе: если кроме id тегов нет (как у "/summon zombie" без NBT), моб проходит finalizeSpawn, то есть получает
 * случайную экипировку и свойства, как при обычном спавне.
 */
public final class SummonedMob {
    private static final String ID_KEY = "id";

    private SummonedMob() {
    }

    /**
     * Фабрика моба по NBT вида {id:"minecraft:zombie", ...теги сущности...}.
     *
     * @throws IllegalArgumentException нет id или такой сущности не существует
     */
    public static MobFactory fromNbt(CompoundTag nbt) {
        EntityType<?> type = EntityType.by(nbt).orElseThrow(() -> new IllegalArgumentException(nbt.contains(ID_KEY)
                ? "неизвестная сущность «" + Objects.requireNonNull(nbt.get(ID_KEY)).getAsString() + "»"
                : "в описании моба нет id"));
        CompoundTag template = nbt.copy();
        boolean randomizeProperties = template.size() == 1;
        return new MobFactory(type, (level, pos) -> create(level, template.copy(), pos, randomizeProperties));
    }

    /**
     * NBT из строки в формате аргументов /summon без самой команды: "&lt;сущность&gt; [x y z] [{nbt}]". Координаты
     * разбираются только чтобы их пропустить: точку появления выбирает тот, кто создаёт моба.
     */
    public static CompoundTag parseSummonArguments(String text) throws CommandSyntaxException {
        StringReader reader = new StringReader(text.trim());
        ResourceLocation id = ResourceLocation.read(reader);
        while (reader.canRead() && reader.peek() != '{') {
            reader.skip();
        }
        CompoundTag nbt = reader.canRead() ? new TagParser(reader).readStruct() : new CompoundTag();
        nbt.putString(ID_KEY, id.toString());
        return nbt;
    }

    @Nullable
    private static Entity create(ServerLevel level, CompoundTag nbt, Vec3 pos, boolean randomizeProperties) {
        Entity entity = EntityType.loadEntityRecursive(nbt, level, loaded -> {
            loaded.moveTo(pos.x, pos.y, pos.z, loaded.getYRot(), loaded.getXRot());
            return loaded;
        });
        if (randomizeProperties && entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
        }
        return entity;
    }
}
