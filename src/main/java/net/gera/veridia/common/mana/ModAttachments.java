package net.gera.veridia.common.mana;

import com.mojang.serialization.Codec;
import net.gera.veridia.Veridia;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Veridia.MOD_ID);

    public static final Supplier<AttachmentType<Integer>> MANA = ATTACHMENT_TYPES.register(
            "mana",
            () -> AttachmentType.builder(() -> ManaProgression.BASE_MAX_MANA)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final Supplier<AttachmentType<Integer>> MANA_UPGRADE_PROGRESS = ATTACHMENT_TYPES.register(
            "mana_upgrade_progress",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final Supplier<AttachmentType<Float>> MANA_REGEN_ACCUMULATOR = ATTACHMENT_TYPES.register(
            "mana_regen_accumulator",
            () -> AttachmentType.builder(() -> 0.0F)
                    .serialize(Codec.FLOAT)
                    .build()
    );

    /** Временный бонус к максимальной мане от еды (см. ManaCapacityFoodItem) - активен, пока не истёк MANA_MAX_BONUS_EXPIRES_AT. */
    public static final Supplier<AttachmentType<Integer>> MANA_MAX_BONUS = ATTACHMENT_TYPES.register(
            "mana_max_bonus",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build()
    );

    /** Игровое время (level.getGameTime()), после которого MANA_MAX_BONUS больше не действует. */
    public static final Supplier<AttachmentType<Long>> MANA_MAX_BONUS_EXPIRES_AT = ATTACHMENT_TYPES.register(
            "mana_max_bonus_expires_at",
            () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG)
                    .sync(ByteBufCodecs.VAR_LONG)
                    .build()
    );

    /** Пока false - вся система маны (бар, реген, трата на способности) выключена для игрока. */
    public static final Supplier<AttachmentType<Boolean>> MANA_UNLOCKED = ATTACHMENT_TYPES.register(
            "mana_unlocked",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .build()
    );

    /** /veridiadebug mana god on|off - отладочный тумблер, намеренно не сохраняется между рестартами сервера. */
    public static final Supplier<AttachmentType<Boolean>> MANA_GOD_MODE = ATTACHMENT_TYPES.register(
            "mana_god_mode",
            () -> AttachmentType.builder(() -> false).build()
    );

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
