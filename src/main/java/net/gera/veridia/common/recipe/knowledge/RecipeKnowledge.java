package net.gera.veridia.common.recipe.knowledge;

import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.mana.ModKnowledgeAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class RecipeKnowledge {

    public record KnowledgeTier(String id, String treeId, int tierIndex, Supplier<Item> itemSupplier,
                                 Supplier<AttachmentType<Boolean>> attachment, int requiredNeuronClumps) {
        public Item resultItem() {
            return itemSupplier.get();
        }

        public boolean isUnlocked(Player player) {
            return player.getData(attachment);
        }

        public void unlock(Player player) {
            player.setData(attachment, true);
        }
    }

    private static KnowledgeTier tier(String id, String treeId, int tierIndex, Supplier<Item> item,
                                       Supplier<AttachmentType<Boolean>> attachment, int requiredNeuronClumps) {
        return new KnowledgeTier(id, treeId, tierIndex, item, attachment, requiredNeuronClumps);
    }

    public static final List<KnowledgeTier> KNOCKBACK_RING_TREE = List.of(
            tier("knockback_ring_2", "knockback_ring", 0, ModItems.KNOCKBACK_RING_2,
                    ModKnowledgeAttachments.KNOCKBACK_RING_2_UNLOCKED, 16),
            tier("knockback_ring_3", "knockback_ring", 1, ModItems.KNOCKBACK_RING_3,
                    ModKnowledgeAttachments.KNOCKBACK_RING_3_UNLOCKED, 32),
            tier("knockback_ring_4", "knockback_ring", 2, ModItems.KNOCKBACK_RING_4,
                    ModKnowledgeAttachments.KNOCKBACK_RING_4_UNLOCKED, 48),
            tier("knockback_ring_5", "knockback_ring", 3, ModItems.KNOCKBACK_RING_5,
                    ModKnowledgeAttachments.KNOCKBACK_RING_5_UNLOCKED, 64)
    );

    public static final List<KnowledgeTier> SLOW_KILLER_TREE = List.of(
            tier("slow_killer_2", "slow_killer", 0, ModItems.SLOW_KILLER_2,
                    ModKnowledgeAttachments.SLOW_KILLER_2_UNLOCKED, 16),
            tier("slow_killer_3", "slow_killer", 1, ModItems.SLOW_KILLER_3,
                    ModKnowledgeAttachments.SLOW_KILLER_3_UNLOCKED, 32),
            tier("slow_killer_4", "slow_killer", 2, ModItems.SLOW_KILLER_4,
                    ModKnowledgeAttachments.SLOW_KILLER_4_UNLOCKED, 48),
            tier("slow_killer_5", "slow_killer", 3, ModItems.SLOW_KILLER_5,
                    ModKnowledgeAttachments.SLOW_KILLER_5_UNLOCKED, 64)
    );

    public static final List<KnowledgeTier> EYE_OF_SAURON_TREE = List.of(
            tier("eye_of_sauron_2", "eye_of_sauron", 0, ModItems.EYE_OF_SAURON_2,
                    ModKnowledgeAttachments.EYE_OF_SAURON_2_UNLOCKED, 16),
            tier("eye_of_sauron_3", "eye_of_sauron", 1, ModItems.EYE_OF_SAURON_3,
                    ModKnowledgeAttachments.EYE_OF_SAURON_3_UNLOCKED, 32),
            tier("eye_of_sauron_4", "eye_of_sauron", 2, ModItems.EYE_OF_SAURON_4,
                    ModKnowledgeAttachments.EYE_OF_SAURON_4_UNLOCKED, 48),
            tier("eye_of_sauron_5", "eye_of_sauron", 3, ModItems.EYE_OF_SAURON_5,
                    ModKnowledgeAttachments.EYE_OF_SAURON_5_UNLOCKED, 64)
    );
    public static final List<KnowledgeTier> AQUATIC_AMULET_TREE = List.of(
            tier("aquatic_amulet_2", "aquatic_amulet", 0, ModItems.AQUATIC_AMULET_2,
                    ModKnowledgeAttachments.AQUATIC_AMULET_2_UNLOCKED, 16),
            tier("aquatic_amulet_3", "aquatic_amulet", 1, ModItems.AQUATIC_AMULET_3,
                    ModKnowledgeAttachments.AQUATIC_AMULET_3_UNLOCKED, 32),
            tier("aquatic_amulet_4", "aquatic_amulet", 2, ModItems.AQUATIC_AMULET_4,
                    ModKnowledgeAttachments.AQUATIC_AMULET_4_UNLOCKED, 48),
            tier("aquatic_amulet_5", "aquatic_amulet", 3, ModItems.AQUATIC_AMULET_5,
                    ModKnowledgeAttachments.AQUATIC_AMULET_5_UNLOCKED, 64)
    );

    public static final List<KnowledgeTier> INFERNAL_STAFF_TREE = List.of(
            tier("infernal_staff_2", "infernal_staff", 0, ModItems.INFERNAL_STAFF_2,
                    ModKnowledgeAttachments.INFERNAL_STAFF_2_UNLOCKED, 16),
            tier("infernal_staff_3", "infernal_staff", 1, ModItems.INFERNAL_STAFF_3,
                    ModKnowledgeAttachments.INFERNAL_STAFF_3_UNLOCKED, 32),
            tier("infernal_staff_4", "infernal_staff", 2, ModItems.INFERNAL_STAFF_4,
                    ModKnowledgeAttachments.INFERNAL_STAFF_4_UNLOCKED, 48),
            tier("infernal_staff_5", "infernal_staff", 3, ModItems.INFERNAL_STAFF_5,
                    ModKnowledgeAttachments.INFERNAL_STAFF_5_UNLOCKED, 64)
    );

    public static final List<List<KnowledgeTier>> TREES = List.of(KNOCKBACK_RING_TREE, SLOW_KILLER_TREE, EYE_OF_SAURON_TREE, AQUATIC_AMULET_TREE, INFERNAL_STAFF_TREE);

    public static Optional<KnowledgeTier> byId(String id) {
        return TREES.stream().flatMap(List::stream).filter(t -> t.id().equals(id)).findFirst();
    }
}
