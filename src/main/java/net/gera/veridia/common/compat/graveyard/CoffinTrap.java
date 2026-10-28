package net.gera.veridia.common.compat.graveyard;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Ловушка в гробе Graveyard: пока в блок-сущности лежат данные Mob/PresetMob (см. {@link CoffinTrapSpec}), первая же
 * попытка открыть гроб запускает {@link CoffinEmergence} вместо открытия меню. Когда моб вылез, данные снимаются,
 * и гроб дальше открывается как обычно. Вызывается из SarcophagusBlockMixin.
 */
public final class CoffinTrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CoffinTrap() {
    }

    /**
     * Попытка открыть гроб игроком.
     *
     * @return true - открытие перехвачено (идёт или только что началась анимация), меню открывать не надо;
     *         false - гроб без ловушки (или с неверной настройкой), открывается как обычно
     */
    public static boolean onOpenAttempt(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        CoffinSite site = CoffinSite.of(pos, state);
        if (CoffinEmergences.isBusy(level, site)) {
            return true;
        }
        Optional<CoffinTrapHolder> armed = holders(level, site).filter(h -> CoffinTrapSpec.isArmed(h.getCoffinTrap())).findFirst();
        if (armed.isEmpty()) {
            return false;
        }
        CoffinTrapSpec spec;
        try {
            spec = CoffinTrapSpec.read(armed.get().getCoffinTrap());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ловушка гроба в {} настроена неверно: {}", site.foot(), e.getMessage());
            player.displayClientMessage(Component.literal("Гроб: " + e.getMessage()), true);
            return false;
        }
        // На Мирной сложности враждебный моб исчез бы сразу после появления, а ловушка пропала бы впустую: вместо
        // этого говорим об этом и открываем гроб как обычный, ловушка остаётся до смены сложности
        if (level.getDifficulty() == Difficulty.PEACEFUL && spec.mob().vanishesInPeaceful()) {
            player.displayClientMessage(Component.literal("Гроб: на Мирной сложности враждебные мобы не появляются"), true);
            return false;
        }
        CoffinEmergences.start(new CoffinEmergence(level, site, state.getBlock(), spec, player.getUUID()));
        return true;
    }

    /** Снимает ловушку с обеих половин гроба (данные могли быть заданы на любой из них). */
    static void disarm(ServerLevel level, CoffinSite site) {
        holders(level, site).forEach(CoffinTrapHolder::clearCoffinTrap);
    }

    private static Stream<CoffinTrapHolder> holders(ServerLevel level, CoffinSite site) {
        return List.of(site.foot(), site.head()).stream()
                .filter(level::isLoaded)
                .map(level::getBlockEntity)
                .filter(CoffinTrapHolder.class::isInstance)
                .map(CoffinTrapHolder.class::cast);
    }
}
