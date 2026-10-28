package net.gera.veridia.common.compat.graveyard;

import net.gera.veridia.Veridia;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Все идущие сейчас вылезания мобов из гробов: тикает их и следит, чтобы застрявшие мобы не остались замороженными. */
@EventBusSubscriber(modid = Veridia.MOD_ID)
final class CoffinEmergences {
    private static final List<CoffinEmergence> ACTIVE = new ArrayList<>();

    private CoffinEmergences() {
    }

    static void start(CoffinEmergence emergence) {
        ACTIVE.add(emergence);
        emergence.begin();
    }

    /** Идёт ли уже вылезание из этого гроба. */
    static boolean isBusy(ServerLevel level, CoffinSite site) {
        return ACTIVE.stream().anyMatch(emergence -> emergence.occupies(level, site));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && !ACTIVE.isEmpty()) {
            ACTIVE.removeIf(emergence -> emergence.isIn(level) && emergence.tick());
        }
    }

    // Мир остановили (в одиночной игре сервер потом создаётся заново): задачи прошлого мира уже ни к чему
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && ACTIVE.stream().noneMatch(emergence -> emergence.isMob(event.getEntity()))) {
            CoffinEmergence.restoreIfStuck(event.getEntity());
        }
    }
}
