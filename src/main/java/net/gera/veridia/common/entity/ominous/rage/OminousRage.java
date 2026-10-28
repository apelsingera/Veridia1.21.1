package net.gera.veridia.common.entity.ominous.rage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ярость зловещих воинов в измерении. Начинается, когда духовный рунный камень набрал молитвы, и длится, пока играет
 * трек the_big_gundown ({@link #DURATION_TICKS}). Пока она идёт, все воины в пепельных биомах разъярены
 * (см. OminousWarriorEntity), а игроки в пепельных биомах видят фиолетовое небо и слышат трек.
 * <p>
 * Хранится в сохранении мира (время конца по часам мира), клиенты узнают о ней из {@link OminousRagePayload}.
 */
public final class OminousRage extends SavedData {
    private static final String NAME = "veridia_ominous_rage";
    private static final String END_TIME_KEY = "EndGameTime";
    // Длина трека the_big_gundown.ogg - 4:20.8
    public static final int DURATION_TICKS = 5216;

    private long endGameTime;

    private OminousRage() {
    }

    private static OminousRage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(OminousRage::new, OminousRage::load, null), NAME);
    }

    public static boolean isActive(ServerLevel level) {
        return level.getGameTime() < get(level).endGameTime;
    }

    /** Сколько тиков ярости осталось (0 - не идёт). */
    public static long ticksLeft(ServerLevel level) {
        return Math.max(0L, get(level).endGameTime - level.getGameTime());
    }

    /**
     * Начинает ярость (или начинает её заново, если она уже шла) и сообщает всем игрокам измерения.
     * Возвращает время конца ярости по часам мира.
     */
    public static long start(ServerLevel level) {
        OminousRage rage = get(level);
        rage.endGameTime = level.getGameTime() + DURATION_TICKS;
        rage.setDirty();
        PacketDistributor.sendToPlayersInDimension(level, new OminousRagePayload(rage.endGameTime));
        return rage.endGameTime;
    }

    /**
     * Ярость обрывается раньше времени (сломали камень, с которого она началась, или команда) и сообщает об этом
     * игрокам. Активный камень гаснет сам на следующем тике.
     */
    public static void stop(ServerLevel level) {
        OminousRage rage = get(level);
        if (level.getGameTime() >= rage.endGameTime) {
            return;
        }
        rage.endGameTime = level.getGameTime();
        rage.setDirty();
        PacketDistributor.sendToPlayersInDimension(level, new OminousRagePayload(rage.endGameTime));
    }

    /** Игрок зашёл в мир или сменил измерение: сообщаем ему, идёт ли ярость там, где он теперь. */
    public static void sendState(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OminousRagePayload(get(player.serverLevel()).endGameTime));
    }

    private static OminousRage load(CompoundTag tag, HolderLookup.Provider registries) {
        OminousRage rage = new OminousRage();
        rage.endGameTime = tag.getLong(END_TIME_KEY);
        return rage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong(END_TIME_KEY, endGameTime);
        return tag;
    }
}
