package net.gera.veridia.common.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

public enum SummonsRunesCategory {
    SUMMON_EGGS("gui.veridia.tab.summon_eggs"),
    RUNES("gui.veridia.tab.runes");
    public static SummonsRunesCategory active = SUMMON_EGGS;

    public final String titleKey;

    SummonsRunesCategory(String titleKey) {
        this.titleKey = titleKey;
    }

    public void emit(Consumer<ItemLike> out) {
        switch (this) {
            case SUMMON_EGGS -> {
                out.accept(ModItems.ZOMBIE_EGG_WEAK.get());
                out.accept(ModItems.ZOMBIE_EGG_DANGEROUS.get());
                out.accept(ModItems.ZOMBIE_EGG_30.get());
                out.accept(ModItems.ZOMBIE_EGG_31.get());
                out.accept(ModItems.ZOMBIE_EGG_32.get());
                out.accept(ModItems.ZOMBIE_EGG_33.get());
                out.accept(ModItems.ZOMBIE_EGG_34.get());
                out.accept(ModItems.ZOMBIE_EGG_35.get());
                out.accept(ModItems.HUSK_EGG_WEAK.get());
                out.accept(ModItems.HUSK_EGG_DANGEROUS.get());
                out.accept(ModItems.HUSK_EGG_30.get());
                out.accept(ModItems.HUSK_EGG_31.get());
                out.accept(ModItems.HUSK_EGG_32.get());
                out.accept(ModItems.HUSK_EGG_33.get());
                out.accept(ModItems.HUSK_EGG_34.get());
                out.accept(ModItems.HUSK_EGG_35.get());
                out.accept(ModItems.MUTANT_SPAWN_EGG.get());
                out.accept(ModItems.OMINOUS_WARRIOR_SPAWN_EGG.get());
                out.accept(ModItems.BRUTAL_OMINOUS_WARRIOR_SPAWN_EGG.get());
                out.accept(ModItems.CINDER_ROAMER_SPAWN_EGG.get());
            }
            case RUNES -> ModItems.MANA_RUNES.forEach(rune -> out.accept(rune.get()));
        }
    }

    public ItemLike icon() {
        return switch (this) {
            case SUMMON_EGGS -> ModItems.MUTANT_SPAWN_EGG.get();
            case RUNES -> ModItems.MANA_RUNES.isEmpty() ? Items.AMETHYST_SHARD : ModItems.MANA_RUNES.getFirst().get();
        };
    }
}
