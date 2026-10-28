package net.gera.veridia.common.mana;

import com.mojang.serialization.Codec;
import net.gera.veridia.Veridia;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModKnowledgeAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Veridia.MOD_ID);

    public static final Supplier<AttachmentType<Boolean>> KNOCKBACK_RING_2_UNLOCKED = register("knockback_ring_2_unlocked");
    public static final Supplier<AttachmentType<Boolean>> KNOCKBACK_RING_3_UNLOCKED = register("knockback_ring_3_unlocked");
    public static final Supplier<AttachmentType<Boolean>> KNOCKBACK_RING_4_UNLOCKED = register("knockback_ring_4_unlocked");
    public static final Supplier<AttachmentType<Boolean>> KNOCKBACK_RING_5_UNLOCKED = register("knockback_ring_5_unlocked");

    public static final Supplier<AttachmentType<Boolean>> SLOW_KILLER_2_UNLOCKED = register("slow_killer_2_unlocked");
    public static final Supplier<AttachmentType<Boolean>> SLOW_KILLER_3_UNLOCKED = register("slow_killer_3_unlocked");
    public static final Supplier<AttachmentType<Boolean>> SLOW_KILLER_4_UNLOCKED = register("slow_killer_4_unlocked");
    public static final Supplier<AttachmentType<Boolean>> SLOW_KILLER_5_UNLOCKED = register("slow_killer_5_unlocked");

    public static final Supplier<AttachmentType<Boolean>> EYE_OF_SAURON_2_UNLOCKED = register("eye_of_sauron_2_unlocked");
    public static final Supplier<AttachmentType<Boolean>> EYE_OF_SAURON_3_UNLOCKED = register("eye_of_sauron_3_unlocked");
    public static final Supplier<AttachmentType<Boolean>> EYE_OF_SAURON_4_UNLOCKED = register("eye_of_sauron_4_unlocked");
    public static final Supplier<AttachmentType<Boolean>> EYE_OF_SAURON_5_UNLOCKED = register("eye_of_sauron_5_unlocked");

    public static final Supplier<AttachmentType<Boolean>> AQUATIC_AMULET_2_UNLOCKED = register("aquatic_amulet_2_unlocked");
    public static final Supplier<AttachmentType<Boolean>> AQUATIC_AMULET_3_UNLOCKED = register("aquatic_amulet_3_unlocked");
    public static final Supplier<AttachmentType<Boolean>> AQUATIC_AMULET_4_UNLOCKED = register("aquatic_amulet_4_unlocked");
    public static final Supplier<AttachmentType<Boolean>> AQUATIC_AMULET_5_UNLOCKED = register("aquatic_amulet_5_unlocked");

    public static final Supplier<AttachmentType<Boolean>> INFERNAL_STAFF_2_UNLOCKED = register("infernal_staff_2_unlocked");
    public static final Supplier<AttachmentType<Boolean>> INFERNAL_STAFF_3_UNLOCKED = register("infernal_staff_3_unlocked");
    public static final Supplier<AttachmentType<Boolean>> INFERNAL_STAFF_4_UNLOCKED = register("infernal_staff_4_unlocked");
    public static final Supplier<AttachmentType<Boolean>> INFERNAL_STAFF_5_UNLOCKED = register("infernal_staff_5_unlocked");

    private static Supplier<AttachmentType<Boolean>> register(String name) {
        return ATTACHMENT_TYPES.register(name,
                () -> AttachmentType.builder(() -> false)
                        .serialize(Codec.BOOL)
                        .sync(ByteBufCodecs.BOOL)
                        .build());
    }

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
