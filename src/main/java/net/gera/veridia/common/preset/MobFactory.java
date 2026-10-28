package net.gera.veridia.common.preset;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Умеет создать моба заданного типа в нужной точке (как команда /summon), но не добавляет его в мир - это делает
 * вызывающий. Тип известен заранее, чтобы можно было судить о мобе, не создавая его.
 */
public record MobFactory(EntityType<?> type, Creator creator) {
    @FunctionalInterface
    public interface Creator {
        /** Готовый моб в точке pos или null, если создать не получилось. */
        @Nullable
        Entity create(ServerLevel level, Vec3 pos);
    }

    @Nullable
    public Entity create(ServerLevel level, Vec3 pos) {
        return creator.create(level, pos);
    }

    /**
     * Пропадёт ли такой моб сразу, едва появившись, если сложность - Мирная: так делают все не дружелюбные
     * (враждебные) мобы, как и у спавнера с теми же правилами.
     */
    public boolean vanishesInPeaceful() {
        return !type.getCategory().isFriendly();
    }
}
